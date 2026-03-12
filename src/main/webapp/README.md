# 🖼️ Webapp (Frontend Components)

**Path:** `src/main/webapp`

## 📌 Purpose
This folder contains the **Frontend** component of the application. These are the files that actually run in the user's web browser. It uses HTML for structure, vanilla CSS for styling, and vanilla JavaScript (`fetch()`) to communicate with the Java Servlets.

## 📝 Files & Data Flow

### 1. `index.jsp`
* **What it does:** The very first page you see. It contains a classic HTML `<form>` tag. 
* **Data Flow:** When you click login, the form performs an standard HTTP `POST` action to the `/login` Java Servlet.

### 2. `dashboard.html` & `styles.css`
* **What it does:** The main hub after logging in.
* **Data Flow:** Right when the page loads, an inline `<script>` runs `fetch('dashboard?type=json')`. It loops through the JSON response and generates HTML `<div>` blocks dynamically depending on whether you have a "Take Exam" or "View Results" status.

### 3. `exam.html` & `exam.css`
* **What it does:** The active testing environment. Features an instruction overlay, timers, and question rendering. 
* **Data Flow:** 
  1. Calls `fetch('api/exam-config')` on load to figure out how many seconds the timer should run.
  2. Calls `fetch('api/questions?skillId=X')` when the user clicks "Start Exam". It immediately hides the instruction overlay and begins the timer logic.
  3. Uses JavaScript `setInterval()` to count down from 10 seconds.
  4. When you click Submit, JS builds a massive Object summarizing all answers and `fetch('api/submit-exam')` sends it as a JSON payload to the server.

### 4. `exam-results.html` & `exam-results.css`
* **What it does:** Generates a full review with green/red highlighting.
* **Data Flow:** Calls `fetch('api/exam-results')`. It dynamically creates raw HTML strings combining the question text and a big checkmark or X icon depending on the JSON `is_correct` boolean. It also creates clickable hyperlink cards from the `resources` data.

### 5. `faculty-dashboard.html` & `faculty-dashboard.css`
* **What it does:** A comprehensive data visualization panel for faculty members.
* **Data Flow:** Imports a third-party open source library called `Chart.js`. It fetches `faculty-dashboard?type=json` and parses the lists to draw a Bar chart mapping student exam percentages and a Doughnut chart showing the percentage of excellent vs. lower performers.


