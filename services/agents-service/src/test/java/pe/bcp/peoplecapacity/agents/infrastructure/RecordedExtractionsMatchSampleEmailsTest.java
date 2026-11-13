package pe.bcp.peoplecapacity.agents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import pe.bcp.peoplecapacity.agents.domain.heuristic.HeuristicEvidenceExtractor;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceType;
import pe.bcp.peoplecapacity.agents.domain.model.Mode;
import pe.bcp.peoplecapacity.agents.domain.model.RawEvidenceExtraction;
import pe.bcp.peoplecapacity.agents.domain.validation.EvidenceExtractionValidator;
import pe.bcp.peoplecapacity.agents.infrastructure.recorded.JsonRecordedExtractionsAdapter;

/**
 * Coherencia entre los correos de ejemplo del monorepo (fuentes/correos) y las extracciones grabadas.
 * Se omite si la carpeta no está disponible (por ejemplo, al construir la imagen Docker).
 */
class RecordedExtractionsMatchSampleEmailsTest {

    private static final Path EMAILS_DIR = Path.of("..", "..", "fuentes", "correos");

    private final JsonRecordedExtractionsAdapter recorded = new JsonRecordedExtractionsAdapter(
            new ClassPathResource("recorded/evidence-extractions.json"),
            new ObjectMapper().registerModule(new JavaTimeModule()));

    private static EmailInput read(Path file) throws IOException {
        String[] parts = Files.readString(file, StandardCharsets.UTF_8).split("\\R\\s*\\R", 2);
        Map<String, String> headers = new HashMap<>();
        for (String line : parts[0].split("\\R")) {
            String[] header = line.split(":\\s*", 2);
            headers.put(header[0], header[1]);
        }
        return new EmailInput(file.getFileName().toString().replace(".txt", ""), headers.get("De"),
                headers.get("Para"), headers.get("Asunto"), headers.get("Fecha"), parts[1]);
    }

    private static List<EmailInput> sampleEmails() throws IOException {
        assumeTrue(Files.isDirectory(EMAILS_DIR), "fuentes/correos no está disponible");
        try (Stream<Path> files = Files.list(EMAILS_DIR)) {
            List<Path> paths = files.filter(f -> f.getFileName().toString().matches("MAIL-\\d+\\.txt")).sorted().toList();
            List<EmailInput> emails = new java.util.ArrayList<>();
            for (Path path : paths) {
                emails.add(read(path));
            }
            return emails;
        }
    }

    @Test
    void sampleEmailsCoverTheSevenEvidenceTypesAndAllAreRecorded() throws IOException {
        List<EmailInput> emails = sampleEmails();

        assertThat(emails).allSatisfy(email -> {
            assertThat(email.from()).isNotBlank();
            assertThat(email.subject()).isNotBlank();
            assertThat(email.receivedAt()).isNotBlank();
            assertThat(recorded.findByEmailId(email.emailId())).as(email.emailId()).isPresent();
        });
        assertThat(emails.stream().map(e -> recorded.findByEmailId(e.emailId()).orElseThrow().type()).distinct())
                .containsExactlyInAnyOrder(EvidenceType.values());
    }

    @Test
    void everyRecordedExtractionWouldPassTheSameValidationAsTheModel() throws IOException {
        EvidenceExtractionValidator validator = new EvidenceExtractionValidator();
        for (EmailInput email : sampleEmails()) {
            EvidenceExtraction r = recorded.findByEmailId(email.emailId()).orElseThrow();
            assertThat(r.mode()).isEqualTo(Mode.RECORDED);
            assertThat(r.aiGenerated()).isFalse();

            RawEvidenceExtraction asModelOutput = new RawEvidenceExtraction(r.type().name(), r.personIds(),
                    r.movementId(), r.contractId(), Objects.toString(r.effectiveFrom(), null),
                    Objects.toString(r.effectiveUntil(), null), r.approver(), r.quote());
            // Incluye la regla clave: la cita aparece literalmente en el cuerpo del correo.
            validator.validate(asModelOutput, email);
            if (r.quote() != null) {
                assertThat(email.body()).as(email.emailId()).contains(r.quote());
            }
            if (r.approver() != null) {
                assertThat(email.body()).as(email.emailId()).contains(r.approver());
            }
        }
    }

    @Test
    void heuristicAgreesWithTheRecordedExtractions() throws IOException {
        HeuristicEvidenceExtractor heuristic = new HeuristicEvidenceExtractor();
        for (EmailInput email : sampleEmails()) {
            assertThat(heuristic.extract(email)).as(email.emailId())
                    .isEqualTo(recorded.findByEmailId(email.emailId()).orElseThrow());
        }
    }
}
