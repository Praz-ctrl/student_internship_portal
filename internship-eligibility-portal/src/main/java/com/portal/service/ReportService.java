package com.portal.service;

import com.portal.entity.*;
import com.portal.repository.InternshipApplicationRepository;
import com.portal.repository.InternshipRepository;
import com.portal.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final StudentRepository studentRepository;
    private final InternshipRepository internshipRepository;
    private final InternshipApplicationRepository applicationRepository;
    private final EligibilityEngine eligibilityEngine;

    public Map<String, Object> summary() {
        List<Student> students = studentRepository.findAll();
        List<Internship> internships = internshipRepository.findAll();
        List<InternshipApplication> apps = applicationRepository.findAll();
        List<Internship> open = internships.stream().filter(i -> i.getStatus() == InternshipStatus.OPEN).toList();

        long eligibleStudents = students.stream()
                .filter(s -> open.stream().anyMatch(i -> eligibilityEngine.evaluate(s, i.getEligibilityCriteria()).eligible))
                .count();

        Set<ApplicationStatus> shortlistedStates = EnumSet.of(ApplicationStatus.FACULTY_SHORTLISTED,
                ApplicationStatus.COMPANY_SHORTLISTED, ApplicationStatus.INTERVIEW_SCHEDULED, ApplicationStatus.SELECTED);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalStudents", students.size());
        report.put("verifiedStudents", students.stream().filter(Student::isVerified).count());
        report.put("eligibleStudents", eligibleStudents);
        report.put("totalInternships", internships.size());
        report.put("openInternships", open.size());
        report.put("totalApplications", apps.size());
        report.put("shortlistedApplications", apps.stream().filter(a -> shortlistedStates.contains(a.getStatus())).count());
        report.put("selectedStudents", apps.stream().filter(a -> a.getStatus() == ApplicationStatus.SELECTED)
                .map(a -> a.getStudent().getId()).distinct().count());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status.name(), apps.stream().filter(a -> a.getStatus() == status).count());
        }
        report.put("applicationsByStatus", byStatus);

        report.put("internshipWise", apps.stream().collect(Collectors.groupingBy(
                a -> a.getInternship().getRole() + " @ " + a.getInternship().getCompany().getCompanyName(),
                TreeMap::new, Collectors.counting())));
        report.put("companyWise", apps.stream().collect(Collectors.groupingBy(
                a -> a.getInternship().getCompany().getCompanyName(),
                TreeMap::new, Collectors.counting())));
        report.put("departmentWise", apps.stream().collect(Collectors.groupingBy(
                a -> a.getStudent().getDepartment() == null ? "Unknown" : a.getStudent().getDepartment(),
                TreeMap::new, Collectors.counting())));
        return report;
    }
}
