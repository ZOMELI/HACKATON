package pe.bcp.peoplecapacity.agents.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.model.RawEvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.validation.EvidenceExtractionValidator;
import pe.bcp.peoplecapacity.agents.domain.validation.InvalidModelOutputException;

class EvidenceExtractionValidatorTest {

    private static final String QUOTE =
            "Apruebo el movimiento MOV-102 de Luis Ramos (U10002) a la tribu Riesgos desde el 15 de octubre.";
    private static final EmailInput EMAIL = new EmailInput("MAIL-X", "Julia Meza", "Rosa Díaz", "Aprobación",
            "2026-10-10T09:15:00-05:00",
            "Hola Rosa,\n\nApruebo el movimiento MOV-102 de Luis Ramos (U10002)\n   a la tribu Riesgos desde el 15 de octubre.\n\nSaludos,\nJulia Meza (C30001)");

    private final EvidenceExtractionValidator validator = new EvidenceExtractionValidator();

    private static RawEvidenceExtraction raw(String type, List<String> personIds, String movementId,
            String contractId, String from, String until, String quote) {
        return new RawEvidenceExtraction(type, personIds, movementId, contractId, from, until, "C30001", quote);
    }

    @Test
    void acceptsValidOutputAndMarksItAsLive() {
        EvidenceExtraction result = validator.validate(
                raw("MOVEMENT_APPROVAL", List.of("U10002"), "MOV-102", null, "2026-10-15", "", QUOTE), EMAIL);

        assertThat(result.emailId()).isEqualTo("MAIL-X");
        assertThat(result.type()).isEqualTo(EvidenceType.MOVEMENT_APPROVAL);
        assertThat(result.personIds()).containsExactly("U10002");
        assertThat(result.movementId()).isEqualTo("MOV-102");
        assertThat(result.contractId()).isNull();
        assertThat(result.effectiveFrom()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(result.effectiveUntil()).isNull();
        assertThat(result.approver()).isEqualTo("C30001");
        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.mode()).isEqualTo(Mode.LIVE);
    }

    @Test
    void quoteMatchesIgnoringWhitespaceDifferencesOnly() {
        // El cuerpo tiene la cita partida en dos líneas: debe aceptarse.
        assertThat(validator.validate(raw("MOVEMENT_APPROVAL", List.of(), null, null, null, null, QUOTE), EMAIL)
                .quote()).isEqualTo(QUOTE);
    }

    @Test
    void rejectsQuoteThatIsNotInTheEmail() {
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of("U10002"), "MOV-102", null, null, null,
                "Apruebo el movimiento MOV-102 de Luis Ramos a Riesgos."), "cita");
    }

    @Test
    void rejectsMissingQuoteUnlessIrrelevant() {
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of(), null, null, null, null, null), "cita");
        assertThat(validator.validate(raw("IRRELEVANT", List.of(), null, null, null, null, null), EMAIL).quote())
                .isNull();
    }

    @Test
    void rejectsTypeOutsideEnum() {
        assertInvalid(raw("APPROVAL", List.of(), null, null, null, null, QUOTE), "tipo");
        assertInvalid(raw(null, List.of(), null, null, null, null, QUOTE), "tipo");
    }

    @Test
    void rejectsPersonIdWithWrongFormatOrNotInEmail() {
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of("u10002"), null, null, null, null, QUOTE), "matrícula");
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of("U1000"), null, null, null, null, QUOTE), "matrícula");
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of("U99999"), null, null, null, null, QUOTE), "no aparece");
    }

    @Test
    void rejectsIdentifiersWithWrongFormatOrNotInEmail() {
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of(), "ACT-102", null, null, null, QUOTE), "movimiento");
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of(), "MOV-999", null, null, null, QUOTE), "no aparece");
        assertInvalid(raw("CONTRACT_RENEWAL", List.of(), null, "MOV-102", null, null, QUOTE), "acta");
    }

    @Test
    void rejectsInvalidDates() {
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of(), null, null, "15/10/2026", null, QUOTE), "ISO");
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of(), null, null, "2026-02-30", null, QUOTE), "ISO");
        assertInvalid(raw("MOVEMENT_APPROVAL", List.of(), null, null, "2026-10-15", "2026-10-01", QUOTE),
                "anterior");
    }

    @Test
    void rejectsNullOutput() {
        assertThatThrownBy(() -> validator.validate(null, EMAIL)).isInstanceOf(InvalidModelOutputException.class);
    }

    private void assertInvalid(RawEvidenceExtraction raw, String expectedMessagePart) {
        assertThatThrownBy(() -> validator.validate(raw, EMAIL))
                .isInstanceOf(InvalidModelOutputException.class)
                .hasMessageContaining(expectedMessagePart);
    }
}
