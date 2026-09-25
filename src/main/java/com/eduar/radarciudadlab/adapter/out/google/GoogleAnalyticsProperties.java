package com.eduar.radarciudadlab.adapter.out.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "radar.analytics.google")
public record GoogleAnalyticsProperties(
        @NotBlank String credentialsPath,
        @DefaultValue("10000") @Positive int rowLimit
) {}
