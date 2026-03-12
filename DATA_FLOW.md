# 🔄 Data Flow - How Data Moves Through The App

> **Written for easy understanding — even a 10th class student can follow along!**

---

## 🗺️ The Big Picture

Here's how the entire application works, step by step:

```
Student opens website
        ↓
   LOGIN PAGE ──→ Server checks password in DATABASE
        ↓                                    
   DASHBOARD ──→ Server loads skills & exam status
        ↓
   START EXAM ──→ Server sends questions (if not already taken)
        ↓
  ANSWER QUESTIONS (with timers)
        ↓
   SUBMIT EXAM ──→ Server checks answers & saves score
        ↓
  VIEW RESULTS ──→ Server loads answers + recommendations
```

---

## 📋 Step-by-Step Data Flow

### Step 1: Student Login

```
┌──────────┐     username + password     ┌──────────────┐     SQL query     ┌──────────┐
│  Browser  │ ─────────────────────────→ │ LoginServlet  │ ───────────────→ │ Database │
│ (index.   │                            │ (checks       │                  │ (users   │
│  jsp)     │ ←───── redirect to ─────── │  password)    │ ←─── user data ─ │  table)  │
│           │    dashboard               │               │                  │          │
└──────────┘                             └──────────────┘                  └──────────┘
```

**What happens:**
1. You type your email (`24eg105k01@anurag.edu.in`) and password (`au@12345`)
2. Browser sends it to `LoginServlet`
3. Servlet checks the `users` table in the database
4. If correct → creates a **session** (like a temporary ID card) and sends you to Dashboard
5. If wrong → shows error message

---

### Step 2: Dashboard Loads

```
┌──────────┐    GET /dashboard?type=json   ┌──────────────────┐
│  Browser  │ ───────────────────────────→ │ DashboardServlet  │
│ (dashboard│                              │                   │
│  .html)   │                              │ Checks these      │
│           │                              │ tables:           │
│           │                              │ ├─ skills         │
│           │                              │ ├─ assessment     │
│           │ ←── JSON with skill cards ── │ └─ exam_attempts  │
└──────────┘                               └──────────────────┘
```

**What happens:**
1. Dashboard page asks server: "Give me this student's data"
2. Server loads all skills from `skills` table
3. For each skill, checks `exam_attempts` → "Has this student taken this exam?"
4. If YES → shows ✅ "Exam Completed" with score and "View Results" button
5. If NO → shows ⏳ "Not Taken" with "Start Assessment" button

---

### Step 3: Exam Starts

```
┌──────────┐   GET /api/questions?skillId=3   ┌──────────────┐
│  Browser  │ ──────────────────────────────→ │  ExamServlet   │
│ (exam.    │                                 │                │
│  html)    │                                 │ Step 1: Check  │
│           │                                 │ exam_attempts  │
│           │                                 │ (already taken?)│
│           │                                 │                │
│           │                                 │ Step 2: Load   │
│           │ ←── JSON list of questions ──── │ questions      │
│           │    (WITHOUT correct answers!)    │ (random order) │
└──────────┘                                  └──────────────┘
```

**What happens:**
1. Browser asks: "Give me questions for Web Development (skill 3)"
2. Server FIRST checks `exam_attempts` → "Has student already taken this?"
3. If YES → sends back `{"alreadyTaken": true}` → student sees "Exam Already Completed" screen
4. If NO → picks 30 random questions from `questions` table
5. **Important:** Correct answers are NOT sent to the browser (security!)

---

### Step 4: Student Answers Questions

```
This happens ONLY in the browser — no server communication!

┌─────────────────────────────────────────────┐
│  Browser stores answers in memory:          │
│                                             │
│  answers = {                                │
│    "q1": "A",   ← selected option A        │
│    "q2": "C",   ← selected option C        │
│    "q3": "",    ← not answered (skipped)    │
│    ...                                      │
│  }                                          │
│                                             │
│  ⏱️ Per-question timer: 10 seconds          │
│  ⏱️ Total timer: 5 minutes                  │
│                                             │
│  Timer expires → auto-advances / auto-submit │
└─────────────────────────────────────────────┘
```

---

### Step 5: Exam Submission

```
┌──────────┐    POST /api/submit-exam         ┌──────────────────┐
│  Browser  │ ─────────────────────────────→  │ SubmitExamServlet │
│           │                                 │                   │
│  Sends:   │                                 │ Step 1: Check     │
│  {        │                                 │ exam_attempts     │
│   skillId │                                 │ (already taken?)  │
│   answers │                                 │                   │
│  }        │                                 │ Step 2: For each  │
│           │                                 │ answer, look up   │
│           │                                 │ correct answer    │
│           │                                 │ from questions    │
│           │                                 │ table             │
│           │                                 │                   │
│           │                                 │ Step 3: Save to   │
│           │                                 │ exam_answers      │
│           │                                 │                   │
│           │                                 │ Step 4: Calculate │
│           │                                 │ score & level     │
│           │                                 │                   │
│           │                                 │ Step 5: Save to   │
│           │                                 │ assessment        │
│           │                                 │                   │
│           │                                 │ Step 6: Record    │
│           │ ←── {success, score, level} ─── │ exam_attempts     │
│           │                                 │ (prevents retake) │
└──────────┘                                  └──────────────────┘
```

