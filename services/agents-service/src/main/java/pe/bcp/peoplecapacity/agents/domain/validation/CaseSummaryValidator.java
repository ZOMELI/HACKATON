package pe.bcp.peoplecapacity.agents.domain.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary;
import pe.bcp.peoplecapacity.agents.domain.text.TextNormalizer;

/**
 * Valida el resumen que devuelve el modelo: texto no vacío y exactamente las mismas opciones recibidas,
 * sin agregar ni quitar ninguna.
 */
public final class CaseSummaryValidator {

    public CaseSummary validate(RawCaseSummary raw, CaseSummaryRequest request) {
        if (raw == null) {
            throw new InvalidModelOutputException("el modelo no devolvió un resumen");
        }
        String summary = TextNormalizer.blankToNull(raw.summary());
        if (summary == null) {
            throw new InvalidModelOutputException("el resumen está vacío");
        }

        Map<String, String> textById = new LinkedHashMap<>();
        for (RawCaseSummary.RawOption option : raw.options() == null
                ? List.<RawCaseSummary.RawOption>of() : raw.options()) {
            if (option == null || option.id() == null) {
                throw new InvalidModelOutputException("opción sin id");
            }
            String text = TextNormalizer.blankToNull(option.text());
            if (text == null) {
                throw new InvalidModelOutputException("opción con texto vacío: " + option.id());
            }
            if (textById.put(option.id(), text) != null) {
                throw new InvalidModelOutputException("opción repetida: " + option.id());
            }
        }

        List<CaseSummary.OptionText> options = new ArrayList<>();
        for (CaseSummaryRequest.Option expected : request.options()) {
            String text = textById.remove(expected.id());
            if (text == null) {
                throw new InvalidModelOutputException("falta la opción recibida: " + expected.id());
            }
            options.add(new CaseSummary.OptionText(expected.id(), text));
        }
        if (!textById.isEmpty()) {
            throw new InvalidModelOutputException("opciones que no se recibieron: " + textById.keySet());
        }

        return new CaseSummary(request.caseId(), summary, options, true, Mode.LIVE);
    }
}
