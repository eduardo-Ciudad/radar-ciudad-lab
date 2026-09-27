package com.eduar.radarciudadlab.domain.model.lead;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Score de prospecção (0 a 100): quanto o lead parece um bom cliente para um site da CiudadLab.
 *
 * Critério                                  máx.  sem o dado
 * nota no Google (0-5)                        35    15
 * nº de avaliações (porte/movimento)          25    10
 * celular (WhatsApp direto com o dono)        15     0
 * sem site cadastrado (precisa de site)       15     -
 * categoria/nome no nicho-alvo                10     0
 *
 * "Sem o dado" dá pontuação neutra: o CSV atual não traz avaliações e a nota às vezes falta,
 * e isso não pode derrubar o lead para o fim da fila.
 * O ajuste manual (scoreOverride) não passa por aqui: ele substitui o total na leitura.
 */
public final class LeadScorer {

    static final int RATING_MAX = 35;
    static final int RATING_UNKNOWN = 15;
    static final int REVIEWS_MAX = 25;
    static final int REVIEWS_UNKNOWN = 10;
    static final int REVIEWS_FULL_AT = 200;
    static final int MOBILE_POINTS = 15;
    static final int NO_WEBSITE_POINTS = 15;
    static final int WEBSITE_POINTS = 5;
    static final int CATEGORY_POINTS = 10;

    private final Set<String> targetKeywords;

    /** @param targetKeywords palavras do nicho-alvo, comparadas sem acento contra categoria e nome */
    public LeadScorer(Set<String> targetKeywords) {
        this.targetKeywords = targetKeywords.stream()
                .map(TextNormalizer::normalize)
                .filter(k -> !k.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public LeadScore score(Lead lead) {
        List<ScoreItem> items = List.of(
                rating(lead.rating()),
                reviews(lead.reviewsCount()),
                mobile(lead.mobile()),
                website(lead.websiteUrl()),
                category(lead.category(), lead.name()));
        int total = items.stream().mapToInt(ScoreItem::points).sum();
        return new LeadScore(Math.min(100, total), items);
    }

    private static ScoreItem rating(BigDecimal rating) {
        if (rating == null) {
            return new ScoreItem("RATING", "Nota no Google", RATING_UNKNOWN, RATING_MAX, "sem nota no arquivo");
        }
        int points = (int) Math.round(rating.doubleValue() / 5.0 * RATING_MAX);
        return new ScoreItem("RATING", "Nota no Google", points, RATING_MAX, "nota " + rating);
    }

    /** Escala logarítmica: 10 avaliações já contam bastante, 200 ou mais dão o máximo. */
    private static ScoreItem reviews(Integer count) {
        if (count == null) {
            return new ScoreItem("REVIEWS", "Avaliações", REVIEWS_UNKNOWN, REVIEWS_MAX,
                    "número de avaliações não informado");
        }
        double ratio = Math.log10(1 + Math.max(0, count)) / Math.log10(1 + REVIEWS_FULL_AT);
        int points = (int) Math.round(Math.min(1.0, ratio) * REVIEWS_MAX);
        return new ScoreItem("REVIEWS", "Avaliações", points, REVIEWS_MAX, count + " avaliações");
    }

    private static ScoreItem mobile(Boolean mobile) {
        boolean isMobile = Boolean.TRUE.equals(mobile);
        return new ScoreItem("MOBILE", "WhatsApp provável", isMobile ? MOBILE_POINTS : 0, MOBILE_POINTS,
                isMobile ? "telefone é celular" : mobile == null ? "sem telefone" : "telefone fixo");
    }

    /** Sem site cadastrado = oportunidade maior. Com site ainda vale algo (redesign). */
    private static ScoreItem website(String websiteUrl) {
        return websiteUrl == null
                ? new ScoreItem("WEBSITE", "Sem site", NO_WEBSITE_POINTS, NO_WEBSITE_POINTS, "nenhum site cadastrado")
                : new ScoreItem("WEBSITE", "Sem site", WEBSITE_POINTS, NO_WEBSITE_POINTS, "já tem site (possível redesign)");
    }

    private ScoreItem category(String category, String name) {
        String haystack = TextNormalizer.normalize(category) + " " + TextNormalizer.normalize(name);
        String match = targetKeywords.stream().filter(haystack::contains).sorted().findFirst().orElse(null);
        return match != null
                ? new ScoreItem("CATEGORY", "Nicho-alvo", CATEGORY_POINTS, CATEGORY_POINTS, "combina com \"" + match + "\"")
                : new ScoreItem("CATEGORY", "Nicho-alvo", 0, CATEGORY_POINTS, "fora do nicho-alvo");
    }
}
