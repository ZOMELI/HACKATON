package pe.bcp.peoplecapacity.agents.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.bcp.peoplecapacity.agents.application.AgentsMode;
import pe.bcp.peoplecapacity.agents.application.port.in.ExtractEvidenceUseCase;
import pe.bcp.peoplecapacity.agents.application.port.in.SummarizeCaseUseCase;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelPort;
import pe.bcp.peoplecapacity.agents.application.port.out.RecordedExtractionsPort;
import pe.bcp.peoplecapacity.agents.application.service.CaseSummaryService;
import pe.bcp.peoplecapacity.agents.application.service.EvidenceExtractionService;

/** Arma los casos de uso con sus puertos y abre CORS para la web local. */
@Configuration
public class AgentsConfiguration implements WebMvcConfigurer {

    /** Valor de relleno de la clave: permite arrancar sin OPENAI_API_KEY. */
    public static final String PLACEHOLDER_API_KEY = "sin-clave";

    private static final Logger log = LoggerFactory.getLogger(AgentsConfiguration.class);

    @Bean
    AgentsMode agentsMode(@Value("${agents.mode:recorded}") String configuredMode,
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:}") String model) {
        AgentsMode mode = AgentsMode.fromText(configuredMode);
        if (!mode.name().equalsIgnoreCase(configuredMode == null ? "" : configuredMode.trim())) {
            log.warn("AGENTS_MODE='{}' no es válido (live | recorded); se usará recorded", configuredMode);
        }
        if (mode == AgentsMode.LIVE) {
            log.info("Modo live: se llamará al modelo {}", model);
            if (apiKey.isBlank() || PLACEHOLDER_API_KEY.equals(apiKey)) {
                log.warn("Modo live sin OPENAI_API_KEY: toda llamada al modelo fallará y se usará el respaldo");
            }
        } else {
            log.info("Modo recorded: no se llamará al modelo; se usan extracciones grabadas y plantillas");
        }
        return mode;
    }

    @Bean
    ExtractEvidenceUseCase extractEvidenceUseCase(AgentsMode mode, LanguageModelPort languageModel,
            RecordedExtractionsPort recordedExtractions) {
        return new EvidenceExtractionService(mode, languageModel, recordedExtractions);
    }

    @Bean
    SummarizeCaseUseCase summarizeCaseUseCase(AgentsMode mode, LanguageModelPort languageModel) {
        return new CaseSummaryService(mode, languageModel);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
