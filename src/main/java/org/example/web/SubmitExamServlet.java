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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@WebServlet("/api/submit-exam")
public class SubmitExamServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int userId = (int) s.getAttribute("userId");
        String jsonPayload = readBody(req);
        System.out.println("[SubmitExam] Received payload length: " + jsonPayload.length());

        int skillId = extractInt(jsonPayload, "skillId", 3);
        int total = extractInt(jsonPayload, "total", 30);

        if (total <= 0)
            total = 30;

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection c = DBUtil.getConnection()) {
            // ========== ONE-TIME EXAM CHECK ==========
            // Reject submission if student has already taken this exam
            if (hasAlreadyTakenExam(c, userId, skillId)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"error\":\"You have already taken this exam. Each exam can only be attempted once.\"}");
                return;
            }


            // ========== SAVE PER-QUESTION ANSWERS ==========
            // Parse answers array from JSON
            List<AnswerItem> answerItems = parseAnswers(jsonPayload);
            System.out.println("[SubmitExam] Parsed " + answerItems.size() + " answers");

            for (AnswerItem item : answerItems) {
                if (item.questionId > 0) {
                    // Look up the actual correct answer from DB
                    String dbCorrect = null;
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT correct_option FROM questions WHERE id = ?")) {
                        ps.setInt(1, item.questionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                dbCorrect = rs.getString("correct_option");
                            }
                        }
                    }

                    int isCorrectFlag = 0;
                    if (dbCorrect != null && item.selected != null && !item.selected.isEmpty()) {
                        isCorrectFlag = dbCorrect.trim().equalsIgnoreCase(item.selected.trim()) ? 1 : 0;
                    }

                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO exam_answers (user_id, skill_id, question_id, selected_option, is_correct) VALUES (?, ?, ?, ?, ?)")) {
                        ins.setInt(1, userId);
                        ins.setInt(2, skillId);
                        ins.setInt(3, item.questionId);
                        ins.setString(4, item.selected);
                        ins.setInt(5, isCorrectFlag);
                        ins.executeUpdate();
                    }
                }
            }

            // ========== CALCULATE SCORE FROM DB ==========
            int dbScore = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) AS cnt FROM exam_answers WHERE user_id = ? AND skill_id = ? AND is_correct = 1")) {
                ps.setInt(1, userId);
                ps.setInt(2, skillId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        dbScore = rs.getInt("cnt");
                }
            }

            int dbTotal = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) AS cnt FROM exam_answers WHERE user_id = ? AND skill_id = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, skillId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        dbTotal = rs.getInt("cnt");
                }
            }

            int dbPct = dbTotal > 0 ? (dbScore * 100) / dbTotal : 0;

            // Level: 0-30% = 1(Lower), 31-70% = 2(Intermediate), 71-100% = 3(Excellent)
            int dbLevel = 1;
            if (dbPct >= 71)
                dbLevel = 3;
            else if (dbPct >= 31)
                dbLevel = 2;

            int requiredLevel = 3;
            int dbGap = Math.max(0, requiredLevel - dbLevel);

            // ========== SAVE/UPDATE ASSESSMENT WITH SCORE ==========
            int assessmentId = saveAssessment(c, userId, skillId, requiredLevel, dbLevel, dbGap, dbScore, dbTotal);

            // ========== RECORD THE EXAM ATTEMPT (one-time enforcement) ==========
            recordExamAttempt(c, userId, skillId, dbScore, dbTotal, dbPct);

            s.setAttribute("lastScore", dbScore);
            s.setAttribute("lastTotal", dbTotal);
            s.setAttribute("lastSkillId", skillId);

            System.out.println("[SubmitExam] User " + userId + " score: " + dbScore + "/" + dbTotal + " = " + dbPct
                    + "% level: " + dbLevel);

            PrintWriter out = resp.getWriter();
            out.print("{\"success\":true, \"assessmentId\":" + assessmentId +
                    ", \"score\":" + dbScore + ", \"total\":" + dbTotal +
                    ", \"percentage\":" + dbPct + ", \"level\":" + dbLevel + "}");
            out.flush();

        } catch (Exception e) {
            System.err.println("[SubmitExam] Error: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Failed to save assessment: " + e.getMessage() + "\"}");
        }
    }

    // Simple answer item holder
    private static class AnswerItem {
        int questionId;
        String selected;
    }

    /**
     * Check if the user has already attempted this exam.
     */
    private boolean hasAlreadyTakenExam(Connection c, int userId, int skillId) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt FROM exam_attempts WHERE user_id = ? AND skill_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
        }
        return false;
    }

    /**
     * Record the exam attempt so the student cannot retake it.
     */
    private void recordExamAttempt(Connection c, int userId, int skillId, int score, int totalQuestions, int percentage)
            throws Exception {
        String sql = "INSERT INTO exam_attempts (user_id, skill_id, score, total_questions, percentage) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            ps.setInt(3, score);
            ps.setInt(4, totalQuestions);
            ps.setInt(5, percentage);
            ps.executeUpdate();
        }
    }

    /**
     * Parse the answers array from the JSON payload.
     * Expected format: "answers":[{"questionId":1,"selected":"A"}, ...]
     */
    private List<AnswerItem> parseAnswers(String json) {
        List<AnswerItem> items = new ArrayList<>();

        // Find the answers array
        int arrStart = json.indexOf("\"answers\":[");
        if (arrStart == -1) {
            arrStart = json.indexOf("\"answers\" :[");
        }
        if (arrStart == -1)
            return items;

        // Find the opening bracket
        int bracketStart = json.indexOf("[", arrStart);
        if (bracketStart == -1)
            return items;

        // Find the matching closing bracket
        int depth = 0;
        int bracketEnd = -1;
        for (int i = bracketStart; i < json.length(); i++) {
            if (json.charAt(i) == '[')
                depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0) {
                    bracketEnd = i;
                    break;
                }
            }
        }
        if (bracketEnd == -1)
            return items;

        String arrContent = json.substring(bracketStart + 1, bracketEnd);

        // Split by each object - find each {...}
        int pos = 0;
        while (pos < arrContent.length()) {
            int objStart = arrContent.indexOf("{", pos);
            if (objStart == -1)
                break;
            int objEnd = arrContent.indexOf("}", objStart);
            if (objEnd == -1)
                break;

            String obj = arrContent.substring(objStart, objEnd + 1);
            AnswerItem item = new AnswerItem();
            item.questionId = extractInt(obj, "questionId", 0);
            item.selected = extractString(obj, "selected");
            if (item.selected == null)
                item.selected = "";

            items.add(item);
            pos = objEnd + 1;
        }

        return items;
    }

    private int saveAssessment(Connection c, int userId, int skillId, int requiredLevel, int actualLevel, int gapLevel,
            int score, int totalQuestions)
            throws Exception {
        String findSql = "SELECT id FROM assessment WHERE user_id = ? AND skill_id = ?";
        String insertSql = "INSERT INTO assessment (user_id, skill_id, required_level, actual_level, gap_level, score, total_questions, assessed_at) VALUES (?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";

        int existingId = 0;
        try (PreparedStatement check = c.prepareStatement(findSql)) {
            check.setInt(1, userId);
            check.setInt(2, skillId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    existingId = rs.getInt("id");
                }
            }
        }

        if (existingId > 0) {
            updateAssessmentLevel(c, userId, skillId, requiredLevel, actualLevel, gapLevel, score, totalQuestions);
            return existingId;
        } else {
            try (PreparedStatement ps = c.prepareStatement(insertSql, new String[] { "ID" })) {
                ps.setInt(1, userId);
                ps.setInt(2, skillId);
                ps.setInt(3, requiredLevel);
                ps.setInt(4, actualLevel);
                ps.setInt(5, gapLevel);
                ps.setInt(6, score);
                ps.setInt(7, totalQuestions);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next())
                        return keys.getInt(1);
                }
            }
            return 0;
        }
    }

    private void updateAssessmentLevel(Connection c, int userId, int skillId, int requiredLevel, int actualLevel,
            int gapLevel, int score, int totalQuestions)
            throws Exception {
        String updateSql = "UPDATE assessment SET required_level = ?, actual_level = ?, gap_level = ?, score = ?, total_questions = ?, assessed_at = SYSTIMESTAMP WHERE user_id = ? AND skill_id = ?";
        try (PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setInt(1, requiredLevel);
            ps.setInt(2, actualLevel);
            ps.setInt(3, gapLevel);
            ps.setInt(4, score);
            ps.setInt(5, totalQuestions);
            ps.setInt(6, userId);
            ps.setInt(7, skillId);
            ps.executeUpdate();
        }
    }

    private String readBody(HttpServletRequest req) throws IOException {
        try (Scanner s = new Scanner(req.getInputStream(), "UTF-8").useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private int extractInt(String json, String key, int defaultVal) {
        String search = "\"" + key + "\":";
        int index = json.indexOf(search);
        if (index == -1) {
            search = "\"" + key + "\" :";
            index = json.indexOf(search);
        }
        if (index == -1)
            return defaultVal;
        int start = index + search.length();
        // Skip whitespace
        while (start < json.length() && json.charAt(start) == ' ')
            start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-'))
            end++;
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int index = json.indexOf(search);
        if (index == -1) {
            search = "\"" + key + "\" :\"";
            index = json.indexOf(search);
        }
        if (index == -1) {
            // Might be empty string - check for "key":""
            search = "\"" + key + "\":\"";
            index = json.indexOf(search);
            if (index == -1)
                return null;
        }
        int start = index + search.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return null;
        return json.substring(start, end);
    }
}
