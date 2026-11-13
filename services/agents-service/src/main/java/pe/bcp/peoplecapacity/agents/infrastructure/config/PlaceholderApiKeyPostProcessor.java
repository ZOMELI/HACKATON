package pe.bcp.peoplecapacity.agents.infrastructure.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Si OPENAI_API_KEY llega vacía (habitual al pasarla desde docker compose sin definirla), pone el valor de
 * relleno para que el servicio arranque igual. Sin esto Spring AI rechaza la clave vacía al iniciar.
 */
public class PlaceholderApiKeyPostProcessor implements EnvironmentPostProcessor {

    private static final String API_KEY_PROPERTY = "spring.ai.openai.api-key";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String apiKey = environment.getProperty(API_KEY_PROPERTY);
        if (apiKey == null || apiKey.isBlank()) {
            environment.getPropertySources().addFirst(new MapPropertySource("agents-placeholder-api-key",
                    Map.of(API_KEY_PROPERTY, AgentsConfiguration.PLACEHOLDER_API_KEY)));
        }
    }
}
