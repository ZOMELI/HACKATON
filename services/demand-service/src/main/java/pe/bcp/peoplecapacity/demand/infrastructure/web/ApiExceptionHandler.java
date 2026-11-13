package pe.bcp.peoplecapacity.demand.infrastructure.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pe.bcp.peoplecapacity.demand.domain.InvalidNeedException;

/** Todos los errores salen como JSON {@code { "message": "..." }}. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    record ErrorResponse(String message) {
    }

    @ExceptionHandler(InvalidNeedException.class)
    ResponseEntity<ErrorResponse> invalidNeed(InvalidNeedException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> invalidParameter(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                "Valor inválido para el parámetro '%s': %s".formatted(e.getName(), e.getValue())));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        log.error("Error no controlado", e);
        return ResponseEntity.internalServerError().body(new ErrorResponse("Error interno del servicio de demanda"));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = "El cuerpo de la solicitud no es un JSON válido";
        if (e.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
            message = "Valor inválido para el campo '%s': %s".formatted(
                    invalid.getPath().get(invalid.getPath().size() - 1).getFieldName(), invalid.getValue());
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    /** Resto de errores estándar de Spring MVC (405, 415, 404...): mismo formato, conservando su código. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String message = resolved == null ? "Error en la solicitud" : "Solicitud rechazada: " + resolved.getReasonPhrase();
        return ResponseEntity.status(status).headers(headers).body(new ErrorResponse(message));
    }
}
