package com.peoplecapacity.ingestion.infrastructure.adapter.out.source;

import com.peoplecapacity.ingestion.application.port.out.SourceReaderPort;
import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import com.peoplecapacity.ingestion.infrastructure.adapter.out.client.MockSourcesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
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
        for (Map<String, Object> employee : sources.sigaEmployees()) {
            String id = String.valueOf(employee.get("employeeId"));
            String name = text(employee.get("fullName"));
            String email = text(employee.get("email"));
            claims.add(claim(id, name, email, "ORG_MEMBERSHIP", "unit=" + employee.get("orgUnitCode"), today));
            claims.add(claim(id, name, email, "ROLE", text(employee.get("role")), today));
            if (employee.get("managerId") != null) {
                claims.add(claim(id, name, email, "MANAGEMENT", text(employee.get("managerId")), today));
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
