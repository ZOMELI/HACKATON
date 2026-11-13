package pe.bcp.peoplecapacity.agents.domain.model;

import java.util.Optional;

/** Tipos de evidencia que el agente puede reconocer en un correo. */
public enum EvidenceType {
    MOVEMENT_APPROVAL,
    SWAP_CONFORMITY,
    CONTRACT_RENEWAL,
    CONTRACT_REPLACEMENT,
    TEMP_SUPPORT,
    INFORMAL_NEED,
    IRRELEVANT;

    public static Optional<EvidenceType> fromText(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(text.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
