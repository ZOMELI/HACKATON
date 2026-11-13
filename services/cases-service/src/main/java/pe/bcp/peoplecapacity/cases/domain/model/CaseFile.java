package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Caso abierto por una regla. Las afirmaciones son una copia del libro al momento de abrirlo. */
public record CaseFile(String id, String dedupeKey, CaseType type, CaseStatus status, String title,
                       String personId, String personName, String ownerId, String ownerName,
                       LocalDate dueDate, Instant openedAt, List<Claim> claims, List<Evidence> evidence,
                       Summary summary, List<CaseOption> options, Decision decision) {

    public static CaseFile open(String id, DetectedCase d, Instant now) {
        return new CaseFile(id, d.dedupeKey(), d.type(), CaseStatus.OPEN, d.title(), d.personId(), d.personName(),
                d.ownerId(), d.ownerName(), d.dueDate(), now, d.claims(), d.evidence(), null, d.options(), null);
    }

    public CaseFile withSummary(Summary newSummary, List<CaseOption> newOptions) {
        return new CaseFile(id, dedupeKey, type, status, title, personId, personName, ownerId, ownerName,
                dueDate, openedAt, claims, evidence, newSummary, newOptions, decision);
    }

    public CaseFile resolve(Decision newDecision) {
        return new CaseFile(id, dedupeKey, type, CaseStatus.RESOLVED, title, personId, personName, ownerId, ownerName,
                dueDate, openedAt, claims, evidence, summary, options, newDecision);
    }

    public CaseOption option(String optionId) {
        return options.stream().filter(o -> o.id().equals(optionId)).findFirst().orElse(null);
    }
}
