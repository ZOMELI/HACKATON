package pe.bcp.peoplecapacity.cases.domain.model;

/** Tipos de caso (contracts/cases.yaml). */
public enum CaseType {
    BUDGET_ASSIGNMENT_MISMATCH,
    MOVEMENT_NOT_REFLECTED,
    MANAGER_MISMATCH,
    MOVEMENT_WITHOUT_EVIDENCE,
    SWAP_INCOMPLETE,
    CONTRACT_EXPIRING,
    AVAILABILITY_UNCONFIRMED,
    TEMP_SUPPORT_EXPIRED,
    /** El inventario del CL y SIGA ubican a la persona en tribus distintas. */
    INVENTORY_MISMATCH,
    /** Usuario con actividad en Jira que no enlaza con ninguna persona conocida. */
    UNMATCHED_IDENTITY
}
