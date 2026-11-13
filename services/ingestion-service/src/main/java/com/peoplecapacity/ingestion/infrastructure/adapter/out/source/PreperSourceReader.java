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

/** Lee PREPER: origen presupuestal del personal del banco. PREPER nunca se modifica, solo se consulta. */
@ApplicationScoped
public class PreperSourceReader implements SourceReaderPort {

    @Inject
    @RestClient
    MockSourcesClient sources;

    @Override
    public String sourceName() {
        return "PREPER";
    }

    @Override
    public List<SourceClaim> read() {
        List<SourceClaim> claims = new ArrayList<>();
        for (Map<String, Object> box : sources.preperBoxes("Ocupada")) {
            // El valor lleva la caja y sus atributos para que las reglas comparen categorías sin volver a PREPER.
            String value = "ref=" + box.get("boxId") + ";category=" + box.get("budgetCategory") + ";tribe="
                    + box.get("fundingTribe") + ";type=" + box.get("assignmentType");
            claims.add(new SourceClaim(String.valueOf(box.get("employeeId")), null, null, "FUNDING", value, "PREPER",
                    date(box.get("validFrom")), date(box.get("validTo")), LocalDate.now(), "ASSERTED"));
        }
        return claims;
    }

    private static LocalDate date(Object value) {
        return value == null ? null : LocalDate.parse(value.toString());
    }
}
