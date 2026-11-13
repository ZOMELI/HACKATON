package pe.bcp.peoplecapacity.agents.application.port.out;

/** El modelo de lenguaje falló, tardó demasiado o respondió algo que no se pudo leer. */
public class LanguageModelException extends RuntimeException {

    public LanguageModelException(String message) {
        super(message);
    }

    public LanguageModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
