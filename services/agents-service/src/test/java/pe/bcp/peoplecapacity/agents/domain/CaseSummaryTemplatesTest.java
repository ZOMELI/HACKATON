package pe.bcp.peoplecapacity.agents.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest.Claim;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest.EvidenceItem;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest.Option;
import pe.bcp.peoplecapacity.agents.domain.model.CaseType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.template.CaseSummaryTemplates;

class CaseSummaryTemplatesTest {

    private static final String OBSERVED = "2026-11-13T08:00:00Z";
    private static final String CONSULTED = "(consultado el 13 de noviembre de 2026)";
    private static final List<Option> OPTIONS = List.of(new Option("OPT_A", "Primera opción"),
            new Option("OPT_B", "Segunda opción"));

    private final CaseSummaryTemplates templates = new CaseSummaryTemplates();

    private static Claim claim(String subject, String value, Map<String, String> attributes, String source,
            String validFrom, String validUntil) {
        return new Claim(subject, value, attributes, source, OBSERVED, validFrom, validUntil);
    }

    private static CaseSummaryRequest request(String type, String personId, String personName, List<Claim> claims,
            List<EvidenceItem> evidence, List<Option> options) {
        return new CaseSummaryRequest("CASE-1", type, "Título del caso", personId, personName, "2026-11-20",
                claims, evidence, options);
    }

