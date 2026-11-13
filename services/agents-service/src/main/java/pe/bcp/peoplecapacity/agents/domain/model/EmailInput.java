package pe.bcp.peoplecapacity.agents.domain.model;

/** Correo de entrada. Las fechas llegan como texto y se interpretan con tolerancia. */
public record EmailInput(String emailId, String from, String to, String subject, String receivedAt, String body) {
}
