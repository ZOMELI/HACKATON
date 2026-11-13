package pe.bcp.peoplecapacity.cases;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

/** Reglas contra un libro de ejemplo pequeño: cada regla con su caso esperado y su negativo. */
class ReconciliationRulesTest {

    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    static List<Claim> claims;
    static JsonNode expected;

    @BeforeAll
    static void load() throws Exception {
        claims = MAPPER.readValue(ReconciliationRulesTest.class.getResourceAsStream("/stub/claims.json"),
                new TypeReference<List<Claim>>() {
                });
        expected = MAPPER.readTree(ReconciliationRulesTest.class.getResourceAsStream("/stub/casos_esperados.json"));
    }

    private List<DetectedCase> detect(List<Claim> input, AlertSettings settings) {
        return ReconciliationRules.detect(new ClaimIndex(input),
                LocalDate.parse(expected.get("referenceDate").asText()), settings, "G40001");
    }

    @Test
    void detectsExactlyTheExpectedCases() {
        List<String> found = detect(claims, AlertSettings.defaults()).stream()
                .map(d -> d.type() + ":" + d.personId() + ":" + d.ownerId()).toList();
        List<String> wanted = new ArrayList<>();
        expected.get("expected").forEach(e ->
                wanted.add(e.get("type").asText() + ":" + e.get("personId").asText() + ":" + e.get("ownerId").asText()));
        assertThat(found).containsExactlyInAnyOrderElementsOf(wanted);
    }

    @Test
    void doesNotFlagNegatives() {
        List<String> flagged = detect(claims, AlertSettings.defaults()).stream().map(DetectedCase::personId).toList();
        expected.get("negatives").forEach(n -> assertThat(flagged).doesNotContain(n.get("personId").asText()));
    }

    @Test
    void everyCaseCarriesSourcedClaimsAndOptions() {
        for (DetectedCase d : detect(claims, AlertSettings.defaults())) {
            assertThat(d.claims()).isNotEmpty().allSatisfy(c -> {
                assertThat(c.source()).isNotBlank();
                assertThat(c.observedAt()).isNotNull();
            });
            assertThat(d.options()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(d.dueDate()).isNotNull();
        }
    }

    @Test
    void widerAvailabilityWindowFlagsFarAvailability() {
        List<DetectedCase> found = detect(claims, new AlertSettings(30, 90, null, null));
        assertThat(found).anyMatch(d -> d.personId().equals("U10014") && d.type().name().equals("AVAILABILITY_UNCONFIRMED"));
    }

    @Test
    void widerContractWindowFlagsOnlyContractsWithoutRenewal() {
        List<DetectedCase> found = detect(claims, new AlertSettings(200, 15, null, null));
        assertThat(found).anyMatch(d -> d.personId().equals("U10010") && d.type().name().equals("CONTRACT_EXPIRING"));
        assertThat(found).noneMatch(d -> d.personId().equals("U10011"));
    }
}
