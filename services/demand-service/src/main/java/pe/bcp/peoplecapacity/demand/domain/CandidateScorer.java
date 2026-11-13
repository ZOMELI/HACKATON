package pe.bcp.peoplecapacity.demand.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Puntaje determinista (0–100) de coincidencia entre una necesidad y una persona disponible.
 *
 * <pre>
 *   Rol idéntico (sin mayúsculas ni tildes) ............. 60
 *   Rol de la misma familia (comparte palabra clave) .... 30
 *   Disponibilidad CONFIRMED ............................ 25
 *   Disponibilidad PLANNED .............................. 10
 *   Disponible en o antes de neededFrom ................. 15
 * </pre>
 *
 * Sin ninguna coincidencia de rol la persona no es candidata.
 */
public final class CandidateScorer {

    public static final int IDENTICAL_ROLE_POINTS = 60;
    public static final int SAME_FAMILY_POINTS = 30;
    public static final int CONFIRMED_POINTS = 25;
    public static final int PLANNED_POINTS = 10;
    public static final int ON_TIME_POINTS = 15;

    /** Palabras que no definen una familia de rol (conectores y niveles de seniority). */
    private static final Set<String> NOISE_WORDS = Set.of(
            "de", "del", "la", "el", "los", "las", "y", "e", "en", "of", "and", "the",
            "senior", "sr", "junior", "jr", "semi", "semisenior", "ssr", "mid", "trainee", "i", "ii", "iii");

    private static final Comparator<Candidate> RANKING = Comparator
            .comparingInt(Candidate::score).reversed()
            .thenComparing(Candidate::availableFrom, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Candidate::personId, Comparator.nullsLast(Comparator.naturalOrder()));

    /** Candidatos de la necesidad, ordenados por puntaje descendente (desempate: disponible antes, matrícula). */
    public List<Candidate> rank(Need need, Collection<AvailabilityClaim> availabilities) {
        return availabilities.stream()
                .map(claim -> score(need, claim))
                .flatMap(Optional::stream)
                .sorted(RANKING)
                .toList();
    }

    /** Vacío si el rol de la persona no coincide en nada con el perfil requerido. */
    public Optional<Candidate> score(Need need, AvailabilityClaim claim) {
        String neededRole = normalize(need.role());
        String personRole = normalize(claim.personRole());
        if (neededRole.isEmpty() || personRole.isEmpty()) {
            return Optional.empty();
        }

        int score;
        List<String> reasons = new ArrayList<>();
        if (neededRole.equals(personRole)) {
            score = IDENTICAL_ROLE_POINTS;
            reasons.add("Rol idéntico (%s) +%d".formatted(claim.personRole().trim(), IDENTICAL_ROLE_POINTS));
        } else {
            Set<String> shared = keywords(neededRole);
            shared.retainAll(keywords(personRole));
            if (shared.isEmpty()) {
                return Optional.empty();
            }
            score = SAME_FAMILY_POINTS;
            reasons.add("Rol de la misma familia (%s; comparte \"%s\") +%d"
                    .formatted(claim.personRole().trim(), String.join("\", \"", shared), SAME_FAMILY_POINTS));
        }

        if (claim.status() == AvailabilityStatus.CONFIRMED) {
            score += CONFIRMED_POINTS;
            reasons.add("disponibilidad confirmada +%d".formatted(CONFIRMED_POINTS));
        } else {
            score += PLANNED_POINTS;
            reasons.add("disponibilidad planificada (sin confirmar) +%d".formatted(PLANNED_POINTS));
        }

        if (claim.availableFrom() == null) {
            reasons.add("sin fecha de disponibilidad informada +0");
        } else if (need.neededFrom() == null) {
            reasons.add("disponible desde %s; la necesidad no indica fecha requerida +0"
                    .formatted(claim.availableFrom()));
        } else if (!claim.availableFrom().isAfter(need.neededFrom())) {
            score += ON_TIME_POINTS;
            reasons.add("disponible desde %s, a tiempo para la fecha requerida %s +%d"
                    .formatted(claim.availableFrom(), need.neededFrom(), ON_TIME_POINTS));
        } else {
            reasons.add("disponible desde %s, después de la fecha requerida %s +0"
                    .formatted(claim.availableFrom(), need.neededFrom()));
        }

        return Optional.of(new Candidate(
                claim.personId(), claim.personName(), claim.personRole(), claim.availableFrom(),
                claim.status(), claim.source(), claim.observedAt(),
                Math.min(100, score), String.join("; ", reasons)));
    }

    /** Minúsculas, sin tildes, y cualquier separador reducido a un espacio. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static Set<String> keywords(String normalizedRole) {
        Set<String> words = new LinkedHashSet<>(List.of(normalizedRole.split(" ")));
        words.removeAll(NOISE_WORDS);
        return words;
    }
}
