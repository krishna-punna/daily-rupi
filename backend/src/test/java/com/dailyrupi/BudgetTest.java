package com.dailyrupi;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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

/**
 * Each test uses its own months in the past, so tests sharing the in-memory database
 * never see each other's budgets or expenses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "USER")
class BudgetTest {

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

    private long paymentMethod() throws Exception {
        String body = mvc.perform(get("/api/payment-methods")).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    /** A category with one item, returned as {categoryId, itemId}. */
    private long[] categoryWithItem(String name) throws Exception {
        long category = create("/api/master-data/categories", name);
        long sub = create("/api/master-data/categories/" + category + "/sub-categories", name + " Sub");
        long item = create("/api/master-data/sub-categories/" + sub + "/items", name + " Item");
        return new long[] {category, item};
    }

    private void spend(long item, String amount, LocalDateTime at) throws Exception {
        mvc.perform(json(post("/api/expenses"), "{\"itemId\":" + item + ",\"paymentMethodId\":" + paymentMethod()
                + ",\"amount\":" + amount + ",\"spentAt\":\"" + at + "\"}"))
                .andExpect(status().isCreated());
    }

    private static String line(long categoryId) {
        return "$.lines[?(@.categoryId == " + categoryId + ")]";
    }

    @Test
    void budgetShowsSpendingForThatMonthOnly() throws Exception {
        long[] food = categoryWithItem("Budget Food");
        long[] fuel = categoryWithItem("Budget Fuel");

        // Spends on the first and last moment of March 2024, and just outside it.
        spend(food[1], "400", LocalDateTime.of(2024, 3, 1, 0, 0));
        spend(food[1], "250.50", LocalDateTime.of(2024, 3, 31, 23, 59));
        spend(food[1], "999", LocalDateTime.of(2024, 4, 1, 0, 0));
        spend(fuel[1], "120", LocalDateTime.of(2024, 3, 15, 9, 30));

        mvc.perform(json(put("/api/budgets/2024-03/categories/" + food[0]), "{\"amount\":600}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2024-03"))
                .andExpect(jsonPath(line(food[0]) + ".budget").value(hasItem(600.0)))
                .andExpect(jsonPath(line(food[0]) + ".spent").value(hasItem(650.5)))
                .andExpect(jsonPath(line(food[0]) + ".remaining").value(hasItem(-50.5)));

        // Fuel has spending but no budget: it is listed and counted as unbudgeted.
        mvc.perform(get("/api/budgets/2024-03")).andExpect(status().isOk())
                .andExpect(jsonPath(line(fuel[0]) + ".budget").value(hasItem((Object) null)))
                .andExpect(jsonPath(line(fuel[0]) + ".spent").value(hasItem(120.0)))
                .andExpect(jsonPath("$.totalBudget").value(600.0))
                .andExpect(jsonPath("$.totalSpent").value(770.5))
                .andExpect(jsonPath("$.unbudgetedSpent").value(120.0));

        // Changing the amount updates the same budget.
        mvc.perform(json(put("/api/budgets/2024-03/categories/" + food[0]), "{\"amount\":700.25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudget").value(700.25));
    }

    @Test
    void budgetCanBeRemoved() throws Exception {
        long[] rent = categoryWithItem("Budget Rent");
        mvc.perform(json(put("/api/budgets/2024-05/categories/" + rent[0]), "{\"amount\":15000}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/budgets/2024-05/categories/" + rent[0]).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(line(rent[0]) + ".budget").value(hasItem((Object) null)))
                .andExpect(jsonPath("$.totalBudget").value(0));
        mvc.perform(delete("/api/budgets/2024-05/categories/" + rent[0]).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void copyFromPreviousMonthKeepsBudgetsAlreadySet() throws Exception {
        long[] a = categoryWithItem("Copy A");
        long[] b = categoryWithItem("Copy B");
        long[] gone = categoryWithItem("Copy Gone");
        mvc.perform(json(put("/api/budgets/2024-06/categories/" + a[0]), "{\"amount\":100}"));
        mvc.perform(json(put("/api/budgets/2024-06/categories/" + b[0]), "{\"amount\":200}"));
        mvc.perform(json(put("/api/budgets/2024-06/categories/" + gone[0]), "{\"amount\":300}"));
        mvc.perform(json(put("/api/budgets/2024-07/categories/" + b[0]), "{\"amount\":250}"));
        mvc.perform(json(patch("/api/master-data/categories/" + gone[0] + "/status"), "{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/budgets/2024-07/copy-previous").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copied").value(1))
                .andExpect(jsonPath("$.month." + line(a[0]).substring(2) + ".budget").value(hasItem(100.0)))
                .andExpect(jsonPath("$.month." + line(b[0]).substring(2) + ".budget").value(hasItem(250.0)))
                .andExpect(jsonPath("$.month.lines[*].categoryId", not(hasItem((int) gone[0]))));

        // Running it again copies nothing new.
        mvc.perform(post("/api/budgets/2024-07/copy-previous").with(csrf()))
                .andExpect(jsonPath("$.copied").value(0));
    }

    @Test
    void inactiveCategoryCannotGetANewBudgetButKeepsAnExistingOne() throws Exception {
        long[] old = categoryWithItem("Budget Old");
        mvc.perform(json(put("/api/budgets/2024-08/categories/" + old[0]), "{\"amount\":500}"))
                .andExpect(status().isOk());
        mvc.perform(json(patch("/api/master-data/categories/" + old[0] + "/status"), "{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/budgets/2024-08"))
                .andExpect(jsonPath(line(old[0]) + ".categoryActive").value(hasItem(false)));
        mvc.perform(json(put("/api/budgets/2024-08/categories/" + old[0]), "{\"amount\":550}"))
                .andExpect(status().isOk());
        mvc.perform(json(put("/api/budgets/2024-09/categories/" + old[0]), "{\"amount\":500}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INACTIVE_CATEGORY"));
        // Without a budget or spending, an inactive category is left out of the month.
        mvc.perform(get("/api/budgets/2024-09"))
                .andExpect(jsonPath("$.lines[*].categoryId", not(hasItem((int) old[0]))));
    }

    @Test
    void deletingACategoryRemovesItsBudgets() throws Exception {
        long category = create("/api/master-data/categories", "Budget Temp");
        mvc.perform(json(put("/api/budgets/2024-10/categories/" + category), "{\"amount\":50}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/master-data/categories/" + category).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/budgets/2024-10"))
                .andExpect(jsonPath("$.lines[*].categoryId", not(hasItem((int) category))));
    }

    @Test
    void badInputIsRejected() throws Exception {
        long[] c = categoryWithItem("Budget Bad");
        mvc.perform(get("/api/budgets/2024-13")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MONTH"));
        mvc.perform(get("/api/budgets/March")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/budgets/1999-12")).andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/budgets/2024-11/categories/" + c[0]), "{\"amount\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(json(put("/api/budgets/2024-11/categories/" + c[0]), "{\"amount\":1.234}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/budgets/2024-11/categories/999999"), "{\"amount\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PASSWORD_CHANGE_REQUIRED")
    void budgetsNeedTheUserRole() throws Exception {
        mvc.perform(get("/api/budgets/2024-03")).andExpect(status().isForbidden());
    }
}
