package pe.bcp.peoplecapacity.cases.domain.model;

/** mode: LIVE | RECORDED (vienen de agents-service) | FALLBACK (plantilla local porque agents no respondió). */
public record Summary(String text, boolean aiGenerated, String mode) {
}
