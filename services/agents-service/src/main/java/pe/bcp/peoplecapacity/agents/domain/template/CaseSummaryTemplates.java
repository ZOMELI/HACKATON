package pe.bcp.peoplecapacity.agents.domain.template;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest.Claim;
import pe.bcp.peoplecapacity.agents.domain.model.CaseType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.text.SpanishDates;
import pe.bcp.peoplecapacity.agents.domain.text.TextNormalizer;

/**
 * Respaldo del agente de expediente: una plantilla por tipo de caso que interpola persona, valores,
 * fuentes y fechas de las afirmaciones recibidas. No elige opción ni cambia las opciones recibidas.
 */
public final class CaseSummaryTemplates {

    private static final Pattern NEGATION = Pattern.compile("\\b(no|sin)\\b");

    private static final Map<String, String> SOURCE_NAMES = Map.of(
            "SIGA", "SIGA",
            "PREPER", "PREPER",
            "JIRA", "Jira",
            "EXCEL_CHAPTER", "el Excel de chapters",
            "EXCEL_MOVEMENTS", "el Excel de movimientos",
            "EXCEL_CONTRACTS", "el Excel de actas",
            "EXCEL_SUPPORT", "el Excel de apoyos temporales",
            "EXCEL_RECRUITMENT", "el Excel de reclutamiento",
            "EMAIL", "un correo",
            "DECISION", "una decisión registrada");

    private static final Map<String, String> SUBJECT_NAMES = Map.of(
            "ORG_MEMBERSHIP", "la tribu de pertenencia",
            "MANAGEMENT_RESPONSIBILITY", "el responsable",
            "BUDGET_ORIGIN", "la tribu que financia",
            "OPERATIONAL_ASSIGNMENT", "la tribu donde trabaja",
            "MOVEMENT", "el movimiento",
            "CONTRACT", "el acta",
            "AVAILABILITY", "la disponibilidad",
            "TEMP_SUPPORT", "el apoyo temporal a la tribu");

    /** Regla que asocia palabras de la opción (id + etiqueta, sin tildes) con la consecuencia de elegirla. */
    private record ConsequenceRule(Pattern keywords, boolean negative, String consequence) {
    }

    private static ConsequenceRule rule(String keywords, String consequence) {
        return new ConsequenceRule(Pattern.compile(keywords), false, consequence);
    }

    private static ConsequenceRule negativeRule(String keywords, String consequence) {
        return new ConsequenceRule(Pattern.compile(keywords), true, consequence);
    }

    private static final Map<CaseType, List<ConsequenceRule>> CONSEQUENCES = Map.of(
            CaseType.BUDGET_ASSIGNMENT_MISMATCH, List.of(
                    rule("apoyo|support", "Quedará registrado que la persona trabaja en la otra tribu como apoyo temporal."),
                    rule("presupuesto|budget|preper", "Quedará registrado que el presupuesto debe corregirse para coincidir con la tribu donde trabaja."),
                    rule("asignaci|assign|jira|regres|volver|return", "Quedará registrado que la persona debe volver a trabajar en la tribu que la financia.")),
            CaseType.MOVEMENT_NOT_REFLECTED, List.of(
                    negativeRule("anul|cancel|revert|descart|no aplic", "Quedará registrado que el movimiento no se aplica."),
                    rule("siga|actualiz|update|aplic|apply", "Quedará registrado que SIGA debe actualizarse con el movimiento aprobado.")),
            CaseType.MANAGER_MISMATCH, List.of(
                    rule("siga", "Se tomará como válido el responsable que indica SIGA."),
                    rule("excel|chapter", "Se tomará como válido el responsable que indica el Excel de chapters.")),
            CaseType.MOVEMENT_WITHOUT_EVIDENCE, List.of(
                    negativeRule("anul|cancel|rechaz|reject", "Quedará registrado que el movimiento no procede."),
                    rule("solicit|pedir|request|evidenc", "Quedará registrado que falta conseguir el correo de aprobación."),
                    rule("confirm|valid|aprob|aprueb|approve", "El movimiento se dará por válido con esta decisión como respaldo.")),
            CaseType.SWAP_INCOMPLETE, List.of(
                    negativeRule("rechaz|reject|anul|cancel", "El enroque quedará sin efecto."),
                    rule("conform|confirm|acept|aprob|aprueb|approve|complet", "El enroque quedará con la conformidad de ambas partes.")),
            CaseType.CONTRACT_EXPIRING, List.of(
                    negativeRule("no renov|sin renov|finaliz|termina|\\bbaja\\b|\\bcese\\b|\\bend\\b", "Quedará registrado que el acta termina en su fecha de vencimiento."),
                    rule("reemplaz|replac", "Quedará registrado que el puesto se cubrirá con un reemplazo."),
                    rule("renov|renew", "Quedará registrada la renovación del acta.")),
            CaseType.AVAILABILITY_UNCONFIRMED, List.of(
                    negativeRule("descart|no disponible|no estara|cancel|reject|rechaz", "Quedará registrado que la persona no estará disponible."),
                    rule("posterg|reprogram|cambi|otra fecha|postpone|change", "Quedará registrado que la fecha de disponibilidad cambia."),
                    rule("confirm", "La disponibilidad quedará confirmada y podrá considerarse para cubrir necesidades.")),
            CaseType.TEMP_SUPPORT_EXPIRED, List.of(
                    rule("extend|extien|ampli|prorrog|prolong", "Quedará registrado que el apoyo temporal continúa."),
                    rule("movimiento|definitiv|permanent|\\bmove\\b", "Quedará registrado que el paso a la tribu anfitriona es definitivo."),
                    rule("regres|retorn|volver|vuelve|return|finaliz|termina|\\bend\\b", "Quedará registrado que la persona debe volver a su tribu de origen.")));

