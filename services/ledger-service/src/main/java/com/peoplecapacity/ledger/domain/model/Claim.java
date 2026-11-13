package com.peoplecapacity.ledger.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Afirmación: lo que una fuente dice sobre una persona en una dimensión, con su fecha. Nunca se modifica. */
public record Claim(Long id, String personId, String personName, String personEmail, String dimension, String value,
        String source, LocalDate validFrom, LocalDate validTo, LocalDate observedAt, String status,
        String evidenceRef, String author, LocalDateTime recordedAt) {

    public Claim recorded(String newStatus, LocalDateTime at) {
        // Dimensión y fuente se guardan en mayúsculas para que las consultas no dependan de cómo llegaron.
        return new Claim(id, personId, personName, personEmail, dimension.toUpperCase(), value, source.toUpperCase(),
                validFrom, validTo, observedAt, newStatus.toUpperCase(), evidenceRef, author, at);
    }
}
