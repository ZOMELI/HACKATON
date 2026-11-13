package pe.bcp.peoplecapacity.agents.application.port.out;

import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.RawEvidenceExtraction;

/**
 * Acceso al modelo de lenguaje. Devuelve la salida sin validar; quien la usa debe validarla.
 * Cualquier fallo (error, tiempo de espera, respuesta ilegible) se informa con LanguageModelException.
 */
public interface LanguageModelPort {

    RawEvidenceExtraction extractEvidence(EmailInput email);

    RawCaseSummary summarizeCase(CaseSummaryRequest request);
}
