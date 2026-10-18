package com.portal.service;

import com.portal.entity.EligibilityCriteria;
import com.portal.entity.Student;
import com.portal.entity.StudentSkill;
import com.portal.repository.StudentSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares a Student's verified profile with an Internship's EligibilityCriteria and
 * returns ELIGIBLE / NOT ELIGIBLE plus human-readable reasons. Every rule is read from
 * the criteria row at call time (never hard-coded), so companies/faculty can change
 * requirements and students are re-evaluated immediately.
 */
@Service
@RequiredArgsConstructor
public class EligibilityEngine {

    private final StudentSkillRepository studentSkillRepository;

    @Value("${app.require-verification:true}")
    private boolean requireVerification;

    public static class EligibilityResult {
        public boolean eligible;
        public List<String> reasons = new ArrayList<>();
    }

    public EligibilityResult evaluate(Student student, EligibilityCriteria criteria) {
        EligibilityResult result = new EligibilityResult();

        // 0. Faculty verification gate (HLD: Faculty Verification -> Eligibility Engine)
        if (requireVerification && !student.isVerified()) {
            result.reasons.add("Your profile has not been verified by faculty yet");
        }

        if (criteria != null) {
            // 1. CGPA
            if (criteria.getMinCgpa() != null && student.getCgpa() != null
                    && student.getCgpa() < criteria.getMinCgpa()) {
                result.reasons.add("CGPA " + student.getCgpa() + " is below the required minimum " + criteria.getMinCgpa());
            }

            // 2. Backlogs
            if (criteria.getMaxBacklogs() != null && student.getBacklogs() != null
                    && student.getBacklogs() > criteria.getMaxBacklogs()) {
                result.reasons.add("Backlogs " + student.getBacklogs() + " exceed the allowed maximum " + criteria.getMaxBacklogs());
            }

            // 3. Branch / Department
            if (notBlank(criteria.getEligibleBranches())) {
                Set<String> allowed = toSet(criteria.getEligibleBranches());
                if (student.getDepartment() == null || !allowed.contains(student.getDepartment().trim().toUpperCase())) {
                    result.reasons.add("Branch " + student.getDepartment() + " is not in the eligible list: " + criteria.getEligibleBranches());
                }
            }

            // 4. Required semester
            if (criteria.getRequiredSemester() != null && student.getSemester() != null
                    && !criteria.getRequiredSemester().equals(student.getSemester())) {
                result.reasons.add("Requires semester " + criteria.getRequiredSemester() + ", student is in semester " + student.getSemester());
            }

            // 5. Required skills
            if (notBlank(criteria.getRequiredSkills())) {
                Set<String> required = toSet(criteria.getRequiredSkills());
                List<StudentSkill> studentSkills = studentSkillRepository.findByStudent(student);
                Set<String> owned = studentSkills.stream()
                        .map(ss -> ss.getSkill().getName().trim().toUpperCase())
                        .collect(Collectors.toSet());
                List<String> missing = required.stream().filter(s -> !owned.contains(s)).sorted().toList();
                if (!missing.isEmpty()) {
                    result.reasons.add("Missing required skills: " + String.join(", ", missing));
                }
            }

            // 6. Required certifications
            if (notBlank(criteria.getRequiredCertifications())) {
                Set<String> required = toSet(criteria.getRequiredCertifications());
                Set<String> owned = notBlank(student.getCertifications())
                        ? toSet(student.getCertifications()) : Set.of();
                List<String> missing = required.stream().filter(c -> !owned.contains(c)).sorted().toList();
                if (!missing.isEmpty()) {
                    result.reasons.add("Missing required certifications: " + String.join(", ", missing));
                }
            }
        }

        result.eligible = result.reasons.isEmpty();
        return result;
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private Set<String> toSet(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }
}
