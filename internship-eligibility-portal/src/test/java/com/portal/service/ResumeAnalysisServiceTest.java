package com.portal.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeAnalysisServiceTest {

    private final ResumeAnalysisService service = new ResumeAnalysisService();

    private static final String RESUME = """
            EDUCATION: B.Tech Computer Science
            SKILLS: Java, SQL, Python. Spring Boot and Git.
            PROJECTS: Internship portal using Node.js and REST API.
            """;

    @Test
    void extractsKnownSkillsEvenNextToPunctuation() {
        Set<String> skills = service.extractSkills(RESUME);
        assertTrue(skills.containsAll(Set.of("JAVA", "SQL", "PYTHON", "SPRING BOOT", "GIT", "NODE.JS", "REST API")));
    }

    @Test
    void matchScoreIsPercentageOfRequiredSkillsFound() {
        assertEquals(100.0, service.computeMatchScore(RESUME, "Java,SQL"));
        assertEquals(50.0, service.computeMatchScore(RESUME, "Java,Docker"));
        assertEquals(0.0, service.computeMatchScore(RESUME, "Docker,Kubernetes"));
    }

    @Test
    void noRequiredSkillsMeansFullScore() {
        assertEquals(100.0, service.computeMatchScore(RESUME, null));
        assertEquals(100.0, service.computeMatchScore(RESUME, "  "));
    }

    @Test
    void doesNotMatchSkillInsideAnotherWord() {
        // "JAVASCRIPT" must not satisfy a "JAVA" requirement
        assertEquals(0.0, service.computeMatchScore("Experienced with JavaScript only", "Java"));
    }

    @Test
    void resumeScoreRewardsSectionCoverage() {
        assertTrue(service.computeResumeScore(RESUME) >= 60.0);
        assertEquals(0.0, service.computeResumeScore("   "));
    }
}
