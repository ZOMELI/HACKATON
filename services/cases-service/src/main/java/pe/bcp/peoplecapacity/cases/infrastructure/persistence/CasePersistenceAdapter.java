package pe.bcp.peoplecapacity.cases.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.cases.application.port.out.AlertSettingsRepository;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseRepository;
import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;

@Component
public class CasePersistenceAdapter implements CaseRepository, AlertSettingsRepository {

    private final CaseJpaRepository cases;
    private final AlertSettingsJpaRepository settings;
    private final ObjectMapper mapper;

    CasePersistenceAdapter(CaseJpaRepository cases, AlertSettingsJpaRepository settings, ObjectMapper mapper) {
        this.cases = cases;
        this.settings = settings;
        this.mapper = mapper;
    }

    @Override
    public CaseFile save(CaseFile c) {
        try {
            cases.save(new CaseEntity(c.id(), c.dedupeKey(), c.type().name(), c.status().name(), c.personId(),
                    c.ownerId(), c.dueDate(), c.openedAt(), mapper.writeValueAsString(c)));
            return c;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el caso " + c.id(), e);
        }
    }

    @Override
    public Optional<CaseFile> findById(String id) {
        return cases.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CaseFile> findByDedupeKey(String dedupeKey) {
        return cases.findByDedupeKey(dedupeKey).map(this::toDomain);
    }

    @Override
    public List<CaseFile> findAll() {
        return cases.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public AlertSettings get() {
        return settings.findById(AlertSettingsEntity.SINGLETON_ID)
                .map(e -> new AlertSettings(e.getContractExpiryDays(), e.getAvailabilityDays(), e.getUpdatedAt(), e.getUpdatedBy()))
                .orElseGet(AlertSettings::defaults);
    }

    @Override
    public AlertSettings save(AlertSettings s) {
        settings.save(new AlertSettingsEntity(s.contractExpiryDays(), s.availabilityDays(), s.updatedAt(), s.updatedBy()));
        return s;
    }

    private CaseFile toDomain(CaseEntity entity) {
        try {
            return mapper.readValue(entity.getPayload(), CaseFile.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer un caso guardado", e);
        }
    }
}
