package pe.bcp.peoplecapacity.cases.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;

public interface CaseRepository {

    CaseFile save(CaseFile caseFile);

    Optional<CaseFile> findById(String id);

    Optional<CaseFile> findByDedupeKey(String dedupeKey);

    List<CaseFile> findAll();
}
