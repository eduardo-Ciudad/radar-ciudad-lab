package com.eduar.radarciudadlab.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        Objects.requireNonNull(from, "from é obrigatório");
        Objects.requireNonNull(to, "to é obrigatório");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to (" + to + ") não pode ser antes de from (" + from + ")");
        }
    }

    public long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    public DateRange previous() {
        return new DateRange(from.minusDays(days()), from.minusDays(1));
    }
}
