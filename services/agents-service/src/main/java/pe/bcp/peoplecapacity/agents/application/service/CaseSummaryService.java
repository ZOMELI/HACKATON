package pe.bcp.peoplecapacity.agents.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.bcp.peoplecapacity.agents.application.AgentsMode;
import pe.bcp.peoplecapacity.agents.application.port.in.SummarizeCaseUseCase;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelPort;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.template.CaseSummaryTemplates;
import pe.bcp.peoplecapacity.agents.domain.validation.CaseSummaryValidator;

/**
 * Agente de expediente. En modo live pide la redacción al modelo y la valida; si el modelo falla o su
 * salida no cumple (por ejemplo, agrega o quita opciones), usa la plantilla del tipo de caso.
 */
public class CaseSummaryService implements SummarizeCaseUseCase {

    private static final Logger log = LoggerFactory.getLogger(CaseSummaryService.class);

    private final AgentsMode mode;
    private final LanguageModelPort languageModel;
    private final CaseSummaryValidator validator = new CaseSummaryValidator();
    private final CaseSummaryTemplates templates = new CaseSummaryTemplates();

    public CaseSummaryService(AgentsMode mode, LanguageModelPort languageModel) {
        this.mode = mode;
        this.languageModel = languageModel;
    }

    @Override
    public CaseSummary summarize(CaseSummaryRequest request) {
        if (mode == AgentsMode.LIVE) {
            try {
                return validator.validate(languageModel.summarizeCase(request), request);
            } catch (RuntimeException e) {
                // Degradación segura: nunca se propaga un fallo del modelo ni una salida inválida.
                log.warn("Agente de expediente: se usó el respaldo para el caso {} ({}: {})",
                        request.caseId(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return templates.render(request);
    }
}
