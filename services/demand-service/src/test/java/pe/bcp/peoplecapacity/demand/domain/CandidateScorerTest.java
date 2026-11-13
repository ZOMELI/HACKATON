package pe.bcp.peoplecapacity.demand.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateScorerTest {

    private static final Instant OBSERVED = Instant.parse("2026-11-13T08:00:00Z");
    private static final LocalDate NEEDED_FROM = LocalDate.of(2026, 12, 1);

    private final CandidateScorer scorer = new CandidateScorer();

    @Test
    void identicalRoleConfirmedAndOnTimeScores100() {
        var candidate = scorer.score(need("Data Engineer", NEEDED_FROM),
                claim("U1", "Data Engineer", AvailabilityStatus.CONFIRMED, LocalDate.of(2026, 11, 20))).orElseThrow();

        assertThat(candidate.score()).isEqualTo(100);
        assertThat(candidate.reason())
                .contains("Rol idéntico", "disponibilidad confirmada", "a tiempo para la fecha requerida 2026-12-01");
    }

    @Test
    void identicalRoleIgnoresCaseAccentsAndSeparators() {
        var candidate = scorer.score(need("Diseñador  UX", NEEDED_FROM),
                claim("U1", "disenador-ux", AvailabilityStatus.PLANNED, NEEDED_FROM)).orElseThrow();

        // 60 (idéntico) + 10 (planificada) + 15 (disponible el mismo día cuenta como a tiempo)
        assertThat(candidate.score()).isEqualTo(85);
    }

    @Test
    void sameFamilySharesKeywordAndWeighsLessThanIdentical() {
        var candidate = scorer.score(need("QA Engineer", NEEDED_FROM),
                claim("U1", "Data Engineer", AvailabilityStatus.CONFIRMED, LocalDate.of(2026, 11, 20))).orElseThrow();

        assertThat(candidate.score()).isEqualTo(70); // 30 + 25 + 15
        assertThat(candidate.reason()).contains("misma familia", "engineer");
    }

    @Test
    void confirmedAddsMoreThanPlanned() {
        var need = need("Backend Developer", NEEDED_FROM);
        int confirmed = scorer.score(need, claim("U1", "Backend Developer", AvailabilityStatus.CONFIRMED, NEEDED_FROM))
                .orElseThrow().score();
        int planned = scorer.score(need, claim("U2", "Backend Developer", AvailabilityStatus.PLANNED, NEEDED_FROM))
                .orElseThrow().score();

        assertThat(confirmed - planned).isEqualTo(15);
    }

    @Test
    void lateAvailabilityOrMissingDatesGetNoOnTimePoints() {
        var late = scorer.score(need("Backend Developer", NEEDED_FROM),
                claim("U1", "Backend Developer", AvailabilityStatus.PLANNED, LocalDate.of(2027, 1, 20))).orElseThrow();
        var needWithoutDate = scorer.score(need("Backend Developer", null),
                claim("U1", "Backend Developer", AvailabilityStatus.PLANNED, LocalDate.of(2026, 11, 20))).orElseThrow();
        var claimWithoutDate = scorer.score(need("Backend Developer", NEEDED_FROM),
                claim("U1", "Backend Developer", AvailabilityStatus.PLANNED, null)).orElseThrow();

        assertThat(late.score()).isEqualTo(70);
        assertThat(late.reason()).contains("después de la fecha requerida");
        assertThat(needWithoutDate.score()).isEqualTo(70);
        assertThat(claimWithoutDate.score()).isEqualTo(70);
    }

    @Test
    void noRoleMatchIsExcluded() {
        assertThat(scorer.score(need("Scrum Master", NEEDED_FROM),
                claim("U1", "Backend Developer", AvailabilityStatus.CONFIRMED, NEEDED_FROM))).isEmpty();
        assertThat(scorer.score(need("Scrum Master", NEEDED_FROM),
                claim("U1", null, AvailabilityStatus.CONFIRMED, NEEDED_FROM))).isEmpty();
    }

    @Test
    void seniorityWordsDoNotMakeAFamily() {
        assertThat(scorer.score(need("Senior Backend Developer", NEEDED_FROM),
                claim("U1", "Senior Scrum Master", AvailabilityStatus.CONFIRMED, NEEDED_FROM))).isEmpty();
    }

    @Test
    void rankOrdersByScoreDescThenEarlierAvailabilityThenPersonId() {
        var ranked = scorer.rank(need("Backend Developer", NEEDED_FROM), List.of(
                claim("U3", "Frontend Developer", AvailabilityStatus.CONFIRMED, LocalDate.of(2026, 11, 20)), // 70
                claim("U2", "Backend Developer", AvailabilityStatus.PLANNED, LocalDate.of(2026, 11, 25)),    // 85
                claim("U1", "Backend Developer", AvailabilityStatus.PLANNED, LocalDate.of(2026, 11, 20)),    // 85
                claim("U5", "Backend Developer", AvailabilityStatus.CONFIRMED, LocalDate.of(2026, 11, 20)),  // 100
                claim("U4", "Scrum Master", AvailabilityStatus.CONFIRMED, LocalDate.of(2026, 11, 20))));     // fuera

        assertThat(ranked).extracting(Candidate::personId).containsExactly("U5", "U1", "U2", "U3");
        assertThat(ranked).extracting(Candidate::score).containsExactly(100, 85, 85, 70);
    }

    private static Need need(String role, LocalDate neededFrom) {
        return new Need("N1", "EXT-1", false, "Pagos", "Proyecto", role, null, Mechanism.UNDEFINED, neededFrom,
                NeedStatus.OPEN, "EXCEL_RECRUITMENT", OBSERVED, null, null, null, null);
    }

    private static AvailabilityClaim claim(String personId, String role, AvailabilityStatus status, LocalDate from) {
        return new AvailabilityClaim(personId, "Persona " + personId, role, status, from, null, "JIRA", OBSERVED);
    }
}
