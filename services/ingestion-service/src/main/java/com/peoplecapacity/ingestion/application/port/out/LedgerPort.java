package com.peoplecapacity.ingestion.application.port.out;

import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import java.util.List;

/** Puerto de salida hacia el libro. Devuelve cuántas afirmaciones aceptó. */
public interface LedgerPort {

    int send(String syncRunId, List<SourceClaim> claims);
}
