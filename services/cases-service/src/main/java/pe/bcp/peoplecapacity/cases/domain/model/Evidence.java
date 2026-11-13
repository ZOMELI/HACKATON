package pe.bcp.peoplecapacity.cases.domain.model;

import java.time.LocalDate;
import java.util.List;

/** Evidencia adjunta a una afirmación (normalmente extraída de un correo por el agente de evidencia). */
public record Evidence(String type, String emailId, String quote, String approver,
                       List<String> personIds, LocalDate date, Boolean aiGenerated) {
}
