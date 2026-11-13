package com.peoplecapacity.mocksources.infrastructure.adapter.in.rest;

import com.peoplecapacity.mocksources.application.port.in.SourceQueryUseCase;
import com.peoplecapacity.mocksources.infrastructure.adapter.in.rest.api.SourcesApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

/** Adaptador de entrada REST. Implementa la interfaz generada desde openapi.yml. */
public class SourcesResource implements SourcesApi {

    @Inject
    SourceQueryUseCase sources;

    @Override
    public Response listJiraProjects() {
        return Response.ok(sources.jiraProjects()).build();
    }

    @Override
    public Response searchJiraIssues(String assignee, String project, String createdAfter, Integer startAt,
            Integer maxResults) {
        return Response.ok(sources.jiraIssues(assignee, project, createdAfter, startAt == null ? 0 : startAt,
                maxResults == null ? 50 : maxResults)).build();
    }

    @Override
    public Response listSigaOrgUnits() {
        return Response.ok(sources.sigaOrgUnits()).build();
    }

    @Override
    public Response listSigaEmployees(String orgUnitCode, String managerId, String email) {
        return Response.ok(sources.sigaEmployees(orgUnitCode, managerId, email)).build();
    }

    @Override
    public Response getSigaEmployee(String employeeId) {
        return found(sources.sigaEmployee(employeeId));
    }

    @Override
    public Response getSigaSalaryGrade(String employeeId, String xRequesterId) {
        Optional<SourceQueryUseCase.GradeResult> result = sources.sigaSalaryGrade(employeeId, xRequesterId);
        if (result.isEmpty()) {
            return notFound();
        }
        if (!result.get().allowed()) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("code", "FORBIDDEN", "message", "No tienes permiso para ver este dato")).build();
        }
        return Response.ok(Map.of("employeeId", employeeId, "salaryGrade", result.get().salaryGrade())).build();
    }

    @Override
    public Response listPreperBoxes(String employeeId, String status, String fundingTribe, String budgetCategory,
            String expiringBefore) {
        return Response.ok(sources.preperBoxes(employeeId, status, fundingTribe, budgetCategory, expiringBefore))
                .build();
    }

    @Override
    public Response getPreperBox(String boxId) {
        return found(sources.preperBox(boxId));
    }

    private static Response found(Optional<Map<String, Object>> record) {
        return record.map(r -> Response.ok(r).build()).orElseGet(SourcesResource::notFound);
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("code", "NOT_FOUND", "message", "El recurso no existe")).build();
    }
}
