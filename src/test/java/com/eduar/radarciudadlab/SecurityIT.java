package com.eduar.radarciudadlab;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(status().isUnauthorized());
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