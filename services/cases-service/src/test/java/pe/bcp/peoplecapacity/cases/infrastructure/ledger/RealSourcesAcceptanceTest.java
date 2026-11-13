package pe.bcp.peoplecapacity.cases.infrastructure.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.bcp.peoplecapacity.cases.domain.model.AlertSettings;
import pe.bcp.peoplecapacity.cases.domain.model.Claim;
import pe.bcp.peoplecapacity.cases.domain.model.ClaimIndex;
import pe.bcp.peoplecapacity.cases.domain.model.DetectedCase;
import pe.bcp.peoplecapacity.cases.domain.rules.ReconciliationRules;

/**
 * Aceptación por casos con las fuentes reales: real/ledger_claims.json es lo que el libro guardó tras una
 * sincronización completa (SIGA, PREPER, Jira, los cinco Excel y los correos). El criterio es
 * esperado/casos_esperados.json, en la raíz del repositorio.
 */
class RealSourcesAcceptanceTest {

    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    static List<DetectedCase> detected;
    static JsonNode expected;

    @BeforeAll
    static void load() throws Exception {
        List<HttpLedgerAdapter.LedgerClaim> ledger = MAPPER.readValue(
                RealSourcesAcceptanceTest.class.getResourceAsStream("/real/ledger_claims.json"),
                new TypeReference<List<HttpLedgerAdapter.LedgerClaim>>() {
                });
        List<Claim> claims = HttpLedgerAdapter.toCurrentClaims(ledger);
        expected = MAPPER.readTree(Path.of("..", "..", "esperado", "casos_esperados.json").toFile());
        JsonNode s = expected.get("alertSettings");
        detected = ReconciliationRules.detect(new ClaimIndex(claims), LocalDate.parse(expected.get("referenceDate").asText()),
                new AlertSettings(s.get("contractExpiryDays").asInt(), s.get("availabilityDays").asInt(), null, null), "B10021");
    }

    @Test
    void detectsExactlyTheExpectedCases() {
        List<String> found = detected.stream().map(d -> d.type() + ":" + d.personId() + ":" + d.ownerId()).toList();
        List<String> wanted = new ArrayList<>();
        expected.get("expected").forEach(e ->
                wanted.add(e.get("type").asText() + ":" + e.get("personId").asText() + ":" + e.get("ownerId").asText()));
        assertThat(found).containsExactlyInAnyOrderElementsOf(wanted);
    }

    @Test
    void doesNotFlagNegatives() {
        List<String> found = detected.stream().map(d -> d.type() + ":" + d.personId()).toList();
        expected.get("negatives").forEach(n ->
                assertThat(found).doesNotContain(n.get("type").asText() + ":" + n.get("personId").asText()));
    }
}
