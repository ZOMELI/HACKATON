package pe.bcp.peoplecapacity.agents.domain.model;

import java.time.LocalDate;
import java.util.List;

/** Evidencia estructurada extraída de un correo. */
public record EvidenceExtraction(
        String emailId,
        EvidenceType type,
        List<String> personIds,
        String movementId,
        String contractId,
        LocalDate effectiveFrom,
        LocalDate effectiveUntil,
        String approver,
        String quote,
        boolean aiGenerated,
        Mode mode) {

    public EvidenceExtraction {
        personIds = personIds == null ? List.of() : List.copyOf(personIds);
    }
}
