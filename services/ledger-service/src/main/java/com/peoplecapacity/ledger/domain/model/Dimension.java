package com.peoplecapacity.ledger.domain.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Dimensiones de información y las fuentes autorizadas para afirmar cada una. Jira nunca afirma presupuesto. */
public enum Dimension {
    ORG_MEMBERSHIP("SIGA", "INVENTORY", "PROVIDER_ACT"),
    ROLE("SIGA", "INVENTORY", "PROVIDER_ACT"),
    MANAGEMENT("SIGA", "INVENTORY", "PROVIDER_ACT"),
    FUNDING("PREPER", "PROVIDER_ACT"),
    OPERATIONAL_ASSIGNMENT("JIRA"),
    AVAILABILITY("CASE_DECISION"),
    EVIDENCE("EMAIL", "MOVIPER"),
    DECISION("CASE_DECISION");

    private final Set<String> authorizedSources;

    Dimension(String... sources) {
        this.authorizedSources = Set.of(sources);
    }

    public boolean accepts(String source) {
        return source != null && authorizedSources.contains(source.toUpperCase());
    }

    public static Optional<Dimension> parse(String name) {
        for (Dimension d : values()) {
            if (d.name().equalsIgnoreCase(name)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    /** Las cuatro dimensiones que muestra la ficha. */
    public static List<Dimension> profileDimensions() {
        return List.of(ORG_MEMBERSHIP, MANAGEMENT, FUNDING, OPERATIONAL_ASSIGNMENT);
    }
}
