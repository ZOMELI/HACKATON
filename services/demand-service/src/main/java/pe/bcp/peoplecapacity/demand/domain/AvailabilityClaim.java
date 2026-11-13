package pe.bcp.peoplecapacity.demand.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Afirmación de disponibilidad de una persona, tal como la publica el libro. */
public record AvailabilityClaim(
        String personId,
        String personName,
        String personRole,
        AvailabilityStatus status,
        LocalDate availableFrom,
        LocalDate availableUntil,
        String source,
        Instant observedAt) {

    /** Vigente si no tiene fecha de fin o esta aún no pasó. */
    public boolean isCurrentOn(LocalDate today) {
        return availableUntil == null || !availableUntil.isBefore(today);
    }
}
