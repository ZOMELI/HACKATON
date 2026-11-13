package com.peoplecapacity.ingestion.infrastructure.adapter.out.source;

import static com.peoplecapacity.ingestion.infrastructure.adapter.out.source.SourceFiles.date;
import static com.peoplecapacity.ingestion.infrastructure.adapter.out.source.SourceFiles.kv;

import com.peoplecapacity.ingestion.application.port.out.SourceReaderPort;
import com.peoplecapacity.ingestion.domain.model.SourceClaim;
import com.peoplecapacity.ingestion.infrastructure.adapter.out.client.AgentsClient;
import com.peoplecapacity.ingestion.infrastructure.adapter.out.client.DemandClient;
import com.peoplecapacity.ingestion.infrastructure.adapter.out.client.MockSourcesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/** Lee MoviPer: movimientos de personal con su estado y la evidencia que se adjuntó (o no). */
@ApplicationScoped
public class MoviperSourceReader implements SourceReaderPort {

    @Inject
    SourceFiles files;

    @Override
    public String sourceName() {
        return "MOVIPER";
    }

    @Override
    public List<SourceClaim> read() {
        List<SourceClaim> claims = new ArrayList<>();
        for (Map<String, String> row : files.sheet("moviper.xlsx", 1)) {
            if (row.get("matricula") == null) {
                continue;
            }
            String value = kv("ref", row.get("id_movimiento"), "kind", row.get("tipo_movimiento"), "from",
                    row.get("valor_anterior"), "to", row.get("valor_nuevo"), "status", row.get("estado"), "requester",
                    row.get("solicitante"), "requiredApprover", row.get("aprobador_requerido"), "evidence",
                    row.get("evidencia"));
            claims.add(new SourceClaim(row.get("matricula"), row.get("apellidos_nombres"), null, "EVIDENCE", value,
                    "MOVIPER", date(row.get("fecha_efectiva")), null, date(row.get("fecha_solicitud")), "ASSERTED"));
        }
        return claims;
    }
}
