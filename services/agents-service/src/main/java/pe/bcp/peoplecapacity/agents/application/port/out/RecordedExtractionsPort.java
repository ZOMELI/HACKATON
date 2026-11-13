package pe.bcp.peoplecapacity.agents.application.port.out;

import java.util.Optional;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;

/** Extracciones grabadas por emailId, usadas como respaldo. */
public interface RecordedExtractionsPort {

    Optional<EvidenceExtraction> findByEmailId(String emailId);
}
