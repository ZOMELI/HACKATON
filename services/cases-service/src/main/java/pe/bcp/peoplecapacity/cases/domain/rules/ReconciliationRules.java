package pe.bcp.peoplecapacity.cases.domain.rules;

import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.AVAILABILITY;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.BUDGET_ORIGIN;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.CONFIRMED;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.CONTRACT;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.MANAGEMENT_RESPONSIBILITY;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.MOVEMENT;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.OPERATIONAL_ASSIGNMENT;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.ORG_MEMBERSHIP;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.PLANNED;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.SRC_EXCEL_CHAPTER;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.SRC_SIGA;
import static pe.bcp.peoplecapacity.cases.domain.model.Subjects.TEMP_SUPPORT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;
import pe.bcp.peoplecapacity.cases.domain.model.CaseOption;
import pe.bcp.peoplecapacity.cases.domain.model.CaseType;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.ClaimIndex;
import pe.bcp.peoplecapacity.cases.domain.model.DetectedCase;
import pe.bcp.peoplecapacity.cases.domain.model.Evidence;
import pe.bcp.peoplecapacity.cases.domain.model.OptionEffect;

/**
 * Las ocho reglas de conciliación. Son deterministas: detectan y proponen opciones, nunca deciden.
 * Cada regla documenta su caso esperado y su caso negativo de esperado/casos_esperados.json.
 */
public final class ReconciliationRules {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClaimIndex idx;
    private final LocalDate today;
    private final AlertSettings settings;
    private final String defaultOwnerId;
    private final List<DetectedCase> out = new ArrayList<>();

    private ReconciliationRules(ClaimIndex idx, LocalDate today, AlertSettings settings, String defaultOwnerId) {
        this.idx = idx;
        this.today = today;
        this.settings = settings;
        this.defaultOwnerId = defaultOwnerId;
    }

    public static List<DetectedCase> detect(ClaimIndex idx, LocalDate today, AlertSettings settings, String defaultOwnerId) {
        ReconciliationRules rules = new ReconciliationRules(idx, today, settings, defaultOwnerId);
        for (String personId : idx.personIds()) {
            rules.budgetAssignmentMismatch(personId);
            rules.managerMismatch(personId);
            rules.movements(personId);
            rules.contractExpiring(personId);
            rules.availabilityUnconfirmed(personId);
            rules.tempSupportExpired(personId);
            rules.inventoryMismatch(personId);
            rules.unmatchedIdentity(personId);
        }
        return rules.out;
    }

    /**
     * 1. Trabaja (Jira) en una tribu distinta a la que lo financia (PREPER).
     * Negativo: existe un apoyo temporal hacia esa tribu (vigente: no hay caso; vencido: lo cubre la regla 8).
     */
    private void budgetAssignmentMismatch(String pid) {
        Claim budget = idx.latest(pid, BUDGET_ORIGIN, null);
        Claim assignment = idx.latest(pid, OPERATIONAL_ASSIGNMENT, null);
        if (budget == null || assignment == null || same(budget.value(), assignment.value())) {
            return;
        }
        boolean hasSupport = idx.of(pid, TEMP_SUPPORT).stream().anyMatch(s -> same(s.value(), assignment.value()));
        if (hasSupport) {
            return;
        }
        add(CaseType.BUDGET_ASSIGNMENT_MISMATCH, key("BAM", pid, budget.value(), assignment.value()),
                "Trabaja en " + assignment.value() + " pero lo financia " + budget.value(),
                pid, ownerOf(pid), today.plusDays(7), List.of(budget, assignment),
                List.of(
                        CaseOption.of("TEMP_SUPPORT", "Es un apoyo temporal: el presupuesto se mantiene en " + budget.value()),
                        CaseOption.of("REQUEST_BUDGET_TRANSFER", "Solicitar el traslado presupuestal a " + assignment.value()),
                        CaseOption.of("FIX_ASSIGNMENT", "La asignación en Jira es incorrecta: pedir su corrección")));
    }

