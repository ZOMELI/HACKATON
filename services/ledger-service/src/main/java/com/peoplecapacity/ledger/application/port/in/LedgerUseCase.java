package com.peoplecapacity.ledger.application.port.in;

import com.peoplecapacity.ledger.domain.model.Claim;
import com.peoplecapacity.ledger.domain.model.PersonProfile;
import java.util.List;
import java.util.Optional;

/** Puerto de entrada del libro de afirmaciones. */
public interface LedgerUseCase {

    BatchResult recordClaims(List<Claim> claims);

    List<Claim> findClaims(String personId, String dimension, String status);

    Optional<PersonProfile> profile(String personId);

    List<Claim> history(String personId);

    List<Claim> audit(String personId);

    Optional<String> salaryGrade(String personId, String requesterId);

    record BatchResult(int recorded, List<Rejected> rejected) {
    }

    record Rejected(int index, String reason) {
    }
}
