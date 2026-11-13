package pe.bcp.peoplecapacity.agents.domain.validation;

/** La salida del modelo no cumple el esquema o contiene datos que no están en la entrada. */
public class InvalidModelOutputException extends RuntimeException {

    public InvalidModelOutputException(String message) {
        super(message);
    }
}
