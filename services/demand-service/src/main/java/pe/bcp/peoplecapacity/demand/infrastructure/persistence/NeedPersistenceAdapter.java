package pe.bcp.peoplecapacity.demand.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.demand.application.port.out.NeedRepository;
import pe.bcp.peoplecapacity.demand.domain.Need;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;

@Component
class NeedPersistenceAdapter implements NeedRepository {

    private final NeedJpaRepository jpa;

    NeedPersistenceAdapter(NeedJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Need> findByExternalId(String externalId) {
        return jpa.findByExternalId(externalId).map(NeedEntity::toDomain);
    }

    @Override
    public Need save(Need need) {
        // saveAndFlush: el upsert por lotes puede repetir un externalId y debe verlo en la siguiente búsqueda.
        return jpa.saveAndFlush(NeedEntity.fromDomain(need)).toDomain();
    }

    @Override
    public List<Need> findAll(NeedStatus status) {
        List<NeedEntity> rows = status == null ? jpa.findAll() : jpa.findByStatus(status);
        return rows.stream().map(NeedEntity::toDomain).toList();
    }
}
