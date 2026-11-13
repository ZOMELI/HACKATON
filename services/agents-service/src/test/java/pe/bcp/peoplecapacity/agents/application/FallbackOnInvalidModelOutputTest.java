package pe.bcp.peoplecapacity.agents.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelException;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelPort;
import pe.bcp.peoplecapacity.agents.application.port.out.RecordedExtractionsPort;
import pe.bcp.peoplecapacity.agents.application.service.CaseSummaryService;
import pe.bcp.peoplecapacity.agents.application.service.EvidenceExtractionService;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary.RawOption;
import pe.bcp.peoplecapacity.agents.domain.model.RawEvidenceExtraction;

/** Con un modelo falso: salida válida → LIVE; salida inválida, error o modo recorded → respaldo. */
class FallbackOnInvalidModelOutputTest {

    private static final String QUOTE = "Apruebo el movimiento MOV-104 de Diego Paredes (U10004) a la tribu Pagos.";
    private static final EmailInput EMAIL = new EmailInput("MAIL-900", null, null, "Movimiento",
            "2026-11-12T10:00:00Z", "Hola,\n\n" + QUOTE + "\n\nSaludos,\nJulia Meza (C30001)");
    private static final EvidenceExtraction RECORDED = new EvidenceExtraction("MAIL-REC",
            EvidenceType.TEMP_SUPPORT, List.of("U10009"), null, null, null, null, "L20002", "grabada", false,
            Mode.RECORDED);
    private static final RecordedExtractionsPort RECORDED_PORT =
            emailId -> "MAIL-REC".equals(emailId) ? Optional.of(RECORDED) : Optional.empty();
    private static final CaseSummaryRequest CASE = new CaseSummaryRequest("CASE-1", "CONTRACT_EXPIRING", null,
            "U10007", "Jorge Salas", "2026-11-20",
            List.of(new CaseSummaryRequest.Claim("CONTRACT", "ACT-301", null, "EXCEL_CONTRACTS",
                    "2026-11-13T08:00:00Z", "2026-06-01", "2026-11-30")),
            List.of(), List.of(new CaseSummaryRequest.Option("RENEW", "Renovar el acta"),
                    new CaseSummaryRequest.Option("END", "No renovar")));

    /** Modelo falso que devuelve lo que se le indique y cuenta las llamadas. */
    private static final class FakeLanguageModel implements LanguageModelPort {
        private final RawEvidenceExtraction evidence;
        private final RawCaseSummary summary;
        private final RuntimeException failure;
        private final AtomicInteger calls = new AtomicInteger();

        FakeLanguageModel(RawEvidenceExtraction evidence, RawCaseSummary summary, RuntimeException failure) {
            this.evidence = evidence;
            this.summary = summary;
            this.failure = failure;
        }

        @Override
        public RawEvidenceExtraction extractEvidence(EmailInput email) {
            calls.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            return evidence;
        }

        @Override
        public RawCaseSummary summarizeCase(CaseSummaryRequest request) {
            calls.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            return summary;
        }
    }

    private static RawEvidenceExtraction rawEvidence(String quote) {
        return new RawEvidenceExtraction("MOVEMENT_APPROVAL", List.of("U10004"), "MOV-104", null, null, null,
                "C30001", quote);
    }

    // ---- Agente de evidencia ----

