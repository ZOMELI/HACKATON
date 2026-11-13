package pe.bcp.peoplecapacity.cases.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.Decision;
import pe.bcp.peoplecapacity.cases.domain.model.Evidence;
import pe.bcp.peoplecapacity.cases.domain.model.Summary;

/** Forma del caso en el contrato (contracts/cases.yaml). No expone la clave interna ni los efectos. */
public record CaseResponse(String id, String type, String status, String title, String personId, String personName,
                           String ownerId, String ownerName, LocalDate dueDate, Instant openedAt,
                           List<ClaimSnapshot> claims, List<Evidence> evidence, Summary summary,
                           List<OptionView> options, Decision decision) {

    public record ClaimSnapshot(String claimId, String subject, String value, Map<String, String> attributes,
                                String source, String sourceRef, Instant observedAt,
                                LocalDate validFrom, LocalDate validUntil) {
        static ClaimSnapshot of(Claim c) {
            return new ClaimSnapshot(c.id(), c.subject(), c.value(), c.attributes(), c.source(), c.sourceRef(),
                    c.observedAt(), c.validFrom(), c.validUntil());
        }
    }

    public record OptionView(String id, String label, String text) {
    }

    public static CaseResponse of(CaseFile c) {
        return new CaseResponse(c.id(), c.type().name(), c.status().name(), c.title(), c.personId(), c.personName(),
                c.ownerId(), c.ownerName(), c.dueDate(), c.openedAt(),
                c.claims().stream().map(ClaimSnapshot::of).toList(), c.evidence(), c.summary(),
                c.options().stream().map(o -> new OptionView(o.id(), o.label(), o.text())).toList(), c.decision());
    }
}
