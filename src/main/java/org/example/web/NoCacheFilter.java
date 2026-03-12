package org.example.web;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Prevents browser caching of HTML/JS/CSS files so users always get the latest
 * version.
 */
@WebFilter(urlPatterns = { "*.html", "*.css", "*.js" })
public class NoCacheFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResp = (HttpServletResponse) response;
        httpResp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResp.setHeader("Pragma", "no-cache");
        httpResp.setDateHeader("Expires", 0);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
