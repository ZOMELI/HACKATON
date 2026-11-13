package pe.bcp.peoplecapacity.cases.infrastructure.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.bcp.peoplecapacity.cases.application.LedgerUnavailableException;
import pe.bcp.peoplecapacity.cases.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.Evidence;
import pe.bcp.peoplecapacity.cases.domain.model.Subjects;
import pe.bcp.peoplecapacity.cases.infrastructure.config.AppConfig;

/**
 * Cliente del ledger-service real (services/ledger-service, openapi.yml). Traduce su modelo plano
 * (dimension, value codificado como "clave=valor;...") al modelo de afirmaciones de este servicio.
 */
@Component
@ConditionalOnProperty(name = "ledger.mode", havingValue = "http", matchIfMissing = true)
public class HttpLedgerAdapter implements LedgerPort {

    private static final String CASE_DECISION = "CASE_DECISION";
    private static final Map<String, String> SUBJECT_BY_DIMENSION = Map.of(
            "ORG_MEMBERSHIP", Subjects.ORG_MEMBERSHIP,
            "MANAGEMENT", Subjects.MANAGEMENT_RESPONSIBILITY,
            "FUNDING", Subjects.BUDGET_ORIGIN,
            "OPERATIONAL_ASSIGNMENT", Subjects.OPERATIONAL_ASSIGNMENT,
            "AVAILABILITY", Subjects.AVAILABILITY,
            "DECISION", Subjects.DECISION);
    private static final ParameterizedTypeReference<List<LedgerClaim>> CLAIM_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;

    public HttpLedgerAdapter(RestClient.Builder builder, @Value("${ledger.url}") String url) {
        this.client = builder.baseUrl(url)
                .requestFactory(AppConfig.requestFactory(Duration.ofSeconds(2), Duration.ofSeconds(10)))
                .build();
    }

    @Override
    public List<Claim> currentClaims() {
        List<LedgerClaim> all;
        try {
            all = client.get().uri("/claims").retrieve().body(CLAIM_LIST);
        } catch (RestClientException e) {
            throw new LedgerUnavailableException("El libro de afirmaciones no respondió", e);
        }
        return all == null ? List.of() : toCurrentClaims(all);
    }

    /** Traduce las afirmaciones del libro (de la más reciente a la más antigua) a las vigentes que usan las reglas. */
    static List<Claim> toCurrentClaims(List<LedgerClaim> all) {
        Map<String, String> roleByPerson = new HashMap<>();
        // Evidencia de correos, indexada por el movimiento o acta que respalda.
        Map<String, List<Evidence>> evidenceByRef = new HashMap<>();
        for (LedgerClaim c : all) {
            if ("ROLE".equals(c.dimension())) {
                roleByPerson.putIfAbsent(c.personId(), c.value());
            }
            if ("EVIDENCE".equals(c.dimension()) && "EMAIL".equals(c.source())) {
                Map<String, String> a = decode(c.value());
                for (String ref : new String[] {a.get("movement"), a.get("contract")}) {
                    if (ref != null) {
                        List<Evidence> list = evidenceByRef.computeIfAbsent(ref, k -> new ArrayList<>());
                        Evidence e = emailEvidence(c, a);
                        if (list.stream().noneMatch(x -> java.util.Objects.equals(x.emailId(), e.emailId()))) {
                            list.add(e);
                        }
                    }
                }
            }
        }
        // El libro responde de la más reciente a la más antigua: la vigente es la primera de cada persona, dimensión y fuente.
        Set<String> seen = new HashSet<>();
        List<Claim> current = new ArrayList<>();
        for (LedgerClaim c : all) {
            String role = roleByPerson.get(c.personId());
            if ("EVIDENCE".equals(c.dimension())) {
                if (seen.add(c.personId() + "|EVIDENCE|" + c.source() + "|" + c.value())) {
                    fromEvidence(c, role, evidenceByRef, current);
                }
                continue;
            }
            if (!SUBJECT_BY_DIMENSION.containsKey(c.dimension())) {
                continue;
            }
            boolean decision = "DECISION".equals(c.dimension());
            if (decision || seen.add(c.personId() + "|" + c.dimension() + "|" + c.source())) {
                Claim claim = toClaim(c, role);
                current.add(claim);
                if ("FUNDING".equals(c.dimension()) && "PROVIDER_ACT".equals(c.source()) && claim.attr("ref") != null) {
                    // El acta también es un contrato con vigencia: lo usa la regla de vencimiento.
                    current.add(new Claim(claim.id() + "-C", claim.personId(), claim.personName(), role, Subjects.CONTRACT,
                            claim.attr("ref"), claim.attributes(), claim.source(), claim.attr("ref"), claim.observedAt(),
                            claim.recordedAt(), claim.validFrom(), claim.validUntil(), null,
                            evidenceByRef.getOrDefault(claim.attr("ref"), List.of()), null));
                }
            }
        }
        return current;
    }

