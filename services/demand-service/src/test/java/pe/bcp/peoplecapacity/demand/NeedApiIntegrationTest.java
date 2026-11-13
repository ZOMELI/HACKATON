package pe.bcp.peoplecapacity.demand;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Perfil local: H2 en memoria, libro en modo stub y necesidades de ejemplo sembradas. */
@SpringBootTest(properties = {"ledger.mode=stub", "demand.seed-examples=true"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class NeedApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void putCreatesThenUpdatesByExternalId() throws Exception {
        String body = """
                {"needs":[{"externalId":"IT-PUT-1","tribe":"Pagos","project":"Core","role":"DevOps Engineer",
                  "mechanism":"PROVIDER","neededFrom":"2026-12-15","source":"EXCEL_RECRUITMENT",
                  "observedAt":"2026-11-13T08:00:00Z",
                  "recruitment":{"status":"%s","source":"EXCEL_RECRUITMENT","observedAt":"2026-11-13T08:00:00Z"}}]}
                """;

        mvc.perform(put("/needs").contentType(MediaType.APPLICATION_JSON).content(body.formatted("SOURCING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(0));

        mvc.perform(put("/needs").contentType(MediaType.APPLICATION_JSON).content(body.formatted("OFFER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(1));

        mvc.perform(get("/needs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.externalId=='IT-PUT-1')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.externalId=='IT-PUT-1')].recruitment.status").value("OFFER"))
                .andExpect(jsonPath("$[?(@.externalId=='IT-PUT-1')].recruitment.source").value("EXCEL_RECRUITMENT"))
                .andExpect(jsonPath("$[?(@.externalId=='IT-PUT-1')].informal").value(false))
                .andExpect(jsonPath("$[?(@.externalId=='IT-PUT-1')].status").value("OPEN"));
    }

    @Test
    void putRejectsNeedWithoutSourceOrObservedAtAndSavesNothing() throws Exception {
        mvc.perform(put("/needs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"needs":[
                          {"externalId":"IT-BAD-0","role":"QA Engineer","source":"EMAIL","observedAt":"2026-11-13T08:00:00Z"},
                          {"externalId":"IT-BAD-1","role":"QA Engineer","observedAt":"2026-11-13T08:00:00Z"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fuente (source)")));

        mvc.perform(put("/needs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"needs":[{"externalId":"IT-BAD-2","role":"QA Engineer","source":"EMAIL"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fecha de consulta (observedAt)")));

        // Todo o nada: la primera necesidad del lote rechazado tampoco quedó guardada.
        mvc.perform(get("/needs"))
                .andExpect(jsonPath("$[?(@.externalId=='IT-BAD-0')]", hasSize(0)));
    }

    @Test
    void postRegistersInformalNeedFromWeb() throws Exception {
        mvc.perform(post("/needs").contentType(MediaType.APPLICATION_JSON).header("X-User-Id", "U20001")
                        .content("""
                                {"tribe":"Data","role":"data engineer","neededFrom":"2026-12-10","note":"Refuerzo de fin de año"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.externalId").isEmpty())
                .andExpect(jsonPath("$.informal").value(true))
                .andExpect(jsonPath("$.source").value("WEB"))
                .andExpect(jsonPath("$.observedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdBy").value("U20001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.mechanism").value("UNDEFINED"))
                .andExpect(jsonPath("$.recruitment").isEmpty())
                .andExpect(jsonPath("$.note").value("Refuerzo de fin de año"))
                .andExpect(jsonPath("$.candidatesAvailable").value(true))
                .andExpect(jsonPath("$.candidates[0].personId").value("U10012"))
                .andExpect(jsonPath("$.candidates[0].score").value(100));
    }

    @Test
    void postRejectsMissingRoleOrMissingTribeAndProject() throws Exception {
        mvc.perform(post("/needs").contentType(MediaType.APPLICATION_JSON).content("{\"tribe\":\"Data\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("perfil")));

        mvc.perform(post("/needs").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"QA Engineer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("tribu")));
    }

    @Test
    void getReturnsSeededNeedsWithCandidatesFromLedgerOrderedByScore() throws Exception {
        mvc.perform(get("/needs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
                // Data Engineer: Marta Ruiz, rol idéntico + CONFIRMED + a tiempo
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidatesAvailable").value(true))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidates[0].personId").value("U10012"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidates[0].personName").value("Marta Ruiz"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidates[0].availabilityStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidates[0].availableFrom").value("2026-11-20"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidates[0].source").value("DECISION"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-003')].candidates[0].score").value(100))
                // Backend Developer para el 1 de diciembre: Nora Pinto, idéntico + PLANNED pero llega tarde
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-001')].recruitment.status").value("INTERVIEWS"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-001')].candidates[*].personId").value("U10014"))
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-001')].candidates[0].score").value(70))
                // Scrum Master: Paola Quispe, idéntico + PLANNED + a tiempo
                .andExpect(jsonPath("$[?(@.externalId=='REQ-2026-002')].candidates[0].score").value(85))
                // QA Engineer sin seguimiento: solo familia Engineer (Marta), nadie sin coincidencia de rol
                .andExpect(jsonPath("$[?(@.externalId=='MAIL-2026-0117')].recruitment").value((Object) null))
                .andExpect(jsonPath("$[?(@.externalId=='MAIL-2026-0117')].candidates[*].personId").value("U10012"))
                .andExpect(jsonPath("$[?(@.externalId=='MAIL-2026-0117')].candidates[0].score").value(70));
    }

    @Test
    void getFiltersByStatusAndRejectsUnknownStatus() throws Exception {
        mvc.perform(get("/needs").param("status", "COVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].externalId").value("REQ-2026-004"));

        mvc.perform(get("/needs").param("status", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status")));
    }

    @Test
    void corsAllowsTheWebOriginAndHealthIsUp() throws Exception {
        mvc.perform(get("/needs").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
