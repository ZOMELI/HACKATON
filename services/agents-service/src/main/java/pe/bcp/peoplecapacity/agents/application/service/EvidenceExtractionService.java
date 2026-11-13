package pe.bcp.peoplecapacity.agents.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.bcp.peoplecapacity.agents.application.AgentsMode;
import pe.bcp.peoplecapacity.agents.application.port.in.ExtractEvidenceUseCase;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelPort;
import pe.bcp.peoplecapacity.agents.application.port.out.RecordedExtractionsPort;
import pe.bcp.peoplecapacity.agents.domain.heuristic.HeuristicEvidenceExtractor;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.validation.EvidenceExtractionValidator;

/**
 * Agente de evidencia. En modo live pide la extracción al modelo y la valida; si el modelo falla o su
 * salida no cumple, usa el respaldo: extracción grabada por emailId o, si no hay, la heurística.
 */
public class EvidenceExtractionService implements ExtractEvidenceUseCase {

    private static final Logger log = LoggerFactory.getLogger(EvidenceExtractionService.class);

    private final AgentsMode mode;
    private final LanguageModelPort languageModel;
    private final RecordedExtractionsPort recordedExtractions;
    private final EvidenceExtractionValidator validator = new EvidenceExtractionValidator();
    private final HeuristicEvidenceExtractor heuristic = new HeuristicEvidenceExtractor();

    public EvidenceExtractionService(AgentsMode mode, LanguageModelPort languageModel,
            RecordedExtractionsPort recordedExtractions) {
        this.mode = mode;
        this.languageModel = languageModel;
        this.recordedExtractions = recordedExtractions;
    }

    @Override
    public EvidenceExtraction extract(EmailInput email) {
        if (mode == AgentsMode.LIVE) {
            try {
                return validator.validate(languageModel.extractEvidence(email), email);
            } catch (RuntimeException e) {
                // Degradación segura: nunca se propaga un fallo del modelo ni una salida inválida.
                log.warn("Agente de evidencia: se usó el respaldo para el correo {} ({}: {})",
                        email.emailId(), e.getClass().getSimpleName(), e.getMessage());
            }
        }
        return recordedExtractions.findByEmailId(email.emailId())
                .orElseGet(() -> heuristic.extract(email));
    }
}
