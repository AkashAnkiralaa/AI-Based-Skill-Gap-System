# 🗄️ Database Scripts

**Path:** `src/main/resources/db`

## 📌 Purpose
This folder contains the SQL setup files that outline exactly how the Oracle Database tables are structured and how data is initialized. When the application runs, these tables **must exist** so data can flow correctly.

## 📝 Files & Structure

### 1. `schema.sql`
* **What it does:** Creates the core tables (`users`, `skills`, `assessment`, `resources`).
* **Data Flow Importance:** Without these, users cannot log in (as there is no `users` table) and the `DashboardServlet` will fail to load `skills`.

### 2. `questions_schema.sql`
* **What it does:** Creates the `questions` table and inserts all of the actual exam questions (`INSERT INTO questions...`). 
* **Data Flow Importance:** When `ExamServlet` queries `SELECT * FROM questions WHERE skill_id = ?`, it pulls directly from these inserted rows.

### 3. `exam_answers_schema.sql`
* **What it does:** Creates the `exam_answers` table. This acts as a log of exactly what option (A/B/C/D) a student chose for each specific question.
* **Data Flow Importance:** Populated by `SubmitExamServlet` right after clicking Submit. Later requested by `ExamResultsServlet` to display the "Correct/Incorrect" colored breakdown.

### 4. `exam_attendance_schema.sql`
* **What it does:** Creates the `exam_attempts` table. This is the **most crucial security structure**. It records the user ID and skill ID along with the final score, and forces a `UNIQUE` constraint.
* **Data Flow Importance:** 
  - When `ExamServlet` loads, it first checks `exam_attempts`. If a row exists, it returns `alreadyTaken: true`.
  - When `SubmitExamServlet` is called, it inserts a copy of the score here to prevent future retakes.
