package com.peoplecapacity.ingestion.infrastructure.adapter.in.rest;

import com.peoplecapacity.ingestion.application.port.in.IngestionUseCase;
import com.peoplecapacity.ingestion.infrastructure.adapter.in.rest.api.IngestionApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** Adaptador de entrada REST. Implementa la interfaz generada desde openapi.yml. */
public class IngestionResource implements IngestionApi {

    @Inject
    IngestionUseCase ingestion;

    @Override
    public Response startSync() {
        return Response.status(Response.Status.CREATED).entity(ingestion.startSync()).build();
    }

    @Override
    public Response listSyncRuns() {
        return Response.ok(ingestion.list()).build();
    }

    @Override
    public Response getSyncRun(Long runId) {
        return ingestion.get(runId).map(run -> Response.ok(run).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("code", "NOT_FOUND", "message", "La sincronización no existe")).build());
    }
}
