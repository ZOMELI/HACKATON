package pe.bcp.peoplecapacity.demand.infrastructure.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.bcp.peoplecapacity.demand.application.port.in.ListNeedsUseCase;
import pe.bcp.peoplecapacity.demand.application.port.in.RegisterInformalNeedUseCase;
import pe.bcp.peoplecapacity.demand.application.port.in.UpsertNeedsUseCase;
import pe.bcp.peoplecapacity.demand.domain.NeedStatus;
import pe.bcp.peoplecapacity.demand.infrastructure.web.NeedDtos.InformalNeedInput;
import pe.bcp.peoplecapacity.demand.infrastructure.web.NeedDtos.NeedDto;
import pe.bcp.peoplecapacity.demand.infrastructure.web.NeedDtos.NeedInput;
import pe.bcp.peoplecapacity.demand.infrastructure.web.NeedDtos.UpsertNeedsRequest;
import pe.bcp.peoplecapacity.demand.infrastructure.web.NeedDtos.UpsertNeedsResponse;

@RestController
@RequestMapping("/needs")
class NeedController {

    private final UpsertNeedsUseCase upsertNeeds;
    private final RegisterInformalNeedUseCase registerInformalNeed;
    private final ListNeedsUseCase listNeeds;

    NeedController(UpsertNeedsUseCase upsertNeeds, RegisterInformalNeedUseCase registerInformalNeed,
                   ListNeedsUseCase listNeeds) {
        this.upsertNeeds = upsertNeeds;
        this.registerInformalNeed = registerInformalNeed;
        this.listNeeds = listNeeds;
    }

    @PutMapping
    UpsertNeedsResponse upsert(@RequestBody UpsertNeedsRequest request) {
        var commands = request.needs() == null
                ? null
                : request.needs().stream().map(n -> n == null ? null : n.toCommand()).toList();
        var result = upsertNeeds.upsert(commands);
        return new UpsertNeedsResponse(result.created(), result.updated());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    NeedDto registerInformal(@RequestBody InformalNeedInput input,
                             @RequestHeader(name = "X-User-Id", required = false) String userId) {
        return NeedDto.from(registerInformalNeed.register(input.toCommand(), userId));
    }

    @GetMapping
    List<NeedDto> list(@RequestParam(name = "status", required = false) NeedStatus status) {
        return listNeeds.list(status).stream().map(NeedDto::from).toList();
    }
}
