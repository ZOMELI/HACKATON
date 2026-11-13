package com.peoplecapacity.ingestion.infrastructure.adapter.out.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Cliente REST de las APIs simuladas (contrato en mock-sources/openapi.yml). */
@RegisterRestClient(configKey = "mock-sources")
public interface MockSourcesClient {

    @GET
    @Path("/preper/boxes")
    List<Map<String, Object>> preperBoxes(@QueryParam("status") String status);

    @GET
    @Path("/siga/employees")
    List<Map<String, Object>> sigaEmployees();

    @GET
    @Path("/siga/org-units")
    List<Map<String, Object>> sigaOrgUnits();

    @GET
    @Path("/jira/rest/api/3/project")
    List<Map<String, Object>> jiraProjects();

    @GET
    @Path("/jira/rest/api/3/search")
    Map<String, Object> jiraIssues(@QueryParam("startAt") int startAt, @QueryParam("maxResults") int maxResults);
}
