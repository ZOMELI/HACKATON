package pe.bcp.peoplecapacity.agents.domain.model;

import java.util.List;

/** Resumen redactado de un caso y el texto de cada opción recibida. */
public record CaseSummary(String caseId, String summary, List<OptionText> options, boolean aiGenerated, Mode mode) {

    public CaseSummary {
        options = options == null ? List.of() : List.copyOf(options);
    }

    public record OptionText(String id, String text) {
    }
}
