-- Sample DBMS queries over the portal schema (MySQL). Useful for the viva:
-- JOINs, GROUP BY, aggregates, subqueries. Run against internship_portal.

-- 1. SQL equivalent of the Eligibility Engine (student id 1):
--    open internships whose CGPA / backlog / branch rules the student satisfies.
SELECT i.id, i.role, c.company_name, ec.min_cgpa, ec.max_backlogs
FROM student s
JOIN internship i              ON i.status = 'OPEN'
JOIN company c                 ON c.id = i.company_id
JOIN eligibility_criteria ec   ON ec.internship_id = i.id
WHERE s.id = 1
  AND s.verified = 1
  AND s.cgpa     >= ec.min_cgpa
  AND s.backlogs <= ec.max_backlogs
  AND (ec.eligible_branches IS NULL OR FIND_IN_SET(s.department, ec.eligible_branches) > 0);

-- 2. Required skills a student is missing for an internship (internship id 1, student id 1)
SELECT sk.name AS skill_the_student_has
FROM student_skill ss JOIN skill sk ON sk.id = ss.skill_id
WHERE ss.student_id = 1;

-- 3. Applications per company (JOIN + GROUP BY)
SELECT c.company_name, COUNT(a.id) AS applications
FROM company c
JOIN internship i ON i.company_id = c.id
LEFT JOIN internship_application a ON a.internship_id = i.id
GROUP BY c.company_name
ORDER BY applications DESC;

-- 4. Department-wise application and selection counts
SELECT s.department,
       COUNT(*)                                            AS applications,
       SUM(CASE WHEN a.status = 'SELECTED' THEN 1 ELSE 0 END) AS selected
FROM internship_application a
JOIN student s ON s.id = a.student_id
GROUP BY s.department;

-- 5. Candidate ranking for one internship: 0.6 * resume match + 0.4 * ATR
SELECT s.name, a.resume_match_score, asm.atr_score,
       ROUND(0.6 * COALESCE(a.resume_match_score, 0) + 0.4 * COALESCE(asm.atr_score, 0), 1) AS composite
FROM internship_application a
JOIN student s            ON s.id = a.student_id
LEFT JOIN assessment asm  ON asm.application_id = a.id
WHERE a.internship_id = 1 AND a.status <> 'WITHDRAWN'
ORDER BY composite DESC;

-- 6. Funnel: how many applications are at each stage (aggregate)
SELECT status, COUNT(*) AS total FROM internship_application GROUP BY status;

-- 7. Students never shortlisted (subquery)
SELECT s.name FROM student s
WHERE s.id NOT IN (
  SELECT a.student_id FROM internship_application a
  WHERE a.status IN ('FACULTY_SHORTLISTED','COMPANY_SHORTLISTED','INTERVIEW_SCHEDULED','SELECTED'));

-- 8. Internships with more applicants than the average (HAVING + subquery)
SELECT i.role, COUNT(a.id) AS applicants
FROM internship i JOIN internship_application a ON a.internship_id = i.id
GROUP BY i.id, i.role
HAVING COUNT(a.id) > (SELECT COUNT(*) / COUNT(DISTINCT internship_id) FROM internship_application);

-- 9. Interview pipeline progress for one application
SELECT type, round, result, scheduled_date
FROM interview WHERE application_id = 1 ORDER BY round;
