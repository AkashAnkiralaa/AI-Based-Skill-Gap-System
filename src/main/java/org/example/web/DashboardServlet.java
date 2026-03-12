package org.example.web;

import org.example.model.Assessment;
import org.example.model.Skill;
import org.example.model.Resource;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String type = req.getParameter("type");
        if ("json".equals(type)) {
            sendJsonResponse(req, resp);
        } else {
            req.getRequestDispatcher("/dashboard.html").forward(req, resp);
        }
    }

    private void sendJsonResponse(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        int userId = (int) s.getAttribute("userId");
        String fullName = (String) s.getAttribute("fullName");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            List<Assessment> assessments = findAssessmentsByUserId(userId);
            List<Skill> allSkills = findAllSkills();
            Set<Integer> attemptedSkills = findAttemptedSkills(userId);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"fullName\":\"").append(escapeJson(fullName)).append("\",");
            json.append("\"userId\":").append(userId).append(",");
            json.append("\"department\":\"CSE\",");
            json.append("\"examPattern\":\"Full Stack Development Assessment\",");
            json.append("\"skillGaps\":[");

            boolean firstSkill = true;
            for (Skill skill : allSkills) {
                if (!firstSkill) json.append(",");
                firstSkill = false;

                Assessment assessment = null;
                for (Assessment a : assessments) {
                    if (a.getSkillId() == skill.getId()) {
                        assessment = a;
                        break;
                    }
                }

                json.append("{");
                json.append("\"skillId\":").append(skill.getId()).append(",");
                json.append("\"skillName\":\"").append(escapeJson(skill.getSkillName())).append("\",");
                json.append("\"category\":\"").append(escapeJson(skill.getCategory())).append("\",");
                json.append("\"description\":\"").append(escapeJson(skill.getDescription())).append("\",");

                // Whether this exam has already been taken
                boolean examTaken = attemptedSkills.contains(skill.getId());
                json.append("\"examTaken\":").append(examTaken).append(",");

                int requiredLevel = 3, actualLevel = 0, gapLevel = 3;
                int score = 0, totalQuestions = 0;
                String gapCategory = "High";

                if (assessment != null) {
                    requiredLevel = assessment.getRequiredLevel();
                    actualLevel = assessment.getActualLevel();
                    gapLevel = assessment.getGapLevel();
                    gapCategory = gapLevel >= 2 ? "High" : (gapLevel == 1 ? "Medium" : "Low");
                } else {
                    requiredLevel = 3;
                    actualLevel = 0;
                    gapLevel = 3;
                    gapCategory = "High";
                }

                // Get score from exam_attempts if exam was taken
                if (examTaken) {
                    int[] scoreData = getExamAttemptScore(userId, skill.getId());
                    score = scoreData[0];
                    totalQuestions = scoreData[1];
                }

                json.append("\"requiredLevel\":").append(requiredLevel).append(",");
                json.append("\"actualLevel\":").append(actualLevel).append(",");
                json.append("\"gapLevel\":").append(gapLevel).append(",");
                json.append("\"gapCategory\":\"").append(gapCategory).append("\",");
                json.append("\"score\":").append(score).append(",");
                json.append("\"totalQuestions\":").append(totalQuestions).append(",");

                List<Resource> resources = findResourcesBySkillId(skill.getId());
                json.append("\"resources\":[");
                boolean firstResource = true;
                for (Resource resource : resources) {
                    if (!firstResource) json.append(",");
                    firstResource = false;
                    json.append("{");
                    json.append("\"title\":\"").append(escapeJson(resource.getTitle())).append("\",");
                    json.append("\"url\":\"").append(escapeJson(resource.getResourceUrl())).append("\",");
                    json.append("\"type\":\"").append(escapeJson(resource.getResourceType())).append("\"");
                    json.append("}");
                }
                json.append("]");

                json.append("}");
            }

            json.append("]");
            json.append("}");

            PrintWriter out = resp.getWriter();
            out.print(json.toString());
            out.flush();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Find all skill IDs that this user has already attempted.
     */
    private Set<Integer> findAttemptedSkills(int userId) throws Exception {
        Set<Integer> skills = new HashSet<>();
        String sql = "SELECT skill_id FROM exam_attempts WHERE user_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getInt("skill_id"));
                }
            }
        }
        return skills;
    }

    /**
     * Get the score and total for a specific exam attempt.
     */
    private int[] getExamAttemptScore(int userId, int skillId) throws Exception {
        String sql = "SELECT score, total_questions FROM exam_attempts WHERE user_id = ? AND skill_id = ?";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("score"), rs.getInt("total_questions")};
                }
            }
        }
        return new int[]{0, 0};
    }

    private List<Assessment> findAssessmentsByUserId(int userId) throws Exception {
        String sql = "SELECT a.id, a.user_id, a.skill_id, s.skill_name, s.category, a.required_level, a.actual_level, a.gap_level, a.assessed_at " +
                     "FROM assessment a " +
                     "JOIN skills s ON a.skill_id = s.id " +
                     "WHERE a.user_id = ? ORDER BY a.assessed_at DESC";
        List<Assessment> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Assessment a = new Assessment();
                    a.setId(rs.getInt("id"));
                    a.setUserId(rs.getInt("user_id"));
                    a.setSkillId(rs.getInt("skill_id"));
                    a.setSkillName(rs.getString("skill_name"));
                    a.setCategory(rs.getString("category"));
                    a.setRequiredLevel(rs.getInt("required_level"));
                    a.setActualLevel(rs.getInt("actual_level"));
                    a.setGapLevel(rs.getInt("gap_level"));
                    a.setAssessedAt(rs.getTimestamp("assessed_at"));
                    list.add(a);
                }
            }
        }
        return list;
    }

    private List<Skill> findAllSkills() throws Exception {
        String sql = "SELECT id, skill_name, category, description FROM skills ORDER BY category, skill_name";
        List<Skill> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Skill s = new Skill();
                    s.setId(rs.getInt("id"));
                    s.setSkillName(rs.getString("skill_name"));
                    s.setCategory(rs.getString("category"));
                    s.setDescription(rs.getString("description"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    private List<Resource> findResourcesBySkillId(int skillId) throws Exception {
        String sql = "SELECT id, skill_id, title, resource_url, resource_type FROM resources WHERE skill_id = ? ORDER BY resource_type, title";
        List<Resource> list = new ArrayList<>();
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Resource r = new Resource();
                    r.setId(rs.getInt("id"));
                    r.setSkillId(rs.getInt("skill_id"));
                    r.setTitle(rs.getString("title"));
                    r.setResourceUrl(rs.getString("resource_url"));
                    r.setResourceType(rs.getString("resource_type"));
                    list.add(r);
                }
            }
        }
        return list;
    }
}
