package system.wgt.orientation.host.research;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import system.wgt.orientation.application.research.SpatialResearchPromptService;
import system.wgt.orientation.host.place.PlaceApiExceptionHandler;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ResearchPromptControllerTests {
    private final MockMvc mvc = standaloneSetup(new ResearchPromptController(new SpatialResearchPromptService()))
            .setControllerAdvice(new PlaceApiExceptionHandler())
            .build();

    @Test
    void returnsExportablePromptBoundToResearchContract() throws Exception {
        mvc.perform(post("/api/v1/research/prompts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "questionRef":"restaurants-nearby",
                                  "text":"Find suitable restaurants nearby.",
                                  "area":{"center":{"label":"Hamburg Hauptbahnhof","coordinate":{"longitude":10.0067,"latitude":53.5526}},"radiusMeters":5000},
                                  "criteria":[
                                    {"criterionRef":"operator-name-pattern","description":"Match the explicit user-defined name pattern.","evaluationMode":"HEURISTIC"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"contract\":\"orientation.spatial-research-bundle\"")))
                .andExpect(content().string(containsString("\"version\":\"1.0\"")))
                .andExpect(content().string(containsString("questionRef: restaurants-nearby")))
                .andExpect(content().string(containsString("operator-name-pattern [HEURISTIC]")));
    }

    @Test
    void rejectsUnsupportedEvaluationMode() throws Exception {
        mvc.perform(post("/api/v1/research/prompts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "questionRef":"restaurants-nearby",
                                  "text":"Find suitable restaurants nearby.",
                                  "area":{"center":{"label":"Hamburg"},"radiusMeters":5000},
                                  "criteria":[
                                    {"criterionRef":"criterion-1","description":"Example.","evaluationMode":"MAGIC"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("invalid-input")));
    }
}
