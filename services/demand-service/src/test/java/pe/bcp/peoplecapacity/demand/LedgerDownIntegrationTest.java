package pe.bcp.peoplecapacity.demand;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Libro caído: adaptador HTTP real apuntando a un puerto donde nadie escucha.
 * El tablero debe responder 200 con candidates=[] y candidatesAvailable=false.
 */
@SpringBootTest(properties = {
        "ledger.mode=http",
        "demand.seed-examples=true",
        "ledger.url=http://localhost:1",
        "ledger.connect-timeout-ms=300",
        "ledger.read-timeout-ms=300",
        // Base propia para no compartir la H2 en memoria con el otro contexto de pruebas
        "spring.datasource.url=jdbc:h2:mem:demand-ledger-down;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS demand"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LedgerDownIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void getDegradesSafelyWhenLedgerDoesNotRespond() throws Exception {
        mvc.perform(get("/needs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[*].candidatesAvailable", everyItem(is(false))))
                .andExpect(jsonPath("$[*].candidates[*]", empty()));
    }

    @Test
    void postStillRegistersTheNeedWhenLedgerDoesNotRespond() throws Exception {
        mvc.perform(post("/needs").contentType(MediaType.APPLICATION_JSON).header("X-User-Id", "U20001")
                        .content("{\"project\":\"Motor de Fraude\",\"role\":\"QA Engineer\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.informal").value(true))
                .andExpect(jsonPath("$.candidates", empty()))
                .andExpect(jsonPath("$.candidatesAvailable").value(false));
    }
}
