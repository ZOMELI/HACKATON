package pe.bcp.peoplecapacity.agents.domain.model;

import java.util.List;

/** Salida del modelo para un resumen, antes de validar. */
public record RawCaseSummary(String summary, List<RawOption> options) {

    public record RawOption(String id, String text) {
    }
}