    public CaseSummary render(CaseSummaryRequest request) {
        CaseType type = CaseType.fromText(request.type()).orElse(null);
        String body = type == null ? null : switch (type) {
            case BUDGET_ASSIGNMENT_MISMATCH -> budgetAssignmentMismatch(request);
            case MOVEMENT_NOT_REFLECTED -> movementNotReflected(request);
            case MANAGER_MISMATCH -> managerMismatch(request);
            case MOVEMENT_WITHOUT_EVIDENCE -> movementWithoutEvidence(request);
            case SWAP_INCOMPLETE -> swapIncomplete(request);
            case CONTRACT_EXPIRING -> contractExpiring(request);
            case AVAILABILITY_UNCONFIRMED -> availabilityUnconfirmed(request);
            case TEMP_SUPPORT_EXPIRED -> tempSupportExpired(request);
        };
        // Tipo desconocido, o faltan las afirmaciones que la plantilla necesita: plantilla genérica.
        StringBuilder summary = new StringBuilder(body != null ? body : generic(request));
        appendEvidence(summary, request);
        String dueDate = SpanishDates.format(request.dueDate());
        if (dueDate != null) {
            summary.append(" Fecha límite para decidir: ").append(dueDate).append('.');
        }

        List<CaseSummary.OptionText> options = request.options().stream()
                .map(option -> new CaseSummary.OptionText(option.id(), optionText(type, option)))
                .toList();
        return new CaseSummary(request.caseId(), summary.toString(), options, false, Mode.RECORDED);
    }

    // ---- Plantillas por tipo ------------------------------------------------------------------

    private static String budgetAssignmentMismatch(CaseSummaryRequest request) {
        Claim assignment = claim(request, "OPERATIONAL_ASSIGNMENT");
        Claim budget = claim(request, "BUDGET_ORIGIN");
        if (assignment == null || budget == null) {
            return null;
        }
        String squad = assignment.attribute("squad");
        return person(request) + " trabaja en la tribu " + assignment.value()
                + (squad == null ? "" : " (" + squad + ")") + cite(assignment)
                + ", pero su presupuesto corresponde a la tribu " + budget.value() + cite(budget)
                + ". No hay un apoyo temporal vigente que explique la diferencia.";
    }

    private static String movementNotReflected(CaseSummaryRequest request) {
        Claim movement = claim(request, "MOVEMENT");
        if (movement == null) {
            return null;
        }
        Claim membership = claim(request, "ORG_MEMBERSHIP");
        String since = SpanishDates.format(movement.validFrom());
        String text = "El movimiento " + movement.value() + " de " + person(request) + tribes(movement)
                + " está aprobado" + (since == null ? "" : " y rige desde el " + since) + cite(movement);
        if (membership != null) {
            text += ", pero " + source(membership) + " todavía registra a la persona en la tribu "
                    + membership.value() + consulted(membership);
        } else {
            text += ", pero SIGA todavía no lo refleja";
        }
        return text + ".";
    }

