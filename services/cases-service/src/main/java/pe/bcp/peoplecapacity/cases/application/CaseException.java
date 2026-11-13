package pe.bcp.peoplecapacity.cases.application;

/** Error de negocio con el código HTTP que le corresponde en el contrato. */
public class CaseException extends RuntimeException {

    private final int status;

    public CaseException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public static CaseException notFound(String id) {
        return new CaseException(404, "No existe el caso " + id);
    }
}
