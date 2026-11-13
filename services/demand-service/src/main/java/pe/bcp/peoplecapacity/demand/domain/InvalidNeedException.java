package pe.bcp.peoplecapacity.demand.domain;

/** La necesidad no cumple las reglas del dominio (p. ej. le falta fuente o fecha de consulta). */
public class InvalidNeedException extends RuntimeException {

    public InvalidNeedException(String message) {
        super(message);
    }
}
