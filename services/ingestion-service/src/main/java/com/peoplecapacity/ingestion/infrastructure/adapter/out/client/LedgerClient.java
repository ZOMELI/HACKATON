package com.peoplecapacity.ingestion.infrastructure.adapter.out.client;

import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST de ledger-service (contrato en ledger-service/openapi.yml). */
@RegisterRestClient(configKey = "ledger")
public interface LedgerClient {

    @POST
    @Path("/claims/batch")
    Map<String, Object> record(ClaimBatchPayload payload);

    record ClaimBatchPayload(String syncRunId, List<SourceClaim> claims) {
    }
}
