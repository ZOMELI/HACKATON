package pe.bcp.peoplecapacity.cases.domain.model;

/** Usuario simulado (cabeceras X-User-Id y X-User-Role). Sin usuario se asume gerente. */
public record UserContext(String userId, String role) {

    public boolean seesAll() {
        return userId == null || userId.isBlank() || "MANAGER".equalsIgnoreCase(role);
    }
}