    /** Un registro de MoviPer es un movimiento; un correo de enroque o de apoyo temporal también genera su afirmación. */
    private static void fromEvidence(LedgerClaim c, String role, Map<String, List<Evidence>> evidenceByRef, List<Claim> out) {
        Map<String, String> a = decode(c.value());
        Instant observedAt = c.observedAt() == null ? null : c.observedAt().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant recordedAt = c.recordedAt() == null ? null : c.recordedAt().atZone(ZoneId.systemDefault()).toInstant();
        if ("MOVIPER".equals(c.source())) {
            String ref = a.get("ref");
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("type", "MOVE");
            attrs.put("status", "Aprobado".equalsIgnoreCase(a.get("status")) ? "APPROVED" : "REQUESTED");
            String kind = a.getOrDefault("kind", "");
            attrs.put("reason", kind);
            if (a.get("from") != null) {
                attrs.put("fromTribe", a.get("from").replace("Ingeniería ", ""));
            }
            if (a.get("to") != null && (kind.contains("unidad") || kind.contains("tribu"))) {
                attrs.put("toTribe", a.get("to").replace("Ingeniería ", ""));
            }
            List<Evidence> evidence = new ArrayList<>(evidenceByRef.getOrDefault(ref, List.of()));
            if (evidence.isEmpty() && a.get("evidence") != null) {
                evidence.add(new Evidence("MOVEMENT_APPROVAL", a.get("evidence"), "Evidencia adjunta en MoviPer: " + a.get("evidence"),
                        a.get("requiredApprover"), List.of(c.personId()), c.observedAt(), false));
            }
            out.add(new Claim(String.valueOf(c.id()), c.personId(), c.personName(), role, Subjects.MOVEMENT, ref, attrs,
                    "EXCEL_MOVEMENTS", ref, observedAt, recordedAt, c.validFrom(), null, null, evidence, null));
            return;
        }
        String type = a.get("type");
        if ("SWAP_CONFORMITY".equals(type)) {
            Map<String, String> attrs = new LinkedHashMap<>();
            attrs.put("type", "SWAP");
            attrs.put("status", "REQUESTED");
            if (a.get("counterpart") != null) {
                attrs.put("counterpartId", a.get("counterpart"));
            }
            out.add(new Claim(String.valueOf(c.id()), c.personId(), c.personName(), role, Subjects.MOVEMENT,
                    "sin registro en MoviPer (correo del " + c.observedAt() + ")", attrs, "EMAIL", a.get("email"), observedAt,
                    recordedAt, c.validFrom(), null, null, List.of(emailEvidence(c, a)), null));
        } else if ("TEMP_SUPPORT".equals(type) && a.get("targetTribe") != null) {
            Map<String, String> attrs = new LinkedHashMap<>();
            if (a.get("targetSquad") != null) {
                attrs.put("squad", a.get("targetSquad"));
            }
            out.add(new Claim(String.valueOf(c.id()), c.personId(), c.personName(), role, Subjects.TEMP_SUPPORT,
                    a.get("targetTribe"), attrs, "EMAIL", a.get("email"), observedAt, recordedAt, c.validFrom(), c.validTo(),
                    null, List.of(emailEvidence(c, a)), null));
        }
    }

    private static Evidence emailEvidence(LedgerClaim c, Map<String, String> a) {
        return new Evidence(a.get("type"), a.get("email"), a.get("quote"), a.get("approver"), List.of(c.personId()),
                c.observedAt(), "true".equals(a.get("ai")));
    }

