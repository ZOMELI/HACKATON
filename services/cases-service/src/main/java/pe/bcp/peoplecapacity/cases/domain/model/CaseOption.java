package pe.bcp.peoplecapacity.cases.domain.model;

/** Opción definida por la regla. text es la redacción (IA o plantilla); effect puede ser null. */
public record CaseOption(String id, String label, String text, OptionEffect effect) {

    public static CaseOption of(String id, String label) {
        return new CaseOption(id, label, label, null);
    }

    public static CaseOption of(String id, String label, OptionEffect effect) {
        return new CaseOption(id, label, label, effect);
    }

    public CaseOption withText(String newText) {
        return new CaseOption(id, label, newText, effect);
    }
}
