package com.peoplecapacity.ingestion.infrastructure.adapter.out.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST del agente de evidencia (contracts/agents.yaml). */
@RegisterRestClient(configKey = "agents")
public interface AgentsClient {

    @POST
    @Path("/evidence-extractions")
    Map<String, Object> extract(Map<String, Object> email);
}
