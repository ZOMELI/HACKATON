package pe.bcp.peoplecapacity.demand.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import pe.bcp.peoplecapacity.demand.application.port.in.NeedWithCandidates;
import pe.bcp.peoplecapacity.demand.application.port.in.RegisterInformalNeedUseCase.InformalNeedCommand;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase.NeedCommand;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase.RecruitmentCommand;
import pe.bcp.peoplecapacity.demand.domain.AvailabilityStatus;
import pe.bcp.peoplecapacity.demand.domain.Candidate;
import pe.bcp.peoplecapacity.demand.domain.Mechanism;
import pe.bcp.peoplecapacity.demand.domain.Need;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStage;
import pe.bcp.peoplecapacity.demand.domain.RecruitmentStatus;

/** Cuerpos JSON de contracts/demand.yaml (mismos nombres de campo). */
final class NeedDtos {

    private NeedDtos() {
    }

    record UpsertNeedsRequest(List<NeedInput> needs) {
    }

    record UpsertNeedsResponse(int created, int updated) {
    }

    record RecruitmentStatusDto(RecruitmentStage status, String source, Instant observedAt) {

        static RecruitmentStatusDto from(RecruitmentStatus r) {
            return r == null ? null : new RecruitmentStatusDto(r.status(), r.source(), r.observedAt());
        }
    }

    record NeedInput(
            String externalId,
            String tribe,
            String project,
            String role,
            String seniority,
            Mechanism mechanism,
            LocalDate neededFrom,
            NeedStatus status,
            String source,
            Instant observedAt,
            RecruitmentStatusDto recruitment,
            String evidenceQuote,
            String note) {

        NeedCommand toCommand() {
            RecruitmentCommand r = recruitment == null
                    ? null
                    : new RecruitmentCommand(recruitment.status(), recruitment.source(), recruitment.observedAt());
            return new NeedCommand(externalId, tribe, project, role, seniority, mechanism, neededFrom, status,
                    source, observedAt, r, evidenceQuote, note);
        }
    }

    record InformalNeedInput(
            String tribe,
            String project,
            String role,
            String seniority,
            Mechanism mechanism,
            LocalDate neededFrom,
            String note) {

        InformalNeedCommand toCommand() {
            return new InformalNeedCommand(tribe, project, role, seniority, mechanism, neededFrom, note);
        }
    }

    record CandidateDto(
            String personId,
            String personName,
            String personRole,
            LocalDate availableFrom,
            AvailabilityStatus availabilityStatus,
            String source,
            Instant observedAt,
            int score,
            String reason) {

        static CandidateDto from(Candidate c) {
            return new CandidateDto(c.personId(), c.personName(), c.personRole(), c.availableFrom(),
                    c.availabilityStatus(), c.source(), c.observedAt(), c.score(), c.reason());
        }
    }

    record NeedDto(
            String id,
            String externalId,
            boolean informal,
            String tribe,
            String project,
            String role,
            String seniority,
            Mechanism mechanism,
            LocalDate neededFrom,
            NeedStatus status,
            String source,
            Instant observedAt,
            RecruitmentStatusDto recruitment,
            String note,
            String createdBy,
            List<CandidateDto> candidates,
            boolean candidatesAvailable) {

        static NeedDto from(NeedWithCandidates view) {
            Need n = view.need();
            return new NeedDto(n.id(), n.externalId(), n.informal(), n.tribe(), n.project(), n.role(),
                    n.seniority(), n.mechanism(), n.neededFrom(), n.status(), n.source(), n.observedAt(),
                    RecruitmentStatusDto.from(n.recruitment()), n.note(), n.createdBy(),
                    view.candidates().stream().map(CandidateDto::from).toList(), view.candidatesAvailable());
        }
    }
}
