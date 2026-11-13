package pe.bcp.peoplecapacity.demand.infrastructure.ledger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.bcp.peoplecapacity.demand.application.port.out.LedgerPort;
import pe.bcp.peoplecapacity.demand.application.port.out.LedgerUnavailableException;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityClaim;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityStatus;

/** ledger.mode=http: consulta el ledger-service real (GET /claims?dimension=...) con timeouts cortos. */
@Component
@ConditionalOnProperty(name = "ledger.mode", havingValue = "http", matchIfMissing = true)
class HttpLedgerAdapter implements LedgerPort {

    private static final ParameterizedTypeReference<List<LedgerClaim>> CLAIM_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;
    private final String baseUrl;

    HttpLedgerAdapter(RestClient.Builder builder,
                      @Value("${ledger.url}") String baseUrl,
                      @Value("${ledger.connect-timeout-ms:1000}") long connectTimeoutMs,
                      @Value("${ledger.read-timeout-ms:2000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.baseUrl = baseUrl;
        this.client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public List<AvailabilityClaim> findCurrentAvailabilities() {
        try {
            List<LedgerClaim> availability = claims("AVAILABILITY");
            Map<String, String> roleByPerson = new HashMap<>();
            Map<String, String> nameByPerson = new HashMap<>();
            for (LedgerClaim role : claims("ROLE")) {
                roleByPerson.putIfAbsent(role.personId(), role.value());
                if (role.personName() != null) {
                    nameByPerson.putIfAbsent(role.personId(), role.personName());
                }
            }
            // El libro responde de la más reciente a la más antigua: vale la primera de cada persona.
            Set<String> seen = new HashSet<>();
            List<AvailabilityClaim> result = new ArrayList<>();
            for (LedgerClaim c : availability) {
                AvailabilityStatus status = status(c.value());
                if (c.personId() == null || status == null || !seen.add(c.personId())) {
                    continue;
                }
                result.add(new AvailabilityClaim(c.personId(),
                        c.personName() != null ? c.personName() : nameByPerson.get(c.personId()), roleByPerson.get(c.personId()), status,
                        c.validFrom(), c.validTo(), "CASE_DECISION".equals(c.source()) ? "DECISION" : c.source(),
                        c.observedAt() == null ? null : c.observedAt().atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            return result;
        } catch (RuntimeException e) {
            // Sin conexión, timeout, 4xx/5xx o cuerpo ilegible: para el tablero es lo mismo.
            throw new LedgerUnavailableException("No se pudo consultar el libro en " + baseUrl, e);
        }
    }

    private List<LedgerClaim> claims(String dimension) {
        List<LedgerClaim> claims = client.get().uri("/claims?dimension={d}", dimension).retrieve().body(CLAIM_LIST);
        return claims == null ? List.of() : claims;
    }

    private static AvailabilityStatus status(String value) {
        if (value == null) {
            return null;
        }
        // Una decisión puede llegar como "value=CONFIRMED;clave=valor".
        String raw = value.startsWith("value=") ? value.substring(6).split(";")[0] : value;
        try {
            return AvailabilityStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Afirmación tal como la publica el ledger-service real (services/ledger-service, openapi.yml). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record LedgerClaim(Long id, String personId, String personName, String dimension, String value, String source,
                       LocalDate validFrom, LocalDate validTo, LocalDate observedAt) {
    }
}
