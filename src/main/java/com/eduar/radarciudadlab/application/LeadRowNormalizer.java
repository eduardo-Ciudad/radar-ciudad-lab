package com.eduar.radarciudadlab.application;

import com.eduar.radarciudadlab.domain.model.lead.AddressParser;
import com.eduar.radarciudadlab.domain.model.lead.ContactNormalizer;
import com.eduar.radarciudadlab.domain.model.lead.LeadDraft;
import com.eduar.radarciudadlab.domain.model.lead.LeadStatus;
import com.eduar.radarciudadlab.domain.model.lead.ParsedAddress;
import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;
import com.eduar.radarciudadlab.domain.model.lead.RawLeadRow;
import com.eduar.radarciudadlab.domain.model.lead.RowIssue;
import com.eduar.radarciudadlab.domain.model.lead.TextNormalizer;
import com.eduar.radarciudadlab.domain.port.out.PhoneNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


@Component
public class LeadRowNormalizer {

    private static final BigDecimal MAX_RATING = BigDecimal.valueOf(5);

    private final PhoneNormalizer phoneNormalizer;

    public LeadRowNormalizer(PhoneNormalizer phoneNormalizer) {
        this.phoneNormalizer = phoneNormalizer;
    }

    public record Result(LeadDraft draft, RowIssue error, List<RowIssue> warnings) {
        public boolean skipped() {
            return draft == null;
        }
    }

    public Result normalize(RawLeadRow row) {
        String name = TextNormalizer.blankToNull(row.name());
        if (name == null) {
            return skip(row, "nome vazio");
        }
        String addressText = TextNormalizer.blankToNull(row.address());
        if (addressText == null) {
            return skip(row, "endereço vazio");
        }

        List<RowIssue> warnings = new ArrayList<>();

        ParsedAddress address = AddressParser.parse(addressText);
        if (!address.isStructured()) {
            warnings.add(new RowIssue(row.line(), "endereço fora do padrão; salvo só o texto original"));
        }

        PhoneNumber phone = null;
        String phoneText = TextNormalizer.blankToNull(row.phone());
        if (phoneText != null) {
            phone = phoneNormalizer.normalize(phoneText).orElse(null);
            if (phone == null) {
                warnings.add(new RowIssue(row.line(), "telefone inválido ignorado: " + phoneText));
            }
        }

        BigDecimal rating = parseRating(row, warnings);

        LeadDraft draft = new LeadDraft(
                row.line(),
                TextNormalizer.limit(name, 255),
                TextNormalizer.limit(TextNormalizer.blankToNull(row.category()), 100),
                phone,
                address,
                TextNormalizer.limit(TextNormalizer.blankToNull(row.website()), 500),
                ContactNormalizer.instagramHandle(row.instagram()),
                rating,
                LeadStatus.fromCsv(row.status()));

        return new Result(draft, null, List.copyOf(warnings));
    }

    private static Result skip(RawLeadRow row, String reason) {
        return new Result(null, new RowIssue(row.line(), reason), List.of());
    }

    /** "5", "4.5" ou "4,5". Vazio = sem nota; fora de 0..5 ou texto = aviso. */
    private static BigDecimal parseRating(RawLeadRow row, List<RowIssue> warnings) {
        String text = TextNormalizer.blankToNull(row.rating());
        if (text == null) {
            return null;
        }
        try {
            BigDecimal rating = new BigDecimal(text.replace(',', '.'));
            if (rating.signum() < 0 || rating.compareTo(MAX_RATING) > 0) {
                warnings.add(new RowIssue(row.line(), "nota fora de 0 a 5 ignorada: " + text));
                return null;
            }
            return rating.setScale(1, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            warnings.add(new RowIssue(row.line(), "nota inválida ignorada: " + text));
            return null;
        }
    }
}
