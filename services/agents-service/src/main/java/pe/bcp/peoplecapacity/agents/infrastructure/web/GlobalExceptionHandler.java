package pe.bcp.peoplecapacity.agents.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Convierte cualquier error en JSON { "message": "..." }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorResponseBody> badRequest(BadRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponseBody(e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponseBody> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponseBody("El cuerpo de la petición no es un JSON válido para este servicio"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponseBody> other(Exception e) {
        // Errores propios de Spring MVC (ruta inexistente, método no permitido...) conservan su código.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status).body(new ErrorResponseBody(messageFor(status)));
        }
        log.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseBody("Error interno del servicio de agentes"));
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 415 -> "Tipo de contenido no soportado; envíe application/json";
            default -> status.is4xxClientError() ? "Petición inválida" : "Error interno del servicio de agentes";
        };
    }
}
