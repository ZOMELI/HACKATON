package pe.bcp.peoplecapacity.agents.domain.text;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de texto compartidas por validadores, heurística y plantillas. */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** Unifica la forma Unicode y colapsa cualquier secuencia de espacios o saltos de línea en un espacio. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC)
                .replace(' ', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Indica si la cita aparece literalmente en el texto, ignorando solo diferencias de espacios. */
    public static boolean containsLiterally(String text, String quote) {
        String normalizedQuote = normalize(quote);
        return !normalizedQuote.isEmpty() && normalize(text).contains(normalizedQuote);
    }

    /** Minúsculas y sin tildes, para buscar palabras clave. */
    public static String fold(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Devuelve null si el texto es nulo, vacío o la palabra "null"; si no, el texto sin espacios sobrantes. */
    public static String blankToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }
}
