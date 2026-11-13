package pe.bcp.peoplecapacity.cases.infrastructure.agents;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.bcp.peoplecapacity.cases.application.port.out.CaseSummaryPort;
import pe.bcp.peoplecapacity.cases.domain.model.CaseFile;

/** Cliente del agente de expediente según contracts/agents.yaml. Cualquier fallo devuelve vacío. */
@Component
public class HttpCaseSummaryAdapter implements CaseSummaryPort {

    private static final Logger log = LoggerFactory.getLogger(HttpCaseSummaryAdapter.class);

    private final RestClient client;
    private final boolean enabled;

    public HttpCaseSummaryAdapter(RestClient.Builder builder, @Value("${agents.url}") String url,
                                  @Value("${agents.enabled:true}") boolean enabled) {
        this.client = builder.baseUrl(url)
                .requestFactory(pe.bcp.peoplecapacity.cases.infrastructure.config.AppConfig.requestFactory(Duration.ofSeconds(2), Duration.ofSeconds(30)))
                .build();
        this.enabled = enabled;
    }

    @Override
    public Optional<Drafted> draft(CaseFile c) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            Response r = client.post().uri("/case-summaries").body(request(c)).retrieve().body(Response.class);
            if (r == null || r.options() == null) {
                return Optional.empty();
            }
            Map<String, String> texts = new LinkedHashMap<>();
            r.options().forEach(o -> texts.put(o.id(), o.text()));
            return Optional.of(new Drafted(r.summary(), texts, r.aiGenerated(), r.mode()));
        } catch (RestClientException e) {
            log.warn("agents-service no respondió para el caso {}: {}", c.id(), e.getMessage());
            return Optional.empty();
        }
    }

    private static Map<String, Object> request(CaseFile c) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("caseId", c.id());
        body.put("type", c.type().name());
        body.put("title", c.title());
        body.put("personId", c.personId());
        body.put("personName", c.personName());
        body.put("dueDate", c.dueDate());
        body.put("claims", c.claims().stream().map(claim -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subject", claim.subject());
            m.put("value", claim.value());
            m.put("attributes", claim.attributes());
            m.put("source", claim.source());
            m.put("observedAt", claim.observedAt());
            m.put("validFrom", claim.validFrom());
            m.put("validUntil", claim.validUntil());
            return m;
        }).toList());
        body.put("evidence", c.evidence().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", e.type());
            m.put("quote", e.quote());
            m.put("approver", e.approver());
            return m;
        }).toList());
        body.put("options", c.options().stream().map(o -> Map.of("id", o.id(), "label", o.label())).toList());
        return body;
    }

    record Response(String caseId, String summary, List<OptionText> options, boolean aiGenerated, String mode) {
    }

    record OptionText(String id, String text) {
    }
}
