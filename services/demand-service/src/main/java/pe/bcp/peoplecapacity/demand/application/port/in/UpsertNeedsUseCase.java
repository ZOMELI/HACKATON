package pe.bcp.peoplecapacity.demand.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import pe.bcp.peoplecapacity.demand.domain.Mechanism;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStage;

/** Ingestion entrega necesidades y seguimiento de Reclutamiento (upsert por externalId). */
public interface UpsertNeedsUseCase {

    /** Todo o nada: si una necesidad es inválida no se guarda ninguna. */
    UpsertResult upsert(List<NeedCommand> needs);

    record NeedCommand(
            String externalId,
            String tribe,
            String project,
            String role,
            String seniority,
            Mechanism mechanism,
            LocalDate neededFrom,
            NeedStatus status,
            String source,
            Instant observedAt,
            RecruitmentCommand recruitment,
            String evidenceQuote,
            String note) {
    }

    record RecruitmentCommand(RecruitmentStage status, String source, Instant observedAt) {
    }

    record UpsertResult(int created, int updated) {
    }
}
