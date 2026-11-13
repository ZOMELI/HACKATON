package com.peoplecapacity.ingestion.application.port.out;

import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import java.util.List;

/**
 * Puerto de salida: un adaptador por fuente. Para agregar una fuente basta crear otra implementación.
 * Implementados: PREPER, SIGA y JIRA. Pendientes con SDD: INVENTORY, MOVIPER, PROVIDER_ACT,
 * STAFFING_PORTAL, RECRUITMENT y EMAIL.
 */
public interface SourceReaderPort {

    String sourceName();

    List<SourceClaim> read();
}
