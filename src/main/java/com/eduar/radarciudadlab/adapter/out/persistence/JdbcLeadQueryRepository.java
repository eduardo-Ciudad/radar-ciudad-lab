package com.eduar.radarciudadlab.adapter.out.persistence;

import com.eduar.radarciudadlab.domain.model.lead.BoardColumn;
import com.eduar.radarciudadlab.domain.model.lead.FilterOption;
import com.eduar.radarciudadlab.domain.model.lead.LeadBoard;
import com.eduar.radarciudadlab.domain.model.lead.LeadCard;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilter;
import com.eduar.radarciudadlab.domain.model.lead.LeadFilterOptions;
import com.eduar.radarciudadlab.domain.model.lead.LeadPage;
import com.eduar.radarciudadlab.domain.model.lead.LeadSort;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.RatingPoint;
import com.eduar.radarciudadlab.domain.model.lead.RelatedLead;
import com.eduar.radarciudadlab.domain.model.lead.StatusChange;
import com.eduar.radarciudadlab.domain.model.lead.TextNormalizer;
import com.eduar.radarciudadlab.domain.port.out.LeadQueryRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Leituras do funil em SQL direto: filtros dinâmicos, contagem por status e "top N por coluna"
 * (row_number) ficam mais simples e rápidos aqui do que em JPQL/Criteria.
 * A escrita continua no JpaLeadRepository.
 */
@Repository
public class JdbcLeadQueryRepository implements LeadQueryRepository {

    static final List<LeadStatus> FUNNEL = List.of(
            LeadStatus.NOVO, LeadStatus.CONTATADO, LeadStatus.REUNIAO, LeadStatus.PROPOSTA, LeadStatus.FECHADO);

    private static final String EFFECTIVE_SCORE = "COALESCE(l.score_override, l.score)";

    private static final String CARD_COLUMNS = """
            l.id, l.name, l.category, l.neighborhood, l.city, l.phone_e164, l.is_mobile, l.website_url,
            l.instagram_handle, l.rating, l.reviews_count, l.status,
            COALESCE(l.score_override, l.score) AS effective_score,
            l.score_override IS NOT NULL AS score_overridden, l.updated_at
            """;

    private static final Map<LeadSort, String> ORDER_BY = Map.of(
            LeadSort.SCORE, EFFECTIVE_SCORE + " DESC NULLS LAST, l.rating DESC NULLS LAST, l.id",
            LeadSort.RATING, "l.rating DESC NULLS LAST, " + EFFECTIVE_SCORE + " DESC NULLS LAST, l.id",
            LeadSort.NAME, "lower(l.name), l.id",
            LeadSort.RECENT, "l.updated_at DESC, l.id DESC");

    private static final RowMapper<LeadCard> CARD = (rs, i) -> new LeadCard(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("neighborhood"),
            rs.getString("city"),
            rs.getString("phone_e164"),
            rs.getObject("is_mobile", Boolean.class),
            rs.getString("website_url"),
            rs.getString("instagram_handle"),
            rs.getBigDecimal("rating"),
            rs.getObject("reviews_count", Integer.class),
            LeadStatus.valueOf(rs.getString("status")),
            rs.getObject("effective_score", Integer.class),
            rs.getBoolean("score_overridden"),
            toInstant(rs.getObject("updated_at", OffsetDateTime.class)));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcLeadQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LeadBoard board(LeadFilter filter, int perColumn) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(filter.withStatus(null), params);

        Map<LeadStatus, Long> counts = new EnumMap<>(LeadStatus.class);
        jdbc.query("SELECT l.status, count(*) AS total FROM lead l WHERE " + where + " GROUP BY l.status",
                params, rs -> {
                    counts.put(LeadStatus.valueOf(rs.getString("status")), rs.getLong("total"));
                });

        params.addValue("funnel", FUNNEL.stream().map(Enum::name).toList());
        params.addValue("perColumn", perColumn);
        List<LeadCard> top = jdbc.query("""
                SELECT * FROM (
                    SELECT %s, row_number() OVER (PARTITION BY l.status ORDER BY %s) AS rn
                    FROM lead l
                    WHERE %s AND l.status IN (:funnel)
                ) ranked
                WHERE rn <= :perColumn
                ORDER BY rn
                """.formatted(CARD_COLUMNS, ORDER_BY.get(LeadSort.SCORE), where), params, CARD);

        Map<LeadStatus, List<LeadCard>> byStatus = new EnumMap<>(LeadStatus.class);
        top.forEach(card -> byStatus.computeIfAbsent(card.status(), s -> new ArrayList<>()).add(card));

