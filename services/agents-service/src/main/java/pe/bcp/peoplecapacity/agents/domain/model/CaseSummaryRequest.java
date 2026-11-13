package pe.bcp.peoplecapacity.agents.domain.model;

import java.util.List;
import java.util.Map;

/** Caso a resumir: afirmaciones en conflicto, evidencia y opciones que las reglas ya definieron. */
public record CaseSummaryRequest(
        String caseId,
        String type,
        String title,
        String personId,
        String personName,
        String dueDate,
        List<Claim> claims,
        List<EvidenceItem> evidence,
        List<Option> options) {

    public CaseSummaryRequest {
        claims = claims == null ? List.of() : List.copyOf(claims);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        options = options == null ? null : List.copyOf(options);
    }

    public record Claim(
            String subject,
            String value,
            Map<String, String> attributes,
            String source,
            String observedAt,
            String validFrom,
            String validUntil) {

        public Claim {
            attributes = attributes == null ? Map.of() : attributes;
        }

        public String attribute(String name) {
            String value = attributes.get(name);
            return value == null || value.isBlank() ? null : value;
        }
    }

    public record EvidenceItem(String type, String quote, String approver) {
    }

    public record Option(String id, String label) {
    }
}
