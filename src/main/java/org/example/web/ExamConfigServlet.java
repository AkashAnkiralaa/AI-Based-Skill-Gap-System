package org.example.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

@WebServlet("/api/exam-config")
public class ExamConfigServlet extends HttpServlet {

    private int numQuestions = 30;
    private int totalTimeMinutes = 5;
    private int questionTimeSeconds = 10;

    @Override
    public void init() throws ServletException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                numQuestions = Integer.parseInt(props.getProperty("exam.numQuestions", "30"));
                totalTimeMinutes = Integer.parseInt(props.getProperty("exam.totalTimeMinutes", "5"));
                questionTimeSeconds = Integer.parseInt(props.getProperty("exam.questionTimeSeconds", "10"));
            }
        } catch (Exception e) {
            System.err.println("Failed to load exam config: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String fullName = (String) s.getAttribute("fullName");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"numQuestions\":").append(numQuestions).append(",");
        json.append("\"totalTimeMinutes\":").append(totalTimeMinutes).append(",");
        json.append("\"questionTimeSeconds\":").append(questionTimeSeconds).append(",");
        json.append("\"candidateName\":\"").append(esc(fullName != null ? fullName : "Exam Candidate")).append("\"");
        json.append("}");

        PrintWriter out = resp.getWriter();
        out.print(json.toString());
        out.flush();
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
