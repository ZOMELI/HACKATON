package pe.bcp.peoplecapacity.cases;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Flujo de punta a punta con H2, libro simulado y agents-service apagado (plantilla local). */
@SpringBootTest(properties = {"cases.reference-date=2026-11-13", "cases.reconcile-on-startup=false",
        "agents.enabled=false", "ledger.mode=stub"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CasesApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    @Order(1)
    void reconciliationOpensEightCasesAndIsIdempotent() throws Exception {
        mvc.perform(post("/reconciliations").contentType(MediaType.APPLICATION_JSON).content("{\"syncId\":\"t1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.casesOpened").value(8));
        mvc.perform(post("/reconciliations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casesOpened").value(0))
                .andExpect(jsonPath("$.casesAlreadyKnown").value(8));
    }

    @Test
    @Order(2)
    void eachUserSeesOnlyTheirPeople() throws Exception {
        mvc.perform(get("/cases").header("X-User-Id", "G40001").header("X-User-Role", "MANAGER"))
                .andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/cases").header("X-User-Id", "L20002").header("X-User-Role", "CL"))
                .andExpect(jsonPath("$[*].personId", containsInAnyOrder("U10004", "U10007", "U10009")));
        mvc.perform(get("/cases").header("X-User-Id", "L20001").header("X-User-Role", "CL"))
                .andExpect(jsonPath("$[*].personId", containsInAnyOrder("U10001", "U10002", "U10006", "U10008")));
        // La CAL ve lo de su CL (Rosa) más el caso que le toca decidir a ella.
        mvc.perform(get("/cases").header("X-User-Id", "C30001").header("X-User-Role", "CAL"))
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @Order(3)
    void caseShowsSourcedClaimsAndFallbackSummary() throws Exception {
        mvc.perform(get("/cases").param("type", "BUDGET_ASSIGNMENT_MISMATCH"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].claims[*].source", containsInAnyOrder("PREPER", "JIRA")))
                .andExpect(jsonPath("$[0].summary.aiGenerated").value(false))
                .andExpect(jsonPath("$[0].summary.mode").value("FALLBACK"))
                .andExpect(jsonPath("$[0].options", hasSize(3)))
                .andExpect(jsonPath("$[0].dedupeKey").doesNotExist());
    }

    @Test
    @Order(4)
    void decisionNeedsAnIdentifiedAndAuthorizedPerson() throws Exception {
        String id = caseId("AVAILABILITY_UNCONFIRMED");
        String body = "{\"optionId\":\"CONFIRM_AVAILABLE\",\"comment\":\"Confirmado con la tribu\"}";
        mvc.perform(post("/cases/" + id + "/decisions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/cases/" + id + "/decisions").header("X-User-Id", "L20002").header("X-User-Role", "CL")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cases/" + id + "/decisions").header("X-User-Id", "L20001").header("X-User-Role", "CL")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"optionId\":\"INVENTADA\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/cases/" + id + "/decisions").header("X-User-Id", "L20001").header("X-User-Role", "CL")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.decision.decidedBy").value("L20001"))
                .andExpect(jsonPath("$.decision.ledgerClaimIds", hasSize(2)));
        mvc.perform(post("/cases/" + id + "/decisions").header("X-User-Id", "L20001").header("X-User-Role", "CL")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        // La decisión quedó en el libro: la disponibilidad ahora figura confirmada y el caso no se reabre.
        mvc.perform(get("/availability"))
                .andExpect(jsonPath("$[?(@.personId=='U10008')].status", hasItem("CONFIRMED")));
        mvc.perform(post("/reconciliations")).andExpect(jsonPath("$.casesOpened").value(0));
        mvc.perform(get("/cases/NO-EXISTE")).andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void availabilityAndAlertSettings() throws Exception {
        mvc.perform(get("/availability"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].personId").value("U10012"))
                .andExpect(jsonPath("$[?(@.personId=='U10014')].alert", hasItem(false)))
                .andExpect(jsonPath("$[?(@.personId=='U10014')].confirmerId", hasItem("L20002")));
        mvc.perform(get("/availability").header("X-User-Id", "L20001").header("X-User-Role", "CL"))
                .andExpect(jsonPath("$[*].personId", not(hasItem("U10014"))));
        mvc.perform(get("/alert-settings")).andExpect(jsonPath("$.availabilityDays").value(15));
        mvc.perform(put("/alert-settings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractExpiryDays\":0,\"availabilityDays\":90}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/alert-settings").header("X-User-Id", "G40001").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractExpiryDays\":30,\"availabilityDays\":90}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.updatedBy").value("G40001"));
        mvc.perform(get("/availability"))
                .andExpect(jsonPath("$[?(@.personId=='U10014')].alert", hasItem(true)));
        mvc.perform(post("/reconciliations")).andExpect(jsonPath("$.casesOpened").value(1));
    }

    private String caseId(String type) throws Exception {
        String json = mvc.perform(get("/cases").param("type", type)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[0].id");
    }
}
