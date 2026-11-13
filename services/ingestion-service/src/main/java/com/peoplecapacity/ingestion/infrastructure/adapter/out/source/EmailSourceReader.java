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
 * Lee los correos: cada uno pasa por el agente de evidencia (agents-service), que devuelve el tipo, las matrículas,
 * el movimiento o acta y la cita textual. Aquí solo se adjunta como evidencia; nunca cambia otra dimensión.
 * Una necesidad informal se entrega a demand-service.
 */
@ApplicationScoped
public class EmailSourceReader implements SourceReaderPort {

    private static final Pattern PERSON_ID = Pattern.compile("[BP]\\d{5}");
    private static final Pattern SENDER = Pattern.compile("(?m)^De:\\s*(.*?)\\s*<([^>]+)>");

    @Inject
    SourceFiles files;
    @Inject
    @RestClient
    AgentsClient agents;
    @Inject
    @RestClient
    DemandClient demand;
    @Inject
    @RestClient
    MockSourcesClient sources;

    @Override
    public String sourceName() {
        return "EMAIL";
    }

    @Override
    public List<SourceClaim> read() {
        Map<String, String> idByEmail = new HashMap<>();
        for (Map<String, Object> e : sources.sigaEmployees()) {
            if (e.get("email") != null) {
                idByEmail.put(e.get("email").toString().toLowerCase(), String.valueOf(e.get("employeeId")));
            }
        }
        List<Map<String, Object>> projects = sources.jiraProjects();
        List<SourceClaim> claims = new ArrayList<>();
        for (Map.Entry<String, String> mail : files.emails().entrySet()) {
            String body = mail.getValue();
            if (!PERSON_ID.matcher(body).find() && !body.contains("@bancosimulado")) {
                continue; // correos del paquete anterior, con personas que no existen en estas fuentes
            }
            String subject = line(body, "Asunto");
            LocalDate sent = line(body, "Fecha") == null ? LocalDate.now() : LocalDate.parse(line(body, "Fecha").substring(0, 10));
            Matcher sender = SENDER.matcher(body);
            String senderName = sender.find() ? sender.group(1) : null;
            String senderId = senderName == null ? null : idByEmail.get(sender.group(2).toLowerCase());

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("emailId", mail.getKey());
            request.put("subject", subject);
            request.put("body", body);
            Map<String, Object> x = agents.extract(request);
            String type = String.valueOf(x.get("type"));
            if ("IRRELEVANT".equals(type)) {
                continue;
            }
            String quote = x.get("quote") == null ? null : x.get("quote").toString();
            if (quote != null && quote.startsWith("De:")) {
                quote = null; // el respaldo sin modelo devolvió el encabezado, no una cita
            }
            if (quote != null && quote.length() > 400) {
                quote = quote.substring(0, 400);
            }
            if ("INFORMAL_NEED".equals(type)) {
                deliverInformalNeed(mail.getKey(), subject, sent, quote, body, projects);
                continue;
            }
            // Squad anfitrión de un apoyo temporal: el primero que el correo menciona.
            String targetTribe = null;
            String targetSquad = null;
            int best = Integer.MAX_VALUE;
            for (Map<String, Object> p : projects) {
                String squad = String.valueOf(p.get("squad"));
                int at = body.indexOf("squad " + squad);
                if (at >= 0 && at < best) {
                    best = at;
                    targetSquad = squad;
                    targetTribe = p.get("tribe") == null ? null : p.get("tribe").toString();
                }
            }
            List<String> people = new ArrayList<>();
            if (x.get("personIds") instanceof List<?> ids) {
                for (Object id : ids) {
                    if (id != null && PERSON_ID.matcher(id.toString()).matches() && !people.contains(id.toString())) {
                        people.add(id.toString());
                    }
                }
            }
            for (String person : people) {
                String counterpart = people.stream().filter(p -> !p.equals(person)).findFirst().orElse(null);
                // El id completo del movimiento o acta se toma del texto: es literal y no depende del modelo.
                String value = kv("type", type, "email", mail.getKey(), "subject", subject,
                        "movement", first(body, "MOV-\\d+", str(x.get("movementId"))),
                        "contract", first(body, "ACT-\\d{4}-\\d+", str(x.get("contractId"))), "approver", senderId != null ? senderId : senderName,
                        "approverName", senderName, "counterpart", "SWAP_CONFORMITY".equals(type) ? counterpart : null,
                        "targetTribe", "TEMP_SUPPORT".equals(type) ? targetTribe : null,
                        "targetSquad", "TEMP_SUPPORT".equals(type) ? targetSquad : null,
                        "ai", String.valueOf(Boolean.TRUE.equals(x.get("aiGenerated"))), "quote", quote);
                claims.add(new SourceClaim(person, null, null, "EVIDENCE", value, "EMAIL", isoDate(x.get("effectiveFrom")),
                        isoDate(x.get("effectiveUntil")), sent, "ASSERTED"));
            }
        }
        return claims;
    }

    private void deliverInformalNeed(String emailId, String subject, LocalDate sent, String quote, String body,
            List<Map<String, Object>> projects) {
        Map<String, Object> need = new LinkedHashMap<>();
        need.put("externalId", emailId);
        need.put("role", "Perfil por definir (ver correo)");
        need.put("project", subject);
        for (Map<String, Object> p : projects) {
            if (body.contains("squad " + p.get("squad")) && p.get("tribe") != null) {
                need.put("tribe", p.get("tribe"));
                break;
            }
        }
        need.put("mechanism", "UNDEFINED");
        need.put("status", "OPEN");
        need.put("source", "EMAIL");
        need.put("observedAt", sent + "T00:00:00Z");
        if (quote != null) {
            need.put("evidenceQuote", quote);
        }
        demand.putNeeds(Map.of("needs", List.of(need)));
    }

    private static String line(String body, String header) {
        Matcher m = Pattern.compile("(?m)^" + header + ":\\s*(.+)$").matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }

    private static String first(String body, String regex, String fallback) {
        Matcher m = Pattern.compile(regex).matcher(body);
        return m.find() ? m.group() : fallback;
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    private static LocalDate isoDate(Object value) {
        try {
            return value == null ? null : LocalDate.parse(value.toString().substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
