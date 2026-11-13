package pe.bcp.peoplecapacity.agents.infrastructure.web;

import java.util.HashSet;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.bcp.peoplecapacity.agents.application.port.in.SummarizeCaseUseCase;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;

@RestController
public class CaseSummaryController {

    private final SummarizeCaseUseCase summarizeCase;

    public CaseSummaryController(SummarizeCaseUseCase summarizeCase) {
        this.summarizeCase = summarizeCase;
    }

    @PostMapping("/case-summaries")
    public CaseSummary summarize(@RequestBody CaseSummaryRequest request) {
        if (isBlank(request.caseId())) {
            throw new BadRequestException("Falta caseId");
        }
        if (isBlank(request.type())) {
            throw new BadRequestException("Falta type");
        }
        if (request.options() == null) {
            throw new BadRequestException("Falta options");
        }
        Set<String> ids = new HashSet<>();
        for (CaseSummaryRequest.Option option : request.options()) {
            if (option == null || isBlank(option.id()) || isBlank(option.label())) {
                throw new BadRequestException("Cada opción debe tener id y label");
            }
            if (!ids.add(option.id())) {
                throw new BadRequestException("Opción repetida: " + option.id());
            }
        }
        return summarizeCase.summarize(request);
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
