package com.peoplecapacity.ingestion.application.port.out;

import com.peoplecapacity.ingestion.domain.model.SyncRun;
import java.util.List;
import java.util.Optional;

public interface SyncRunRepositoryPort {

    SyncRun store(SyncRun run);

    List<SyncRun> recent();

    Optional<SyncRun> load(long runId);
}
