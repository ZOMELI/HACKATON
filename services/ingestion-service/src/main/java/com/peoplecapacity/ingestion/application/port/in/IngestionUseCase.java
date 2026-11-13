package com.peoplecapacity.ingestion.application.port.in;

import com.peoplecapacity.ingestion.domain.model.SyncRun;
import java.util.List;
import java.util.Optional;

/** Puerto de entrada de la ingesta. */
public interface IngestionUseCase {

    SyncRun startSync();

    List<SyncRun> list();

    Optional<SyncRun> get(long runId);
}