    /**
     * 3. SIGA y el Excel de chapters nombran responsables distintos.
     * Decide quien está por encima del responsable que figura en SIGA.
     */
    private void managerMismatch(String pid) {
        Claim siga = idx.latest(pid, MANAGEMENT_RESPONSIBILITY, SRC_SIGA);
        Claim excel = idx.latest(pid, MANAGEMENT_RESPONSIBILITY, SRC_EXCEL_CHAPTER);
        if (siga == null || excel == null || same(siga.value(), excel.value())) {
            return;
        }
        String above = idx.managerOf(siga.value());
        String owner = above != null ? above : siga.value();
        add(CaseType.MANAGER_MISMATCH, key("MM", pid, siga.value(), excel.value()),
                "Dos responsables distintos: " + idx.nameOf(siga.value()) + " (SIGA) y " + idx.nameOf(excel.value()) + " (Excel de chapters)",
                pid, owner, today.plusDays(7), List.of(siga, excel),
                List.of(
                        CaseOption.of("CONFIRM_SIGA", "Confirmar a " + idx.nameOf(siga.value()) + " (SIGA)",
                                new OptionEffect(MANAGEMENT_RESPONSIBILITY, siga.value(), null, excel.id())),
                        CaseOption.of("CONFIRM_EXCEL", "Confirmar a " + idx.nameOf(excel.value()) + " (Excel de chapters)",
                                new OptionEffect(MANAGEMENT_RESPONSIBILITY, excel.value(), null, siga.id()))));
    }

    /** Reglas 2, 4 y 5: todas parten de una afirmación MOVEMENT. */
    private void movements(String pid) {
        for (Claim movement : idx.of(pid, MOVEMENT)) {
            if ("SWAP".equalsIgnoreCase(movement.attr("type"))) {
                swapIncomplete(pid, movement);
            } else if ("APPROVED".equalsIgnoreCase(movement.attr("status"))) {
                movementWithoutEvidence(pid, movement);
                movementNotReflected(pid, movement);
            }
        }
    }

    /**
     * 2. Movimiento aprobado y ya efectivo que SIGA aún no refleja.
     * Negativo: SIGA ya muestra la tribu destino, o la fecha efectiva es futura.
     */
    private void movementNotReflected(String pid, Claim movement) {
        Claim org = idx.latest(pid, ORG_MEMBERSHIP, SRC_SIGA);
        String toTribe = movement.attr("toTribe");
        if (org == null || toTribe == null || movement.validFrom() == null
                || movement.validFrom().isAfter(today) || same(org.value(), toTribe)) {
            return;
        }
        add(CaseType.MOVEMENT_NOT_REFLECTED, key("MNR", pid, movement.value()),
                "Movimiento " + movement.value() + " a " + toTribe + " efectivo desde " + fmt(movement.validFrom())
                        + ", pero SIGA sigue mostrando " + org.value(),
                pid, ownerOf(pid), today.plusDays(7), List.of(movement, org),
                List.of(
                        CaseOption.of("REQUEST_SIGA_UPDATE", "Solicitar la actualización de SIGA a " + toTribe),
                        CaseOption.of("MOVEMENT_NOT_EXECUTED", "El movimiento no se ejecutó: pedir su anulación")));
    }

    /**
     * 4. Movimiento aprobado sin correo de aprobación.
     * Negativo: la afirmación trae evidencia de tipo MOVEMENT_APPROVAL.
     */
    private void movementWithoutEvidence(String pid, Claim movement) {
        if (movement.hasEvidenceOf("MOVEMENT_APPROVAL")) {
            return;
        }
        LocalDate due = movement.validFrom() != null && movement.validFrom().isAfter(today)
                ? movement.validFrom() : today.plusDays(3);
        add(CaseType.MOVEMENT_WITHOUT_EVIDENCE, key("MWE", pid, movement.value()),
                "Movimiento " + movement.value() + " figura aprobado sin evidencia de aprobación",
                pid, ownerOf(pid), due, List.of(movement),
                List.of(
                        CaseOption.of("REQUEST_EVIDENCE", "Solicitar el correo de aprobación a quien aprobó"),
                        CaseOption.of("CONFIRM_APPROVAL", "Confirmo la aprobación bajo mi responsabilidad"),
                        CaseOption.of("REJECT_MOVEMENT", "El movimiento no está aprobado: pedir su anulación")));
    }