    @Test
    void validModelOutputIsReturnedAsLive() {
        FakeLanguageModel model = new FakeLanguageModel(rawEvidence(QUOTE), null, null);
        EvidenceExtraction result = new EvidenceExtractionService(AgentsMode.LIVE, model, RECORDED_PORT).extract(EMAIL);

        assertThat(result.mode()).isEqualTo(Mode.LIVE);
        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.quote()).isEqualTo(QUOTE);
    }

    @Test
    void quoteNotInEmailFallsBackToHeuristic() {
        FakeLanguageModel model = new FakeLanguageModel(
                rawEvidence("Apruebo el movimiento MOV-104 de Diego Paredes con efecto inmediato."), null, null);
        EvidenceExtraction result = new EvidenceExtractionService(AgentsMode.LIVE, model, RECORDED_PORT).extract(EMAIL);

        assertThat(model.calls).hasValue(1);
        assertThat(result.mode()).isEqualTo(Mode.RECORDED);
        assertThat(result.aiGenerated()).isFalse();
        // Lo que se devuelve es la heurística sobre el correo real, no lo que inventó el modelo.
        assertThat(result.type()).isEqualTo(EvidenceType.MOVEMENT_APPROVAL);
        assertThat(result.quote()).isEqualTo(QUOTE);
        assertThat(result.approver()).isEqualTo("C30001");
    }

    @Test
    void invalidOutputFallsBackToRecordedExtractionWhenThereIsOne() {
        FakeLanguageModel model = new FakeLanguageModel(rawEvidence("cita inventada"), null, null);
        EmailInput recordedEmail = new EmailInput("MAIL-REC", null, null, null, null, EMAIL.body());

        assertThat(new EvidenceExtractionService(AgentsMode.LIVE, model, RECORDED_PORT).extract(recordedEmail))
                .isSameAs(RECORDED);
    }

    @Test
    void modelFailureFallsBackWithoutPropagating() {
        FakeLanguageModel model = new FakeLanguageModel(null, null, new LanguageModelException("tiempo agotado"));
        EvidenceExtraction result = new EvidenceExtractionService(AgentsMode.LIVE, model, RECORDED_PORT).extract(EMAIL);

        assertThat(result.mode()).isEqualTo(Mode.RECORDED);
        assertThat(result.aiGenerated()).isFalse();
    }

    @Test
    void recordedModeNeverCallsTheModel() {
        FakeLanguageModel model = new FakeLanguageModel(rawEvidence(QUOTE), new RawCaseSummary("x", List.of()), null);

        EvidenceExtraction extraction =
                new EvidenceExtractionService(AgentsMode.RECORDED, model, RECORDED_PORT).extract(EMAIL);
        CaseSummary summary = new CaseSummaryService(AgentsMode.RECORDED, model).summarize(CASE);

        assertThat(model.calls).hasValue(0);
        assertThat(extraction.mode()).isEqualTo(Mode.RECORDED);
        assertThat(summary.mode()).isEqualTo(Mode.RECORDED);
    }

    // ---- Agente de expediente ----

    @Test
    void validSummaryIsReturnedAsLive() {
        FakeLanguageModel model = new FakeLanguageModel(null, new RawCaseSummary("El acta ACT-301 vence pronto.",
                List.of(new RawOption("RENEW", "Renovar."), new RawOption("END", "Dejar que termine."))), null);
        CaseSummary result = new CaseSummaryService(AgentsMode.LIVE, model).summarize(CASE);

        assertThat(result.mode()).isEqualTo(Mode.LIVE);
        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.summary()).isEqualTo("El acta ACT-301 vence pronto.");
    }

    @Test
    void inventedOptionFallsBackToTemplate() {
        FakeLanguageModel model = new FakeLanguageModel(null, new RawCaseSummary("Recomiendo renovar.",
                List.of(new RawOption("RENEW", "Renovar."), new RawOption("END", "Terminar."),
                        new RawOption("ESCALATE", "Escalar al gerente."))), null);
        CaseSummary result = new CaseSummaryService(AgentsMode.LIVE, model).summarize(CASE);

        assertThat(model.calls).hasValue(1);
        assertThat(result.mode()).isEqualTo(Mode.RECORDED);
        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.summary()).contains("ACT-301", "Jorge Salas (U10007)").doesNotContain("Recomiendo");
        assertThat(result.options()).extracting(CaseSummary.OptionText::id).containsExactly("RENEW", "END");
    }

    @Test
    void removedOptionOrModelFailureFallsBackToTemplate() {
        FakeLanguageModel missing = new FakeLanguageModel(null,
                new RawCaseSummary("Resumen.", List.of(new RawOption("RENEW", "Renovar."))), null);
        FakeLanguageModel failing = new FakeLanguageModel(null, null, new IllegalStateException("401"));

        for (FakeLanguageModel model : List.of(missing, failing)) {
            CaseSummary result = new CaseSummaryService(AgentsMode.LIVE, model).summarize(CASE);
            assertThat(result.mode()).isEqualTo(Mode.RECORDED);
            assertThat(result.options()).extracting(CaseSummary.OptionText::id).containsExactly("RENEW", "END");
        }
    }
}
