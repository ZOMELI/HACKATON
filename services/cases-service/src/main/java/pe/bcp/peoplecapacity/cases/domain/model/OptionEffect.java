package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.LocalDate;

/**
 * Afirmación adicional que se registra en el libro si el responsable elige la opción.
 * Solo existe cuando el efecto es inequívoco; nunca se aplica sin la decisión de una persona.
 */
public record OptionEffect(String subject, String value, LocalDate validFrom, String supersedesClaimId) {
}
