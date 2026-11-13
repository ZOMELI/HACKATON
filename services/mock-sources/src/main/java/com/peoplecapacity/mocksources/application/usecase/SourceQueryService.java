package com.peoplecapacity.mocksources.application.usecase;

import com.peoplecapacity.mocksources.application.port.in.SourceQueryUseCase;
import com.peoplecapacity.mocksources.application.port.out.SourceDataPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class SourceQueryService implements SourceQueryUseCase {

    private static final Set<String> GRADE_ROLES = Set.of("CAL", "Gerente");

    private final SourceDataPort data;

    @Inject
    public SourceQueryService(SourceDataPort data) {
        this.data = data;
    }

    @Override
    public List<Map<String, Object>> jiraProjects() {
        return data.jiraProjects();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> jiraIssues(String assignee, String project, String createdAfter, int startAt,
            int maxResults) {
        List<Map<String, Object>> filtered = data.jiraIssues().stream().filter(issue -> {
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            Map<String, Object> user = (Map<String, Object>) fields.get("assignee");
            Map<String, Object> proj = (Map<String, Object>) fields.get("project");
            String created = String.valueOf(fields.get("created"));
            return (assignee == null || assignee.equalsIgnoreCase(String.valueOf(user.get("emailAddress"))))
                    && (project == null || project.equalsIgnoreCase(String.valueOf(proj.get("key"))))
                    && (createdAfter == null || created.substring(0, 10).compareTo(createdAfter) >= 0);
        }).toList();
        int from = Math.min(Math.max(startAt, 0), filtered.size());
        int to = Math.min(from + Math.max(maxResults, 1), filtered.size());
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("startAt", from);
        page.put("maxResults", maxResults);
        page.put("total", filtered.size());
        page.put("issues", filtered.subList(from, to));
        return page;
    }

    @Override
    public List<Map<String, Object>> sigaOrgUnits() {
        return data.sigaOrgUnits();
    }

    @Override
    public List<Map<String, Object>> sigaEmployees(String orgUnitCode, String managerId, String email) {
        return data.sigaEmployees().stream()
                .filter(e -> orgUnitCode == null || orgUnitCode.equals(e.get("orgUnitCode")))
                .filter(e -> managerId == null || managerId.equals(e.get("managerId")))
                .filter(e -> email == null || email.equalsIgnoreCase(String.valueOf(e.get("email"))))
                .map(SourceQueryService::withoutGrade)
                .toList();
    }

    @Override
    public Optional<Map<String, Object>> sigaEmployee(String employeeId) {
        return rawEmployee(employeeId).map(SourceQueryService::withoutGrade);
    }

    @Override
    public Optional<GradeResult> sigaSalaryGrade(String employeeId, String requesterId) {
        Optional<Map<String, Object>> target = rawEmployee(employeeId);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        Optional<Map<String, Object>> requester = rawEmployee(requesterId);
        boolean roleAllowed = requester.map(r -> GRADE_ROLES.contains(String.valueOf(r.get("role")))).orElse(false);
        if (!roleAllowed || !isInHierarchy(target.get(), requesterId)) {
            return Optional.of(GradeResult.denied());
        }
        return Optional.of(new GradeResult(true, String.valueOf(target.get().get("salaryGrade"))));
    }

    @Override
    public List<Map<String, Object>> preperBoxes(String employeeId, String status, String fundingTribe,
            String budgetCategory, String expiringBefore) {
        return data.preperBoxes().stream()
                .filter(b -> employeeId == null || employeeId.equals(b.get("employeeId")))
                .filter(b -> status == null || status.equalsIgnoreCase(String.valueOf(b.get("status"))))
                .filter(b -> fundingTribe == null || fundingTribe.equalsIgnoreCase(String.valueOf(b.get("fundingTribe"))))
                .filter(b -> budgetCategory == null
                        || budgetCategory.equalsIgnoreCase(String.valueOf(b.get("budgetCategory"))))
                .filter(b -> expiringBefore == null
                        || (b.get("validTo") != null && String.valueOf(b.get("validTo")).compareTo(expiringBefore) <= 0))
                .toList();
    }

    @Override
    public Optional<Map<String, Object>> preperBox(String boxId) {
        return data.preperBoxes().stream().filter(b -> Objects.equals(boxId, b.get("boxId"))).findFirst();
    }

    private Optional<Map<String, Object>> rawEmployee(String employeeId) {
        return data.sigaEmployees().stream().filter(e -> Objects.equals(employeeId, e.get("employeeId"))).findFirst();
    }

    /** Sube por la cadena de responsables de la persona hasta encontrar a quien consulta. */
    private boolean isInHierarchy(Map<String, Object> target, String requesterId) {
        Object managerId = target.get("managerId");
        for (int level = 0; level < 6 && managerId != null; level++) {
            if (managerId.equals(requesterId)) {
                return true;
            }
            managerId = rawEmployee(String.valueOf(managerId)).map(m -> m.get("managerId")).orElse(null);
        }
        return false;
    }

    private static Map<String, Object> withoutGrade(Map<String, Object> employee) {
        Map<String, Object> copy = new HashMap<>(employee);
        copy.remove("salaryGrade");
        return copy;
    }
}
