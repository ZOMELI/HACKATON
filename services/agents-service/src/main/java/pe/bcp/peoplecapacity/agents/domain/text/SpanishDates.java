package pe.bcp.peoplecapacity.agents.domain.text;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/** Fechas en español: nombres de mes y formato legible. */
public final class SpanishDates {

    private static final List<String> MONTHS = List.of(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre");

    private SpanishDates() {
    }

    /** Número de mes (1-12) a partir de su nombre; acepta "setiembre". */
    public static Optional<Integer> monthNumber(String name) {
        String folded = TextNormalizer.fold(name).trim();
        if ("setiembre".equals(folded)) {
            return Optional.of(9);
        }
        int index = MONTHS.indexOf(folded);
        return index < 0 ? Optional.empty() : Optional.of(index + 1);
    }

    /** Interpreta los diez primeros caracteres como fecha ISO (sirve para fechas y fecha-hora). */
    public static Optional<LocalDate> parseIsoPrefix(String text) {
        if (text == null || text.trim().length() < 10) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(text.trim().substring(0, 10)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static Optional<LocalDate> of(int year, int month, int day) {
        try {
            return Optional.of(LocalDate.of(year, month, day));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    public static String format(LocalDate date) {
        return date.getDayOfMonth() + " de " + MONTHS.get(date.getMonthValue() - 1) + " de " + date.getYear();
    }

    /** "2026-11-30" → "30 de noviembre de 2026". Si no se puede interpretar, devuelve el texto tal cual. */
    public static String format(String isoText) {
        if (isoText == null || isoText.isBlank()) {
            return null;
        }
        return parseIsoPrefix(isoText).map(SpanishDates::format).orElse(isoText.trim());
    }
}