    private static String managerMismatch(CaseSummaryRequest request) {
        List<Claim> managers = claims(request, "MANAGEMENT_RESPONSIBILITY");
        if (managers.size() < 2) {
            return null;
        }
        Claim first = managers.get(0);
        Claim second = managers.stream()
                .filter(c -> !Objects.equals(c.value(), first.value())).findFirst().orElse(managers.get(1));
        return "Las fuentes no coinciden sobre quién es responsable de " + person(request) + ": "
                + source(first) + " indica a " + manager(first) + consulted(first) + " y "
                + source(second) + " indica a " + manager(second) + consulted(second) + ".";
    }

    private static String movementWithoutEvidence(CaseSummaryRequest request) {
        Claim movement = claim(request, "MOVEMENT");
        if (movement == null) {
            return null;
        }
        String since = SpanishDates.format(movement.validFrom());
        return "El movimiento " + movement.value() + " de " + person(request) + tribes(movement)
                + " figura como aprobado" + cite(movement)
                + (since == null ? "" : ", con vigencia desde el " + since)
                + ", pero no se encontró un correo de aprobación que lo respalde.";
    }

    private static String swapIncomplete(CaseSummaryRequest request) {
        Claim movement = claim(request, "MOVEMENT");
        if (movement == null) {
            return null;
        }
        String counterpartName = movement.attribute("counterpartName");
        String counterpartId = movement.attribute("counterpartId");
        String counterpart = counterpartName != null && counterpartId != null
                ? counterpartName + " (" + counterpartId + ")"
                : counterpartName != null ? counterpartName : counterpartId;
        String since = SpanishDates.format(movement.validFrom());
        return "El enroque " + movement.value()
                + (counterpart == null ? " de " + person(request) : " entre " + person(request) + " y " + counterpart)
                + (since == null ? "" : ", previsto desde el " + since + ",")
                + " tiene la conformidad de una sola de las partes" + cite(movement)
                + ". Falta la conformidad de la otra parte para completarlo.";
    }

    private static String contractExpiring(CaseSummaryRequest request) {
        Claim contract = claim(request, "CONTRACT");
        if (contract == null) {
            return null;
        }
        String provider = contract.attribute("provider");
        String until = SpanishDates.format(contract.validUntil());
        return "El acta " + contract.value() + " de " + person(request)
                + (provider == null ? "" : " con el proveedor " + provider)
                + (until == null ? " está por vencer o ya venció" : " vence el " + until)
                + cite(contract)
                + " y no se encontró una renovación ni un reemplazo registrados.";
    }

    private static String availabilityUnconfirmed(CaseSummaryRequest request) {
        Claim availability = claim(request, "AVAILABILITY");
        if (availability == null) {
            return null;
        }
        String since = SpanishDates.format(availability.validFrom());
        String reason = availability.attribute("reason");
        return person(request) + " quedaría disponible"
                + (since == null ? " próximamente" : " desde el " + since)
                + cite(availability)
                + (reason == null ? "" : ". Motivo registrado: " + reason)
                + ". Esa disponibilidad está prevista pero todavía no está confirmada.";
    }

    private static String tempSupportExpired(CaseSummaryRequest request) {
        Claim support = claim(request, "TEMP_SUPPORT");
        if (support == null) {
            return null;
        }
        Claim assignment = claim(request, "OPERATIONAL_ASSIGNMENT");
        String until = SpanishDates.format(support.validUntil());
        String text = "El apoyo temporal de " + person(request) + " a la tribu " + support.value()
                + (until == null ? " ya venció" : " venció el " + until) + cite(support);
        if (assignment != null) {
            text += ", pero " + source(assignment) + " todavía registra a la persona trabajando en la tribu "
                    + assignment.value() + consulted(assignment);
        } else {
            text += ", pero la persona sigue asignada a esa tribu";
        }
        return text + ".";
    }

