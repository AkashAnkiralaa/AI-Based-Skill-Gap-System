package org.example.web;

import org.example.model.User;
import org.example.util.DBUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        String error = null;
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            error = "Username and password are required.";
        } else {
            try {
                User u = findByUsername(username);
                if (u == null || !password.equals(u.getPassword())) {
                    error = "Invalid username or password.";
                } else {
                    // success
                    HttpSession s = req.getSession(true);
                    s.setAttribute("userId", u.getId());
                    s.setAttribute("username", u.getUsername());
                    s.setAttribute("fullName", u.getFullName());
                    s.setAttribute("role", u.getRole());
                    // Role-based redirect
                    if ("faculty".equalsIgnoreCase(u.getRole())) {
                        resp.sendRedirect(req.getContextPath() + "/faculty-dashboard");
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/dashboard");
                    }
                    return;
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        resp.sendRedirect(req.getContextPath() + "/index.jsp?error=" + java.net.URLEncoder.encode(error, "UTF-8"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/index.jsp").forward(req, resp);
    }

    private User findByUsername(String username) throws Exception {
        String sql = "SELECT id, username, password, role, full_name FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection c = DBUtil.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPassword(rs.getString("password"));
                    u.setRole(rs.getString("role"));
                    u.setFullName(rs.getString("full_name"));
                    return u;
                }
            }
        }
        return null;
    }
}
