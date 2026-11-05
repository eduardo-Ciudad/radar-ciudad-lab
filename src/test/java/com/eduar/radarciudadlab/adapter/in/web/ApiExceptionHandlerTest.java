package com.eduar.radarciudadlab.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste unitário do ApiExceptionHandler: MockMvc standalone, sem Spring Boot e sem banco.
 * Um controller falso lança as exceções, então o teste cobre a ligação exceção -> handler
 * (o bug do import javax.naming.AuthenticationException passava justamente por aí).
 */
class ApiExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void senhaErradaViraFalhaNoLoginGenerica() throws Exception {
        mvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Falha no login"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"))
                .andExpect(content().string(not(containsString(FailingController.INTERNAL_MESSAGE))));
    }

    @Test
    void usuarioDesativadoRecebeExatamenteAMesmaResposta() throws Exception {
        // Mensagem diferente para conta desativada revelaria que o e-mail existe
        mvc.perform(get("/test/disabled"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.title").value("Falha no login"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"))
                .andExpect(content().string(not(containsString(FailingController.INTERNAL_MESSAGE))));
    }

    /** Aninhado na classe de teste: o TestTypeExcludeFilter impede que ele entre no contexto dos @SpringBootTest. */
    @RestController
    static class FailingController {

        static final String INTERNAL_MESSAGE = "detalhe interno que nao pode vazar";

        @GetMapping("/test/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException(INTERNAL_MESSAGE);
        }

        @GetMapping("/test/disabled")
        void disabled() {
            throw new DisabledException(INTERNAL_MESSAGE);
        }
    }
}
