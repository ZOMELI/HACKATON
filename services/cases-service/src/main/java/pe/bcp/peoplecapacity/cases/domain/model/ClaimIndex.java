package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Afirmaciones vigentes agrupadas por persona, con la jerarquía de gestión que se deduce de ellas. */
public final class ClaimIndex {

    private final Map<String, List<Claim>> byPerson = new LinkedHashMap<>();
    private final int size;

    public ClaimIndex(List<Claim> claims) {
        for (Claim c : claims) {
            if (c.personId() != null && c.subject() != null) {
                byPerson.computeIfAbsent(c.personId(), k -> new ArrayList<>()).add(c);
            }
        }
        this.size = claims.size();
    }

    public int size() {
        return size;
    }

    public Set<String> personIds() {
        return byPerson.keySet();
    }

    public List<Claim> of(String personId, String subject) {
        return byPerson.getOrDefault(personId, List.of()).stream().filter(c -> c.is(subject)).toList();
    }

    /** La afirmación más reciente de ese subject y fuente, o null. */
    public Claim latest(String personId, String subject, String source) {
        return of(personId, subject).stream()
                .filter(c -> source == null || source.equals(c.source()))
                .max(Comparator.comparing(ClaimIndex::recency))
                .orElse(null);
    }

    public List<Claim> all(String subject) {
        return byPerson.values().stream().flatMap(List::stream).filter(c -> c.is(subject)).toList();
    }

    /** Responsable de gestión: primero una decisión registrada, luego SIGA, luego cualquier otra fuente. */
    public String managerOf(String personId) {
        Claim c = latest(personId, Subjects.MANAGEMENT_RESPONSIBILITY, Subjects.SRC_DECISION);
        if (c == null) {
            c = latest(personId, Subjects.MANAGEMENT_RESPONSIBILITY, Subjects.SRC_SIGA);
        }
        if (c == null) {
            c = latest(personId, Subjects.MANAGEMENT_RESPONSIBILITY, null);
        }
        return c == null ? null : c.value();
    }

    /** Cadena de responsables hacia arriba, sin incluir a la persona. */
    public List<String> chainOf(String personId) {
        List<String> chain = new ArrayList<>();
        String current = managerOf(personId);
        while (current != null && !chain.contains(current) && !current.equals(personId)) {
            chain.add(current);
            current = managerOf(current);
        }
        return chain;
    }

    /** true si userId es la persona responsable indicada o está por encima de ella. */
    public boolean isSameOrAbove(String userId, String responsibleId) {
        return userId != null && responsibleId != null
                && (userId.equals(responsibleId) || chainOf(responsibleId).contains(userId));
    }

    public String nameOf(String personId) {
        if (personId == null) {
            return null;
        }
        for (Claim c : byPerson.getOrDefault(personId, List.of())) {
            if (c.personName() != null) {
                return c.personName();
            }
        }
        for (Claim c : all(Subjects.MANAGEMENT_RESPONSIBILITY)) {
            if (personId.equals(c.value()) && c.attr("managerName") != null) {
                return c.attr("managerName");
            }
        }
        return personId;
    }

    public String roleOf(String personId) {
        return byPerson.getOrDefault(personId, List.of()).stream()
                .map(Claim::personRole).filter(r -> r != null).findFirst().orElse(null);
    }

    private static Instant recency(Claim c) {
        if (c.recordedAt() != null) {
            return c.recordedAt();
        }
        return c.observedAt() != null ? c.observedAt() : Instant.EPOCH;
    }
}
