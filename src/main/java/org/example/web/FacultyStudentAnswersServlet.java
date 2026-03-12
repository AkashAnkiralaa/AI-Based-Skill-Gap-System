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

@WebServlet("/api/faculty-student-answers")
public class FacultyStudentAnswersServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String role = (String) s.getAttribute("role");
        if (!"faculty".equalsIgnoreCase(role)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        int userId = Integer.parseInt(req.getParameter("userId"));
        int skillId = Integer.parseInt(req.getParameter("skillId"));

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection c = DBUtil.getConnection()) {
            String sql = "SELECT ea.question_id, ea.selected_option, ea.is_correct, " +
                    "q.question_text, q.correct_option " +
                    "FROM exam_answers ea JOIN questions q ON ea.question_id = q.id " +
                    "WHERE ea.user_id = ? AND ea.skill_id = ? ORDER BY ea.id";

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            int num = 0;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, skillId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        num++;
                        if (!first)
                            json.append(",");
                        first = false;
                        json.append("{");
                        json.append("\"number\":").append(num).append(",");
                        json.append("\"questionId\":").append(rs.getInt("question_id")).append(",");
                        json.append("\"text\":\"").append(esc(rs.getString("question_text"))).append("\",");
                        String sel = rs.getString("selected_option");
                        json.append("\"selectedOption\":\"").append(sel != null ? esc(sel) : "").append("\",");
                        json.append("\"correctOption\":\"").append(esc(rs.getString("correct_option"))).append("\",");
                        json.append("\"isCorrect\":").append(rs.getInt("is_correct") == 1);
                        json.append("}");
                    }
                }
            }
            json.append("]");

            PrintWriter out = resp.getWriter();
            out.print(json.toString());
            out.flush();

        } catch (Exception e) {
            System.err.println("Error loading student answers: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("[]");
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
