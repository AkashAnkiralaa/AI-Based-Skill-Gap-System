package org.example.web;

import org.example.model.Question;
import org.example.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@WebServlet("/api/questions")
public class ExamServlet extends HttpServlet {

    private int numQuestions = 30;

    @Override
    public void init() throws ServletException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                numQuestions = Integer.parseInt(props.getProperty("exam.numQuestions", "30"));
            }
        } catch (Exception e) {
            System.err.println("Failed to load exam config from db.properties, using defaults: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        int userId = (int) s.getAttribute("userId");

        // e.g. /api/questions?skillId=3 for Web Development
        String skillIdParam = req.getParameter("skillId");
        int skillId = skillIdParam != null ? Integer.parseInt(skillIdParam) : 3;

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            // Check if student has already taken this exam
            if (hasAlreadyTakenExam(userId, skillId)) {
                PrintWriter out = resp.getWriter();
                out.print("{\"alreadyTaken\":true, \"message\":\"You have already taken this exam. Each exam can only be attempted once.\"}");
                out.flush();
                return;
            }

            List<Question> questions = fetchQuestions(skillId, numQuestions);

            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean first = true;
            for (Question q : questions) {
                if (!first)
                    json.append(",");
                first = false;
                json.append("{");
                json.append("\"id\":").append(q.getId()).append(",");
                json.append("\"number\":").append(questions.indexOf(q) + 1).append(",");
                json.append("\"text\":\"").append(escapeJson(q.getText())).append("\",");

                json.append("\"options\":{");
                json.append("\"A\":\"").append(escapeJson(q.getOptionA())).append("\",");
                json.append("\"B\":\"").append(escapeJson(q.getOptionB())).append("\",");
                json.append("\"C\":\"").append(escapeJson(q.getOptionC())).append("\",");
                json.append("\"D\":\"").append(escapeJson(q.getOptionD())).append("\"");
                json.append("}");

                // NOTE: correct answer is NOT sent to the client for security.
                // The server validates answers during submission.
                json.append("}");
            }
            json.append("]");

            PrintWriter out = resp.getWriter();
            out.print(json.toString());
            out.flush();

        } catch (Exception e) {
            System.err.println("Error loading questions for skillId=" + skillId + ": " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Check if the user has already attempted this exam.
     */
    private boolean hasAlreadyTakenExam(int userId, int skillId) throws Exception {
        String sql = "SELECT COUNT(*) AS cnt FROM exam_attempts WHERE user_id = ? AND skill_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
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

    private List<Question> fetchQuestions(int skillId, int limit) throws Exception {
        // Find random questions for the skill
        String sql = "SELECT * FROM (SELECT * FROM questions WHERE skill_id = ? ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= ?";
        List<Question> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, skillId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question();
                    q.setId(rs.getInt("id"));
                    q.setSkillId(rs.getInt("skill_id"));
                    q.setText(rs.getString("question_text"));
                    q.setOptionA(rs.getString("option_a"));
                    q.setOptionB(rs.getString("option_b"));
                    q.setOptionC(rs.getString("option_c"));
                    q.setOptionD(rs.getString("option_d"));
                    q.setCorrectOption(rs.getString("correct_option"));
                    q.setDifficultyLevel(rs.getInt("difficulty_level"));
                    list.add(q);
                }
            }
        }
        return list;
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
