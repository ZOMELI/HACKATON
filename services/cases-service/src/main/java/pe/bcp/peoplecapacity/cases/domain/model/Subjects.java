package pe.bcp.peoplecapacity.cases.domain.model;

/** Nombres de subject y source del contrato del libro. */
public final class Subjects {
    public static final String ORG_MEMBERSHIP = "ORG_MEMBERSHIP";
    public static final String MANAGEMENT_RESPONSIBILITY = "MANAGEMENT_RESPONSIBILITY";
    public static final String BUDGET_ORIGIN = "BUDGET_ORIGIN";
    public static final String OPERATIONAL_ASSIGNMENT = "OPERATIONAL_ASSIGNMENT";
    public static final String AVAILABILITY = "AVAILABILITY";
    public static final String CONTRACT = "CONTRACT";
    public static final String MOVEMENT = "MOVEMENT";
    public static final String TEMP_SUPPORT = "TEMP_SUPPORT";
    public static final String DECISION = "DECISION";

    public static final String SRC_SIGA = "SIGA";
    public static final String SRC_EXCEL_CHAPTER = "EXCEL_CHAPTER";
    public static final String SRC_DECISION = "DECISION";

    public static final String PLANNED = "PLANNED";
    public static final String CONFIRMED = "CONFIRMED";

    private Subjects() {
    }

    /** Etiqueta en español para textos de respaldo. */
    public static String label(String subject) {
        return switch (subject == null ? "" : subject) {
            case ORG_MEMBERSHIP -> "Pertenencia organizacional";
            case MANAGEMENT_RESPONSIBILITY -> "Responsable de gestión";
            case BUDGET_ORIGIN -> "Origen presupuestal";
            case OPERATIONAL_ASSIGNMENT -> "Asignación operativa";
            case AVAILABILITY -> "Disponibilidad";
            case CONTRACT -> "Acta";
            case MOVEMENT -> "Movimiento";
            case TEMP_SUPPORT -> "Apoyo temporal";
            case DECISION -> "Decisión";
            default -> subject;
        };
    }
}
