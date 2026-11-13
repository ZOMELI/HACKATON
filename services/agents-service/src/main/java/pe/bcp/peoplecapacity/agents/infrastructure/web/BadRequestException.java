package pe.bcp.peoplecapacity.agents.infrastructure.web;

/** La petición no trae los campos obligatorios del contrato. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
