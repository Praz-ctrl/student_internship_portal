package com.portal.service;

import com.portal.dto.StudentDtos.AcademicRecordRequest;
import com.portal.dto.StudentDtos.ProfileUpdateRequest;
import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final StudentSkillRepository studentSkillRepository;
    private final ResumeRepository resumeRepository;
    private final StudentDocumentRepository documentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final ResumeAnalysisService resumeAnalysisService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Student getByAppUser(AppUser user) {
        return studentRepository.findByAppUser(user)
                .orElseThrow(() -> new ApiException("Student profile not found", HttpStatus.NOT_FOUND));
    }

    // ---------------- profile ----------------
    public Student updateProfile(AppUser user, ProfileUpdateRequest req) {
        Student student = getByAppUser(user);
        if (req.getName() != null) student.setName(req.getName());
        if (req.getDepartment() != null) student.setDepartment(req.getDepartment().trim().toUpperCase());
        if (req.getSemester() != null) student.setSemester(req.getSemester());
        if (req.getCgpa() != null) {
            if (req.getCgpa() < 0 || req.getCgpa() > 10) {
                throw new ApiException("CGPA must be between 0 and 10", HttpStatus.BAD_REQUEST);
            }
            student.setCgpa(req.getCgpa());
        }
        if (req.getBacklogs() != null) {
            if (req.getBacklogs() < 0) throw new ApiException("Backlogs cannot be negative", HttpStatus.BAD_REQUEST);
            student.setBacklogs(req.getBacklogs());
        }
        if (req.getPhone() != null) student.setPhone(req.getPhone());
        if (req.getInternshipPreferences() != null) student.setInternshipPreferences(req.getInternshipPreferences());
        if (req.getCertifications() != null) student.setCertifications(req.getCertifications());
        if (req.getProjects() != null) student.setProjects(req.getProjects());

        // Changing verified academic data invalidates the faculty verification.
        if (req.getCgpa() != null || req.getBacklogs() != null || req.getDepartment() != null
                || req.getSemester() != null) {
            student.setVerified(false);
        }
        return studentRepository.save(student);
    }

    // ---------------- semester-wise marksheet records ----------------
    public AcademicRecord addRecord(AppUser user, AcademicRecordRequest req) {
        Student student = getByAppUser(user);
        if (req.getSemester() == null || req.getSubject() == null || req.getSubject().isBlank()) {
            throw new ApiException("Semester and subject are required", HttpStatus.BAD_REQUEST);
        }
        student.setVerified(false);
        studentRepository.save(student);
        return academicRecordRepository.save(AcademicRecord.builder()
                .student(student)
                .semester(req.getSemester())
                .subject(req.getSubject().trim())
                .marks(req.getMarks())
                .sgpa(req.getSgpa())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AcademicRecord> records(AppUser user) {
        return academicRecordRepository.findByStudent(getByAppUser(user));
    }

    public void deleteRecord(AppUser user, Long id) {
        Student student = getByAppUser(user);
        AcademicRecord record = academicRecordRepository.findById(id)
                .orElseThrow(() -> new ApiException("Record not found", HttpStatus.NOT_FOUND));
        if (!record.getStudent().getId().equals(student.getId())) {
            throw new ApiException("Not your record", HttpStatus.FORBIDDEN);
        }
        academicRecordRepository.delete(record);
    }

    /** CGPA = unweighted average of the per-semester average SGPA values entered by the student. */
    public Student recalculateCgpa(AppUser user) {
        Student student = getByAppUser(user);
        Map<Integer, Double> perSemester = academicRecordRepository.findByStudent(student).stream()
                .filter(r -> r.getSemester() != null && r.getSgpa() != null)
                .collect(Collectors.groupingBy(AcademicRecord::getSemester,
                        Collectors.averagingDouble(AcademicRecord::getSgpa)));
        if (perSemester.isEmpty()) {
            throw new ApiException("Add at least one record with an SGPA first", HttpStatus.BAD_REQUEST);
        }
        double cgpa = perSemester.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        student.setCgpa(Math.round(cgpa * 100.0) / 100.0);
        student.setVerified(false);
        return studentRepository.save(student);
    }

    // ---------------- skills ----------------
    public List<StudentSkill> replaceSkills(AppUser user, List<String> skillNames) {
        Student student = getByAppUser(user);
        studentSkillRepository.deleteByStudent(student);
        if (skillNames == null) return List.of();

        Set<String> unique = skillNames.stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return unique.stream().map(name -> {
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> skillRepository.save(Skill.builder().name(name).build()));
            return studentSkillRepository.save(StudentSkill.builder()
                    .student(student).skill(skill).proficiency("INTERMEDIATE").build());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSkill> getSkills(AppUser user) {
        return studentSkillRepository.findByStudent(getByAppUser(user));
    }

    // ---------------- resume ----------------
    public Resume saveResumeText(AppUser user, String rawText) {
        return saveResume(getByAppUser(user), null, rawText);
    }

    public Resume saveResumeFile(AppUser user, MultipartFile file) {
        Student student = getByAppUser(user);
        String ext = FileStorageService.extensionOf(file == null ? null : file.getOriginalFilename());
        if (!ext.equals("pdf") && !ext.equals("txt")) {
            throw new ApiException("Resume must be a PDF or TXT file", HttpStatus.BAD_REQUEST);
        }
        String text;
        try {
            text = resumeAnalysisService.extractText(ext, file.getBytes());
        } catch (IOException e) {
            throw new ApiException("Could not read the uploaded file", HttpStatus.BAD_REQUEST);
        }
        String stored = fileStorageService.store(file);
        return saveResume(student, stored, text);
    }

    private Resume saveResume(Student student, String filePath, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new ApiException("The resume has no readable text (scanned image PDFs cannot be parsed)", HttpStatus.BAD_REQUEST);
        }
        Resume resume = resumeRepository.findByStudent(student)
                .orElse(Resume.builder().student(student).build());
        if (filePath != null) resume.setFilePath(filePath);
        resume.setRawText(rawText);
        resume.setExtractedSkills(String.join(",", resumeAnalysisService.extractSkills(rawText)));
        resume.setResumeScore(resumeAnalysisService.computeResumeScore(rawText));
        return resumeRepository.save(resume);
    }

    @Transactional(readOnly = true)
    public Resume getResume(AppUser user) {
        return resumeRepository.findByStudent(getByAppUser(user)).orElse(null);
    }

    // ---------------- documents ----------------
    public StudentDocument uploadDocument(AppUser user, String documentType, MultipartFile file) {
        Student student = getByAppUser(user);
        if (documentType == null || documentType.isBlank()) {
            throw new ApiException("Document type is required", HttpStatus.BAD_REQUEST);
        }
        String stored = fileStorageService.store(file);
        return documentRepository.save(StudentDocument.builder()
                .student(student)
                .documentType(documentType.trim().toUpperCase())
                .filePath(stored)
                .verificationStatus(VerificationStatus.PENDING)
                .build());
    }

    @Transactional(readOnly = true)
    public List<StudentDocument> getDocuments(AppUser user) {
        return documentRepository.findByStudent(getByAppUser(user));
    }

    @Transactional(readOnly = true)
    public StudentDocument getOwnDocument(AppUser user, Long documentId) {
        Student student = getByAppUser(user);
        StudentDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException("Document not found", HttpStatus.NOT_FOUND));
        if (!doc.getStudent().getId().equals(student.getId())) {
            throw new ApiException("Not your document", HttpStatus.FORBIDDEN);
        }
        return doc;
    }
}
