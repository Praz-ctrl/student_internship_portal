CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BIT NOT NULL DEFAULT 1,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    lockout_expiry DATETIME(6),
    created_at DATETIME(6)
);

CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    prn VARCHAR(255) UNIQUE,
    department VARCHAR(255),
    semester INT,
    cgpa DOUBLE,
    backlogs INT,
    phone VARCHAR(255),
    internship_preferences VARCHAR(255),
    certifications VARCHAR(2000),
    projects VARCHAR(4000),
    verified BIT NOT NULL DEFAULT 0,
    CONSTRAINT fk_student_app_user FOREIGN KEY (app_user_id) REFERENCES app_user(id)
);

CREATE TABLE faculty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    CONSTRAINT fk_faculty_app_user FOREIGN KEY (app_user_id) REFERENCES app_user(id)
);

CREATE TABLE company (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(255) NOT NULL,
    industry VARCHAR(255),
    contact VARCHAR(255),
    CONSTRAINT fk_company_app_user FOREIGN KEY (app_user_id) REFERENCES app_user(id)
);

CREATE TABLE academic_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    semester INT,
    subject VARCHAR(255),
    marks DOUBLE,
    sgpa DOUBLE,
    CONSTRAINT fk_academic_student FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE student_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    document_type VARCHAR(255),
    file_path VARCHAR(255),
    verification_status VARCHAR(20),
    CONSTRAINT fk_document_student FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE resume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE,
    file_path VARCHAR(255),
    raw_text LONGTEXT,
    extracted_skills LONGTEXT,
    resume_score DOUBLE,
    CONSTRAINT fk_resume_student FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE student_skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency VARCHAR(255),
    CONSTRAINT fk_student_skill_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_student_skill_skill FOREIGN KEY (skill_id) REFERENCES skill(id)
);

CREATE TABLE internship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    description LONGTEXT,
    duration VARCHAR(255),
    stipend DOUBLE,
    location VARCHAR(255),
    work_mode VARCHAR(255),
    vacancies INT,
    deadline DATE,
    status VARCHAR(20),
    interview_stages VARCHAR(255),
    CONSTRAINT fk_internship_company FOREIGN KEY (company_id) REFERENCES company(id)
);

CREATE TABLE eligibility_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    internship_id BIGINT NOT NULL UNIQUE,
    min_cgpa DOUBLE,
    max_backlogs INT,
    eligible_branches VARCHAR(255),
    required_skills VARCHAR(255),
    required_certifications VARCHAR(255),
    required_semester INT,
    min_resume_score DOUBLE,
    min_atr_score DOUBLE,
    CONSTRAINT fk_criteria_internship FOREIGN KEY (internship_id) REFERENCES internship(id)
);

CREATE TABLE internship_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    internship_id BIGINT NOT NULL,
    application_date DATETIME(6),
    status VARCHAR(30),
    resume_match_score DOUBLE,
    CONSTRAINT uq_student_internship UNIQUE (student_id, internship_id),
    CONSTRAINT fk_application_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_application_internship FOREIGN KEY (internship_id) REFERENCES internship(id)
);

CREATE TABLE assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE,
    atr_score DOUBLE,
    aptitude_score DOUBLE,
    coding_score DOUBLE,
    CONSTRAINT fk_assessment_application FOREIGN KEY (application_id) REFERENCES internship_application(id)
);

CREATE TABLE interview (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    type VARCHAR(30),
    round INT,
    scheduled_date DATETIME(6),
    mode VARCHAR(255),
    result VARCHAR(10),
    remarks LONGTEXT,
    CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES internship_application(id)
);

CREATE TABLE offer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE,
    offer_date DATE,
    status VARCHAR(20),
    CONSTRAINT fk_offer_application FOREIGN KEY (application_id) REFERENCES internship_application(id)
);

CREATE TABLE verification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    faculty_id BIGINT,
    verification_type VARCHAR(255),
    status VARCHAR(20),
    remarks LONGTEXT,
    CONSTRAINT fk_verification_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_verification_faculty FOREIGN KEY (faculty_id) REFERENCES faculty(id)
);

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_user_id BIGINT NOT NULL,
    type VARCHAR(255),
    message LONGTEXT,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6),
    CONSTRAINT fk_notification_user FOREIGN KEY (app_user_id) REFERENCES app_user(id)
);
