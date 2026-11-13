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

/** Lee SIGA: unidad organizacional, rol y responsable del personal del banco. No pide el grado salarial. */
@ApplicationScoped
public class SigaSourceReader implements SourceReaderPort {

    @Inject
    @RestClient
    MockSourcesClient sources;

    @Override
    public String sourceName() {
        return "SIGA";
    }

    @Override
    public List<SourceClaim> read() {
        List<SourceClaim> claims = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Map<String, String> tribeByUnit = new HashMap<>();
        for (Map<String, Object> unit : sources.sigaOrgUnits()) {
            tribeByUnit.put(text(unit.get("code")), text(unit.get("tribe")));
        }
        List<Map<String, Object>> employees = sources.sigaEmployees();
        Map<String, String> nameById = new HashMap<>();
        for (Map<String, Object> employee : employees) {
            nameById.put(text(employee.get("employeeId")), text(employee.get("fullName")));
        }
        for (Map<String, Object> employee : employees) {
            String id = String.valueOf(employee.get("employeeId"));
            String name = text(employee.get("fullName"));
            String email = text(employee.get("email"));
            // El valor lleva la tribu de la unidad para que las reglas comparen sin volver a SIGA.
            String unit = text(employee.get("orgUnitCode"));
            String tribe = tribeByUnit.get(unit);
            claims.add(claim(id, name, email, "ORG_MEMBERSHIP", (tribe == null ? "" : "tribe=" + tribe + ";") + "unit=" + unit, today));
            claims.add(claim(id, name, email, "ROLE", text(employee.get("role")), today));
            String managerId = text(employee.get("managerId"));
            if (managerId != null) {
                String managerName = nameById.get(managerId);
                claims.add(claim(id, name, email, "MANAGEMENT",
                        "manager=" + managerId + (managerName == null ? "" : ";managerName=" + managerName), today));
            }
        }
        return claims;
    }

    private static SourceClaim claim(String id, String name, String email, String dimension, String value,
            LocalDate today) {
        return new SourceClaim(id, name, email, dimension, value, "SIGA", null, null, today, "ASSERTED");
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
