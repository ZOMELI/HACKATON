package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.LocalDate;
import java.util.List;

/** Resultado de una regla: un caso candidato. dedupeKey evita reabrir lo ya conocido o ya decidido. */
public record DetectedCase(CaseType type, String dedupeKey, String title, String personId, String personName,
                           String ownerId, String ownerName, LocalDate dueDate,
                           List<Claim> claims, List<Evidence> evidence, List<CaseOption> options) {
}
