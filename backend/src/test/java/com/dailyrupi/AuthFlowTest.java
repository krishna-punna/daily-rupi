package com.dailyrupi;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dailyrupi.audit.AuditLogRepository;
import com.dailyrupi.user.DefaultAdminInitializer;
import com.dailyrupi.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String NEW_PASSWORD = "correct-horse-battery-9";

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    AuditLogRepository auditLog;
    @Autowired
    DefaultAdminInitializer initializer;

    @BeforeEach
    void resetAccounts() {
        auditLog.deleteAll();
        users.deleteAll();
        initializer.ensureDefaultAdmin();
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", username).param("password", password));
    }

    private MockHttpSession sessionOf(ResultActions result) {
        return (MockHttpSession) result.andReturn().getRequest().getSession(false);
    }

    private void changePassword(MockHttpSession session, String current, String next) throws Exception {
        mvc.perform(put("/api/auth/password").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/admin/audit-log")).andExpect(status().isUnauthorized());
        mvc.perform(get("/anything-else")).andExpect(status().isUnauthorized());
    }

    @Test
    void csrfEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent());
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "admin").param("password", "admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        login("admin", "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        login("nobody", "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void defaultAdminMustChangePasswordBeforeAnythingElseWorks() throws Exception {
        ResultActions result = login("admin", "admin").andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.roles").isEmpty());
        MockHttpSession session = sessionOf(result);

        mvc.perform(get("/api/admin/audit-log").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        changePassword(session, "admin", NEW_PASSWORD);

        // The old session and the old password are both dead.
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        login("admin", "admin").andExpect(status().isUnauthorized());

        MockHttpSession fresh = sessionOf(login("admin", NEW_PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("USER")));

        mvc.perform(get("/api/admin/audit-log").session(fresh)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event").value("LOGIN_SUCCESS"));
    }

    @Test
    void weakNewPasswordIsRejected() throws Exception {
        MockHttpSession session = sessionOf(login("admin", "admin"));
        mvc.perform(put("/api/auth/password").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"admin\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/auth/password").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"admin\",\"newPassword\":\"my-admin-password-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_TOO_WEAK"));
    }

    @Test
    void accountLocksAfterFiveFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("admin", "wrong-" + i).andExpect(status().isUnauthorized());
        }
        // Even the right password is refused while the lock holds.
        login("admin", "admin").andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = sessionOf(login("admin", "admin"));
        mvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
}
