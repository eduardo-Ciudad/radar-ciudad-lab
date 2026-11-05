package com.eduar.radarciudadlab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORS na cadeia de segurança real, com a configuração de dev (só http://localhost:3000). */
@SpringBootTest(properties = {
        "radar.security.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1kby1yYWRhci1jaXVkYWRsYWItY29tLW1haXMtZGUtMzItYnl0ZXM=",
        "radar.analytics.sync.enabled=false"
})
@AutoConfigureMockMvc
class CorsIT {

    @Autowired MockMvc mvc;

    @Test
    void preflightDaOrigemDeDevEAceito() throws Exception {
        mvc.perform(options("/api/sites")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void preflightDeOutraOrigemERecusado() throws Exception {
        mvc.perform(options("/api/sites")
                        .header("Origin", "https://site-malicioso.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
