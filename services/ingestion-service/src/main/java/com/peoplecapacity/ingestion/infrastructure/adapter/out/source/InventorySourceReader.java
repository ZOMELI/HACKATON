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

/** Lee "Valida tu equipo": el inventario que mantiene cada CL, con personal del banco y de proveedores. */
@ApplicationScoped
public class InventorySourceReader implements SourceReaderPort {

    @Inject
    SourceFiles files;

    @Override
    public String sourceName() {
        return "INVENTORY";
    }

    @Override
    public List<SourceClaim> read() {
        List<SourceClaim> claims = new ArrayList<>();
        for (Map<String, String> row : files.sheet("valida_tu_equipo.xlsx", 1)) {
            String id = row.get("matricula");
            if (id == null) {
                continue;
            }
            LocalDate seen = date(row.get("fecha_validacion"));
            String name = row.get("apellidos_nombres");
            String email = row.get("correo");
            claims.add(claim(id, name, email, "ORG_MEMBERSHIP", kv("tribe", row.get("tribu"), "unitName",
                    row.get("unidad_organizacional"), "chapter", row.get("chapter"), "squad", row.get("squad_declarado"),
                    "company", row.get("empresa")), seen));
            if (row.get("rol") != null) {
                claims.add(claim(id, name, email, "ROLE", row.get("rol"), seen));
            }
            if (row.get("matricula_cl") != null) {
                claims.add(claim(id, name, email, "MANAGEMENT",
                        kv("manager", row.get("matricula_cl"), "managerName", row.get("cl_responsable")), seen));
            }
        }
        return claims;
    }

    private static SourceClaim claim(String id, String name, String email, String dimension, String value, LocalDate seen) {
        return new SourceClaim(id, name, email, dimension, value, "INVENTORY", null, null, seen, "ASSERTED");
    }
}
