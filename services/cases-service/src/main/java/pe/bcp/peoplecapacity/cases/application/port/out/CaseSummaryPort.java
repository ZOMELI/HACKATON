package pe.bcp.peoplecapacity.cases.application.port.out;

import java.util.Map;
import java.util.Optional;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;

/** Agente de expediente (agents-service). Vacío si no responde: quien llama usa la plantilla local. */
public interface CaseSummaryPort {

    Optional<Drafted> draft(CaseFile caseFile);

    record Drafted(String summary, Map<String, String> optionTexts, boolean aiGenerated, String mode) {
    }
}