        List<BoardColumn> columns = FUNNEL.stream()
                .map(status -> new BoardColumn(status, counts.getOrDefault(status, 0L),
                        List.copyOf(byStatus.getOrDefault(status, List.of()))))
                .toList();
        long active = columns.stream().mapToLong(BoardColumn::count).sum();
        long lost = counts.getOrDefault(LeadStatus.PERDIDO, 0L);
        long discarded = counts.getOrDefault(LeadStatus.DESCARTADO, 0L);
        return new LeadBoard(columns, active, lost + discarded, lost, discarded);
    }

    @Override
    public LeadPage page(LeadFilter filter, LeadSort sort, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(filter, params);

        Long total = jdbc.queryForObject("SELECT count(*) FROM lead l WHERE " + where, params, Long.class);
        long totalElements = total == null ? 0 : total;

        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);
        List<LeadCard> items = jdbc.query(
                "SELECT " + CARD_COLUMNS + " FROM lead l WHERE " + where
                        + " ORDER BY " + ORDER_BY.get(sort) + " LIMIT :limit OFFSET :offset",
                params, CARD);

        int totalPages = (int) ((totalElements + size - 1) / size);
        return new LeadPage(items, page, size, totalElements, totalPages);
    }

    @Override
    public LeadFilterOptions filterOptions() {
        return new LeadFilterOptions(options("category"), options("neighborhood"));
    }

    @Override
    public List<RatingPoint> ratingHistory(Long leadId) {
        return jdbc.query("""
                SELECT batch_id, rating, reviews_count, captured_at
                FROM lead_snapshot
                WHERE lead_id = :id
                ORDER BY captured_at, id
                """, new MapSqlParameterSource("id", leadId), (rs, i) -> new RatingPoint(
                rs.getLong("batch_id"),
                rs.getBigDecimal("rating"),
                rs.getObject("reviews_count", Integer.class),
                toInstant(rs.getObject("captured_at", OffsetDateTime.class))));
    }

    @Override
    public List<StatusChange> statusHistory(Long leadId) {
        return jdbc.query("""
                SELECT from_status, to_status, changed_at, note
                FROM lead_status_history
                WHERE lead_id = :id
                ORDER BY changed_at, id
                """, new MapSqlParameterSource("id", leadId), (rs, i) -> {
            String from = rs.getString("from_status");
            return new StatusChange(
                    from == null ? null : LeadStatus.valueOf(from),
                    LeadStatus.valueOf(rs.getString("to_status")),
                    toInstant(rs.getObject("changed_at", OffsetDateTime.class)),
                    rs.getString("note"));
        });
    }

    @Override
    public List<RelatedLead> sameLocation(Long leadId) {
        // A view guarda cada par uma vez só (a.id < b.id): o lead pode estar em qualquer um dos lados
        return jdbc.query("""
                SELECT o.id, o.name, o.status, v.match_type
                FROM lead_shared_location v
                JOIN lead o ON o.id = CASE WHEN v.lead_id = :id THEN v.related_lead_id ELSE v.lead_id END
                WHERE v.lead_id = :id OR v.related_lead_id = :id
                ORDER BY o.name
                """, new MapSqlParameterSource("id", leadId), (rs, i) -> new RelatedLead(
                rs.getLong("id"),
                rs.getString("name"),
                LeadStatus.valueOf(rs.getString("status")),
                rs.getString("match_type")));
    }

    /**
     * Monta o WHERE a partir dos filtros preenchidos. Os valores vão sempre como parâmetro;
     * só nomes de coluna fixos entram no texto do SQL.
     */
    static String where(LeadFilter f, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("TRUE");
        if (f.status() != null) {
            conditions.add("l.status = :status");
            params.addValue("status", f.status().name());
        }
        if (f.category() != null && !f.category().isBlank()) {
            conditions.add("l.category = :category");
            params.addValue("category", f.category().trim());
        }
        if (f.neighborhood() != null && !f.neighborhood().isBlank()) {
            conditions.add("l.neighborhood = :neighborhood");
            params.addValue("neighborhood", f.neighborhood().trim());
        }
        if (Boolean.TRUE.equals(f.withoutWebsite())) {
            conditions.add("l.website_url IS NULL");
        }
        if (Boolean.TRUE.equals(f.mobile())) {
            conditions.add("l.is_mobile = TRUE");
        }
        if (f.minScore() != null) {
            conditions.add(EFFECTIVE_SCORE + " >= :minScore");
            params.addValue("minScore", f.minScore());
        }
        String search = TextNormalizer.normalize(f.search());
        if (!search.isEmpty()) {
            // O começo da dedup_key é o nome já sem acento e em minúsculas: busca "clinica" acha "Clínica"
            String byName = "split_part(l.dedup_key, '|', 1) LIKE :search";
            params.addValue("search", "%" + search + "%");
            String digits = search.replaceAll("\\D", "");
            if (digits.length() >= 4) {
                conditions.add("(" + byName + " OR l.phone_e164 LIKE :phone)");
                params.addValue("phone", "%" + digits + "%");
            } else {
                conditions.add(byName);
            }
        }
        return String.join(" AND ", conditions);
    }

    private List<FilterOption> options(String column) {
        // column vem só das chamadas acima ("category"/"neighborhood"), nunca do usuário
        return jdbc.query("""
                SELECT %1$s AS value, count(*) AS total
                FROM lead
                WHERE %1$s IS NOT NULL AND %1$s <> ''
                GROUP BY %1$s
                ORDER BY total DESC, %1$s
                """.formatted(column), new MapSqlParameterSource(),
                (rs, i) -> new FilterOption(rs.getString("value"), rs.getLong("total")));
    }

    private static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
