package com.eduar.radarciudadlab.adapter.out.google;
import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(GoogleAnalyticsProperties.class)
public class GoogleAnalyticsConfig {

    private static final String READONLY_SCOPE = "https://www.googleapis.com/auth/analytics.readonly";

    @Bean
    public BetaAnalyticsDataClient analyticsDataClient(GoogleAnalyticsProperties properties) throws IOException {
        Path keyPath = Path.of(properties.credentialsPath());
        if (!Files.isReadable(keyPath)) {
            throw new IllegalStateException(
                    "Chave da service account do GA não encontrada em " + keyPath.toAbsolutePath()
                            + ". Defina a variável GA_CREDENTIALS_PATH.");
        }

        GoogleCredentials credentials;
        try (InputStream in = Files.newInputStream(keyPath)) {
            credentials = GoogleCredentials.fromStream(in).createScoped(READONLY_SCOPE);
        }

        BetaAnalyticsDataSettings settings = BetaAnalyticsDataSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return BetaAnalyticsDataClient.create(settings);
    }
}
