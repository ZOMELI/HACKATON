package com.peoplecapacity.ingestion.infrastructure.adapter.out.persistence;

import com.peoplecapacity.ingestion.application.port.out.SyncRunRepositoryPort;
import com.peoplecapacity.ingestion.domain.model.SourceResult;
import com.peoplecapacity.ingestion.domain.model.SyncRun;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/** Adaptador de salida: guarda cada sincronización en el esquema INGESTION. */
@ApplicationScoped
public class SyncRunRepositoryAdapter implements SyncRunRepositoryPort, PanacheRepository<SyncRunEntity> {

    @Override
    @Transactional
    public SyncRun store(SyncRun run) {
        SyncRunEntity entity = run.id() == null ? new SyncRunEntity() : findById(run.id());
        entity.startedAt = run.startedAt();
        entity.finishedAt = run.finishedAt();
        entity.status = run.status();
        entity.claimsSent = run.claimsSent();
        entity.sources.clear();
        for (SourceResult result : run.sources()) {
            SourceResultEmbeddable row = new SourceResultEmbeddable();
            row.source = result.source();
            row.status = result.status();
            row.records = result.records();
            row.message = result.message() == null ? null
                    : result.message().substring(0, Math.min(result.message().length(), 500));
            entity.sources.add(row);
        }
        if (entity.id == null) {
            persist(entity);
        }
        return toDomain(entity);
    }

    @Override
    public List<SyncRun> recent() {
        return list("order by startedAt desc").stream().map(SyncRunRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<SyncRun> load(long runId) {
        return findByIdOptional(runId).map(SyncRunRepositoryAdapter::toDomain);
    }

    private static SyncRun toDomain(SyncRunEntity e) {
        List<SourceResult> results = e.sources.stream()
                .map(s -> new SourceResult(s.source, s.status, s.records, s.message)).toList();
        return new SyncRun(e.id, e.startedAt, e.finishedAt, e.status, e.claimsSent, results);
    }
}
