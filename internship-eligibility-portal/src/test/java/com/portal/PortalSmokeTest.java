package com.portal;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end smoke test against the seeded demo data (in-memory H2). */
@SpringBootTest
@AutoConfigureMockMvc
class PortalSmokeTest {

    @Autowired
    private MockMvc mvc;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void protectedEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/students/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleIsForbidden() throws Exception {
        String company = login("company@demo.com");
        mvc.perform(get("/api/students/me").header("Authorization", "Bearer " + company))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordHashIsNeverExposed() throws Exception {
        String student = login("student@demo.com");
        String body = mvc.perform(get("/api/students/me").header("Authorization", "Bearer " + student))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("password"));
    }

    @Test
    void verifiedStudentSeesEligibilityAndAppliesOnlyOnce() throws Exception {
        String token = login("student@demo.com");

        String body = mvc.perform(get("/api/students/me/internships").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.internship.role=='Backend Development Intern')].eligible", hasItem(true)))
                .andExpect(jsonPath("$[?(@.internship.role=='Data Science Intern')].eligible", hasItem(false)))
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(body, "$[?(@.internship.role=='Backend Development Intern')].internship.id");
        long internshipId = ids.get(0).longValue();

        mvc.perform(post("/api/students/me/applications/" + internshipId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/students/me/applications/" + internshipId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void unverifiedStudentCannotApply() throws Exception {
        String token = login("student2@demo.com");
        String body = mvc.perform(get("/api/students/me/internships").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.internship.role=='Backend Development Intern')].eligible", hasItem(false)))
                .andReturn().getResponse().getContentAsString();

        List<Integer> ids = JsonPath.read(body, "$[?(@.internship.role=='Backend Development Intern')].internship.id");
        mvc.perform(post("/api/students/me/applications/" + ids.get(0)).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
