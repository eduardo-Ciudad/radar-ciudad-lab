package com.eduar.radarciudadlab.adapter.out.google;

import com.eduar.radarciudadlab.domain.model.BreakdownDimension;
import com.eduar.radarciudadlab.domain.model.DateRange;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Teste manual contra o GA real. Rode pela IDE; o `mvn test` ignora classes *IT. */
class GoogleAnalyticsProviderIT {

    private static final String KEY = System.getenv().getOrDefault(
            "GA_CREDENTIALS_PATH", "secret/radar-ciudadlab-cbad39e44a50.json");
    private static final String PROPERTY_ID = System.getenv().getOrDefault(
            "GA_TEST_PROPERTY_ID", "554830905");

    @Test
    void buscaUltimos7DiasDaPropriedadeReal() throws Exception {
        assumeTrue(Files.exists(Path.of(KEY)), "Chave do GA ausente: teste pulado");

        var properties = new GoogleAnalyticsProperties(KEY, 10_000);
        try (var client = new GoogleAnalyticsConfig().analyticsDataClient(properties)) {
            var provider = new GoogleAnalyticsProvider(client, properties);
            var range = new DateRange(LocalDate.now().minusDays(7), LocalDate.now().minusDays(1));

            var daily = provider.fetchDailyMetrics(PROPERTY_ID, range);
            daily.rows().forEach(d -> System.out.println("DIA     " + d));

            var channels = provider.fetchDailyBreakdown(PROPERTY_ID, range, BreakdownDimension.CHANNEL);
            channels.rows().forEach(b -> System.out.println("CANAL   " + b));

            var events = provider.fetchDailyBreakdown(PROPERTY_ID, range, BreakdownDimension.EVENT);
            events.rows().forEach(b -> System.out.println("EVENTO  " + b));

            var users = provider.fetchPeriodUsers(PROPERTY_ID, range);
            System.out.println("USUÁRIOS " + users.rows().get(0));
            System.out.println("TOKENS   " + daily.quota().plus(channels.quota()).plus(events.quota()).plus(users.quota()));

            assertFalse(daily.rows().isEmpty(), "Esperava ao menos um dia com dados");
        }
    }
}
