package com.peoplecapacity.ledger.application.port.out;

import com.peoplecapacity.ledger.domain.model.Claim;
import java.util.List;

/** Puerto de salida: almacenamiento de afirmaciones. Solo agrega. */
public interface ClaimRepositoryPort {

    Claim append(Claim claim);

    /** Filtros opcionales; devuelve de la más reciente a la más antigua. */
    List<Claim> search(String personId, String dimension, String status);
}
