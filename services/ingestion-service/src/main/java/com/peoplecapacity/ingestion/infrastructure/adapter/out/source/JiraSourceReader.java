package com.peoplecapacity.ingestion.infrastructure.adapter.out.source;

import com.peoplecapacity.ingestion.application.port.out.SourceReaderPort;
import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import com.peoplecapacity.ingestion.infrastructure.adapter.out.client.MockSourcesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Lee Jira: en qué squad trabaja hoy cada persona, según su issue más reciente. Jira identifica por correo;
 * el puente a la matrícula es el correo de SIGA. Quien no está en SIGA (proveedores) se omite.
 */
@ApplicationScoped
public class JiraSourceReader implements SourceReaderPort {

    @Inject
    @RestClient
    MockSourcesClient sources;
    @Inject
    SourceFiles files;

    @Override
    public String sourceName() {
        return "JIRA";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SourceClaim> read() {
        Map<String, Map<String, Object>> employeeByEmail = new HashMap<>();
        for (Map<String, Object> employee : sources.sigaEmployees()) {
            if (employee.get("email") != null) {
                employeeByEmail.put(employee.get("email").toString().toLowerCase(), employee);
            }
        }
        // Los proveedores no están en SIGA: su correo y código salen del inventario.
        for (Map<String, String> row : files.sheet("valida_tu_equipo.xlsx", 1)) {
            if (row.get("correo") != null && row.get("matricula") != null) {
                employeeByEmail.putIfAbsent(row.get("correo").toLowerCase(),
                        Map.of("employeeId", row.get("matricula"), "fullName", String.valueOf(row.get("apellidos_nombres"))));
            }
        }
        // Un reemplazo reciente puede estar ya en el acta y todavía no en el inventario.
        for (Map<String, String> row : files.sheet("actas_proveedores.xlsx", 1)) {
            if (row.get("correo") != null && row.get("codigo_persona") != null) {
                employeeByEmail.putIfAbsent(row.get("correo").toLowerCase(),
                        Map.of("employeeId", row.get("codigo_persona"), "fullName", String.valueOf(row.get("apellidos_nombres"))));
            }
        }
        Map<String, Map<String, Object>> projectByKey = new HashMap<>();
        for (Map<String, Object> project : sources.jiraProjects()) {
            projectByKey.put(String.valueOf(project.get("key")), project);
        }
        // Issue más reciente (por fecha de actualización) de cada persona.
        Map<String, Map<String, Object>> latestByEmail = new HashMap<>();
        Object issues = sources.jiraIssues(0, 100000).get("issues");
        for (Map<String, Object> issue : (List<Map<String, Object>>) issues) {
            Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
            if (assignee == null || assignee.get("emailAddress") == null) {
                continue;
            }
            String email = assignee.get("emailAddress").toString().toLowerCase();
            Map<String, Object> current = latestByEmail.get(email);
            if (current == null || updated(fields).compareTo(updated(current)) > 0) {
                latestByEmail.put(email, fields);
            }
        }
        List<SourceClaim> claims = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : latestByEmail.entrySet()) {
            Map<String, Object> employee = employeeByEmail.get(entry.getKey());
            boolean unmatched = employee == null;
            if (unmatched) {
                // No se inventa una identidad: se registra con su correo y las reglas abren un caso.
                Object name = ((Map<String, Object>) entry.getValue().get("assignee")).get("displayName");
                employee = Map.of("employeeId", entry.getKey(), "fullName", String.valueOf(name == null ? entry.getKey() : name));
            }
            Map<String, Object> fields = entry.getValue();
            String key = String.valueOf(((Map<String, Object>) fields.get("project")).get("key"));
            Map<String, Object> project = projectByKey.getOrDefault(key, Map.of());
            Object category = project.get("projectCategory") instanceof Map<?, ?> c ? c.get("name") : null;
            StringBuilder value = new StringBuilder();
            append(value, "tribe", project.get("tribe"));
            append(value, "squad", project.get("squad"));
            append(value, "category", category);
            append(value, "project", fields.get("customfield_proyecto"));
            append(value, "key", key);
            if (unmatched) {
                append(value, "unmatched", "true");
            }
            claims.add(new SourceClaim(String.valueOf(employee.get("employeeId")), String.valueOf(employee.get("fullName")),
                    entry.getKey(), "OPERATIONAL_ASSIGNMENT", value.toString(), "JIRA", null, null,
                    LocalDate.parse(updated(fields).substring(0, 10)), "ASSERTED"));
        }
        return claims;
    }

    private static String updated(Map<String, Object> fields) {
        Object value = fields.get("updated") != null ? fields.get("updated") : fields.get("created");
        return String.valueOf(value);
    }

    private static void append(StringBuilder out, String key, Object value) {
        if (value != null) {
            out.append(out.length() == 0 ? "" : ";").append(key).append('=').append(value.toString().replace(';', ','));
        }
    }
}
