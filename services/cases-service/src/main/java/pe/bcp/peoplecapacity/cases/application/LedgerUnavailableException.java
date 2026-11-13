package pe.bcp.peoplecapacity.cases.application;

/** El libro no respondió o rechazó la operación. */
public class LedgerUnavailableException extends RuntimeException {

    public LedgerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
