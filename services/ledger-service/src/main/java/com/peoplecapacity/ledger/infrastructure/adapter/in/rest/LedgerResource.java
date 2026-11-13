package com.peoplecapacity.ledger.infrastructure.adapter.in.rest;

import com.peoplecapacity.ledger.application.port.in.LedgerUseCase;
import com.peoplecapacity.ledger.domain.model.Claim;
import com.peoplecapacity.ledger.infrastructure.adapter.in.rest.api.LedgerApi;
import com.peoplecapacity.ledger.infrastructure.adapter.in.rest.dto.ClaimBatch;
import com.peoplecapacity.ledger.infrastructure.adapter.in.rest.dto.ClaimInput;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/** Adaptador de entrada REST. Implementa la interfaz generada desde openapi.yml. */
public class LedgerResource implements LedgerApi {

    @Inject
    LedgerUseCase ledger;

    @Override
    public Response recordClaims(ClaimBatch claimBatch) {
        List<Claim> claims = claimBatch.getClaims().stream().map(LedgerResource::toDomain).toList();
        return Response.status(Response.Status.CREATED).entity(ledger.recordClaims(claims)).build();
    }

    @Override
    public Response listClaims(String personId, String dimension, String status) {
        return Response.ok(ledger.findClaims(personId, dimension, status)).build();
    }

    @Override
    public Response getProfile(String personId, String xUserId) {
        return ledger.profile(personId)
                .map(profile -> Response.ok(profile).build())
                .orElseGet(() -> error(Response.Status.NOT_FOUND, "NOT_FOUND", "No hay afirmaciones para " + personId));
    }

    @Override
    public Response getHistory(String personId) {
        return Response.ok(ledger.history(personId)).build();
    }

    @Override
    public Response getSalaryGrade(String personId, String xUserId) {
        return ledger.salaryGrade(personId, xUserId)
                .map(grade -> Response.ok(Map.of("personId", personId, "salaryGrade", grade)).build())
                .orElseGet(() -> error(Response.Status.FORBIDDEN, "FORBIDDEN", "No tienes permiso para ver este dato"));
    }

    @Override
    public Response listAudit(String personId) {
        return Response.ok(ledger.audit(personId)).build();
    }

    private static Claim toDomain(ClaimInput in) {
        return new Claim(null, in.getPersonId(), in.getPersonName(), in.getPersonEmail(), in.getDimension(),
                in.getValue(), in.getSource(), in.getValidFrom(), in.getValidTo(), in.getObservedAt(),
                in.getStatus(), in.getEvidenceRef(), in.getAuthor(), null);
    }

    private static Response error(Response.Status status, String code, String message) {
        return Response.status(status).entity(Map.of("code", code, "message", message)).build();
    }
}
