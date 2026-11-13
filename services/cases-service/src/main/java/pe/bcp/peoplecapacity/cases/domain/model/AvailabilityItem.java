package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.Instant;
import java.time.LocalDate;

public record AvailabilityItem(String personId, String personName, String personRole, LocalDate availableFrom,
                               String status, String source, Instant observedAt,
                               String confirmerId, String confirmerName, boolean alert, String caseId) {
}
