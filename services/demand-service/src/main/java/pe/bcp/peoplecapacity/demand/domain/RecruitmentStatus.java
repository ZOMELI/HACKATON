package pe.bcp.peoplecapacity.demand.domain;

import java.time.Instant;

/** Estado en Reclutamiento, siempre con su fuente y fecha de consulta. */
public record RecruitmentStatus(RecruitmentStage status, String source, Instant observedAt) {

    public RecruitmentStatus {
        if (status == null) {
            throw new InvalidNeedException("El seguimiento de Reclutamiento debe indicar su estado (status)");
        }
        if (source == null || source.isBlank() || observedAt == null) {
            throw new InvalidNeedException(
                    "El seguimiento de Reclutamiento debe tener fuente (source) y fecha de consulta (observedAt)");
        }
    }
}
