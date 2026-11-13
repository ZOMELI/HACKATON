package pe.bcp.peoplecapacity.agents.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary.RawOption;
import pe.bcp.peoplecapacity.agents.domain.validation.CaseSummaryValidator;
import pe.bcp.peoplecapacity.agents.domain.validation.InvalidModelOutputException;

class CaseSummaryValidatorTest {

    private static final CaseSummaryRequest REQUEST = new CaseSummaryRequest("CASE-1", "CONTRACT_EXPIRING",
            null, "U10007", "Jorge Salas", null, List.of(), List.of(),
            List.of(new CaseSummaryRequest.Option("RENEW", "Renovar"),
                    new CaseSummaryRequest.Option("END", "No renovar")));

    private final CaseSummaryValidator validator = new CaseSummaryValidator();

    @Test
    void acceptsSameOptionsAndKeepsRequestOrder() {
        CaseSummary result = validator.validate(new RawCaseSummary("El acta vence pronto.",
                List.of(new RawOption("END", "Dejar que termine."), new RawOption("RENEW", "Renovarla."))), REQUEST);

        assertThat(result.caseId()).isEqualTo("CASE-1");
        assertThat(result.summary()).isEqualTo("El acta vence pronto.");
        assertThat(result.options()).extracting(CaseSummary.OptionText::id).containsExactly("RENEW", "END");
        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.mode()).isEqualTo(Mode.LIVE);
    }

    @Test
    void rejectsInventedOption() {
        assertInvalid(new RawCaseSummary("Resumen.", List.of(new RawOption("RENEW", "a"), new RawOption("END", "b"),
                new RawOption("ESCALATE", "Escalar al gerente."))), "no se recibieron");
    }

    @Test
    void rejectsMissingOption() {
        assertInvalid(new RawCaseSummary("Resumen.", List.of(new RawOption("RENEW", "a"))), "falta");
    }

    @Test
    void rejectsRenamedOrDuplicatedOption() {
        assertInvalid(new RawCaseSummary("Resumen.", List.of(new RawOption("RENEW", "a"), new RawOption("end", "b"))),
                "falta");
        assertInvalid(new RawCaseSummary("Resumen.", List.of(new RawOption("RENEW", "a"), new RawOption("RENEW", "b"),
                new RawOption("END", "c"))), "repetida");
    }

    @Test
    void rejectsEmptyTexts() {
        assertInvalid(new RawCaseSummary("  ", List.of(new RawOption("RENEW", "a"), new RawOption("END", "b"))),
                "resumen");
        assertInvalid(new RawCaseSummary("Resumen.", List.of(new RawOption("RENEW", " "), new RawOption("END", "b"))),
                "vacío");
        assertInvalid(new RawCaseSummary("Resumen.", null), "falta");
        assertInvalid(null, "resumen");
    }

    private void assertInvalid(RawCaseSummary raw, String expectedMessagePart) {
        assertThatThrownBy(() -> validator.validate(raw, REQUEST))
                .isInstanceOf(InvalidModelOutputException.class)
                .hasMessageContaining(expectedMessagePart);
    }
}
