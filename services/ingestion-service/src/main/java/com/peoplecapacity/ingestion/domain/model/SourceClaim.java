package com.peoplecapacity.ingestion.domain.model;

import java.time.LocalDate;

/** Dato leído de una fuente, listo para enviarse al libro como afirmación. */
public record SourceClaim(String personId, String personName, String personEmail, String dimension, String value,
        String source, LocalDate validFrom, LocalDate validTo, LocalDate observedAt, String status) {
}
