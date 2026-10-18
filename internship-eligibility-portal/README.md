# Internship Management & Eligibility Portal

B.Tech mini project: a centralized portal where students enter their academic, personal and
resume information **once**; faculty verify it; the system automatically decides which internships
each student is **ELIGIBLE / NOT ELIGIBLE** for; and companies define roles, dynamic requirements and
configurable interview pipelines.

3-tier architecture (as in the HLD poster):

```
Presentation (HTML/CSS/JS + Bootstrap)  ->  Application (Java 17 / Spring Boot 3, REST + JWT)  ->  Database (MySQL, or in-memory H2 for the demo)
```

## 1. Run it (2 minutes, no database needed)

Requirements: **JDK 17+** and **Maven 3.8+** (or open the folder in IntelliJ / Eclipse / VS Code as a Maven project).
In an IDE, install the **Lombok plugin** and enable annotation processing.

```bash
mvn spring-boot:run
```

Open **http://localhost:8080**. The default `demo` profile uses an in-memory H2 database and seeds sample data
(data resets on every restart). H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:portal`, user `sa`, empty password).

Run the automated tests with `mvn test` (unit tests for the Eligibility Engine and Resume Analysis, plus an
end-to-end smoke test of login, role security and the apply flow).

### Demo accounts (password for all: `password123`)

| Account | What it is |
|---|---|
| `student@demo.com` | Verified student: CSE, CGPA 8.2, 0 backlogs, skills Java / SQL / Python (the HLD worked example) |
| `student2@demo.com` | Unverified student: CSE, CGPA 7.9, 1 backlog, skill Java (shows the verification gate and NOT ELIGIBLE reasons) |
| `faculty@demo.com` | Faculty: verification, shortlisting, approvals, reports |
| `company@demo.com` | NexaTech Solutions, with two OPEN internships |
| `admin@demo.com` | Admin: user management + reports |

## 2. Suggested viva walkthrough

1. **Student** (`student@demo.com`) -> *Internships*: "Backend Development Intern" shows **ELIGIBLE**; "Data Science Intern" shows **NOT ELIGIBLE** with reasons (CGPA, missing skills). Apply is disabled for the second one.
2. *Skills & Resume*: upload a PDF/TXT resume -> extracted skills + completeness score. Then *Applications* after applying shows the **Resume Match Score**.
3. **Student 2** sees *"Your profile has not been verified by faculty yet"* on every internship.
4. **Faculty** -> *Student Verification*: verify student 2's profile (or view/verify uploaded documents). Student 2 is re-evaluated immediately.
5. **Company** -> *Post Internship* with its own criteria and interview stages. Status = *pending approval*.
6. **Faculty** -> *Internship Approvals*: approve it; every currently eligible student gets a notification.
7. **Faculty** -> *Applications*: record an **ATR score**, then **Shortlist** (a minimum ATR can be enforced per internship).
8. **Company** -> *My Internships*: applicants are **ranked** (0.6 x resume match + 0.4 x ATR). Shortlist -> **Schedule** the interview stages in order -> record **Pass/Fail** -> **Select** -> **Extend Offer**.
9. **Company** -> *Edit criteria* (e.g. lower the minimum CGPA): students who just became eligible are notified (**Dynamic Preference Engine**).
10. **Student** -> *Offers*: accept/decline; *Notifications*: the full event trail.
11. **Faculty / Admin** -> *Reports & Analytics*: totals, funnel, internship-, company- and department-wise statistics.

## 3. How the HLD maps to the code

| HLD element | Where it lives |
|---|---|
| Student / Faculty / Company / Admin UI | `src/main/resources/static/*.html`, `js/*.js` |
| Authentication, role-based access | `security/`, `config/SecurityConfig.java` (JWT; `/api/students/**`, `/api/companies/**`, `/api/faculty/**`, `/api/interviews/**`, `/api/reports/**`, `/api/admin/**` role-locked) |
| Student profile, marksheets, documents, resume | `StudentService`, `FileStorageService`, `ResumeAnalysisService` |
| Document / profile verification | `FacultyService` (sets `Student.verified`, which the engine checks) |
| **Eligibility Engine** | `EligibilityEngine`: verification, min CGPA, max backlogs, branch, semester, required skills, required certifications. Rules come from the `eligibility_criteria` row, never hard-coded |
| Resume Analysis + Match Score | `ResumeAnalysisService` (PDF/TXT text via Apache PDFBox, whole-word skill matching) |
| ATR / Assessment score | `Assessment` entity; enforced as a minimum at faculty shortlisting; used in ranking |
| Dynamic Preference Engine | `CompanyService.updateInternship` re-evaluates students and notifies the newly eligible; live preview via `GET /api/companies/me/internships/{id}/eligible-students` |
| Shortlisting Engine | `ApplicationService`: apply -> faculty shortlist -> company shortlist (order enforced) + candidate ranking |
| Interview Management | `InterviewService`: per-internship ordered stages; a stage can only be scheduled after the previous one is passed; a FAIL rejects the application; selection requires all stages passed |
| Offer management | `OfferService` |
| Notifications | `NotificationService` (stored in DB, shown in the UI) |
| Reporting & Analytics | `ReportService` |
| Database layer | JPA entities in `entity/`; reference DDL in `docs/schema.sql`; DBMS practice queries in `docs/sample-queries.sql` |

## 4. Business rules enforced by the server (not just the UI)

* Only faculty-**verified** students can apply (`app.require-verification=true`). Editing CGPA, backlogs, branch, semester or marksheet records resets verification.
* A student can apply only if ELIGIBLE, only once per internship, only before the deadline, only to OPEN internships.
* Company-posted internships need faculty approval (`app.require-internship-approval=true`).
* A company can only act on applications for **its own** internships; students can only see their own data.
* Pipeline order: faculty shortlist -> company shortlist -> interview stages (in order) -> select -> offer.
* Passwords are BCrypt-hashed and never returned by the API; uploads are stored under random names, limited to PDF/PNG/JPG/TXT, max 5 MB.

## 5. Using MySQL instead of H2

1. In `src/main/resources/application.properties` change `spring.profiles.active=demo` to `spring.profiles.active=` (empty).
2. Set `spring.datasource.username` / `password` for your MySQL server (the database `internship_portal` is created automatically).
3. `mvn spring-boot:run`. Tables are created automatically. (`docs/schema.sql` shows the same schema if you prefer to create it by hand.)
4. No demo data is seeded in this mode: register accounts from the UI. To make an admin, insert a user with role `ADMIN` (BCrypt password hash) or run once with the demo profile.

## 6. Project layout

```
src/main/java/com/portal/
  entity/ repository/ dto/      data model
  service/                      business logic (EligibilityEngine, ApplicationService, InterviewService, ...)
  controller/                   REST API
  security/ config/ exception/  JWT, security rules, demo seeder, JSON error handling
src/main/resources/static/      frontend
src/test/java/                  unit + smoke tests
docs/                           schema.sql, sample-queries.sql
```

## 7. Scope notes (honest limitations)

* Resume analysis is rule-based (text extraction + keyword matching). Scanned/image-only PDFs cannot be read. It is not an ML model.
* CGPA recalculation is an unweighted average of per-semester SGPA values; there is no credit weighting.
* Notifications are in-app only (no email/SMS).
* Uploaded files are stored on local disk (`uploads/`); use object storage in a real deployment.
* Change `app.jwt.secret` before any real deployment. CORS is not configured because the UI is served by the same app.
