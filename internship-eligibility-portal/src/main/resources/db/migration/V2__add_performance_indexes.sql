-- Performance Indexes on Foreign Keys, Status Columns, and Frequent Lookups
CREATE INDEX idx_app_user_role ON app_user(role);

CREATE INDEX idx_student_user_id ON student(app_user_id);
CREATE INDEX idx_student_department ON student(department);
CREATE INDEX idx_student_verified ON student(verified);
CREATE INDEX idx_student_cgpa ON student(cgpa);

CREATE INDEX idx_faculty_user_id ON faculty(app_user_id);
CREATE INDEX idx_company_user_id ON company(app_user_id);

CREATE INDEX idx_academic_student ON academic_record(student_id);

CREATE INDEX idx_document_student ON student_document(student_id);
CREATE INDEX idx_document_status ON student_document(verification_status);

CREATE INDEX idx_student_skill_student ON student_skill(student_id);
CREATE INDEX idx_student_skill_skill ON student_skill(skill_id);

CREATE INDEX idx_internship_company ON internship(company_id);
CREATE INDEX idx_internship_status ON internship(status);
CREATE INDEX idx_internship_deadline ON internship(deadline);

CREATE INDEX idx_criteria_internship ON eligibility_criteria(internship_id);

CREATE INDEX idx_application_student ON internship_application(student_id);
CREATE INDEX idx_application_internship ON internship_application(internship_id);
CREATE INDEX idx_application_status ON internship_application(status);

CREATE INDEX idx_assessment_application ON assessment(application_id);

CREATE INDEX idx_interview_application ON interview(application_id);
CREATE INDEX idx_interview_result ON interview(result);

CREATE INDEX idx_offer_application ON offer(application_id);
CREATE INDEX idx_offer_status ON offer(status);

CREATE INDEX idx_verification_student ON verification(student_id);
CREATE INDEX idx_verification_faculty ON verification(faculty_id);
CREATE INDEX idx_verification_status ON verification(status);

CREATE INDEX idx_notification_user ON notification(app_user_id);
CREATE INDEX idx_notification_is_read ON notification(is_read);
