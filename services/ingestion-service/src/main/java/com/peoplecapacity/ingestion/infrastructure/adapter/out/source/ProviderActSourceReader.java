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

/** Lee las actas de proveedores: quién financia a cada persona externa y hasta cuándo. */
@ApplicationScoped
public class ProviderActSourceReader implements SourceReaderPort {

    /** Un acta que vence dentro de este plazo anuncia una disponibilidad prevista (no confirmada). */
    private static final int PLANNED_WINDOW_DAYS = 90;

    @Inject
    SourceFiles files;

    @Override
    public String sourceName() {
        return "PROVIDER_ACT";
    }

    @Override
    public List<SourceClaim> read() {
        List<SourceClaim> claims = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Map<String, String> row : files.sheet("actas_proveedores.xlsx", 1)) {
            String id = row.get("codigo_persona");
            if (id == null) {
                continue; // acta en creación, todavía sin persona
            }
            String name = row.get("apellidos_nombres");
            String email = row.get("correo");
            LocalDate seen = date(row.get("fecha_actualizacion"));
            LocalDate from = date(row.get("vigencia_inicio"));
            LocalDate to = date(row.get("vigencia_fin"));
            claims.add(new SourceClaim(id, name, email, "FUNDING", kv("ref", row.get("id_acta"), "category",
                    row.get("categoria_presupuesto"), "tribe", row.get("tribu_financiadora"), "provider", row.get("proveedor"),
                    "status", row.get("estado")), "PROVIDER_ACT", from, to, seen, "ASSERTED"));
            if (row.get("matricula_cl") != null) {
                claims.add(new SourceClaim(id, name, email, "MANAGEMENT", kv("manager", row.get("matricula_cl"),
                        "managerName", row.get("cl_responsable")), "PROVIDER_ACT", null, null, seen, "ASSERTED"));
            }
            if (to != null && !to.isBefore(today) && !to.isAfter(today.plusDays(PLANNED_WINDOW_DAYS))) {
                claims.add(new SourceClaim(id, name, email, "AVAILABILITY", "PLANNED", "PROVIDER_ACT", to.plusDays(1),
                        null, seen, "ASSERTED"));
            }
        }
        return claims;
    }
}
