package pe.bcp.peoplecapacity.agents.infrastructure.recorded;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.agents.application.port.out.RecordedExtractionsPort;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;

/** Lee las extracciones grabadas de un archivo JSON del classpath, indexadas por emailId. */
@Component
public class JsonRecordedExtractionsAdapter implements RecordedExtractionsPort {

    private final Map<String, EvidenceExtraction> byEmailId = new HashMap<>();

    /** Forma de cada registro del archivo. */
    record RecordedExtraction(String emailId, EvidenceType type, List<String> personIds, String movementId,
            String contractId, LocalDate effectiveFrom, LocalDate effectiveUntil, String approver, String quote) {
    }

    public JsonRecordedExtractionsAdapter(
            @Value("${agents.recorded.extractions:classpath:recorded/evidence-extractions.json}") Resource file,
            ObjectMapper objectMapper) {
        try (InputStream in = file.getInputStream()) {
            RecordedExtraction[] records = objectMapper.readValue(in, RecordedExtraction[].class);
            for (RecordedExtraction r : records) {
                byEmailId.put(r.emailId(), new EvidenceExtraction(r.emailId(), r.type(), r.personIds(),
                        r.movementId(), r.contractId(), r.effectiveFrom(), r.effectiveUntil(), r.approver(),
                        r.quote(), false, Mode.RECORDED));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo de extracciones grabadas", e);
        }
    }

    @Override
    public Optional<EvidenceExtraction> findByEmailId(String emailId) {
        return Optional.ofNullable(byEmailId.get(emailId));
    }
}
