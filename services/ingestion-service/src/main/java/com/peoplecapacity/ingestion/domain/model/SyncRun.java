package com.peoplecapacity.ingestion.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/** Una sincronización. status: COMPLETED o PARTIAL (alguna fuente falló). */
public record SyncRun(Long id, LocalDateTime startedAt, LocalDateTime finishedAt, String status, int claimsSent,
        List<SourceResult> sources) {
}
