package pe.bcp.peoplecapacity.demand.application.port.out;

public class LedgerUnavailableException extends RuntimeException {

    public LedgerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
