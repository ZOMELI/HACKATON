package pe.bcp.peoplecapacity.agents.application;

/** Modo de trabajo del servicio (variable AGENTS_MODE). En RECORDED nunca se llama al modelo. */
public enum AgentsMode {
    LIVE,
    RECORDED;

    /** Cualquier valor que no sea "live" se trata como recorded: ante la duda no se llama al modelo. */
    public static AgentsMode fromText(String text) {
        return text != null && "live".equalsIgnoreCase(text.trim()) ? LIVE : RECORDED;
    }
}
