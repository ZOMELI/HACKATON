package pe.bcp.peoplecapacity.cases.infrastructure.web;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.bcp.peoplecapacity.cases.application.CaseException;
import pe.bcp.peoplecapacity.cases.application.LedgerUnavailableException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(CaseException.class)
    ResponseEntity<Map<String, String>> business(CaseException e) {
        return ResponseEntity.status(e.status()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(LedgerUnavailableException.class)
    ResponseEntity<Map<String, String>> ledger(LedgerUnavailableException e) {
        log.warn("Libro no disponible: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(502).body(Map.of("message", e.getMessage() + ". No se cambió nada."));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Solicitud inválida: revise el cuerpo y los parámetros"));
    }
}
