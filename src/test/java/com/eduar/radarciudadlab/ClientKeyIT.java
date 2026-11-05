package com.eduar.radarciudadlab;

import com.eduar.radarciudadlab.config.security.ClientKeyFilter;
import com.eduar.radarciudadlab.domain.model.UserRole;
import com.eduar.radarciudadlab.domain.port.out.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Com a chave configurada (como em produção), só quem manda o header chega no login. */
@SpringBootTest(properties = {
        "radar.security.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1kby1yYWRhci1jaXVkYWRsYWItY29tLW1haXMtZGUtMzItYnl0ZXM=",
        "radar.analytics.sync.enabled=false",
        "radar.security.client-key.value=" + ClientKeyIT.KEY,
        "radar.security.client-key.required=true"
})
@AutoConfigureMockMvc
@Transactional
class ClientKeyIT {

    static final String KEY = "chave-do-bff-de-integracao-com-mais-de-32-caracteres";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private String email;

    @BeforeEach
    void criaUsuario() {
        email = "client-key-" + UUID.randomUUID() + "@teste.local";
        users.create(email, passwordEncoder.encode("senha-correta-123"), UserRole.ADMIN);
    }

    @Test
    void loginSemAChaveRecebe401AntesDeConferirASenha() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Cliente não autorizado"));
    }

    @Test
    void loginComChaveErradaRecebe401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .header(ClientKeyFilter.HEADER, "chave-errada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Cliente não autorizado"));
    }

    @Test
    void loginComAChaveFunciona() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .header(ClientKeyFilter.HEADER, KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    private String credentials() {
        return "{\"email\":\"" + email + "\",\"password\":\"senha-correta-123\"}";
    }
}
