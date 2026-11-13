package pe.bcp.peoplecapacity.agents.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pe.bcp.peoplecapacity.agents.domain.heuristic.HeuristicEvidenceExtractor;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.text.TextNormalizer;

class HeuristicEvidenceExtractorTest {

    private final HeuristicEvidenceExtractor heuristic = new HeuristicEvidenceExtractor();

    private static EmailInput email(String receivedAt, String body) {
        return new EmailInput("MAIL-900", null, null, null, receivedAt, body);
    }

    @Test
    void movementApprovalWithSignatureApprover() {
        String body = """
                Hola Pedro,

                Apruebo el movimiento MOV-104 de Diego Paredes (U10004) a la tribu Pagos
                a partir del 1 de diciembre.

                Saludos,
                Julia Meza (C30001)
                """;
        EvidenceExtraction result = heuristic.extract(email("2026-11-12T10:00:00Z", body));

        assertThat(result.type()).isEqualTo(EvidenceType.MOVEMENT_APPROVAL);
        assertThat(result.personIds()).containsExactly("U10004");
        assertThat(result.movementId()).isEqualTo("MOV-104");
        assertThat(result.contractId()).isNull();
        assertThat(result.effectiveFrom()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(result.approver()).isEqualTo("C30001");
        assertThat(result.quote()).startsWith("Apruebo el movimiento MOV-104").endsWith("1 de diciembre.");
        assertThat(TextNormalizer.containsLiterally(body, result.quote())).isTrue();
        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.mode()).isEqualTo(Mode.RECORDED);
    }

    @Test
    void swapConformity() {
        EvidenceExtraction result = heuristic.extract(email("2026-11-05",
                "Doy mi conformidad al enroque ENR-201 entre Sofía Vega (U10005) y Marco Ríos (U10006)."));

        assertThat(result.type()).isEqualTo(EvidenceType.SWAP_CONFORMITY);
        assertThat(result.personIds()).containsExactly("U10005", "U10006");
        assertThat(result.movementId()).isEqualTo("ENR-201");
        assertThat(result.approver()).isNull();
    }

    @Test
    void contractRenewalWithExplicitYearAndSlashDate() {
        EvidenceExtraction result = heuristic.extract(email(null,
                "Se renueva el acta ACT-311 de Hugo Lazo (U10011) desde el 29/11/2026 hasta el 28 de mayo de 2027."));

        assertThat(result.type()).isEqualTo(EvidenceType.CONTRACT_RENEWAL);
        assertThat(result.contractId()).isEqualTo("ACT-311");
        assertThat(result.effectiveFrom()).isEqualTo(LocalDate.of(2026, 11, 29));
        assertThat(result.effectiveUntil()).isEqualTo(LocalDate.of(2027, 5, 28));
    }

    @Test
    void contractReplacement() {
        EvidenceExtraction result = heuristic.extract(email("2026-11-09",
                "En el acta ACT-320 se reemplaza a Raúl Tello (U10015) por Kevin Ore (U10016)."));

        assertThat(result.type()).isEqualTo(EvidenceType.CONTRACT_REPLACEMENT);
        assertThat(result.personIds()).containsExactly("U10015", "U10016");
        assertThat(result.contractId()).isEqualTo("ACT-320");
    }

    @Test
    void tempSupportTakesApproverFromSenderHeader() {
        EvidenceExtraction result = heuristic.extract(new EmailInput("MAIL-901", "Pedro Alva (L20002)", null,
                "Apoyo temporal", "2026-07-28T11:05:00-05:00",
                "Renzo Flores (U10009) apoyará a Riesgos hasta el 6 de noviembre."));

        assertThat(result.type()).isEqualTo(EvidenceType.TEMP_SUPPORT);
        assertThat(result.personIds()).containsExactly("U10009");
        assertThat(result.effectiveUntil()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(result.approver()).isEqualTo("L20002");
    }

    @Test
    void informalNeedHasNoApprover() {
        EvidenceExtraction result = heuristic.extract(email("2026-11-11",
                "Hola,\nNecesitamos un Backend Developer para el Squad Transferencias.\n\nGracias,\nFernando (T50001)"));

        assertThat(result.type()).isEqualTo(EvidenceType.INFORMAL_NEED);
        assertThat(result.approver()).isNull();
        assertThat(result.quote()).isEqualTo("Necesitamos un Backend Developer para el Squad Transferencias.");
    }

    @Test
    void noSignalIsIrrelevant() {
        EvidenceExtraction result = heuristic.extract(email("2026-11-12",
                "Los invitamos a la charla de seguridad del jueves. La persona de contacto es exacta: U10001."));

        assertThat(result.type()).isEqualTo(EvidenceType.IRRELEVANT);
        assertThat(result.personIds()).containsExactly("U10001");
        assertThat(result.quote()).isNull();
        assertThat(result.movementId()).isNull();
    }

    @Test
    void dateWithoutYearIsNotInventedWhenThereIsNoReceptionDate() {
        EvidenceExtraction result = heuristic.extract(email(null,
                "Apruebo el movimiento MOV-104 de Diego Paredes (U10004) desde el 1 de diciembre."));

        assertThat(result.effectiveFrom()).isNull();
    }

    @Test
    void dateWithoutYearRollsToNextYearWhenItWouldBeFarInThePast() {
        EvidenceExtraction result = heuristic.extract(email("2026-12-20",
                "Apruebo el movimiento MOV-104 de Diego Paredes (U10004) desde el 5 de enero."));

        assertThat(result.effectiveFrom()).isEqualTo(LocalDate.of(2027, 1, 5));
    }
}
