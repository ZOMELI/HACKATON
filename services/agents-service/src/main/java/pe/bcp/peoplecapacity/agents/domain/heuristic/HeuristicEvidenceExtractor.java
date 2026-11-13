package pe.bcp.peoplecapacity.agents.domain.heuristic;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.text.SpanishDates;
import pe.bcp.peoplecapacity.agents.domain.text.TextNormalizer;

/**
 * Respaldo determinista para correos sin extracción grabada: expresiones regulares para matrículas,
 * ids y fechas, y palabras clave para el tipo. Sin señal clara devuelve IRRELEVANT.
 */
public final class HeuristicEvidenceExtractor {

    private static final Pattern PERSON_ID = Pattern.compile("\\b[A-Z]\\d{5}\\b");
    private static final Pattern MOVEMENT_ID = Pattern.compile("\\b(?:MOV|ENR)-\\d{1,6}\\b");
    private static final Pattern SWAP_ID = Pattern.compile("\\bENR-\\d{1,6}\\b");
    private static final Pattern CONTRACT_ID = Pattern.compile("\\bACT-\\d{1,6}\\b");
    private static final Pattern CONTRACT_WORD = Pattern.compile("\\bactas?\\b");
    private static final Pattern SIGN_OFF =
            Pattern.compile("^(atentamente|atte|saludos|cordialmente|gracias|slds|un abrazo)\\b.*");

    private static final String DATE =
            "(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2}\\s+de\\s+\\p{L}+(?:\\s+de(?:l)?\\s+\\d{4})?)";
    private static final Pattern FROM_DATE = Pattern.compile(
            "(?:desde|a partir de)(?:l|\\s+el)?\\s+" + DATE, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern UNTIL_DATE = Pattern.compile(
            "hasta(?:\\s+el)?\\s+" + DATE, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern TEXT_DATE = Pattern.compile(
            "(\\d{1,2})\\s+de\\s+(\\p{L}+)(?:\\s+de(?:l)?\\s+(\\d{4}))?");
    private static final Pattern SLASH_DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");

    public EvidenceExtraction extract(EmailInput email) {
        String body = email.body() == null ? "" : email.body();
        String subject = email.subject() == null ? "" : email.subject();
        String text = TextNormalizer.normalize(subject + " " + body);
        String folded = TextNormalizer.fold(text);

        EvidenceType type = classify(text, folded);
        Set<String> mentioned = new LinkedHashSet<>(findAll(PERSON_ID, text));
        if (type == EvidenceType.IRRELEVANT) {
            return new EvidenceExtraction(email.emailId(), type, List.copyOf(mentioned),
                    null, null, null, null, null, null, false, Mode.RECORDED);
        }

        String approver = type == EvidenceType.INFORMAL_NEED ? null : approver(email.from(), body);
        if (approver != null) {
            mentioned.remove(approver);
        }
        List<String> personIds = List.copyOf(mentioned);

        String movementId = first(MOVEMENT_ID, text);
        String contractId = first(CONTRACT_ID, text);
        LocalDate reference = SpanishDates.parseIsoPrefix(email.receivedAt()).orElse(null);
        LocalDate effectiveFrom = dateAfter(FROM_DATE, text, reference);
        LocalDate effectiveUntil = dateAfter(UNTIL_DATE, text, reference);
        if (effectiveFrom != null && effectiveUntil != null && effectiveUntil.isBefore(effectiveFrom)) {
            effectiveUntil = null;
        }

        return new EvidenceExtraction(email.emailId(), type, personIds, movementId, contractId,
                effectiveFrom, effectiveUntil, approver,
                quote(body, type, movementId, contractId, personIds), false, Mode.RECORDED);
    }

    private static EvidenceType classify(String text, String folded) {
        boolean contract = CONTRACT_ID.matcher(text).find() || CONTRACT_WORD.matcher(folded).find();
        if (folded.contains("enroque") || SWAP_ID.matcher(text).find()) {
            return EvidenceType.SWAP_CONFORMITY;
        }
        if (contract && folded.contains("reemplaz")) {
            return EvidenceType.CONTRACT_REPLACEMENT;
        }
        if (contract && (folded.contains("renueva") || folded.contains("renov"))) {
            return EvidenceType.CONTRACT_RENEWAL;
        }
        if (containsAny(folded, keywords(EvidenceType.TEMP_SUPPORT))) {
            return EvidenceType.TEMP_SUPPORT;
        }
        if (containsAny(folded, keywords(EvidenceType.MOVEMENT_APPROVAL))
                && (folded.contains("movimiento") || MOVEMENT_ID.matcher(text).find())) {
            return EvidenceType.MOVEMENT_APPROVAL;
        }
        if (containsAny(folded, keywords(EvidenceType.INFORMAL_NEED))) {
            return EvidenceType.INFORMAL_NEED;
        }
        return EvidenceType.IRRELEVANT;
    }

    /** Palabras clave (sin tildes, en minúsculas) que delatan cada tipo; también guían la elección de la cita. */
    private static List<String> keywords(EvidenceType type) {
        return switch (type) {
            case SWAP_CONFORMITY -> List.of("enroque", "enr-");
            case CONTRACT_REPLACEMENT -> List.of("reemplaz");
            case CONTRACT_RENEWAL -> List.of("renueva", "renov");
            case TEMP_SUPPORT -> List.of("apoyo temporal", "apoyara", "en apoyo", "apoyo a ");
            case MOVEMENT_APPROVAL -> List.of("aprueb", "aprob", "autoriz");
            case INFORMAL_NEED -> List.of("necesitamos", "necesito", "requerimos", "buscamos", "nos falta",
                    "necesidad de");
            case IRRELEVANT -> List.of();
        };
    }

    /** Quien aprueba: la matrícula del remitente o, si no viene, la que aparece en la firma del correo. */
    private static String approver(String from, String body) {
        String fromHeader = first(PERSON_ID, from == null ? "" : from);
        if (fromHeader != null) {
            return fromHeader;
        }
        String[] lines = body.split("\\R");
        int signOff = -1;
        for (int i = 0; i < lines.length; i++) {
            if (SIGN_OFF.matcher(TextNormalizer.fold(lines[i]).trim()).matches()) {
                signOff = i;
            }
        }
        if (signOff < 0) {
            return null;
        }
        StringBuilder signature = new StringBuilder();
        for (int i = signOff; i < lines.length; i++) {
            signature.append(lines[i]).append(' ');
        }
        return first(PERSON_ID, signature.toString());
    }

    /** Elige la oración del cuerpo con más señal: la que nombra el id, luego una matrícula, luego la palabra clave. */
    private static String quote(String body, EvidenceType type, String movementId, String contractId,
            List<String> personIds) {
        List<String> sentences = sentences(body);
        List<String> strongSignals = new ArrayList<>();
        if (movementId != null) {
            strongSignals.add(movementId);
        }
        if (contractId != null) {
            strongSignals.add(contractId);
        }
        Optional<String> byId = sentences.stream()
                .filter(s -> strongSignals.stream().anyMatch(s::contains)).findFirst();
        if (byId.isPresent()) {
            return byId.get();
        }
        List<String> typeKeywords = keywords(type);
        Optional<String> byKeyword = sentences.stream()
                .filter(s -> containsAny(TextNormalizer.fold(s), typeKeywords)).findFirst();
        if (byKeyword.isPresent()) {
            return byKeyword.get();
        }
        return sentences.stream()
                .filter(s -> personIds.stream().anyMatch(s::contains)).findFirst().orElse(null);
    }

    private static List<String> sentences(String body) {
        List<String> sentences = new ArrayList<>();
        // Un párrafo termina en línea en blanco o en un saludo/encabezado que acaba en coma o dos puntos.
        for (String paragraph : body.split("\\R\\s*\\R|(?<=[,:])\\s*\\R")) {
            for (String sentence : TextNormalizer.normalize(paragraph).split("(?<=[.!?])\\s+")) {
                if (!sentence.isBlank()) {
                    sentences.add(sentence.trim());
                }
            }
        }
        return sentences;
    }

    private static LocalDate dateAfter(Pattern pattern, String text, LocalDate reference) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            Optional<LocalDate> date = parseDate(matcher.group(1), reference);
            if (date.isPresent()) {
                return date.get();
            }
        }
        return null;
    }

