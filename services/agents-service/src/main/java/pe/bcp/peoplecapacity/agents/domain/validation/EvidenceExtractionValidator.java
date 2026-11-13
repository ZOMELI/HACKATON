package pe.bcp.peoplecapacity.agents.domain.validation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.model.RawEvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.text.TextNormalizer;

/**
 * Valida la extracción que devuelve el modelo contra el esquema del contrato y contra el propio correo:
 * nada que no esté escrito en el correo se acepta.
 */
public final class EvidenceExtractionValidator {

    public static final Pattern PERSON_ID = Pattern.compile("[A-Z]\\d{5}");
    public static final Pattern MOVEMENT_ID = Pattern.compile("(?:MOV|ENR)-\\d{1,6}");
    public static final Pattern CONTRACT_ID = Pattern.compile("ACT-\\d{1,6}");

    public EvidenceExtraction validate(RawEvidenceExtraction raw, EmailInput email) {
        if (raw == null) {
            throw new InvalidModelOutputException("el modelo no devolvió una extracción");
        }
        EvidenceType type = EvidenceType.fromText(raw.type())
                .orElseThrow(() -> new InvalidModelOutputException("tipo fuera del catálogo: " + raw.type()));

        String emailText = TextNormalizer.normalize(
                (email.subject() == null ? "" : email.subject()) + " " + email.body());

        List<String> personIds = new ArrayList<>();
        for (String candidate : raw.personIds() == null ? List.<String>of() : raw.personIds()) {
            String personId = TextNormalizer.blankToNull(candidate);
            if (personId == null || !PERSON_ID.matcher(personId).matches()) {
                throw new InvalidModelOutputException("matrícula con formato inválido: " + candidate);
            }
            requirePresent(emailText, personId, "matrícula");
            if (!personIds.contains(personId)) {
                personIds.add(personId);
            }
        }

        String movementId = identifier(raw.movementId(), MOVEMENT_ID, "id de movimiento", emailText);
        String contractId = identifier(raw.contractId(), CONTRACT_ID, "id de acta", emailText);

        LocalDate effectiveFrom = date(raw.effectiveFrom(), "effectiveFrom");
        LocalDate effectiveUntil = date(raw.effectiveUntil(), "effectiveUntil");
        if (effectiveFrom != null && effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            throw new InvalidModelOutputException("effectiveUntil es anterior a effectiveFrom");
        }

        String quote = TextNormalizer.blankToNull(raw.quote());
        if (quote == null && type != EvidenceType.IRRELEVANT) {
            throw new InvalidModelOutputException("falta la cita textual que respalda la extracción");
        }
        if (quote != null && !TextNormalizer.containsLiterally(email.body(), quote)) {
            throw new InvalidModelOutputException("la cita no aparece literalmente en el correo");
        }

        return new EvidenceExtraction(
                email.emailId(),
                type,
                personIds,
                movementId,
                contractId,
                effectiveFrom,
                effectiveUntil,
                TextNormalizer.blankToNull(raw.approver()),
                quote,
                true,
                Mode.LIVE);
    }

    private static String identifier(String rawValue, Pattern pattern, String name, String emailText) {
        String value = TextNormalizer.blankToNull(rawValue);
        if (value == null) {
            return null;
        }
        if (!pattern.matcher(value).matches()) {
            throw new InvalidModelOutputException(name + " con formato inválido: " + rawValue);
        }
        requirePresent(emailText, value, name);
        return value;
    }

    private static void requirePresent(String emailText, String value, String name) {
        if (!emailText.contains(value)) {
            throw new InvalidModelOutputException(name + " que no aparece en el correo: " + value);
        }
    }

    private static LocalDate date(String rawValue, String field) {
        String value = TextNormalizer.blankToNull(rawValue);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidModelOutputException(field + " no es una fecha ISO válida: " + rawValue);
        }
    }
}
