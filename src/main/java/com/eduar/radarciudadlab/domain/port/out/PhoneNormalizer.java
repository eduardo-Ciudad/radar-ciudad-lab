package com.eduar.radarciudadlab.domain.port.out;

import com.eduar.radarciudadlab.domain.model.lead.PhoneNumber;

import java.util.Optional;

public interface PhoneNormalizer {

    Optional<PhoneNumber> normalize(String raw);
}
