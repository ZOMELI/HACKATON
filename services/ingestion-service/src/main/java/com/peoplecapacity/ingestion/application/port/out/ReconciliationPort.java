package com.peoplecapacity.ingestion.application.port.out;

/** Puerto de salida: avisa a cases-service que hay datos nuevos para conciliar. */
public interface ReconciliationPort {

    void trigger(String syncRunId);
}
