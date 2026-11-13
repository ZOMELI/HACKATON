package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Afirmación del libro (contracts/ledger.yaml). subject y source son texto para tolerar
 * valores nuevos del libro sin romper la lectura.
 */
public record Claim(String id, String personId, String personName, String personRole,
                    String subject, String value, Map<String, String> attributes,
                    String source, String sourceRef, Instant observedAt, Instant recordedAt,
                    LocalDate validFrom, LocalDate validUntil, String supersedes,
                    List<Evidence> evidence, String recordedBy) {

    public Claim {
        attributes = attributes == null ? Map.of() : attributes;
        evidence = evidence == null ? List.of() : evidence;
    }

    public String attr(String key) {
        return attributes.get(key);
    }

    public boolean is(String subjectName) {
        return subjectName.equals(subject);
    }

    public boolean hasEvidenceOf(String... types) {
        List<String> wanted = Arrays.asList(types);
        return evidence.stream().anyMatch(e -> wanted.contains(e.type()));
    }
}
