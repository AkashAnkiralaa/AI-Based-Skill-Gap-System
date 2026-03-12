# 📦 Model Package

**Path:** `src/main/java/org/example/model`

## 📌 Purpose
This folder contains the **Data Models** (also known as POJOs or JavaBeans). These Java classes act as blueprints for the data moving through the application. They correspond directly to the structure of the database tables.

When the application retrieves data from the Oracle Database, it packages that data into these objects to easily pass it around. When the application needs to save data, it extracts the details from these objects. 

## 📝 Files & Data Flow

### 1. `User.java`
* **What it is:** Represents a person logging into the application.
* **Database mapping:** Corresponds to the `users` table.
* **Fields:** `id`, `username`, `password`, `role` (student/faculty), `fullName`, `department`.
* **Flow:** Created by `LoginServlet` when verifying credentials or `FacultyDashboardServlet` when loading students.

### 2. `Skill.java`
* **What it is:** Represents a technical skill being evaluated (e.g., Java, Web Development).
* **Database mapping:** Corresponds to the `skills` table.
* **Fields:** `id`, `skillName`, `category`, `description`.
* **Flow:** Retrieved by `DashboardServlet` to display skill cards and `ExamServlet` to identify which exam to give.

### 3. `Question.java`
* **What it is:** Represents a single multiple-choice question in an exam.
* **Database mapping:** Corresponds to the `questions` table.
* **Fields:** `id`, `skillId`, `text` (the question itself), `options` (A/B/C/D map), `correctOption`.
* **Flow:** Fetched by `ExamServlet` and sent (without the `correctOption`) to `exam.html`. Verified later by `SubmitExamServlet`.

### 4. `Assessment.java`
* **What it is:** The final skill evaluation record for a student for a specific skill.
* **Database mapping:** Corresponds to the `assessment` table.
* **Fields:** `id`, `userId`, `skillId`, `requiredLevel`, `actualLevel`, `gapLevel`, `score`, `totalQuestions`.
* **Flow:** Generated and saved by `SubmitExamServlet`. Loaded by `DashboardServlet` and `ResultsServlet` to display student dashboards. 

### 5. `Resource.java`
* **What it is:** A learning recommendation link (like a YouTube video or W3Schools article).
* **Database mapping:** Corresponds to the `resources` table.
* **Fields:** `id`, `skillId`, `title`, `resourceUrl`, `resourceType`.
* **Flow:** Loaded by `ExamResultsServlet` or `DashboardServlet` to show students how to improve upon their skill gaps.
