package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.Instant;

/** Días de anticipación de las alertas de vencimiento. */
public record AlertSettings(int contractExpiryDays, int availabilityDays, Instant updatedAt, String updatedBy) {

    public static AlertSettings defaults() {
        return new AlertSettings(30, 15, null, null);
    }

    public boolean valid() {
        return inRange(contractExpiryDays) && inRange(availabilityDays);
    }

    private static boolean inRange(int days) {
        return days >= 1 && days <= 365;
    }
}
