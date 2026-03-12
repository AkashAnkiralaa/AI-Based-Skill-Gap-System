package org.example.web;

import org.example.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/api/exam-results")
public class ExamResultsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int userId = (int) s.getAttribute("userId");
        String skillIdParam = req.getParameter("skillId");
        int skillId = skillIdParam != null ? Integer.parseInt(skillIdParam) : 3;

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection c = DBUtil.getConnection()) {
            String fullName = (String) s.getAttribute("fullName");

            // Debug logging - check all tables for this user
            System.out.println("[ExamResults] Fetching results for userId=" + userId + ", skillId=" + skillId);

            // Check if there's any exam_attempts record
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM exam_attempts WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[ExamResults] exam_attempts count: " + rs.getInt(1));
                    }
                }
            }

            // Check if there's any exam_answers record for this user
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM exam_answers WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[ExamResults] exam_answers count: " + rs.getInt(1));
                    }
                }
            }

            // Get assessment level
            int actualLevel = 1;
            String assessSql = "SELECT * FROM (SELECT actual_level, gap_level FROM assessment WHERE user_id = ? AND skill_id = ? ORDER BY assessed_at DESC) WHERE ROWNUM = 1";
            try (PreparedStatement ps = c.prepareStatement(assessSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, skillId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        actualLevel = rs.getInt("actual_level");
                    }
                }
            }

            // Get answers with question details
            String answersSql = "SELECT ea.question_id, ea.selected_option, ea.is_correct, " +
                    "q.question_text, q.option_a, q.option_b, q.option_c, q.option_d, q.correct_option " +
                    "FROM exam_answers ea JOIN questions q ON ea.question_id = q.id " +
                    "WHERE ea.user_id = ? AND ea.skill_id = ? ORDER BY ea.id";

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"candidateName\":\"").append(esc(fullName)).append("\",");
            json.append("\"skillId\":").append(skillId).append(",");

            int correctCount = 0;
            int totalCount = 0;
            StringBuilder questionsJson = new StringBuilder();
            questionsJson.append("[");
            boolean first = true;

            try (PreparedStatement ps = c.prepareStatement(answersSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, skillId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        totalCount++;
                        if (rs.getInt("is_correct") == 1)
                            correctCount++;
                        if (!first)
                            questionsJson.append(",");
                        first = false;
                        questionsJson.append("{");
                        questionsJson.append("\"questionId\":").append(rs.getInt("question_id")).append(",");
                        questionsJson.append("\"number\":").append(totalCount).append(",");
                        questionsJson.append("\"text\":\"").append(esc(rs.getString("question_text"))).append("\",");
                        questionsJson.append("\"optionA\":\"").append(esc(rs.getString("option_a"))).append("\",");
                        questionsJson.append("\"optionB\":\"").append(esc(rs.getString("option_b"))).append("\",");
                        questionsJson.append("\"optionC\":\"").append(esc(rs.getString("option_c"))).append("\",");
                        questionsJson.append("\"optionD\":\"").append(esc(rs.getString("option_d"))).append("\",");
                        questionsJson.append("\"correctOption\":\"").append(esc(rs.getString("correct_option")))
                                .append("\",");
                        String sel = rs.getString("selected_option");
                        questionsJson.append("\"selectedOption\":\"").append(sel != null ? esc(sel) : "").append("\",");
                        questionsJson.append("\"isCorrect\":").append(rs.getInt("is_correct"));
                        questionsJson.append("}");
                    }
                }
            }

            // Debug logging for results count
            System.out.println("[ExamResults] Found " + totalCount + " questions, " + correctCount
                    + " correct for userId=" + userId + ", skillId=" + skillId);
            questionsJson.append("]");

            int percentage = totalCount > 0 ? (correctCount * 100) / totalCount : 0;
            String levelLabel;
            if (percentage >= 71)
                levelLabel = "Excellent";
            else if (percentage >= 31)
                levelLabel = "Intermediate";
            else
                levelLabel = "Lower";

            json.append("\"score\":").append(correctCount).append(",");
            json.append("\"total\":").append(totalCount).append(",");
            json.append("\"percentage\":").append(percentage).append(",");
            json.append("\"level\":\"").append(levelLabel).append("\",");
            json.append("\"levelNum\":").append(actualLevel).append(",");
            json.append("\"questions\":").append(questionsJson.toString()).append(",");

            // Get resource suggestions based on wrong answers
            StringBuilder resourcesJson = new StringBuilder();
            resourcesJson.append("[");
            String resSql = "SELECT DISTINCT r.title, r.resource_url, r.resource_type, r.topic_tag " +
                    "FROM resources r WHERE r.skill_id = ? AND r.topic_tag IS NOT NULL " +
                    "ORDER BY r.topic_tag, r.title";
            boolean firstRes = true;
            try (PreparedStatement ps = c.prepareStatement(resSql)) {
                ps.setInt(1, skillId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (!firstRes)
                            resourcesJson.append(",");
                        firstRes = false;
                        resourcesJson.append("{");
                        resourcesJson.append("\"title\":\"").append(esc(rs.getString("title"))).append("\",");
                        resourcesJson.append("\"url\":\"").append(esc(rs.getString("resource_url"))).append("\",");
                        resourcesJson.append("\"type\":\"").append(esc(rs.getString("resource_type"))).append("\",");
                        resourcesJson.append("\"topic\":\"").append(esc(rs.getString("topic_tag"))).append("\"");
                        resourcesJson.append("}");
                    }
                }
            }
            resourcesJson.append("]");
            json.append("\"resources\":").append(resourcesJson.toString());

            json.append("}");

            PrintWriter out = resp.getWriter();
            out.print(json.toString());
            out.flush();

        } catch (Exception e) {
            System.err.println("Error loading exam results: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Failed to load results\"}");
        }
    }

    private String esc(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
