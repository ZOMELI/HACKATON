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

/**
 * Lee el portal de staffing y el seguimiento de Reclutamiento. No son afirmaciones sobre personas:
 * se entregan a demand-service (PUT /needs, upsert por id de necesidad).
 */
@ApplicationScoped
public class StaffingSourceReader implements SourceReaderPort {

    private static final Map<String, String> RECRUITMENT = Map.of("Entrevistas", "INTERVIEWS", "En búsqueda", "SOURCING",
            "Cubierta", "HIRED", "Oferta", "OFFER", "Cancelada", "CANCELLED", "No iniciado", "NOT_STARTED");

    @Inject
    SourceFiles files;
    @Inject
    @RestClient
    DemandClient demand;
    @Inject
    @RestClient
    MockSourcesClient sources;

    @Override
    public String sourceName() {
        return "STAFFING_PORTAL";
    }

    @Override
    public List<SourceClaim> read() {
        Map<String, Map<String, String>> tracking = new HashMap<>();
        for (Map<String, String> row : files.sheet("reclutamiento.xlsx", 1)) {
            tracking.put(row.get("id_necesidad"), row);
        }
        // Respaldo: la caja PREPER que la necesidad dice tener, verificada contra PREPER.
        Map<String, Map<String, Object>> boxes = new HashMap<>();
        for (Map<String, Object> box : sources.preperBoxes(null)) {
            boxes.put(String.valueOf(box.get("boxId")), box);
        }
        List<Map<String, Object>> needs = new ArrayList<>();
        for (Map<String, String> row : files.sheet("portal_staffing.xlsx", 1)) {
            if (row.get("id_necesidad") == null) {
                continue;
            }
            Map<String, Object> need = new LinkedHashMap<>();
            need.put("externalId", row.get("id_necesidad"));
            need.put("tribe", row.get("tribu"));
            need.put("project", row.get("squad") + " · " + row.get("proyecto"));
            need.put("role", row.get("perfil"));
            need.put("mechanism", "EXTERNAL_HIRE");
            need.put("neededFrom", String.valueOf(date(row.get("fecha_requerida"))));
            need.put("source", "STAFFING_PORTAL");
            need.put("observedAt", date(row.get("fecha_registro")) + "T00:00:00Z");
            String boxId = row.get("id_caja_preper");
            if (boxId != null) {
                Map<String, Object> box = boxes.get(boxId);
                need.put("note", box == null ? "La caja " + boxId + " no existe en PREPER"
                        : "Caja PREPER " + boxId + " · " + String.valueOf(box.get("status")).toLowerCase());
            }
            Map<String, String> track = tracking.get(row.get("id_necesidad"));
            String status = track == null ? null : RECRUITMENT.get(track.get("estado"));
            need.put("status", "HIRED".equals(status) ? "COVERED" : "OPEN");
            if (status != null) {
                Map<String, Object> recruitment = new LinkedHashMap<>();
                recruitment.put("status", status);
                recruitment.put("source", "EXCEL_RECRUITMENT");
                recruitment.put("observedAt", date(track.get("fecha_actualizacion")) + "T00:00:00Z");
                need.put("recruitment", recruitment);
            }
            needs.add(need);
        }
        if (!needs.isEmpty()) {
            demand.putNeeds(Map.of("needs", needs));
        }
        return List.of(); // nada va al libro: las necesidades viven en demand-service
    }
}
