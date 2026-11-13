package pe.bcp.peoplecapacity.agents.application.port.in;

import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;

/** Agente de evidencia: convierte un correo en evidencia estructurada. */
public interface ExtractEvidenceUseCase {

    EvidenceExtraction extract(EmailInput email);
}
