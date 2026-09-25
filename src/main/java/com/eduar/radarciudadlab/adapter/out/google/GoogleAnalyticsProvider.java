package com.eduar.radarciudadlab.adapter.out.google;

import com.eduar.radarciudadlab.domain.exception.AnalyticsAccessDeniedException;
import com.eduar.radarciudadlab.domain.exception.AnalyticsQuotaExceededException;
import com.eduar.radarciudadlab.domain.model.*;
import com.eduar.radarciudadlab.domain.port.out.AnalyticsProvider;
import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.DateRange.Builder;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.NotFoundException;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.api.gax.rpc.ResourceExhaustedException;
import com.google.api.gax.rpc.UnauthenticatedException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoogleAnalyticsProvider implements AnalyticsProvider {

    /** Único lugar do sistema que conhece os nomes de dimensão do GA4. */
    private static final Map<BreakdownDimension, String> GA_DIMENSION = Map.of(
            BreakdownDimension.CHANNEL, "sessionDefaultChannelGroup",
            BreakdownDimension.SOURCE, "sessionSource",
            BreakdownDimension.LANDING_PAGE, "landingPage",
            BreakdownDimension.PAGE, "pagePath",
            BreakdownDimension.DEVICE, "deviceCategory",
            BreakdownDimension.CITY, "city",
            BreakdownDimension.EVENT, "eventName"
    );

    private static final List<String> DAILY_METRICS = List.of(
            "sessions", "engagedSessions", "newUsers", "activeUsers",
            "screenPageViews", "eventCount", "keyEvents", "userEngagementDuration");

    private static final List<String> BREAKDOWN_METRICS = List.of(
            "sessions", "engagedSessions", "screenPageViews", "eventCount", "keyEvents");

    private static final List<String> EVENT_METRICS = List.of("eventCount", "keyEvents");

    private static final List<String> PERIOD_METRICS = List.of("totalUsers", "activeUsers", "newUsers");

    private static final DateTimeFormatter GA_DATE = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd
    private static final int MAX_VALUE_LENGTH = 500; // tamanho da coluna ga_daily_breakdown.value

    private final BetaAnalyticsDataClient client;
    private final GoogleAnalyticsProperties properties;

    public GoogleAnalyticsProvider(BetaAnalyticsDataClient client, GoogleAnalyticsProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public FetchResult<DailyMetrics> fetchDailyMetrics(String propertyId, DateRange range) {
        GaReport report = run(propertyId, range, List.of("date"), DAILY_METRICS);

        List<DailyMetrics> rows = report.rows().stream()
                .map(r -> new DailyMetrics(
                        parseDate(r.dimension(0)),
                        r.intMetric("sessions"),
                        r.intMetric("engagedSessions"),
                        r.intMetric("newUsers"),
                        r.intMetric("activeUsers"),
                        r.intMetric("screenPageViews"),
                        r.intMetric("eventCount"),
                        r.intMetric("keyEvents"),
                        r.longMetric("userEngagementDuration")))
                .toList();

        return new FetchResult<>(rows, report.quota());
    }

    @Override
    public FetchResult<DailyBreakdown> fetchDailyBreakdown(String propertyId, DateRange range,
                                                           BreakdownDimension dimension) {
        List<String> metrics = dimension == BreakdownDimension.EVENT ? EVENT_METRICS : BREAKDOWN_METRICS;
        GaReport report = run(propertyId, range, List.of("date", GA_DIMENSION.get(dimension)), metrics);

        List<DailyBreakdown> rows = report.rows().stream()
                .map(r -> new DailyBreakdown(
                        parseDate(r.dimension(0)),
                        dimension,
                        truncate(r.dimension(1)),
                        r.intMetric("sessions"),
                        r.intMetric("engagedSessions"),
                        r.intMetric("screenPageViews"),
                        r.intMetric("eventCount"),
                        r.intMetric("keyEvents")))
                .toList();

        return new FetchResult<>(rows, report.quota());
    }

    @Override
    public FetchResult<PeriodUsers> fetchPeriodUsers(String propertyId, DateRange range) {
        GaReport report = run(propertyId, range, List.of(), PERIOD_METRICS);

        PeriodUsers users = report.rows().isEmpty()
                ? new PeriodUsers(range, 0, 0, 0)
                : new PeriodUsers(range,
                report.rows().get(0).intMetric("totalUsers"),
                report.rows().get(0).intMetric("activeUsers"),
                report.rows().get(0).intMetric("newUsers"));

        return new FetchResult<>(List.of(users), report.quota());
    }


    private GaReport run(String propertyId, DateRange range, List<String> dimensions, List<String> metrics) {
        RunReportRequest.Builder request = RunReportRequest.newBuilder()
                .setProperty("properties/" + propertyId)
                .addDateRanges(dateRange(range))
                .setLimit(properties.rowLimit())
                .setKeepEmptyRows(false)
                .setReturnPropertyQuota(true);
        dimensions.forEach(d -> request.addDimensions(Dimension.newBuilder().setName(d)));
        metrics.forEach(m -> request.addMetrics(Metric.newBuilder().setName(m)));

        try {
            List<GaRow> rows = new ArrayList<>();
            QuotaUsage quota = QuotaUsage.NONE;
            long offset = 0;
            int totalRows;

            do {
                RunReportResponse response = client.runReport(request.setOffset(offset).build());
                for (Row row : response.getRowsList()) {
                    rows.add(GaRow.from(row, metrics));
                }
                quota = quota.plus(new QuotaUsage(
                        response.getPropertyQuota().getTokensPerDay().getConsumed(),
                        response.getPropertyQuota().getTokensPerDay().getRemaining()));
                offset += response.getRowsCount();
                totalRows = response.getRowCount();
                if (response.getRowsCount() == 0) break;
            } while (offset < totalRows);

            return new GaReport(rows, quota);

        } catch (PermissionDeniedException | UnauthenticatedException | NotFoundException e) {
            throw new AnalyticsAccessDeniedException(propertyId, e);
        } catch (ResourceExhaustedException e) {
            throw new AnalyticsQuotaExceededException(propertyId, e);
        } catch (InvalidArgumentException e) {
            throw new IllegalArgumentException(
                    "Requisição inválida para a propriedade " + propertyId + ": " + e.getMessage(), e);
        }
    }

    private static Builder dateRange(DateRange range) {
        return com.google.analytics.data.v1beta.DateRange.newBuilder()
                .setStartDate(range.from().toString())
                .setEndDate(range.to().toString());
    }

    private static LocalDate parseDate(String yyyymmdd) {
        return LocalDate.parse(yyyymmdd, GA_DATE);
    }

    private static String truncate(String value) {
        return value.length() <= MAX_VALUE_LENGTH ? value : value.substring(0, MAX_VALUE_LENGTH);
    }


    private record GaReport(List<GaRow> rows, QuotaUsage quota) {}

    private record GaRow(List<String> dimensions, Map<String, Double> metrics) {

        static GaRow from(Row row, List<String> metricNames) {
            List<String> dims = row.getDimensionValuesList().stream().map(v -> v.getValue()).toList();
            Map<String, Double> values = new HashMap<>();
            for (int i = 0; i < metricNames.size(); i++) {
                values.put(metricNames.get(i), Double.parseDouble(row.getMetricValues(i).getValue()));
            }
            return new GaRow(dims, values);
        }

        String dimension(int index) {
            return dimensions.get(index);
        }

        int intMetric(String name) {
            return (int) Math.round(metrics.getOrDefault(name, 0.0));
        }

        long longMetric(String name) {
            return Math.round(metrics.getOrDefault(name, 0.0));
        }
    }
}