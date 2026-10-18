package com.portal.service;

import com.portal.exception.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resume parsing and matching: extracts text from PDF/TXT resumes, finds known skill
 * keywords, scores resume completeness, and computes a Resume Match Score against an
 * internship's required skills. Dependency-light on purpose (PDFBox only for PDF text).
 */
@Service
public class ResumeAnalysisService {

    // Vocabulary used to show "extracted skills". Matching against an internship's
    // required skills does NOT depend on this list (see computeMatchScore).
    private static final Set<String> KNOWN_SKILLS = Set.of(
            "JAVA", "PYTHON", "C", "C++", "C#", "JAVASCRIPT", "TYPESCRIPT", "SQL", "MYSQL",
            "POSTGRESQL", "MONGODB", "SPRING", "SPRING BOOT", "HIBERNATE", "REACT", "ANGULAR",
            "NODE.JS", "NODEJS", "EXPRESS", "HTML", "CSS", "BOOTSTRAP", "GIT", "GITHUB", "DOCKER",
            "KUBERNETES", "AWS", "AZURE", "GCP", "LINUX", "REST API", "MACHINE LEARNING",
            "DEEP LEARNING", "DATA STRUCTURES", "ALGORITHMS", "DBMS", "OOP", "DJANGO", "FLASK",
            "PANDAS", "NUMPY", "TENSORFLOW", "PYTORCH", "ANDROID", "KOTLIN", "SWIFT", "PHP",
            "JDBC", "MAVEN", "JUNIT", "POSTMAN", "FIGMA"
    );

    /** Reads the text of a PDF or TXT resume. */
    public String extractText(String extension, byte[] data) {
        try {
            if ("pdf".equalsIgnoreCase(extension)) {
                try (PDDocument doc = Loader.loadPDF(data)) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            if ("txt".equalsIgnoreCase(extension)) {
                return new String(data, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new ApiException("Could not read the resume file", HttpStatus.BAD_REQUEST);
        }
        throw new ApiException("Resume must be a PDF or TXT file", HttpStatus.BAD_REQUEST);
    }

    /** Upper-cases and strips punctuation so skills can be matched as whole words/phrases. */
    private String normalize(String text) {
        if (text == null) return " ";
        String t = text.toUpperCase().replaceAll("[^A-Z0-9+#. ]", " ");
        t = t.replaceAll("\\.(?=\\s|$)", " ");   // sentence-ending dots, keep "NODE.JS"
        t = t.replaceAll("\\s+", " ");
        return " " + t.trim() + " ";
    }

    /** Known skill keywords found in the resume text. */
    public Set<String> extractSkills(String rawText) {
        if (rawText == null || rawText.isBlank()) return Set.of();
        String normalized = normalize(rawText);
        Set<String> found = new LinkedHashSet<>();
        for (String skill : KNOWN_SKILLS) {
            if (normalized.contains(" " + skill + " ")) {
                found.add(skill);
            }
        }
        return found;
    }

    /** Generic resume completeness score (0-100) based on section coverage and length. */
    public double computeResumeScore(String rawText) {
        if (rawText == null || rawText.isBlank()) return 0.0;
        String upper = rawText.toUpperCase();
        double score = 0;
        if (upper.contains("EDUCATION")) score += 20;
        if (upper.contains("PROJECT")) score += 20;
        if (upper.contains("SKILL")) score += 20;
        if (upper.contains("CERTIFICATION") || upper.contains("CERTIFICATE")) score += 15;
        if (upper.contains("EXPERIENCE") || upper.contains("INTERNSHIP")) score += 15;
        if (rawText.length() > 800) score += 10;
        return Math.min(100.0, score);
    }

    /**
     * Resume Match Score (0-100): percentage of the internship's required skills that
     * literally appear in the resume text. No required skills means a score of 100.
     */
    public double computeMatchScore(String rawText, String requiredSkillsCsv) {
        if (requiredSkillsCsv == null || requiredSkillsCsv.isBlank()) return 100.0;
        Set<String> required = Arrays.stream(requiredSkillsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (required.isEmpty()) return 100.0;

        String normalized = normalize(rawText);
        long matched = required.stream()
                .filter(skill -> normalized.contains(" " + skill + " "))
                .count();
        return (matched * 100.0) / required.size();
    }
}
