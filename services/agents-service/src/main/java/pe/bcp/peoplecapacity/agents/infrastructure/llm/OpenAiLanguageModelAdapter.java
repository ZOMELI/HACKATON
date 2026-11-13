package pe.bcp.peoplecapacity.agents.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelException;
import pe.bcp.peoplecapacity.agents.application.port.out.LanguageModelPort;
import pe.bcp.peoplecapacity.agents.domain.model.CaseSummaryRequest;
import pe.bcp.peoplecapacity.agents.domain.model.EmailInput;
import pe.bcp.peoplecapacity.agents.domain.model.RawCaseSummary;
import pe.bcp.peoplecapacity.agents.domain.model.RawEvidenceExtraction;

/**
 * Llama al modelo (OpenAI vía Spring AI) con un tiempo máximo de espera. Devuelve la salida sin validar;
 * la validación contra el contrato la hacen los casos de uso.
 */
@Component
public class OpenAiLanguageModelAdapter implements LanguageModelPort, DisposableBean {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public OpenAiLanguageModelAdapter(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper,
            @Value("${agents.llm.timeout-seconds:20}") long timeoutSeconds) {
        this(chatClientBuilder, objectMapper, Duration.ofSeconds(timeoutSeconds));
    }

    OpenAiLanguageModelAdapter(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper, Duration timeout) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.timeout = timeout;
    }

    @Override
    public RawEvidenceExtraction extractEvidence(EmailInput email) {
        String user = """
                Fecha de recepción: %s
                Remitente: %s
                Destinatario: %s
                Asunto: %s
                <correo>
                %s
                </correo>
                """.formatted(
                orDash(email.receivedAt()), orDash(email.from()), orDash(email.to()),
                withoutTag(orDash(email.subject()), "correo"), withoutTag(email.body(), "correo"));
        return ask(Prompts.EVIDENCE_SYSTEM, user, RawEvidenceExtraction.class);
    }

    @Override
    public RawCaseSummary summarizeCase(CaseSummaryRequest request) {
        String caseJson;
        try {
            caseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new LanguageModelException("No se pudo preparar el caso para el modelo", e);
        }
        String user = "<caso>\n" + withoutTag(caseJson, "caso") + "\n</caso>\n";
        return ask(Prompts.CASE_SUMMARY_SYSTEM, user, RawCaseSummary.class);
    }

    private <T> T ask(String system, String user, Class<T> outputType) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(outputType);
        // Se pasan mensajes ya armados (sin plantillas) para que llaves o símbolos del correo no se interpreten.
        List<Message> messages = List.of(
                new SystemMessage(system),
                new UserMessage(user + "\n" + converter.getFormat()));
        Future<T> answer = executor.submit(() -> {
            String content = chatClient.prompt().messages(messages).call().content();
            if (content == null || content.isBlank()) {
                throw new LanguageModelException("El modelo devolvió una respuesta vacía");
            }
            return converter.convert(content);
        });
        try {
            return answer.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            answer.cancel(true);
            throw new LanguageModelException("El modelo no respondió en " + timeout.toSeconds() + " s");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw new LanguageModelException("Falló la llamada al modelo: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            answer.cancel(true);
            Thread.currentThread().interrupt();
            throw new LanguageModelException("Se interrumpió la llamada al modelo");
        }
    }

    /** Evita que el contenido cierre por su cuenta el delimitador que lo marca como datos. */
    private static String withoutTag(String text, String tag) {
        return text == null ? "" : text.replaceAll("(?i)</?\\s*" + tag + "\\s*>", "");
    }

    private static String orDash(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