    /**
     * 5. Enroque con conformidad de una sola parte: falta la del responsable de esta persona.
     * Negativo: el responsable de la persona ya figura como quien dio conformidad.
     */
    private void swapIncomplete(String pid, Claim swap) {
        String owner = ownerOf(pid);
        boolean conformed = swap.evidence().stream()
                .anyMatch(e -> "SWAP_CONFORMITY".equals(e.type()) && owner.equals(e.approver()));
        if (conformed) {
            return;
        }
        List<Claim> claims = new ArrayList<>(List.of(swap));
        String counterpart = swap.attr("counterpartId");
        if (counterpart != null) {
            idx.of(counterpart, MOVEMENT).stream()
                    .filter(c -> same(c.value(), swap.value())).findFirst().ifPresent(claims::add);
        }
        LocalDate due = swap.validFrom() != null && swap.validFrom().isAfter(today) ? swap.validFrom() : today.plusDays(7);
        add(CaseType.SWAP_INCOMPLETE, key("SWI", pid, swap.value()),
                "Enroque " + swap.value() + (counterpart != null ? " con " + idx.nameOf(counterpart) : "")
                        + ": falta la conformidad de " + idx.nameOf(owner),
                pid, owner, due, claims,
                List.of(
                        CaseOption.of("GIVE_CONFORMITY", "Doy mi conformidad al enroque"),
                        CaseOption.of("REJECT_SWAP", "No doy mi conformidad al enroque")));
    }

    /**
     * 6. Acta vencida o por vencer dentro de los días de alerta, sin renovación ni reemplazo.
     * Negativo: hay correo de renovación o reemplazo, o vence fuera de la ventana.
     */
    private void contractExpiring(String pid) {
        for (Claim contract : idx.of(pid, CONTRACT)) {
            LocalDate until = contract.validUntil();
            if (until == null || until.isAfter(today.plusDays(settings.contractExpiryDays()))
                    || contract.hasEvidenceOf("CONTRACT_RENEWAL", "CONTRACT_REPLACEMENT")) {
                continue;
            }
            String verb = until.isBefore(today) ? " venció el " : " vence el ";
            add(CaseType.CONTRACT_EXPIRING, key("CEX", pid, contract.value(), until.toString()),
                    "El acta " + contract.value() + verb + fmt(until) + " sin renovación ni reemplazo",
                    pid, ownerOf(pid), until, List.of(contract),
                    List.of(
                            CaseOption.of("RENEW", "Renovar el acta"),
                            CaseOption.of("REPLACE", "Reemplazar a la persona en el acta"),
                            CaseOption.of("LET_EXPIRE", "No renovar: queda disponible desde el " + fmt(until.plusDays(1)),
                                    new OptionEffect(AVAILABILITY, CONFIRMED, until.plusDays(1), null))));
        }
    }

    /**
     * 7. Disponibilidad prevista dentro de los días de alerta y sin confirmar.
     * Negativo: ya hay una disponibilidad confirmada, o la fecha queda fuera de la ventana.
     */
    private void availabilityUnconfirmed(String pid) {
        List<Claim> availability = idx.of(pid, AVAILABILITY);
        if (availability.stream().anyMatch(c -> CONFIRMED.equalsIgnoreCase(c.value()))) {
            return;
        }
        for (Claim planned : availability) {
            LocalDate from = planned.validFrom();
            if (!PLANNED.equalsIgnoreCase(planned.value()) || from == null
                    || from.isAfter(today.plusDays(settings.availabilityDays()))) {
                continue;
            }
            add(CaseType.AVAILABILITY_UNCONFIRMED, availabilityKey(pid, from),
                    "Disponibilidad prevista desde el " + fmt(from) + " sin confirmar",
                    pid, ownerOf(pid), from, List.of(planned),
                    List.of(
                            CaseOption.of("CONFIRM_AVAILABLE", "Confirmar que estará disponible desde el " + fmt(from),
                                    new OptionEffect(AVAILABILITY, CONFIRMED, from, planned.id())),
                            CaseOption.of("NOT_AVAILABLE", "Continúa asignada: no estará disponible en esa fecha")));
        }
    }