    static Stream<Arguments> casesByType() {
        return Stream.of(
                Arguments.of("BUDGET_ASSIGNMENT_MISMATCH", "Ana Torres", List.of(
                                claim("BUDGET_ORIGIN", "Pagos", Map.of(), "PREPER", null, null),
                                claim("OPERATIONAL_ASSIGNMENT", "Canales Digitales", Map.of("squad", "Squad Onboarding"),
                                        "JIRA", null, null)),
                        List.of("Ana Torres (U10001)", "Canales Digitales", "Squad Onboarding", "Jira", "Pagos",
                                "PREPER", "apoyo temporal vigente")),
                Arguments.of("MOVEMENT_NOT_REFLECTED", "Luis Ramos", List.of(
                                claim("MOVEMENT", "MOV-102", Map.of("fromTribe", "Pagos", "toTribe", "Riesgos"),
                                        "EXCEL_MOVEMENTS", "2026-10-15", null),
                                claim("ORG_MEMBERSHIP", "Pagos", Map.of(), "SIGA", null, null)),
                        List.of("MOV-102", "Luis Ramos", "de la tribu Pagos a la tribu Riesgos",
                                "15 de octubre de 2026", "el Excel de movimientos", "SIGA todavía registra")),
                Arguments.of("MANAGER_MISMATCH", "Carla Mendoza", List.of(
                                claim("MANAGEMENT_RESPONSIBILITY", "L20001", Map.of("managerName", "Rosa Díaz"), "SIGA",
                                        null, null),
                                claim("MANAGEMENT_RESPONSIBILITY", "L20002", Map.of("managerName", "Pedro Alva"),
                                        "EXCEL_CHAPTER", null, null)),
                        List.of("Carla Mendoza", "SIGA indica a Rosa Díaz (L20001)",
                                "el Excel de chapters indica a Pedro Alva (L20002)")),
                Arguments.of("MOVEMENT_WITHOUT_EVIDENCE", "Diego Paredes", List.of(
                                claim("MOVEMENT", "MOV-104", Map.of("fromTribe", "Canales Digitales", "toTribe", "Pagos"),
                                        "EXCEL_MOVEMENTS", "2026-12-01", null)),
                        List.of("MOV-104", "Diego Paredes", "el Excel de movimientos", "1 de diciembre de 2026",
                                "no se encontró un correo de aprobación")),
                Arguments.of("SWAP_INCOMPLETE", "Marco Ríos", List.of(
                                claim("MOVEMENT", "ENR-201", Map.of("type", "SWAP", "counterpartId", "U10005"),
                                        "EXCEL_MOVEMENTS", "2026-12-01", null)),
                        List.of("ENR-201", "Marco Ríos", "U10005", "1 de diciembre de 2026",
                                "conformidad de una sola de las partes", "el Excel de movimientos")),
                Arguments.of("CONTRACT_EXPIRING", "Jorge Salas", List.of(
                                claim("CONTRACT", "ACT-301", Map.of("provider", "TechPerú SAC"), "EXCEL_CONTRACTS",
                                        "2026-06-01", "2026-11-30")),
                        List.of("ACT-301", "Jorge Salas", "TechPerú SAC", "vence el 30 de noviembre de 2026",
                                "el Excel de actas", "renovación ni un reemplazo")),
                Arguments.of("AVAILABILITY_UNCONFIRMED", "Paola Quispe", List.of(
                                claim("AVAILABILITY", "PLANNED", Map.of("reason", "Fin de asignación en Squad Cobranzas"),
                                        "JIRA", "2026-11-25", null)),
                        List.of("Paola Quispe", "25 de noviembre de 2026", "Jira",
                                "Fin de asignación en Squad Cobranzas", "todavía no está confirmada")),
                Arguments.of("TEMP_SUPPORT_EXPIRED", "Renzo Flores", List.of(
                                claim("TEMP_SUPPORT", "Riesgos", Map.of(), "EXCEL_SUPPORT", "2026-08-01", "2026-11-06"),
                                claim("OPERATIONAL_ASSIGNMENT", "Riesgos", Map.of(), "JIRA", null, null)),
                        List.of("Renzo Flores", "tribu Riesgos", "venció el 6 de noviembre de 2026",
                                "el Excel de apoyos temporales", "Jira todavía registra")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casesByType")
    void eachTypeInterpolatesPersonValuesSourcesAndDates(String type, String personName, List<Claim> claims,
            List<String> expectedParts) {
        CaseSummary result = templates.render(request(type, "U10001", personName, claims, List.of(), OPTIONS));

        assertThat(result.caseId()).isEqualTo("CASE-1");
        assertThat(result.summary()).contains(expectedParts).contains(CONSULTED)
                .contains("Fecha límite para decidir: 20 de noviembre de 2026.")
                .doesNotContain("null").doesNotContain("Título del caso");
        assertThat(result.options()).containsExactly(
                new CaseSummary.OptionText("OPT_A", "Primera opción"),
                new CaseSummary.OptionText("OPT_B", "Segunda opción"));
        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.mode()).isEqualTo(Mode.RECORDED);
    }

    @Test
    void theParameterizedTestCoversTheEightTypes() {
        assertThat(casesByType().map(a -> (String) a.get()[0]))
                .containsExactlyInAnyOrder(Stream.of(CaseType.values()).map(Enum::name).toArray(String[]::new));
    }

    @ParameterizedTest
    @EnumSource(CaseType.class)
    void knownTypeWithoutItsClaimsFallsBackToGenericTextWithoutFailing(CaseType type) {
        CaseSummary result = templates.render(request(type.name(), "U10001", null, List.of(), List.of(), OPTIONS));

        assertThat(result.summary()).contains("Título del caso", "U10001").doesNotContain("null");
        assertThat(result.options()).hasSize(2);
    }

    @Test
    void unknownTypeUsesGenericTemplateListingClaims() {
        CaseSummary result = templates.render(request("SOMETHING_NEW", "U10007", "Jorge Salas",
                List.of(claim("CONTRACT", "ACT-301", Map.of(), "EXCEL_CONTRACTS", "2026-06-01", "2026-11-30")),
                List.of(), OPTIONS));

        assertThat(result.summary()).contains("Título del caso sobre Jorge Salas (U10007).",
                "El Excel de actas registra el acta: ACT-301, desde el 1 de junio de 2026, hasta el 30 de noviembre de 2026 "
                        + CONSULTED);
        assertThat(result.options()).extracting(CaseSummary.OptionText::text)
                .containsExactly("Primera opción", "Segunda opción");
    }

    @Test
    void evidenceQuoteIsIncluded() {
        CaseSummary result = templates.render(request("SWAP_INCOMPLETE", "U10006", "Marco Ríos",
                List.of(claim("MOVEMENT", "ENR-201", Map.of("counterpartId", "U10005"), "EXCEL_MOVEMENTS", null, null)),
                List.of(new EvidenceItem("SWAP_CONFORMITY", "Doy mi conformidad al enroque ENR-201.", "L20002")),
                OPTIONS));

        assertThat(result.summary()).contains("Evidencia en correo de L20002: «Doy mi conformidad al enroque ENR-201.».");
    }

    @Test
    void knownOptionsGetAConsequenceSentenceAndNegatedOnesAreNotConfused() {
        CaseSummary result = templates.render(request("CONTRACT_EXPIRING", "U10007", "Jorge Salas", List.of(),
                List.of(), List.of(new Option("RENEW", "Renovar el acta"),
                        new Option("REPLACE", "Gestionar un reemplazo."),
                        new Option("NO_RENEW", "No renovar"),
                        new Option("OTHER", "Revisar con el proveedor"))));

        assertThat(result.options()).extracting(CaseSummary.OptionText::id)
                .containsExactly("RENEW", "REPLACE", "NO_RENEW", "OTHER");
        assertThat(result.options()).extracting(CaseSummary.OptionText::text).containsExactly(
                "Renovar el acta. Quedará registrada la renovación del acta.",
                "Gestionar un reemplazo. Quedará registrado que el puesto se cubrirá con un reemplazo.",
                "No renovar. Quedará registrado que el acta termina en su fecha de vencimiento.",
                "Revisar con el proveedor");
    }
}
