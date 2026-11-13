package pe.bcp.peoplecapacity.cases.application.port.in;

import java.util.List;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;
import pe.bcp.peoplecapacity.cases.domain.model.CaseStatus;
import pe.bcp.peoplecapacity.cases.domain.model.CaseType;
import pe.bcp.peoplecapacity.cases.domain.model.UserContext;

public interface CaseUseCase {

    /** Casos visibles para el usuario, ordenados por fecha límite. Los filtros nulos no aplican. */
    List<CaseFile> list(UserContext user, CaseStatus status, CaseType type, String personId);

    CaseFile get(String id);

    CaseFile decide(String id, UserContext user, String optionId, String comment);
}
