# 🌐 Web Package (Backend Servlets)

**Path:** `src/main/java/org/example/web`

## 📌 Purpose
This folder is the **Brain** of the backend. It contains Java Servlets, which are specialized Java classes that listen for and respond to HTTP requests coming from the frontend (the web browser). Each Servlet acts like a traffic controller for a specific part of the application.

## 📝 Files & Detailed Data Flow

### 1. `LoginServlet.java` (`/login`)
* **What it does:** Authenticates users and establishes sessions.
* **Data Flow:** 
  1. Receives `username` and `password` from the frontend `index.jsp` form.
  2. Queries the `users` database table to find a match.
  3. If matched, it generates an `HttpSession` cookie and stores the `userId` in it.
  4. Finally, it redirects the browser to `/dashboard` (for students) or `/faculty-dashboard` (for faculty).

### 2. `DashboardServlet.java` (`/dashboard`)
* **What it does:** Loads the student's profile and skill overview cards.
* **Data Flow:** 
  1. Checks the `HttpSession` for a valid `userId`.
  2. Queries the `skills`, `assessment`, and `exam_attempts` database tables to see which exams the student has taken.
  3. Joins this data with `resources` recommendations if they have gaps.
  4. Responds with JSON data that the frontend JavaScript in `dashboard.html` uses to render dynamic UI cards.

### 3. `ExamServlet.java` (`/api/questions`)
* **What it does:** Fetches the questions for a specific skill's exam.
* **Data Flow:** 
  1. Receives a `skillId` from the frontend URL parameters.
  2. First Queries: `exam_attempts` to make sure the student hasn't taken it already. If they have, it returns an error.
  3. Second Query: Loads random rows from the `questions` table for that `skillId`.
  4. Important: It strips out the `correctOption` data so the browser never receives the answers.

### 4. `SubmitExamServlet.java` (`/api/submit-exam`)
* **What it does:** Grades a student's exam and saves their final score.
* **Data Flow:** 
  1. Receives a large JSON object from the browser containing the `skillId` and every answer the student chose (e.g., Q1: A, Q2: C).
  2. Queries the `questions` table to look up the real `correctOption` for each question.
  3. Compares the student's answers and calculates a final score and `percentage`.
  4. Assigns a level: Excellent, Intermediate, or Lower.
  5. Inserts the results into `exam_answers` (per-question log), `assessment` (overall grade), and `exam_attempts` (locks out further retakes).

### 5. `ExamResultsServlet.java` (`/api/exam-results`)
* **What it does:** Provides the detailed breakdown of exactly what you got right and wrong.
* **Data Flow:** Queries `exam_answers` and `questions` to join the real question text with the student's selected option and whether it was correct. Passes this as JSON to `exam-results.html`.

### 6. `FacultyDashboardServlet.java` (`/faculty-dashboard`)
* **What it does:** Powerhouse query engine for the teacher view.
* **Data Flow:** Loads ALL student data across all tables (`users`, `assessment`, `exam_answers`) and formats it into arrays so Chart.js can draw bar graphs and pie charts.

### 7. Other Important Classes
* `ExamConfigServlet.java` - Exposes the exam timing rules (like 30 questions, 10 seconds each) to the frontend JS.
* `LogoutServlet.java` - Invalidates the browser's `HttpSession` cookie, forcing them back to the login page.
* `NoCacheFilter.java` - Adds HTTP headers to force the browser to not cache secure pages.
