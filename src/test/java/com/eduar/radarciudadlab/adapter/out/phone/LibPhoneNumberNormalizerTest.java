package com.eduar.radarciudadlab.adapter.out.phone;

import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibPhoneNumberNormalizerTest {

    private final LibPhoneNumberNormalizer normalizer = new LibPhoneNumberNormalizer();

    @Test
    void celularDoCsv() {
        PhoneNumber phone = normalizer.normalize("+55 17 99721-6373").orElseThrow();

        assertEquals("+5517997216373", phone.e164());
        assertTrue(phone.mobile());
    }

    @Test
    void fixoDoCsv() {
        PhoneNumber phone = normalizer.normalize("+55 17 3225-1791").orElseThrow();

        assertEquals("+551732251791", phone.e164());
        assertFalse(phone.mobile());
    }

    @Test
    void semCodigoDoPaisAssumeBrasil() {
        assertEquals("+5517997216373", normalizer.normalize("(17) 99721-6373").orElseThrow().e164());
    }

    @Test
    void numeroInvalidoVoltaVazio() {
        assertTrue(normalizer.normalize("123").isEmpty());
        assertTrue(normalizer.normalize("").isEmpty());
    }
}
