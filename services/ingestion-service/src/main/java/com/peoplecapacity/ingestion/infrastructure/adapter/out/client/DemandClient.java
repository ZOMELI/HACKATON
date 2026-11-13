package com.peoplecapacity.ingestion.infrastructure.adapter.out.client;

import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST de demand-service (contracts/demand.yaml). */
@RegisterRestClient(configKey = "demand")
public interface DemandClient {

    @PUT
    @Path("/needs")
    Map<String, Object> putNeeds(Map<String, Object> body);
}
