package com.portal.service;

import com.portal.entity.EligibilityCriteria;
import com.portal.entity.Skill;
import com.portal.entity.Student;
import com.portal.entity.StudentSkill;
import com.portal.repository.StudentSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EligibilityEngineTest {

    private StudentSkillRepository skillRepo;
    private EligibilityEngine engine;

    @BeforeEach
    void setUp() {
        skillRepo = mock(StudentSkillRepository.class);
        engine = new EligibilityEngine(skillRepo);
        ReflectionTestUtils.setField(engine, "requireVerification", true);
        when(skillRepo.findByStudent(any())).thenReturn(List.of(skill("Java"), skill("SQL"), skill("Python")));
    }

    private StudentSkill skill(String name) {
        return StudentSkill.builder().skill(Skill.builder().name(name).build()).build();
    }

    /** The exact worked example from the HLD poster. */
    private Student student() {
        return Student.builder().name("A").department("CSE").semester(7)
                .cgpa(8.2).backlogs(0).verified(true).build();
    }

    private EligibilityCriteria criteria() {
        return EligibilityCriteria.builder().minCgpa(7.5).maxBacklogs(0)
                .eligibleBranches("CSE,IT").requiredSkills("Java,SQL").build();
    }

    @Test
    void hldWorkedExampleIsEligible() {
        EligibilityEngine.EligibilityResult r = engine.evaluate(student(), criteria());
        assertTrue(r.eligible);
        assertTrue(r.reasons.isEmpty());
    }

    @Test
    void lowCgpaIsNotEligible() {
        Student s = student();
        s.setCgpa(7.0);
        EligibilityEngine.EligibilityResult r = engine.evaluate(s, criteria());
        assertFalse(r.eligible);
        assertTrue(r.reasons.get(0).contains("CGPA"));
    }

    @Test
    void backlogsAreNotEligible() {
        Student s = student();
        s.setBacklogs(1);
        assertFalse(engine.evaluate(s, criteria()).eligible);
    }

    @Test
    void wrongBranchIsNotEligible() {
        Student s = student();
        s.setDepartment("MECH");
        assertFalse(engine.evaluate(s, criteria()).eligible);
    }

    @Test
    void missingSkillIsNotEligible() {
        EligibilityCriteria c = criteria();
        c.setRequiredSkills("Java,SQL,Docker");
        EligibilityEngine.EligibilityResult r = engine.evaluate(student(), c);
        assertFalse(r.eligible);
        assertTrue(r.reasons.get(0).contains("DOCKER"));
    }

    @Test
    void unverifiedStudentIsNotEligible() {
        Student s = student();
        s.setVerified(false);
        EligibilityEngine.EligibilityResult r = engine.evaluate(s, criteria());
        assertFalse(r.eligible);
        assertTrue(r.reasons.get(0).contains("verified"));
    }

    @Test
    void missingCertificationIsNotEligible() {
        EligibilityCriteria c = criteria();
        c.setRequiredCertifications("AWS Cloud Practitioner");
        assertFalse(engine.evaluate(student(), c).eligible);

        Student s = student();
        s.setCertifications("aws cloud practitioner, Oracle Java SE");
        assertTrue(engine.evaluate(s, c).eligible);
    }

    @Test
    void noCriteriaMeansOpenToVerifiedStudents() {
        assertTrue(engine.evaluate(student(), null).eligible);
    }

    @Test
    void criteriaChangesTakeEffectImmediately() {
        EligibilityCriteria c = criteria();
        assertTrue(engine.evaluate(student(), c).eligible);
        c.setMinCgpa(9.0); // company tightens its requirement
        assertFalse(engine.evaluate(student(), c).eligible);
    }
}
