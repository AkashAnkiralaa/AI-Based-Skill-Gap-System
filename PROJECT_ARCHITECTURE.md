# 🎯 AI Based Skill Gap Analysis - Project Architecture

> **This document is written so that even a 10th class student can understand it easily!**

---

## 📌 What Does This Project Do?

Imagine your college wants to check how good you are at **Web Development** (HTML, CSS, JavaScript).
This project is a **website** where:

1. **Students** login and take an online exam (MCQ questions)
2. The system **checks your answers** and gives you a score
3. Based on your score, it tells you if you are **Excellent**, **Intermediate**, or **Lower** level
4. It shows you **learning resources** (YouTube links, tutorials) to improve your weak areas
5. **Faculty** can see all students' results in one dashboard

---

## 🏗️ How Is The Project Built?

Think of the project like a **3-layer cake**:

```
┌─────────────────────────────────┐
│     🌐 FRONTEND (Browser)       │  ← What you see (HTML pages)
│     HTML + CSS + JavaScript      │
├─────────────────────────────────┤
│     ⚙️ BACKEND (Java Server)     │  ← The brain (Java Servlets)
│     Java Servlets on Tomcat      │
├─────────────────────────────────┤
│     💾 DATABASE (Oracle)         │  ← The memory (stores data)
│     Oracle Database              │
└─────────────────────────────────┘
```

### Layer 1: Frontend (What You See)
These are the **web pages** you see in the browser:

| Page | What It Does |
|------|-------------|
| `index.jsp` | Login page — enter your email and password |
| `dashboard.html` | Your home page after login — shows skill cards |
| `exam.html` | The exam page — 30 MCQ questions with timer |
| `exam-results.html` | Shows your score, correct/wrong answers |
| `faculty-dashboard.html` | Faculty can see all students' results |

### Layer 2: Backend (The Brain)
These are **Java programs** that run on the server. Each one handles a specific job:

| Servlet | URL | What It Does |
|---------|-----|-------------|
| `LoginServlet` | `/login` | Checks your username and password |
| `DashboardServlet` | `/dashboard` | Loads your skill cards and exam status |
| `ExamServlet` | `/api/questions` | Gives you exam questions from the database |
| `SubmitExamServlet` | `/api/submit-exam` | Saves your answers and calculates score |
| `ExamResultsServlet` | `/api/exam-results` | Loads your detailed results |
| `ExamConfigServlet` | `/api/exam-config` | Sends exam settings (time, number of questions) |
| `FacultyDashboardServlet` | `/faculty-dashboard` | Loads all students' data for faculty |
| `LogoutServlet` | `/logout` | Logs you out |

### Layer 3: Database (The Memory)
All data is stored in an **Oracle Database**. Think of tables like Excel sheets:

| Table | What It Stores |
|-------|---------------|
| `users` | Student and faculty accounts (name, email, password) |
| `skills` | List of skills (Java, Web Dev, Python, etc.) |
| `questions` | Exam questions with options A, B, C, D and correct answer |
| `exam_answers` | Each student's answer for every question |
| `assessment` | Score, level (Excellent/Intermediate/Lower) per student |
| `exam_attempts` | Records who has taken which exam (prevents retake!) |
| `resources` | Learning links (W3Schools, MDN, FreeCodeCamp, etc.) |

---

## 📁 Folder Structure

```
AI Based Skill Gapp/
├── pom.xml                          ← Project configuration (like a recipe)
├── user_inserts.sql                 ← SQL to create student accounts
├── PROJECT_ARCHITECTURE.md          ← This file!
├── DATA_FLOW.md                     ← How data moves through the app
│
└── src/main/
    ├── java/org/example/
    │   ├── model/                   ← Data classes (like blueprints)
    │   │   ├── User.java
    │   │   ├── Question.java
    │   │   ├── Assessment.java
    │   │   ├── Skill.java
    │   │   └── Resource.java
    │   │
    │   ├── web/                     ← Servlets (the traffic controllers)
    │   │   ├── LoginServlet.java
    │   │   ├── DashboardServlet.java
    │   │   ├── ExamServlet.java
    │   │   ├── SubmitExamServlet.java
    │   │   ├── ExamResultsServlet.java
    │   │   ├── ExamConfigServlet.java
    │   │   ├── FacultyDashboardServlet.java
    │   │   ├── FacultyStudentAnswersServlet.java
    │   │   ├── LogoutServlet.java
    │   │   └── NoCacheFilter.java
    │   │
    │   ├── util/
    │   │   └── DBUtil.java          ← Connects to Oracle Database
    │
    ├── resources/
    │   ├── db.properties            ← Database connection settings
    │   └── db/
    │       ├── schema.sql           ← Creates main tables
    │       ├── questions_schema.sql  ← Creates questions table + data
    │       ├── exam_answers_schema.sql  ← Creates exam_answers table
    │       └── exam_attendance_schema.sql ← One-time exam enforcement
    │
    └── webapp/
        ├── WEB-INF/web.xml          ← Web app configuration
        ├── index.jsp                ← Login page
        ├── dashboard.html           ← Student dashboard
        ├── exam.html                ← Exam page
        ├── exam-results.html        ← Detailed results
        ├── faculty-dashboard.html   ← Faculty view
        ├── styles.css               ← Main stylesheet
        ├── exam.css                 ← Exam page styles
        ├── exam-results.css         ← Results page styles
        └── faculty-dashboard.css    ← Faculty dashboard styles
```

---

## 🔑 Key Features

1. **Role-based Login** — Students and Faculty see different dashboards
2. **One-Time Exam** — Each student can take each exam only ONCE
3. **Per-Question Timer** — 10 seconds per question, auto-advances
4. **Total Timer** — 5 minutes total, auto-submits
5. **Server-Side Grading** — Answers are checked on the server (secure!)
6. **Skill Level Classification** — Excellent (71%+), Intermediate (31-70%), Lower (0-30%)
7. **Learning Resources** — Suggests tutorials based on wrong answers
8. **Faculty Dashboard** — Charts and tables showing all student performance

---

## 🛠️ Technologies Used

| Technology | What For |
|-----------|---------|
| **Java 17** | Backend programming language |
| **Java Servlets** | Handle web requests |
| **Oracle Database** | Store all data |
| **HTML/CSS/JavaScript** | Frontend web pages |
| **Apache Tomcat** | Web server to run the app |
| **Maven** | Build tool (compiles and packages the code) |
| **Chart.js** | Beautiful charts on faculty dashboard |
