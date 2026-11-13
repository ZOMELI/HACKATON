package pe.bcp.peoplecapacity.agents.domain.model;

import java.util.Optional;

/** Los ocho tipos de caso definidos en contracts/cases.yaml. */
public enum CaseType {
    /** Trabaja (Jira) en una tribu distinta a la que lo financia (PREPER) sin apoyo temporal vigente. */
    BUDGET_ASSIGNMENT_MISMATCH,
    /** Movimiento aprobado y efectivo que SIGA aún no refleja. */
    MOVEMENT_NOT_REFLECTED,
    /** SIGA y el Excel de chapters nombran responsables distintos. */
    MANAGER_MISMATCH,
    /** Movimiento aprobado sin correo de aprobación. */
    MOVEMENT_WITHOUT_EVIDENCE,
    /** Enroque con conformidad de una sola parte. */
    SWAP_INCOMPLETE,
    /** Acta por vencer (o vencida) sin renovación ni reemplazo. */
    CONTRACT_EXPIRING,
    /** Disponibilidad prevista próxima sin confirmar. */
    AVAILABILITY_UNCONFIRMED,
    /** Apoyo temporal vencido y la persona sigue asignada a la tribu anfitriona. */
    TEMP_SUPPORT_EXPIRED;

    public static Optional<CaseType> fromText(String text) {
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(text.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
