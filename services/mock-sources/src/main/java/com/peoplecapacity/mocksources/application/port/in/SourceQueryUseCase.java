package com.peoplecapacity.mocksources.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Puerto de entrada: consultas de solo lectura sobre las fuentes simuladas. */
public interface SourceQueryUseCase {

    List<Map<String, Object>> jiraProjects();

    Map<String, Object> jiraIssues(String assignee, String project, String createdAfter, int startAt, int maxResults);

    List<Map<String, Object>> sigaOrgUnits();

    List<Map<String, Object>> sigaEmployees(String orgUnitCode, String managerId, String email);

    Optional<Map<String, Object>> sigaEmployee(String employeeId);

    /** Vacío si la persona no existe; GradeResult.denied() si quien consulta no tiene permiso. */
    Optional<GradeResult> sigaSalaryGrade(String employeeId, String requesterId);

    List<Map<String, Object>> preperBoxes(String employeeId, String status, String fundingTribe, String budgetCategory,
            String expiringBefore);

    Optional<Map<String, Object>> preperBox(String boxId);

    record GradeResult(boolean allowed, String salaryGrade) {
        public static GradeResult denied() {
            return new GradeResult(false, null);
        }
    }
}