    /** Interpreta una fecha escrita. Si no trae año se toma el de la fecha de recepción; sin ella no se inventa. */
    static Optional<LocalDate> parseDate(String token, LocalDate reference) {
        Optional<LocalDate> iso = token.length() == 10 ? SpanishDates.parseIsoPrefix(token) : Optional.empty();
        if (iso.isPresent()) {
            return iso;
        }
        Matcher slash = SLASH_DATE.matcher(token);
        if (slash.matches()) {
            return SpanishDates.of(Integer.parseInt(slash.group(3)), Integer.parseInt(slash.group(2)),
                    Integer.parseInt(slash.group(1)));
        }
        Matcher written = TEXT_DATE.matcher(token);
        if (!written.matches()) {
            return Optional.empty();
        }
        Optional<Integer> month = SpanishDates.monthNumber(written.group(2));
        if (month.isEmpty()) {
            return Optional.empty();
        }
        int day = Integer.parseInt(written.group(1));
        if (written.group(3) != null) {
            return SpanishDates.of(Integer.parseInt(written.group(3)), month.get(), day);
        }
        if (reference == null) {
            return Optional.empty();
        }
        // Sin año: el de la recepción, salvo que quede muy atrás (correo de diciembre que habla de enero).
        return SpanishDates.of(reference.getYear(), month.get(), day)
                .map(date -> date.isBefore(reference.minusMonths(6)) ? date.plusYears(1) : date);
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static String first(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private static List<String> findAll(Pattern pattern, String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }
}