    /**
     * 8. Apoyo temporal vencido y la persona sigue asignada a la tribu anfitriona.
     * Negativo: el apoyo sigue vigente, o existe otro apoyo vigente hacia la misma tribu.
     */
    private void tempSupportExpired(String pid) {
        Claim assignment = idx.latest(pid, OPERATIONAL_ASSIGNMENT, null);
        if (assignment == null) {
            return;
        }
        List<Claim> supports = idx.of(pid, TEMP_SUPPORT).stream().filter(s -> same(s.value(), assignment.value())).toList();
        boolean anyActive = supports.stream().anyMatch(s -> s.validUntil() == null || !s.validUntil().isBefore(today));
        if (supports.isEmpty() || anyActive) {
            return;
        }
        Claim support = supports.stream().max((a, b) -> a.validUntil().compareTo(b.validUntil())).orElseThrow();
        Claim budget = idx.latest(pid, BUDGET_ORIGIN, null);
        String origin = budget != null ? budget.value() : "su tribu de origen";
        List<Claim> claims = new ArrayList<>(List.of(support, assignment));
        if (budget != null) {
            claims.add(budget);
        }
        add(CaseType.TEMP_SUPPORT_EXPIRED, key("TSE", pid, support.value(), support.validUntil().toString()),
                "El apoyo temporal a " + support.value() + " venció el " + fmt(support.validUntil()) + " y sigue asignado allí",
                pid, ownerOf(pid), today.plusDays(3), claims,
                List.of(
                        CaseOption.of("EXTEND_SUPPORT", "Extender el apoyo temporal (requiere nuevo sustento)"),
                        CaseOption.of("RETURN_TO_ORIGIN", "Vuelve a " + origin + ": pedir la corrección de la asignación en Jira"),
                        CaseOption.of("FORMALIZE_MOVE", "Formalizar como movimiento definitivo a " + support.value())));
    }

    /**
     * 9. El inventario del CL ("Valida tu equipo") y SIGA ubican a la persona en tribus distintas.
     * Negativo: coinciden, o SIGA no informa tribu para su unidad.
     */
    private void inventoryMismatch(String pid) {
        Claim siga = idx.latest(pid, ORG_MEMBERSHIP, SRC_SIGA);
        Claim inventory = idx.latest(pid, ORG_MEMBERSHIP, SRC_EXCEL_CHAPTER);
        if (siga == null || inventory == null || siga.attr("unit") == null || inventory.value() == null || inventory.value().contains("=")
                || same(siga.value(), inventory.value())) {
            return;
        }
        List<Claim> claims = new ArrayList<>(List.of(inventory, siga));
        claims.addAll(idx.of(pid, MOVEMENT)); // un movimiento reciente suele explicar el desfase
        add(CaseType.INVENTORY_MISMATCH, key("INV", pid, siga.value(), inventory.value()),
                "El inventario dice " + inventory.value() + " y SIGA dice " + siga.value(),
                pid, ownerOf(pid), today.plusDays(7), claims,
                List.of(
                        CaseOption.of("CONFIRM_SIGA", "SIGA es correcto: actualizar el inventario a " + siga.value()),
                        CaseOption.of("CONFIRM_INVENTORY", "El inventario es correcto: solicitar la corrección de SIGA a " + inventory.value())));
    }

    /**
     * 10. Usuario con actividad en Jira que no enlaza con ninguna persona de SIGA ni del inventario.
     * No se le asigna identidad: alguien debe decir quién es.
     */
    private void unmatchedIdentity(String pid) {
        Claim assignment = idx.latest(pid, OPERATIONAL_ASSIGNMENT, null);
        if (assignment == null || !"true".equals(assignment.attr("unmatched"))) {
            return;
        }
        add(CaseType.UNMATCHED_IDENTITY, key("UNM", pid),
                "Usuario de Jira que no enlaza con ninguna persona: " + pid,
                pid, defaultOwnerId, today.plusDays(7), List.of(assignment),
                List.of(
                        CaseOption.of("REGISTER_PERSON", "Es personal de la CIO: pedir su registro en el inventario"),
                        CaseOption.of("NOT_STAFF", "No es personal de la CIO: pedir la baja del usuario en Jira")));
    }

    // ---- utilitarios ----

    public static String availabilityKey(String personId, LocalDate from) {
        return key("AVU", personId, from.toString());
    }

    private void add(CaseType type, String dedupeKey, String title, String pid, String ownerId, LocalDate due,
                     List<Claim> claims, List<CaseOption> options) {
        List<Evidence> evidence = new ArrayList<>();
        for (Claim c : claims) {
            for (Evidence e : c.evidence()) {
                if (!evidence.contains(e)) {
                    evidence.add(e);
                }
            }
        }
        out.add(new DetectedCase(type, dedupeKey, title, pid, idx.nameOf(pid), ownerId, idx.nameOf(ownerId),
                due, List.copyOf(claims), evidence, options));
    }

    private String ownerOf(String pid) {
        String manager = idx.managerOf(pid);
        return manager != null ? manager : defaultOwnerId;
    }

    private static String key(String prefix, String... parts) {
        return prefix + "|" + String.join("|", parts);
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private static String fmt(LocalDate date) {
        return date.format(DATE);
    }
}
