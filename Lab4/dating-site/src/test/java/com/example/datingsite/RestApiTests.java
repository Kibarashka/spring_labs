package com.example.datingsite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RestApiTests {

    private static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");
    private static final MediaType JSON_PATCH = MediaType.parseMediaType("application/json-patch+json");

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createProfileReturns201WithLocation() throws Exception {
        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Taras\",\"age\":23,\"keywords\":[\"Музика\",\" музика \",\"кіно\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/profiles/6")))
                .andExpect(jsonPath("$.keywords", contains("музика", "кіно")))
                .andExpect(jsonPath("$.privateInfo").doesNotExist());
    }

    @Test
    void invalidBodyReturnsProblemDetailWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"age\":15}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("age")));
    }

    @Test
    void filteringAndPagination() throws Exception {
        mockMvc.perform(get("/api/profiles").param("minAge", "22").param("maxAge", "25")
                        .param("sort", "age,desc").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].age").value(25))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/profiles").param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mergePatchUpdatesOnlyGivenFieldsAndNullRemovesField() throws Exception {
        mockMvc.perform(patch("/api/profiles/1").header("X-User-Id", "1").contentType(MERGE_PATCH)
                        .content("{\"publicInfo\":\"Нове\",\"privateInfo\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicInfo").value("Нове"))
                .andExpect(jsonPath("$.age").value(22));

        mockMvc.perform(get("/api/profiles/1/private-info").header("X-User-Id", "1"))
                .andExpect(jsonPath("$.privateInfo").value(nullValue()));
    }

    @Test
    void jsonPatchIsAtomicAndFailedTestReturns409() throws Exception {
        mockMvc.perform(patch("/api/profiles/1").header("X-User-Id", "1").contentType(JSON_PATCH)
                        .content("[{\"op\":\"replace\",\"path\":\"/age\",\"value\":30},"
                                + "{\"op\":\"test\",\"path\":\"/name\",\"value\":\"Wrong\"}]"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/profiles/1")).andExpect(jsonPath("$.age").value(22));

        mockMvc.perform(patch("/api/profiles/1").header("X-User-Id", "1").contentType(JSON_PATCH)
                        .content("[{\"op\":\"add\",\"path\":\"/keywords/-\",\"value\":\"кава\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keywords", contains("музика", "подорожі", "кава")));
    }

    @Test
    void patchResultIsValidated() throws Exception {
        mockMvc.perform(patch("/api/profiles/1").header("X-User-Id", "1").contentType(MERGE_PATCH)
                        .content("{\"age\":10}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errors[0].field").value("age"));
    }

    @Test
    void invitationLifecycle() throws Exception {
        mockMvc.perform(post("/api/invitations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderId\":1,\"receiverId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/invitations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderId\":1,\"receiverId\":2}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/profiles/2/private-info").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/invitations/1").header("X-User-Id", "1").contentType(MERGE_PATCH)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/invitations/1").header("X-User-Id", "2").contentType(MERGE_PATCH)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/profiles/2/private-info").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateInfo").value("oleg@example.com"));

        mockMvc.perform(delete("/api/invitations/1").header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteOwnProfileOnly() throws Exception {
        mockMvc.perform(delete("/api/profiles/2").header("X-User-Id", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/profiles/2").header("X-User-Id", "2"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/profiles/2"))
                .andExpect(status().isNotFound());
    }
}
