package com.peoplecapacity.ledger.domain.service;

import com.peoplecapacity.ledger.domain.model.Claim;
import com.peoplecapacity.ledger.domain.model.Dimension;
import java.util.Optional;

/** Regla de dominio: una afirmación solo se acepta si su fuente está autorizada para su dimensión. */
public final class SourceAuthorityPolicy {

    public Optional<String> rejectionReason(Claim claim) {
        Optional<Dimension> dimension = Dimension.parse(claim.dimension());
        if (dimension.isEmpty()) {
            return Optional.of("Dimensión desconocida: " + claim.dimension());
        }
        if (!dimension.get().accepts(claim.source())) {
            return Optional.of("La fuente " + claim.source() + " no está autorizada para " + dimension.get().name());
        }
        return Optional.empty();
    }
}
