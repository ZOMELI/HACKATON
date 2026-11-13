package pe.bcp.peoplecapacity.agents.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import pe.bcp.peoplecapacity.agents.application.AgentsMode;

/** Con la clave vacía y un modo inválido el servicio arranca igual, en recorded y con la clave de relleno. */
@SpringBootTest(properties = {"spring.ai.openai.api-key=", "agents.mode=cualquiera"})
class StartsWithEmptyApiKeyTest {

    @Autowired
    private Environment environment;

    @Autowired
    private AgentsMode mode;

    @Test
    void contextStartsInRecordedModeWithPlaceholderKey() {
        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("sin-clave");
        assertThat(mode).isEqualTo(AgentsMode.RECORDED);
    }
}
