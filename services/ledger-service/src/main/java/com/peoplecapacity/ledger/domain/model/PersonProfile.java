package com.peoplecapacity.ledger.domain.model;

import java.util.List;

/** Ficha de una persona: una vista por dimensión, sin mezclar dimensiones. */
public record PersonProfile(String personId, String fullName, String email, List<DimensionView> dimensions) {

    /** state: CONSISTENT, DISCREPANCY (las fuentes no coinciden) o PENDING (sin dato). */
    public record DimensionView(String dimension, String state, List<Claim> claims) {
    }
}
