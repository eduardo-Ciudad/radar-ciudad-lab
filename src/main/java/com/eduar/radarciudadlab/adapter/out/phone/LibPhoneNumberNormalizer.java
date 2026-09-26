package com.eduar.radarciudadlab.adapter.out.phone;

import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;
import com.eduar.radarciudadlab.domain.port.out.PhoneNormalizer;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class LibPhoneNumberNormalizer implements PhoneNormalizer {

    private static final String DEFAULT_REGION = "BR";

    private final PhoneNumberUtil util = PhoneNumberUtil.getInstance();

    @Override
    public Optional<PhoneNumber> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            Phonenumber.PhoneNumber parsed = util.parse(raw, DEFAULT_REGION);
            if (!util.isValidNumber(parsed)) {
                return Optional.empty();
            }
            PhoneNumberType type = util.getNumberType(parsed);
            boolean mobile = type == PhoneNumberType.MOBILE || type == PhoneNumberType.FIXED_LINE_OR_MOBILE;
            return Optional.of(new PhoneNumber(util.format(parsed, PhoneNumberFormat.E164), mobile));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }
}
