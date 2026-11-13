package com.peoplecapacity.ingestion.domain.model;

/** Resultado de leer una fuente. status: OK, FAILED o NOT_IMPLEMENTED. */
public record SourceResult(String source, String status, int records, String message) {
}
