package com.peoplecapacity.ingestion.application.usecase;

import com.peoplecapacity.ingestion.application.port.in.IngestionUseCase;
import com.peoplecapacity.ingestion.application.port.out.LedgerPort;
import com.peoplecapacity.ingestion.application.port.out.ReconciliationPort;
import com.peoplecapacity.ingestion.application.port.out.SourceReaderPort;
import com.peoplecapacity.ingestion.application.port.out.SyncRunRepositoryPort;
import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import com.peoplecapacity.ingestion.domain.model.SourceResult;
import com.peoplecapacity.ingestion.domain.model.SyncRun;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class IngestionService implements IngestionUseCase {

    private static final List<String> PENDING_SOURCES = List.of();

    @Inject
    Instance<SourceReaderPort> readers;

    @Inject
    LedgerPort ledger;

    @Inject
    ReconciliationPort reconciliation;

    @Inject
    SyncRunRepositoryPort runs;

    @Override
    public SyncRun startSync() {
        LocalDateTime startedAt = LocalDateTime.now();
        List<SourceResult> results = new ArrayList<>();
        List<SourceClaim> claims = new ArrayList<>();
        boolean failed = false;

        // Una fuente caída no detiene a las demás.
        for (SourceReaderPort reader : readers) {
            try {
                List<SourceClaim> read = reader.read();
                claims.addAll(read);
                results.add(new SourceResult(reader.sourceName(), "OK", read.size(), null));
            } catch (RuntimeException e) {
                failed = true;
                results.add(new SourceResult(reader.sourceName(), "FAILED", 0, e.getMessage()));
            }
        }
        for (String pending : PENDING_SOURCES) {
            results.add(new SourceResult(pending, "NOT_IMPLEMENTED", 0, "Adaptador pendiente"));
        }

        SyncRun run = runs.store(new SyncRun(null, startedAt, null, "RUNNING", 0, results));
        String runId = String.valueOf(run.id());
        int sent = 0;
        try {
            sent = ledger.send(runId, claims);
        } catch (RuntimeException e) {
            failed = true;
            results.add(new SourceResult("LEDGER", "FAILED", 0, e.getMessage()));
        }
        try {
            reconciliation.trigger(runId);
        } catch (RuntimeException e) {
            failed = true;
            results.add(new SourceResult("RECONCILIATION", "FAILED", 0, e.getMessage()));
        }
        return runs.store(new SyncRun(run.id(), startedAt, LocalDateTime.now(), failed ? "PARTIAL" : "COMPLETED",
                sent, results));
    }

    @Override
    public List<SyncRun> list() {
        return runs.recent();
    }

    @Override
    public Optional<SyncRun> get(long runId) {
        return runs.load(runId);
    }
}