    @Override
    public Claim record(Claim claim) {
        String dimension = dimensionOf(claim.subject());
        String value;
        if (dimension == null) {
            // El libro solo acepta decisiones y disponibilidad desde un caso: cualquier otro efecto se guarda como decisión.
            dimension = "DECISION";
            Map<String, String> effect = new LinkedHashMap<>();
            effect.put("effect", claim.subject());
            effect.putAll(claim.attributes());
            value = encode(claim.value(), effect);
        } else {
            Map<String, String> attributes = new LinkedHashMap<>(claim.attributes());
            attributes.remove("caseId"); // ya viaja en evidenceRef
            value = encode(claim.value(), attributes);
        }
        LocalDate observed = (claim.observedAt() == null ? Instant.now() : claim.observedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
        NewClaim body = new NewClaim(claim.personId(), claim.personName(), dimension, value, CASE_DECISION,
                claim.validFrom(), claim.validUntil(), observed, "ASSERTED", claim.sourceRef(), claim.recordedBy());
        try {
            BatchResult result = client.post().uri("/claims/batch").body(Map.of("claims", List.of(body))).retrieve()
                    .body(BatchResult.class);
            if (result == null || result.recorded() < 1) {
                String reason = result == null || result.rejected() == null || result.rejected().isEmpty()
                        ? "sin motivo" : String.valueOf(result.rejected().get(0).get("reason"));
                throw new LedgerUnavailableException("El libro rechazó la afirmación: " + reason, null);
            }
            String uri = "/claims?personId={p}&dimension={d}";
            List<LedgerClaim> stored = client.get().uri(uri, claim.personId(), dimension).retrieve().body(CLAIM_LIST);
            if (stored == null || stored.isEmpty()) {
                throw new LedgerUnavailableException("El libro no devolvió la afirmación registrada", null);
            }
            return toClaim(stored.get(0), claim.personRole());
        } catch (RestClientException e) {
            throw new LedgerUnavailableException("El libro de afirmaciones no registró la decisión", e);
        }
    }

    private static String dimensionOf(String subject) {
        if (Subjects.AVAILABILITY.equals(subject)) {
            return "AVAILABILITY";
        }
        return Subjects.DECISION.equals(subject) ? "DECISION" : null;
    }

    private static Claim toClaim(LedgerClaim c, String role) {
        Map<String, String> attributes = decode(c.value());
        String value = c.value();
        for (String key : List.of("value", "tribe", "manager", "unit")) {
            if (attributes.containsKey(key)) {
                value = attributes.remove(key);
                break;
            }
        }
        String source = CASE_DECISION.equals(c.source()) ? Subjects.SRC_DECISION
                : "INVENTORY".equals(c.source()) ? Subjects.SRC_EXCEL_CHAPTER : c.source();
        Instant observedAt = c.observedAt() == null ? null : c.observedAt().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant recordedAt = c.recordedAt() == null ? null : c.recordedAt().atZone(ZoneId.systemDefault()).toInstant();
        return new Claim(String.valueOf(c.id()), c.personId(), c.personName(), role,
                SUBJECT_BY_DIMENSION.getOrDefault(c.dimension(), c.dimension()), value, attributes, source,
                c.evidenceRef(), observedAt, recordedAt, c.validFrom(), c.validTo(), null, List.of(), c.author());
    }

    /** "a=1;b=2" → {a:1, b:2}. Un valor sin "=" no tiene atributos. */
    static Map<String, String> decode(String value) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (value == null || !value.contains("=")) {
            return attributes;
        }
        for (String part : value.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                attributes.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return attributes;
    }

    static String encode(String value, Map<String, String> attributes) {
        if (attributes.isEmpty()) {
            return value;
        }
        StringBuilder out = new StringBuilder("value=").append(clean(value));
        attributes.forEach((k, v) -> out.append(';').append(k).append('=').append(clean(v)));
        return out.length() > 1000 ? out.substring(0, 1000) : out.toString();
    }

    private static String clean(String text) {
        return text == null ? "" : text.replace(';', ',').replace('=', ':');
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LedgerClaim(Long id, String personId, String personName, String personEmail, String dimension, String value,
                       String source, LocalDate validFrom, LocalDate validTo, LocalDate observedAt, String status,
                       String evidenceRef, String author, LocalDateTime recordedAt) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record NewClaim(String personId, String personName, String dimension, String value, String source,
                    LocalDate validFrom, LocalDate validTo, LocalDate observedAt, String status, String evidenceRef,
                    String author) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BatchResult(int recorded, List<Map<String, Object>> rejected) {
    }
}
