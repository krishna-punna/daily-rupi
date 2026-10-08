package com.dailyrupi;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Client ids and the changes feed used by the Android app's offline sync. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "USER")
class ExpenseSyncTest {

    @Autowired
    MockMvc mvc;

    private long create(String url, String name) throws Exception {
        String body = mvc.perform(json(post(url), "{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private long item(String name) throws Exception {
        long category = create("/api/master-data/categories", name + " Cat");
        long subCategory = create("/api/master-data/categories/" + category + "/sub-categories", name + " Sub");
        return create("/api/master-data/sub-categories/" + subCategory + "/items", name + " Item");
    }

    private long paymentMethod() throws Exception {
        String body = mvc.perform(get("/api/payment-methods")).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    private static String expenseJson(long itemId, long paymentMethodId, String amount, String clientId) {
        return expenseJson(itemId, paymentMethodId, amount, clientId, LocalDateTime.now().minusHours(1));
    }

    private static String expenseJson(long itemId, long paymentMethodId, String amount, String clientId,
            LocalDateTime spentAt) {
        return "{\"itemId\":" + itemId + ",\"paymentMethodId\":" + paymentMethodId + ",\"amount\":" + amount
                + ",\"spentAt\":\"" + spentAt.truncatedTo(ChronoUnit.MINUTES) + "\""
                + (clientId == null ? "" : ",\"clientId\":\"" + clientId + "\"") + "}";
    }

    private static long id(String body) {
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void retriedCreateWithTheSameClientIdIsSavedOnce() throws Exception {
        long item = item("Retry");
        long payment = paymentMethod();
        String clientId = UUID.randomUUID().toString();

        String first = mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "42.50", clientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn().getResponse().getContentAsString();
        String retry = mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "42.50", clientId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(id(retry)).isEqualTo(id(first));
        mvc.perform(get("/api/expenses/changes"))
                .andExpect(jsonPath("$.expenses[?(@.clientId == '" + clientId + "')].id").value(
                        org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void webExpensesHaveNoClientIdAndBadClientIdsAreRefused() throws Exception {
        long item = item("Web");
        long payment = paymentMethod();

        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "5", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").doesNotExist());
        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "5", "not-a-uuid")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changesReturnsOnlyWhatChangedIncludingDeletions() throws Exception {
        long item = item("Changes");
        long payment = paymentMethod();
        String keptClientId = UUID.randomUUID().toString();
        String deletedClientId = UUID.randomUUID().toString();

        long kept = id(mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "10", keptClientId)))
                .andReturn().getResponse().getContentAsString());
        long deleted = id(mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "20", deletedClientId)))
                .andReturn().getResponse().getContentAsString());

        // Everything when no time is given.
        mvc.perform(get("/api/expenses/changes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nextSince").exists())
                .andExpect(jsonPath("$.expenses[*].id", hasItem((int) kept)))
                .andExpect(jsonPath("$.deleted").isEmpty());

        LocalDateTime since = LocalDateTime.now();
        Thread.sleep(5);
        mvc.perform(json(put("/api/expenses/" + kept), expenseJson(item, payment, "11", null)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/expenses/" + deleted).with(csrf())).andExpect(status().isNoContent());

        mvc.perform(get("/api/expenses/changes").param("since", since.toString())).andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses[*].id", hasItem((int) kept)))
                .andExpect(jsonPath("$.expenses[*].id", not(hasItem((int) deleted))))
                .andExpect(jsonPath("$.expenses[?(@.id == " + kept + ")].amount", hasItem(11.0)))
                .andExpect(jsonPath("$.expenses[?(@.id == " + kept + ")].clientId", hasItem(keptClientId)))
                .andExpect(jsonPath("$.deleted[*].id", hasItem((int) deleted)))
                .andExpect(jsonPath("$.deleted[*].clientId", hasItem(deletedClientId)));

        // Nothing from before is repeated once the window has moved on.
        mvc.perform(get("/api/expenses/changes").param("since", LocalDateTime.now().plusSeconds(1).toString()))
                .andExpect(jsonPath("$.expenses").isEmpty())
                .andExpect(jsonPath("$.deleted").isEmpty());

        // A late retry of the deleted expense's create does not bring it back.
        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "20", deletedClientId)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("DELETED"));
    }

    @Test
    void listAndFirstFetchCanBeLimitedToDays() throws Exception {
        long item = item("Days");
        long payment = paymentMethod();
        LocalDate today = LocalDate.now();
        LocalDate old = today.minusDays(20);

        long recent = id(mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "7", null)))
                .andReturn().getResponse().getContentAsString());
        long older = id(mvc.perform(json(post("/api/expenses"),
                        expenseJson(item, payment, "8", null, old.atTime(10, 0))))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/expenses").param("from", old.toString()).param("to", old.toString()).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) older)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) recent))))
                .andExpect(jsonPath("$.content[*].spentAt", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.startsWith(old.toString()))));
        mvc.perform(get("/api/expenses").param("from", today.toString()).param("to", old.toString()))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/expenses/changes").param("from", today.minusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenses[*].id", hasItem((int) recent)))
                .andExpect(jsonPath("$.expenses[*].id", not(hasItem((int) older))));
    }
}
