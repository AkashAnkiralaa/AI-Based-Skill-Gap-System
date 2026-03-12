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

@WebServlet("/faculty-dashboard")
public class FacultyDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String role = (String) s.getAttribute("role");
        if (!"faculty".equalsIgnoreCase(role)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }

        String type = req.getParameter("type");
        if ("json".equals(type)) {
            sendJsonResponse(req, resp);
        } else {
            req.getRequestDispatcher("/faculty-dashboard.html").forward(req, resp);
        }
    }

    private void sendJsonResponse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(false);
        String facultyName = (String) s.getAttribute("fullName");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection c = DBUtil.getConnection()) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"facultyName\":\"").append(esc(facultyName)).append("\",");

            // Get all students with their latest assessment
            String studentSql = "SELECT u.id AS user_id, u.username, u.full_name, u.department, " +
                    "a.skill_id, s.skill_name, a.actual_level, a.required_level, a.gap_level, a.assessed_at, " +
                    "(SELECT COUNT(*) FROM exam_answers ea WHERE ea.user_id = u.id AND ea.is_correct = 1 AND ea.skill_id = a.skill_id) AS correct_count, "
                    +
                    "(SELECT COUNT(*) FROM exam_answers ea WHERE ea.user_id = u.id AND ea.skill_id = a.skill_id) AS total_count "
                    +
                    "FROM users u " +
                    "JOIN assessment a ON u.id = a.user_id " +
                    "JOIN skills s ON a.skill_id = s.id " +
                    "WHERE u.role = 'student' " +
                    "ORDER BY u.full_name, a.assessed_at DESC";

            json.append("\"students\":[");
            boolean first = true;
            int totalStudents = 0;
            int totalScore = 0;
            int lowerCount = 0, intermediateCount = 0, excellentCount = 0;

            try (PreparedStatement ps = c.prepareStatement(studentSql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalStudents++;
                    int correct = rs.getInt("correct_count");
                    int total = rs.getInt("total_count");
                    int pct = total > 0 ? (correct * 100) / total : 0;
                    totalScore += pct;

                    String level;
                    if (pct >= 71) {
                        level = "Excellent";
                        excellentCount++;
                    } else if (pct >= 31) {
                        level = "Intermediate";
                        intermediateCount++;
                    } else {
                        level = "Lower";
                        lowerCount++;
                    }

                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("{");
                    json.append("\"userId\":").append(rs.getInt("user_id")).append(",");
                    json.append("\"username\":\"").append(esc(rs.getString("username"))).append("\",");
                    json.append("\"fullName\":\"").append(esc(rs.getString("full_name"))).append("\",");
                    json.append("\"department\":\"").append(esc(rs.getString("department"))).append("\",");
                    json.append("\"skillName\":\"").append(esc(rs.getString("skill_name"))).append("\",");
                    json.append("\"correctCount\":").append(correct).append(",");
                    json.append("\"totalCount\":").append(total).append(",");
                    json.append("\"percentage\":").append(pct).append(",");
                    json.append("\"level\":\"").append(level).append("\",");
                    json.append("\"actualLevel\":").append(rs.getInt("actual_level")).append(",");
                    json.append("\"requiredLevel\":").append(rs.getInt("required_level")).append(",");
                    json.append("\"gapLevel\":").append(rs.getInt("gap_level")).append(",");
                    json.append("\"skillId\":").append(rs.getInt("skill_id"));
                    json.append("}");
                }
            }
            json.append("],");

            // Summary stats
            int avgScore = totalStudents > 0 ? totalScore / totalStudents : 0;
            json.append("\"summary\":{");
            json.append("\"totalStudents\":").append(totalStudents).append(",");
            json.append("\"averageScore\":").append(avgScore).append(",");
            json.append("\"lowerCount\":").append(lowerCount).append(",");
            json.append("\"intermediateCount\":").append(intermediateCount).append(",");
            json.append("\"excellentCount\":").append(excellentCount);
            json.append("}");

            json.append("}");

            PrintWriter out = resp.getWriter();
            out.print(json.toString());
            out.flush();

        } catch (Exception e) {
            System.err.println("Error loading faculty dashboard: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Failed to load dashboard data\"}");
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