**What happens:**
1. Browser sends all answers to `SubmitExamServlet`
2. Server checks `exam_attempts` → blocks if already submitted
3. For each answer, server looks up the correct answer from `questions` table
4. Compares student's answer with correct answer → marks as correct/wrong
5. Saves each answer in `exam_answers` table
6. Calculates total score and percentage
7. Determines skill level: **Excellent** (71%+), **Intermediate** (31-70%), **Lower** (0-30%)
8. Saves to `assessment` table
9. Records the attempt in `exam_attempts` (so student can't retake)
10. Sends back the score to browser

---

### Step 6: View Results

```
┌──────────┐   GET /api/exam-results?skillId=3   ┌────────────────────┐
│  Browser  │ ─────────────────────────────────→ │ ExamResultsServlet  │
│ (exam-    │                                    │                     │
│ results.  │                                    │ Loads from:         │
│  html)    │                                    │ ├─ assessment       │
│           │                                    │ ├─ exam_answers     │
│           │                                    │ ├─ questions        │
│           │                                    │ └─ resources        │
│           │ ←── JSON with all details ──────── │                     │
│           │                                    │                     │
│  Shows:   │                                    │                     │
│  ✅ Correct│                                    │                     │
│  ❌ Wrong  │                                    │                     │
│  📚 Resources                                  │                     │
└──────────┘                                     └────────────────────┘
```

**What happens:**
1. Browser requests detailed results
2. Server loads all stored answers from `exam_answers`
3. Joins with `questions` to get question text and options
4. Calculates score and determines level
5. Loads relevant learning `resources` based on skill
6. Sends everything to browser
7. Browser shows: each question, your answer, correct answer, score circle, and learning links

---

## 🗃️ Database Tables — How They Connect

```
┌─────────┐        ┌────────────┐        ┌──────────────┐
│  users  │───┐    │   skills   │───┐    │  questions   │
│─────────│   │    │────────────│   │    │──────────────│
│ id      │   │    │ id         │   │    │ id           │
│ username│   │    │ skill_name │   │    │ skill_id ──→ │skills
│ password│   │    │ category   │   │    │ question_text│
│ role    │   │    │ description│   │    │ option_a/b/c/d│
│ full_name│  │    └────────────┘   │    │ correct_option│
│ department│ │                      │    └──────────────┘
└─────────┘  │                      │
      │      │    ┌─────────────┐   │
      │      ├───→│ assessment  │←──┘
      │      │    │─────────────│
      │      │    │ user_id ──→ │users
      │      │    │ skill_id ──→│skills
      │      │    │ actual_level│
      │      │    │ gap_level   │
      │      │    │ score       │
      │      │    │ total_ques  │
      │      │    └─────────────┘
      │      │
      │      │    ┌──────────────┐
      │      ├───→│ exam_answers │
      │      │    │──────────────│
      │      │    │ user_id ──→  │users
      │      │    │ skill_id ──→ │skills
      │      │    │ question_id→ │questions
      │      │    │ selected_opt │
      │      │    │ is_correct   │
      │      │    └──────────────┘
      │      │
      │      │    ┌───────────────┐
      │      └───→│ exam_attempts │  ← PREVENTS RETAKE!
      │           │───────────────│
      │           │ user_id ──→   │users
      │           │ skill_id ──→  │skills
      │           │ score         │
      │           │ total_ques    │
      │           │ percentage    │
      │           │ UNIQUE(user,  │
      │           │   skill)      │
      │           └───────────────┘
      │
      │           ┌──────────────┐
      └──────────→│  resources   │
                  │──────────────│
                  │ skill_id ──→ │skills
                  │ title        │
                  │ resource_url │
                  │ topic_tag    │
                  └──────────────┘
```

---

## 🔒 Security Features

| Feature | How It Works |
|---------|-------------|
| **Server-side grading** | Correct answers are NEVER sent to the browser. Server checks them. |
| **Session-based auth** | Server creates a session cookie after login. No localStorage. |
| **One-time exam** | `exam_attempts` table with UNIQUE constraint prevents retake. |
| **Role-based access** | Faculty and students see different dashboards. |
| **No-cache headers** | Browser always gets fresh pages (no stale data). |

---

## 👨‍🏫 Faculty Dashboard Flow

```
Faculty logs in → LoginServlet detects role="faculty"
        ↓
Redirected to /faculty-dashboard
        ↓
FacultyDashboardServlet loads ALL students' data:
  ├── From users (student names)
  ├── From assessment (levels)
  ├── From exam_answers (scores)
  └── Calculates summary stats
        ↓
Faculty sees:
  ├── 📊 Bar chart (student scores)
  ├── 🎯 Doughnut chart (level distribution)
  └── 📋 Table (search + expand each student)
```
