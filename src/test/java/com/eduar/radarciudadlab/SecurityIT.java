package com.eduar.radarciudadlab;

import com.eduar.radarciudadlab.domain.model.AppUser;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa a cadeia de segurança real (filtros, JWT, papéis) contra o banco local. Tudo é desfeito no final. */
@SpringBootTest(properties = {
        "radar.security.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1kby1yYWRhci1jaXVkYWRsYWItY29tLW1haXMtZGUtMzItYnl0ZXM=",
        "radar.analytics.sync.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class SecurityIT {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void criaUsuarios() {
        users.create("admin@teste.local", passwordEncoder.encode("senha-admin-123"), UserRole.ADMIN);
        users.create("viewer@teste.local", passwordEncoder.encode("senha-viewer-123"), UserRole.VIEWER);
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(get("/api/sites/1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void senhaErradaRecebe401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("admin@teste.local", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.title").value("Falha no login"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void emailInexistenteRecebeMesmaRespostaQueSenhaErrada() throws Exception {
        // Se a resposta fosse diferente, daria para descobrir quais e-mails têm conta
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ninguem@teste.local", "senha-qualquer-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha no login"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void usuarioDesativadoRecebe401Generico() throws Exception {
        users.deactivate(userId("viewer@teste.local"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("viewer@teste.local", "senha-viewer-123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha no login"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    void loginDevolveTokenQueAbreRotaProtegida() throws Exception {
        String token = login("admin@teste.local", "senha-admin-123");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@teste.local"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void viewerNaoPodeSincronizar() throws Exception {
        String token = login("viewer@teste.local", "senha-viewer-123");

        mvc.perform(post("/api/sites/sync").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenComAssinaturaAdulteradaRecebe401() throws Exception {
        String token = login("admin@teste.local", "senha-admin-123");
        String adulterado = token.substring(0, token.lastIndexOf('.') + 1) + "assinatura-falsa";

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cincoFalhasBloqueiamAteASenhaCerta() throws Exception {
        // E-mail aleatório: o limiter é um bean compartilhado entre os testes e não volta com o rollback
        String email = randomEmail();
        users.create(email, passwordEncoder.encode("senha-correta-123"), UserRole.VIEWER);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(email, "senha-errada-" + i)))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "senha-correta-123")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Muitas tentativas"));
    }

    @Test
    void emailInexistenteTambemEBloqueado() throws Exception {
        // Se só contas reais fossem bloqueadas, o 429 revelaria quais e-mails existem
        String email = randomEmail();

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(email, "senha-qualquer-" + i)))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "senha-qualquer-5")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void tokenParaDeFuncionarQuandoUsuarioEDesativado() throws Exception {
        String token = login("viewer@teste.local", "senha-viewer-123");

        users.deactivate(userId("viewer@teste.local"));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevogaOTokenEUmLoginNovoFunciona() throws Exception {
        String token = login("admin@teste.local", "senha-admin-123");

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        String novo = login("admin@teste.local", "senha-admin-123");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + novo))
                .andExpect(status().isOk());
    }

    @Test
    void viewerNaoVeSitesNemDashboards() throws Exception {
        // Sem vínculo usuário -> cliente, VIEWER veria o painel de todos os clientes: bloqueado até existir
        String token = login("viewer@teste.local", "senha-viewer-123");

        mvc.perform(get("/api/sites").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
        mvc.perform(get("/api/sites/1/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/sites/1/insights").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerContinuaVendoASessaoEFazendoLogout() throws Exception {
        String token = login("viewer@teste.local", "senha-viewer-123");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"));
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminContinuaListandoSites() throws Exception {
        String token = login("admin@teste.local", "senha-admin-123");

        mvc.perform(get("/api/sites").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private Long userId(String email) {
        return users.findByEmail(email).map(AppUser::id).orElseThrow();
    }

    private static String randomEmail() {
        return "limite-" + UUID.randomUUID() + "@teste.local";
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}