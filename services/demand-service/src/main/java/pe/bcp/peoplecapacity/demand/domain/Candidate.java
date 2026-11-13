package pe.bcp.peoplecapacity.demand.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Persona disponible propuesta para una necesidad, con puntaje 0–100 y su razón legible. */
public record Candidate(
        String personId,
        String personName,
        String personRole,
        LocalDate availableFrom,
        AvailabilityStatus availabilityStatus,
        String source,
        Instant observedAt,
        int score,
        String reason) {
}
