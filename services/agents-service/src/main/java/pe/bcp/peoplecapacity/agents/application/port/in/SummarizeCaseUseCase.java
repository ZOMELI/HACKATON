package pe.bcp.peoplecapacity.agents.application.port.in;

import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;

/** Agente de expediente: redacta el resumen del caso y el texto de las opciones recibidas. */
public interface SummarizeCaseUseCase {

    CaseSummary summarize(CaseSummaryRequest request);
}
