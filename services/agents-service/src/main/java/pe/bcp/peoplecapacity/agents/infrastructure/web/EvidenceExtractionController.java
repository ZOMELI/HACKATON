package pe.bcp.peoplecapacity.agents.infrastructure.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.bcp.peoplecapacity.agents.application.port.in.ExtractEvidenceUseCase;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.EvidenceExtraction;

@RestController
public class EvidenceExtractionController {

    private final ExtractEvidenceUseCase extractEvidence;

    public EvidenceExtractionController(ExtractEvidenceUseCase extractEvidence) {
        this.extractEvidence = extractEvidence;
    }

    @PostMapping("/evidence-extractions")
    public EvidenceExtraction extract(@RequestBody EmailInput email) {
        if (isBlank(email.emailId())) {
            throw new BadRequestException("Falta emailId");
        }
        if (isBlank(email.body())) {
            throw new BadRequestException("Falta body");
        }
        return extractEvidence.extract(email);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
