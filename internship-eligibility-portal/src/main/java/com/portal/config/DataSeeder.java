package com.portal.config;

import com.portal.entity.*;
import com.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds sample data in the "demo" profile only, so every portal is explorable right after
 * `mvn spring-boot:run`.
 *
 * Demo logins (password for all: password123)
 *   student@demo.com   verified, CSE, CGPA 8.2, 0 backlogs, skills Java/SQL/Python
 *   student2@demo.com  NOT verified, CSE, CGPA 7.9, 1 backlog, skills Java
 *   faculty@demo.com
 *   company@demo.com   has two OPEN internships (one the verified student qualifies for, one not)
 *   admin@demo.com
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CompanyRepository companyRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final InternshipRepository internshipRepository;
    private final EligibilityCriteriaRepository eligibilityCriteriaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String password = passwordEncoder.encode("password123");

        // ---- skills dictionary ----
        Skill java = skillRepository.save(Skill.builder().name("Java").build());
        Skill sql = skillRepository.save(Skill.builder().name("SQL").build());
        Skill python = skillRepository.save(Skill.builder().name("Python").build());
        skillRepository.save(Skill.builder().name("React").build());
        skillRepository.save(Skill.builder().name("Spring Boot").build());
        skillRepository.save(Skill.builder().name("Machine Learning").build());

        // ---- verified student (matches the HLD worked example) ----
        AppUser su = appUserRepository.save(AppUser.builder()
                .email("student@demo.com").password(password).role(Role.STUDENT).build());
        Student student = studentRepository.save(Student.builder()
                .appUser(su).name("Demo Student").prn("PRN2026CSE045")
                .department("CSE").semester(7).cgpa(8.2).backlogs(0)
                .internshipPreferences("Backend Development, Hybrid")
                .certifications("Oracle Java SE")
                .projects("Internship management portal (Spring Boot + MySQL)")
                .verified(true)
                .build());
        studentSkillRepository.save(StudentSkill.builder().student(student).skill(java).proficiency("ADVANCED").build());
        studentSkillRepository.save(StudentSkill.builder().student(student).skill(sql).proficiency("ADVANCED").build());
        studentSkillRepository.save(StudentSkill.builder().student(student).skill(python).proficiency("INTERMEDIATE").build());

        // ---- unverified student (shows the verification gate + NOT ELIGIBLE reasons) ----
        AppUser su2 = appUserRepository.save(AppUser.builder()
                .email("student2@demo.com").password(password).role(Role.STUDENT).build());
        Student student2 = studentRepository.save(Student.builder()
                .appUser(su2).name("Second Student").prn("PRN2026CSE046")
                .department("CSE").semester(7).cgpa(7.9).backlogs(1)
                .verified(false)
                .build());
        studentSkillRepository.save(StudentSkill.builder().student(student2).skill(java).proficiency("BEGINNER").build());

        // ---- faculty ----
        AppUser fu = appUserRepository.save(AppUser.builder()
                .email("faculty@demo.com").password(password).role(Role.FACULTY).build());
        facultyRepository.save(Faculty.builder().appUser(fu).name("Dr. Mehta").department("CSE").build());

        // ---- admin ----
        appUserRepository.save(AppUser.builder()
                .email("admin@demo.com").password(password).role(Role.ADMIN).build());

        // ---- company + two internships ----
        AppUser cu = appUserRepository.save(AppUser.builder()
                .email("company@demo.com").password(password).role(Role.COMPANY).build());
        Company company = companyRepository.save(Company.builder()
                .appUser(cu).companyName("NexaTech Solutions").industry("Software Services")
                .contact("hr@nexatech.example").build());

        createInternship(company, "Backend Development Intern",
                "Work on REST APIs and database design using Java and MySQL.", 15000.0, "Pune", "HYBRID",
                "APTITUDE_TEST,CODING_TEST,TECHNICAL,HR",
                7.5, 0, "CSE,IT", "Java,SQL", null);

        createInternship(company, "Data Science Intern",
                "Build and evaluate machine-learning models on real datasets.", 20000.0, "Bengaluru", "ONLINE",
                "APTITUDE_TEST,TECHNICAL",
                9.0, 0, "CSE", "Python,Machine Learning", 60.0);
    }

    private void createInternship(Company company, String role, String description, double stipend,
                                  String location, String workMode, String stages,
                                  double minCgpa, int maxBacklogs, String branches, String skills,
                                  Double minAtr) {
        Internship internship = internshipRepository.save(Internship.builder()
                .company(company).role(role).description(description)
                .duration("6 months").stipend(stipend).location(location)
                .workMode(workMode).vacancies(3)
                .deadline(LocalDate.now().plusMonths(2))
                .interviewStages(stages)
                .status(InternshipStatus.OPEN)
                .build());
        eligibilityCriteriaRepository.save(EligibilityCriteria.builder()
                .internship(internship)
                .minCgpa(minCgpa).maxBacklogs(maxBacklogs)
                .eligibleBranches(branches).requiredSkills(skills)
                .minAtrScore(minAtr)
                .build());
    }
}
