package pe.bcp.peoplecapacity.demand.application.port.in;

import java.util.List;
import pe.bcp.peoplecapacity.demand.domain.Candidate;
import pe.bcp.peoplecapacity.demand.domain.Need;

/**
 * Necesidad con sus candidatos.
 * {@code candidatesAvailable=false} significa que el libro no respondió y la lista viene vacía.
 */
public record NeedWithCandidates(Need need, List<Candidate> candidates, boolean candidatesAvailable) {
}
