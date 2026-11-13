package pe.bcp.peoplecapacity.demand.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Necesidad de personal. Invariante: toda necesidad tiene perfil, fuente y fecha de consulta.
 * {@code recruitment} es null cuando no hay seguimiento en Reclutamiento.
 */
public record Need(
        String id,
        String externalId,
        boolean informal,
        String tribe,
        String project,
        String role,
        String seniority,
        Mechanism mechanism,
        LocalDate neededFrom,
        NeedStatus status,
        String source,
        Instant observedAt,
        RecruitmentStatus recruitment,
        String note,
        String evidenceQuote,
        String createdBy) {

    public Need {
        if (role == null || role.isBlank()) {
            throw new InvalidNeedException("La necesidad debe indicar el perfil requerido (role)");
        }
        if (source == null || source.isBlank()) {
            throw new InvalidNeedException("La necesidad debe indicar su fuente (source)");
        }
        if (observedAt == null) {
            throw new InvalidNeedException("La necesidad debe indicar la fecha de consulta (observedAt)");
        }
        mechanism = mechanism == null ? Mechanism.UNDEFINED : mechanism;
        status = status == null ? NeedStatus.OPEN : status;
    }
}
