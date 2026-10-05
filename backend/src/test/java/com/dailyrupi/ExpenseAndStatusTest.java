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
import java.time.temporal.ChronoUnit;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "USER")
class ExpenseAndStatusTest {

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

    private long firstPaymentMethod() throws Exception {
        String body = mvc.perform(get("/api/payment-methods")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("UPI"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    private static String expenseJson(long itemId, long paymentMethodId, String amount, LocalDateTime spentAt) {
        return "{\"itemId\":" + itemId + ",\"paymentMethodId\":" + paymentMethodId + ",\"amount\":" + amount
                + ",\"spentAt\":\"" + spentAt.truncatedTo(ChronoUnit.MINUTES) + "\",\"note\":\"test\"}";
    }

    @Test
    void renameStatusAndDeleteWorkAtEveryLevel() throws Exception {
        long category = create("/api/master-data/categories", "Crud Cat");
        long subCategory = create("/api/master-data/categories/" + category + "/sub-categories", "Crud Sub");
        long item = create("/api/master-data/sub-categories/" + subCategory + "/items", "Crud Item");

        mvc.perform(json(put("/api/master-data/items/" + item), "{\"name\":\"Crud Item Renamed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Crud Item Renamed"));
        mvc.perform(json(patch("/api/master-data/sub-categories/" + subCategory + "/status"), "{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        // A parent with children cannot be deleted.
        mvc.perform(delete("/api/master-data/categories/" + category).with(csrf()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));

        mvc.perform(delete("/api/master-data/items/" + item).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/master-data/sub-categories/" + subCategory).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/master-data/categories/" + category).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/master-data/categories/" + category).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactiveEntriesAreHiddenFromDropdownsButNotFromManagement() throws Exception {
        long category = create("/api/master-data/categories", "Hidden Cat");
        long subCategory = create("/api/master-data/categories/" + category + "/sub-categories", "Hidden Sub");
        long item = create("/api/master-data/sub-categories/" + subCategory + "/items", "Hidden Item");
        long payment = firstPaymentMethod();

        mvc.perform(get("/api/master-data")).andExpect(jsonPath("$[*].name", hasItem("Hidden Cat")));

        // Making the category inactive hides it, and everything under it.
        mvc.perform(json(patch("/api/master-data/categories/" + category + "/status"), "{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/master-data")).andExpect(jsonPath("$[*].name", not(hasItem("Hidden Cat"))));
        mvc.perform(get("/api/master-data?includeInactive=true"))
                .andExpect(jsonPath("$[*].name", hasItem("Hidden Cat")));

        // The API refuses it too, not only the dropdown.
        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "10.00", LocalDateTime.now())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INACTIVE_ITEM"));
    }

    @Test
    void expenseCanBeAddedEditedListedAndDeleted() throws Exception {
        long category = create("/api/master-data/categories", "Spend Cat");
        long subCategory = create("/api/master-data/categories/" + category + "/sub-categories", "Spend Sub");
        long item = create("/api/master-data/sub-categories/" + subCategory + "/items", "Spend Item");
        long payment = firstPaymentMethod();

        String body = mvc.perform(json(post("/api/expenses"),
                expenseJson(item, payment, "250.50", LocalDateTime.now().minusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(250.50))
                .andExpect(jsonPath("$.categoryName").value("Spend Cat"))
                .andExpect(jsonPath("$.subCategoryName").value("Spend Sub"))
                .andExpect(jsonPath("$.itemName").value("Spend Item"))
                .andExpect(jsonPath("$.paymentMethodName").value("UPI"))
                .andReturn().getResponse().getContentAsString();
        long expense = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(get("/api/expenses")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].itemName", hasItem("Spend Item")));

        // An item with expenses cannot be deleted, only made inactive.
        mvc.perform(delete("/api/master-data/items/" + item).with(csrf()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IN_USE"));

        // Once inactive, the existing expense can still be edited with the same item.
        mvc.perform(json(patch("/api/master-data/items/" + item + "/status"), "{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(json(put("/api/expenses/" + expense), expenseJson(item, payment, "300", LocalDateTime.now())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(300));

        mvc.perform(delete("/api/expenses/" + expense).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/expenses/" + expense).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void futureDatesAndBadAmountsAreRejected() throws Exception {
        long category = create("/api/master-data/categories", "Rule Cat");
        long subCategory = create("/api/master-data/categories/" + category + "/sub-categories", "Rule Sub");
        long item = create("/api/master-data/sub-categories/" + subCategory + "/items", "Rule Item");
        long payment = firstPaymentMethod();

        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "10", LocalDateTime.now().plusHours(1))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FUTURE_DATE"));
        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "0", LocalDateTime.now())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(json(post("/api/expenses"), expenseJson(item, payment, "-5", LocalDateTime.now())))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/expenses"), expenseJson(999999, payment, "10", LocalDateTime.now())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REFERENCE"));
    }
}
