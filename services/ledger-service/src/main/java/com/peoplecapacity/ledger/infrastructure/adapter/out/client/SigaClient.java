package com.peoplecapacity.ledger.infrastructure.adapter.out.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST de SIGA (simulado en mock-sources). */
@RegisterRestClient(configKey = "mock-sources")
@Path("/siga")
public interface SigaClient {

    @GET
    @Path("/employees/{employeeId}/salary-grade")
    Map<String, Object> salaryGrade(@PathParam("employeeId") String employeeId,
            @HeaderParam("X-Requester-Id") String requesterId);
}
