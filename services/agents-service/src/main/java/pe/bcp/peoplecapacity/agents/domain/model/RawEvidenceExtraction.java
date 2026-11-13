package pe.bcp.peoplecapacity.agents.domain.model;

import java.util.List;

/** Salida del modelo (o registro grabado) antes de validar: todo llega como texto. */
public record RawEvidenceExtraction(
        String type,
        List<String> personIds,
        String movementId,
        String contractId,
        String effectiveFrom,
        String effectiveUntil,
        String approver,
        String quote) {
}