    private static String generic(CaseSummaryRequest request) {
        String title = TextNormalizer.blankToNull(request.title());
        StringBuilder text = new StringBuilder(title != null ? title : "Caso " + request.caseId());
        if (!text.toString().endsWith(".")) {
            text.append(" sobre ").append(person(request)).append('.');
        }
        for (Claim claim : request.claims()) {
            if (claim.value() == null) {
                continue;
            }
            String subject = claim.subject() == null ? "el dato"
                    : SUBJECT_NAMES.getOrDefault(claim.subject(), claim.subject());
            String from = SpanishDates.format(claim.validFrom());
            String until = SpanishDates.format(claim.validUntil());
            text.append(' ').append(capitalize(source(claim))).append(" registra ").append(subject)
                    .append(": ").append(claim.value())
                    .append(from == null ? "" : ", desde el " + from)
                    .append(until == null ? "" : ", hasta el " + until)
                    .append(consulted(claim)).append('.');
        }
        if (request.claims().isEmpty()) {
            text.append(" No se recibieron afirmaciones para detallar el caso.");
        }
        return text.toString();
    }

    // ---- Opciones -----------------------------------------------------------------------------

    /** El texto de la opción es su etiqueta; se agrega una frase de consecuencia solo si la plantilla la conoce. */
    private static String optionText(CaseType type, CaseSummaryRequest.Option option) {
        String label = TextNormalizer.blankToNull(option.label());
        String text = label != null ? label : option.id();
        if (type == null) {
            return text;
        }
        String folded = TextNormalizer.fold(option.id() + " " + (label == null ? "" : label)).replace('_', ' ');
        boolean negated = NEGATION.matcher(folded).find();
        for (ConsequenceRule rule : CONSEQUENCES.getOrDefault(type, List.of())) {
            // Una opción negada ("No renovar") solo puede recibir una consecuencia pensada para negaciones.
            if ((!negated || rule.negative()) && rule.keywords().matcher(folded).find()) {
                return (text.matches(".*[.!?]$") ? text : text + ".") + " " + rule.consequence();
            }
        }
        return text;
    }

    // ---- Utilidades ---------------------------------------------------------------------------

    private static void appendEvidence(StringBuilder summary, CaseSummaryRequest request) {
        for (CaseSummaryRequest.EvidenceItem evidence : request.evidence()) {
            String quote = evidence == null ? null : TextNormalizer.blankToNull(evidence.quote());
            if (quote == null) {
                continue;
            }
            String approver = TextNormalizer.blankToNull(evidence.approver());
            summary.append(" Evidencia en correo")
                    .append(approver == null ? "" : " de " + approver)
                    .append(": «").append(quote).append("».");
        }
    }

    private static String person(CaseSummaryRequest request) {
        String name = TextNormalizer.blankToNull(request.personName());
        String id = TextNormalizer.blankToNull(request.personId());
        if (name != null && id != null) {
            return name + " (" + id + ")";
        }
        return name != null ? name : id != null ? id : "la persona";
    }

    private static Claim claim(CaseSummaryRequest request, String subject) {
        return request.claims().stream()
                .filter(c -> subject.equals(c.subject()) && TextNormalizer.blankToNull(c.value()) != null)
                .findFirst().orElse(null);
    }

    private static List<Claim> claims(CaseSummaryRequest request, String subject) {
        return request.claims().stream()
                .filter(c -> subject.equals(c.subject()) && TextNormalizer.blankToNull(c.value()) != null)
                .toList();
    }

    private static String source(Claim claim) {
        String source = TextNormalizer.blankToNull(claim.source());
        return source == null ? "una fuente no indicada" : SOURCE_NAMES.getOrDefault(source, source);
    }

    /** ", según SIGA (consultado el 13 de noviembre de 2026)". */
    private static String cite(Claim claim) {
        return ", según " + source(claim) + consulted(claim);
    }

    private static String consulted(Claim claim) {
        String observed = SpanishDates.format(claim.observedAt());
        return observed == null ? "" : " (consultado el " + observed + ")";
    }

    private static String tribes(Claim movement) {
        String from = movement.attribute("fromTribe");
        String to = movement.attribute("toTribe");
        if (from != null && to != null) {
            return " de la tribu " + from + " a la tribu " + to;
        }
        return to != null ? " a la tribu " + to : "";
    }

    private static String manager(Claim claim) {
        String name = claim.attribute("managerName");
        return name == null ? claim.value() : name + " (" + claim.value() + ")";
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
