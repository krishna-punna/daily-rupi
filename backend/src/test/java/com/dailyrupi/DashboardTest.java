package com.dailyrupi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

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

/** Uses months in 2022, which no other test spends in. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "USER")
class DashboardTest {

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

    private long paymentMethod(int index) throws Exception {
        String body = mvc.perform(get("/api/payment-methods")).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[" + index + "].id")).longValue();
    }

    private long item(String category, String name) throws Exception {
        long c = create("/api/master-data/categories", category);
        long sub = create("/api/master-data/categories/" + c + "/sub-categories", category + " Sub");
        return create("/api/master-data/sub-categories/" + sub + "/items", name);
    }

    private void spend(long item, long method, String amount, LocalDateTime at) throws Exception {
        mvc.perform(json(post("/api/expenses"), "{\"itemId\":" + item + ",\"paymentMethodId\":" + method
                + ",\"amount\":" + amount + ",\"spentAt\":\"" + at + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void pastMonthHasDailyTotalsComparisonPaymentMethodsAndTopItems() throws Exception {
        long milk = item("Dash Food", "Dash Milk");
        long petrol = item("Dash Fuel", "Dash Petrol");
        long upi = paymentMethod(0);
        long cash = paymentMethod(1);

        // February 2022 has 28 days.
        spend(milk, upi, "50", LocalDateTime.of(2022, 2, 1, 7, 0));
        spend(milk, cash, "50", LocalDateTime.of(2022, 2, 1, 19, 0));
        spend(petrol, upi, "1000.50", LocalDateTime.of(2022, 2, 28, 23, 59));
        // January 2022: 200 on the 10th and 300 on the 31st, past February's 28 days.
        spend(milk, upi, "200", LocalDateTime.of(2022, 1, 10, 8, 0));
        spend(milk, upi, "300", LocalDateTime.of(2022, 1, 31, 8, 0));
        spend(milk, upi, "75", LocalDateTime.of(2021, 11, 5, 8, 0));

        mvc.perform(get("/api/dashboard/2022-02")).andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2022-02"))
                .andExpect(jsonPath("$.total").value(1100.5))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.dailyAverage").value(39.30))
                .andExpect(jsonPath("$.days", hasSize(28)))
                .andExpect(jsonPath("$.days[0].date").value("2022-02-01"))
                .andExpect(jsonPath("$.days[0].total").value(100.0))
                .andExpect(jsonPath("$.days[1].total").value(0))
                .andExpect(jsonPath("$.days[27].total").value(1000.5))
                .andExpect(jsonPath("$.previousSamePeriod").value(200.0))
                .andExpect(jsonPath("$.previousMonthTotal").value(500.0))
                .andExpect(jsonPath("$.recentMonths", hasSize(6)))
                .andExpect(jsonPath("$.recentMonths[0].month").value("2021-09"))
                .andExpect(jsonPath("$.recentMonths[2].total").value(75.0))
                .andExpect(jsonPath("$.recentMonths[4].total").value(500.0))
                .andExpect(jsonPath("$.recentMonths[5].month").value("2022-02"))
                .andExpect(jsonPath("$.recentMonths[5].total").value(1100.5))
                .andExpect(jsonPath("$.paymentMethods", hasSize(2)))
                .andExpect(jsonPath("$.paymentMethods[0].total").value(1050.5))
                .andExpect(jsonPath("$.paymentMethods[0].count").value(2))
                .andExpect(jsonPath("$.paymentMethods[1].total").value(50.0))
                .andExpect(jsonPath("$.topItems", hasSize(2)))
                .andExpect(jsonPath("$.topItems[0].itemName").value("Dash Petrol"))
                .andExpect(jsonPath("$.topItems[0].categoryName").value("Dash Fuel"))
                .andExpect(jsonPath("$.topItems[1].itemName").value("Dash Milk"))
                .andExpect(jsonPath("$.topItems[1].total").value(100.0))
                .andExpect(jsonPath("$.topItems[1].count").value(2));
    }

    @Test
    void emptyMonthIsAllZeros() throws Exception {
        mvc.perform(get("/api/dashboard/2022-06")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.days", hasSize(30)))
                .andExpect(jsonPath("$.paymentMethods", hasSize(0)))
                .andExpect(jsonPath("$.topItems", hasSize(0)));
    }

    @Test
    void daysStopAtTodayAndAFutureMonthHasNone() throws Exception {
        LocalDate today = LocalDate.now();
        mvc.perform(get("/api/dashboard/" + YearMonth.from(today))).andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(today.getDayOfMonth())));
        mvc.perform(get("/api/dashboard/" + YearMonth.from(today).plusMonths(1))).andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(0)))
                .andExpect(jsonPath("$.dailyAverage").value(0))
                .andExpect(jsonPath("$.previousSamePeriod").value(0));
    }

    @Test
    void badMonthIsRejected() throws Exception {
        mvc.perform(get("/api/dashboard/2022-13")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MONTH"));
    }

    @Test
    @WithMockUser(roles = "PASSWORD_CHANGE_REQUIRED")
    void dashboardNeedsTheUserRole() throws Exception {
        mvc.perform(get("/api/dashboard/2022-02")).andExpect(status().isForbidden());
    }
}
