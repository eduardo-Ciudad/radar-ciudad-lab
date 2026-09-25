package com.eduar.radarciudadlab.adapter.out;


import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de saída para a Google Analytics Data API (GA4).
 * Autentica com service account: o e-mail dela precisa estar como "Leitor" em cada propriedade.
 */
public class GoogleAnalyticsClient implements AutoCloseable {

    private static final DateTimeFormatter GA_DATE = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd
    private final BetaAnalyticsDataClient client;

    public GoogleAnalyticsClient(String serviceAccountJsonPath) throws IOException {
        GoogleCredentials credentials;
        try (var in = new FileInputStream(serviceAccountJsonPath)) {
            credentials = GoogleCredentials.fromStream(in)
                    .createScoped("https://www.googleapis.com/auth/analytics.readonly");
        }
        var settings = BetaAnalyticsDataSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        this.client = BetaAnalyticsDataClient.create(settings);
    }

    /** Totais por dia -> ga_daily_metrics */
    public Report dailyTotals(String propertyId, LocalDate from, LocalDate to) {
        return run(propertyId, from, to, List.of("date"), List.of(
                "sessions", "engagedSessions", "newUsers", "activeUsers",
                "screenPageViews", "eventCount", "keyEvents", "userEngagementDuration"));
    }

    /** Quebra por dimensão -> ga_daily_breakdown (ex.: "sessionDefaultChannelGroup", "landingPage", "deviceCategory") */
    public Report dailyBreakdown(String propertyId, LocalDate from, LocalDate to, String gaDimension) {
        return run(propertyId, from, to, List.of("date", gaDimension), List.of(
                "sessions", "engagedSessions", "screenPageViews", "eventCount", "keyEvents"));
    }

    /** Usuários únicos do período inteiro (sem dimensão date) -> ga_period_users */
    public Report periodUsers(String propertyId, LocalDate from, LocalDate to) {
        return run(propertyId, from, to, List.of(), List.of("totalUsers", "activeUsers", "newUsers"));
    }

    private Report run(String propertyId, LocalDate from, LocalDate to,
                       List<String> dimensions, List<String> metrics) {
        var request = RunReportRequest.newBuilder()
                .setProperty("properties/" + propertyId)
                .addDateRanges(DateRange.newBuilder()
                        .setStartDate(from.toString())      // aceita yyyy-MM-dd, "7daysAgo", "yesterday"
                        .setEndDate(to.toString()))
                .setLimit(100_000)
                .setKeepEmptyRows(false)
                .setReturnPropertyQuota(true);            // devolve consumo/saldo de tokens
        dimensions.forEach(d -> request.addDimensions(Dimension.newBuilder().setName(d)));
        metrics.forEach(m -> request.addMetrics(Metric.newBuilder().setName(m)));

        RunReportResponse response = client.runReport(request.build());

        List<ReportRow> rows = new ArrayList<>();
        for (Row row : response.getRowsList()) {
            List<String> dims = row.getDimensionValuesList().stream().map(v -> v.getValue()).toList();
            List<Double> vals = row.getMetricValuesList().stream().map(v -> Double.parseDouble(v.getValue())).toList();
            rows.add(new ReportRow(dims, vals));
        }
        var quota = response.getPropertyQuota().getTokensPerDay();
        return new Report(dimensions, metrics, rows, quota.getConsumed(), quota.getRemaining());
    }

    public static LocalDate parseGaDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, GA_DATE);
    }

    @Override
    public void close() {
        client.close();
    }

    public record ReportRow(List<String> dimensions, List<Double> metrics) {}

    public record Report(List<String> dimensionNames, List<String> metricNames, List<ReportRow> rows,
                         int tokensConsumed, int tokensRemainingToday) {}

    /** Teste manual: java ... GoogleAnalyticsClient /caminho/sa.json 123456789 */
    public static void main(String[] args) throws Exception {
        try (var ga = new GoogleAnalyticsClient(args[0])) {
            var report = ga.dailyBreakdown(args[1], LocalDate.now().minusDays(28), LocalDate.now().minusDays(1),
                    "sessionDefaultChannelGroup");
            report.rows().forEach(r -> System.out.println(r.dimensions() + " -> " + r.metrics()));
            System.out.printf("tokens: %d usados, %d restantes hoje%n",
                    report.tokensConsumed(), report.tokensRemainingToday());
        }
    }
}
