package pe.bcp.peoplecapacity.agents.infrastructure;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Los dos endpoints en modo recorded, sin clave de OpenAI: el contexto debe arrancar y no llamar al modelo. */
@SpringBootTest(properties = {"agents.mode=recorded", "spring.ai.openai.api-key=sin-clave"})
@AutoConfigureMockMvc
class RecordedModeWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void recordedEmailReturnsItsRecordedExtraction() throws Exception {
        postJson("/evidence-extractions", """
                {"emailId":"MAIL-002","from":"Julia Meza","subject":"Aprobación","receivedAt":"2026-10-10T09:15:00-05:00",
                 "body":"Apruebo el movimiento MOV-102 de Luis Ramos (U10002) a la tribu Riesgos desde el 15 de octubre."}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value("MAIL-002"))
                .andExpect(jsonPath("$.type").value("MOVEMENT_APPROVAL"))
                .andExpect(jsonPath("$.personIds[0]").value("U10002"))
                .andExpect(jsonPath("$.movementId").value("MOV-102"))
                .andExpect(jsonPath("$.contractId").value(nullValue()))
                .andExpect(jsonPath("$.effectiveFrom").value("2026-10-15"))
                .andExpect(jsonPath("$.approver").value("C30001"))
                .andExpect(jsonPath("$.quote").value(containsString("Apruebo el movimiento MOV-102")))
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.mode").value("RECORDED"));
    }

    @Test
    void unrecordedEmailUsesTheHeuristic() throws Exception {
        postJson("/evidence-extractions", """
                {"emailId":"MAIL-777","receivedAt":"2026-11-12T10:00:00Z",
                 "body":"Hola,\\n\\nSe renueva el acta ACT-305 de Iván Soto (U10013) hasta el 30 de junio de 2027.\\n\\nSaludos,\\nPedro Alva (L20002)"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailId").value("MAIL-777"))
                .andExpect(jsonPath("$.type").value("CONTRACT_RENEWAL"))
                .andExpect(jsonPath("$.personIds", hasSize(1)))
                .andExpect(jsonPath("$.personIds[0]").value("U10013"))
                .andExpect(jsonPath("$.contractId").value("ACT-305"))
                .andExpect(jsonPath("$.effectiveUntil").value("2027-06-30"))
                .andExpect(jsonPath("$.approver").value("L20002"))
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.mode").value("RECORDED"));
    }

    @Test
    void unrecordedEmailWithoutSignalIsIrrelevant() throws Exception {
        postJson("/evidence-extractions", """
                {"emailId":"MAIL-778","body":"Recuerden registrar sus horas antes del viernes."}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("IRRELEVANT"))
                .andExpect(jsonPath("$.personIds", hasSize(0)))
                .andExpect(jsonPath("$.quote").value(nullValue()));
    }

    @Test
    void evidenceExtractionRequiresEmailIdAndBody() throws Exception {
        postJson("/evidence-extractions", "{\"emailId\":\"MAIL-1\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Falta body"));
        postJson("/evidence-extractions", "{\"body\":\"texto\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Falta emailId"));
        postJson("/evidence-extractions", "{esto no es json")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void caseSummaryUsesTheTemplateAndKeepsTheOptions() throws Exception {
        // Incluye campos extra de cases.yaml (claimId, sourceRef, emailId) que deben ignorarse sin error.
        postJson("/case-summaries", """
                {"caseId":"CASE-006","type":"CONTRACT_EXPIRING","title":"Acta por vencer","personId":"U10007",
                 "personName":"Jorge Salas","dueDate":"2026-11-30",
                 "claims":[{"claimId":"CLM-0038","subject":"CONTRACT","value":"ACT-301",
                            "attributes":{"provider":"TechPerú SAC"},"source":"EXCEL_CONTRACTS","sourceRef":"ACT-301",
                            "observedAt":"2026-11-13T08:00:00Z","validFrom":"2026-06-01","validUntil":"2026-11-30"}],
                 "evidence":[],
                 "options":[{"id":"RENEW","label":"Renovar el acta"},{"id":"REPLACE","label":"Reemplazar"},
                            {"id":"END","label":"No renovar"}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseId").value("CASE-006"))
                .andExpect(jsonPath("$.summary").value(containsString(
                        "El acta ACT-301 de Jorge Salas (U10007) con el proveedor TechPerú SAC vence el 30 de noviembre de 2026")))
                .andExpect(jsonPath("$.options", hasSize(3)))
                .andExpect(jsonPath("$.options[0].id").value("RENEW"))
                .andExpect(jsonPath("$.options[1].id").value("REPLACE"))
                .andExpect(jsonPath("$.options[2].id").value("END"))
                .andExpect(jsonPath("$.options[0].text").value(containsString("Renovar el acta")))
                .andExpect(jsonPath("$.aiGenerated").value(false))
                .andExpect(jsonPath("$.mode").value("RECORDED"));
    }

    @Test
    void caseSummaryRequiresCaseIdTypeAndOptions() throws Exception {
        postJson("/case-summaries", "{\"type\":\"CONTRACT_EXPIRING\",\"options\":[]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Falta caseId"));
        postJson("/case-summaries", "{\"caseId\":\"C1\",\"options\":[]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Falta type"));
        postJson("/case-summaries", "{\"caseId\":\"C1\",\"type\":\"CONTRACT_EXPIRING\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Falta options"));
        postJson("/case-summaries", "{\"caseId\":\"C1\",\"type\":\"X\",\"options\":[{\"id\":\"A\"}]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void corsIsOpenForTheLocalWeb() throws Exception {
        mockMvc.perform(options("/case-summaries")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void healthIsUpAndUnknownRoutesAnswerJson() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").isNotEmpty());
    }
}
