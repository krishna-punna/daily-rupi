package com.dailyrupi;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterDataTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousCannotReadMasterData() throws Exception {
        mvc.perform(get("/api/master-data")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PASSWORD_CHANGE_REQUIRED")
    void temporaryPasswordCannotReadMasterData() throws Exception {
        mvc.perform(get("/api/master-data")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void seededTreeIsReturned() throws Exception {
        mvc.perform(get("/api/master-data")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Housing"))
                .andExpect(jsonPath("$[?(@.isDefault == true)]", hasSize(17)))
                .andExpect(jsonPath("$[?(@.name == 'Loans & EMIs')].subCategories[*].items[*].name",
                        hasItem("Car EMI")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void customEntriesCanBeAddedAtEveryLevel() throws Exception {
        mvc.perform(post("/api/master-data/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Farm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Farm"))
                .andExpect(jsonPath("$.isDefault").value(false));

        mvc.perform(post("/api/master-data/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"farm\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));

        mvc.perform(post("/api/master-data/categories/999999/sub-categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Seeds\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void markupInNamesIsRejected() throws Exception {
        mvc.perform(post("/api/master-data/categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void writesWithoutCsrfTokenAreForbidden() throws Exception {
        mvc.perform(post("/api/master-data/categories")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Farm2\"}"))
                .andExpect(status().isForbidden());
    }
}
