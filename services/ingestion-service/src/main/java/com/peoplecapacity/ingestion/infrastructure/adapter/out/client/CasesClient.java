package com.peoplecapacity.ingestion.infrastructure.adapter.out.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST de cases-service (contrato en cases-service/openapi.yml). */
@RegisterRestClient(configKey = "cases")
public interface CasesClient {

    @POST
    @Path("/reconciliations")
    Map<String, Object> reconcile(Map<String, String> request);
}
