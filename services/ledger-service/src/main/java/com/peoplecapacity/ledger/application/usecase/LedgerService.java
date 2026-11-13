package com.peoplecapacity.ledger.application.usecase;

import com.peoplecapacity.ledger.application.port.in.LedgerUseCase;
import com.peoplecapacity.ledger.application.port.out.ClaimRepositoryPort;
import com.peoplecapacity.ledger.application.port.out.SalaryGradePort;
import com.peoplecapacity.ledger.domain.model.Claim;
import com.peoplecapacity.ledger.domain.model.Dimension;
import com.peoplecapacity.ledger.domain.model.PersonProfile;
import com.peoplecapacity.ledger.domain.model.PersonProfile.DimensionView;
import com.peoplecapacity.ledger.domain.service.SourceAuthorityPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class LedgerService implements LedgerUseCase {

    private final ClaimRepositoryPort repository;
    private final SalaryGradePort salaryGrades;
    private final SourceAuthorityPolicy policy = new SourceAuthorityPolicy();

    @Inject
    public LedgerService(ClaimRepositoryPort repository, SalaryGradePort salaryGrades) {
        this.repository = repository;
        this.salaryGrades = salaryGrades;
    }

    @Override
    @Transactional
    public BatchResult recordClaims(List<Claim> claims) {
        List<Rejected> rejected = new ArrayList<>();
        int recorded = 0;
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = claims.get(i);
            Optional<String> reason = policy.rejectionReason(claim);
            if (reason.isPresent()) {
                rejected.add(new Rejected(i, reason.get()));
                continue;
            }
            String status = claim.status() == null ? "ASSERTED" : claim.status();
            repository.append(claim.recorded(status, LocalDateTime.now()));
            recorded++;
        }
        return new BatchResult(recorded, rejected);
    }

    @Override
    public List<Claim> findClaims(String personId, String dimension, String status) {
        return repository.search(personId, dimension, status);
    }

    @Override
    public Optional<PersonProfile> profile(String personId) {
        List<Claim> all = repository.search(personId, null, null);
        if (all.isEmpty()) {
            return Optional.empty();
        }
        String name = all.stream().map(Claim::personName).filter(Objects::nonNull).findFirst().orElse(null);
        String email = all.stream().map(Claim::personEmail).filter(Objects::nonNull).findFirst().orElse(null);
        List<DimensionView> views = new ArrayList<>();
        for (Dimension dimension : Dimension.profileDimensions()) {
            List<Claim> current = latestPerSource(all, dimension.name());
            long distinctValues = current.stream().map(Claim::value).distinct().count();
            String state = current.isEmpty() ? "PENDING" : distinctValues > 1 ? "DISCREPANCY" : "CONSISTENT";
            views.add(new DimensionView(dimension.name(), state, current));
        }
        return Optional.of(new PersonProfile(personId, name, email, views));
    }

    @Override
    public List<Claim> history(String personId) {
        return repository.search(personId, null, null);
    }

    @Override
    public List<Claim> audit(String personId) {
        List<Claim> result = new ArrayList<>(repository.search(personId, Dimension.DECISION.name(), null));
        result.addAll(repository.search(personId, Dimension.EVIDENCE.name(), null));
        result.sort((a, b) -> b.recordedAt().compareTo(a.recordedAt()));
        return result;
    }

    @Override
    public Optional<String> salaryGrade(String personId, String requesterId) {
        return salaryGrades.find(personId, requesterId);
    }

    /** Para una dimensión, la afirmación más reciente de cada fuente (la lista llega ordenada de nueva a antigua). */
    private static List<Claim> latestPerSource(List<Claim> all, String dimension) {
        Map<String, Claim> bySource = new LinkedHashMap<>();
        for (Claim claim : all) {
            if (dimension.equals(claim.dimension())) {
                bySource.putIfAbsent(claim.source(), claim);
            }
        }
        return new ArrayList<>(bySource.values());
    }
}
