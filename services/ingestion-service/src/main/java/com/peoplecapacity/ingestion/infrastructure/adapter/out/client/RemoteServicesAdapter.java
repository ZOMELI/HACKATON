package com.peoplecapacity.ingestion.infrastructure.adapter.out.client;

import com.peoplecapacity.ingestion.application.port.out.LedgerPort;
import com.peoplecapacity.ingestion.application.port.out.ReconciliationPort;
import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** Adaptador de salida hacia ledger-service y cases-service. */
@ApplicationScoped
public class RemoteServicesAdapter implements LedgerPort, ReconciliationPort {

    @Inject
    @RestClient
    LedgerClient ledger;

    @Inject
    @RestClient
    CasesClient cases;

    @Override
    public int send(String syncRunId, List<SourceClaim> claims) {
        if (claims.isEmpty()) {
            return 0;
        }
        Object recorded = ledger.record(new LedgerClient.ClaimBatchPayload(syncRunId, claims)).get("recorded");
        return recorded instanceof Number number ? number.intValue() : 0;
    }

    @Override
    public void trigger(String syncRunId) {
        cases.reconcile(Map.of("syncRunId", syncRunId));
    }
}
