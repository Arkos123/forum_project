package com.example.oss.filter;

import com.example.common.constants.GatewayHeaders;
import com.example.common.entity.RestBean;
import com.example.oss.utils.Const;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class GatewayIdentityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(GatewayHeaders.USER_ID);
        if (userId == null || userId.isBlank()) {
            writeUnauthorized(response);
            return;
        }

        try {
            request.setAttribute(Const.ATTR_USER_ID, Integer.parseInt(userId));
        } catch (NumberFormatException e) {
            writeUnauthorized(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/images/")
                || path.startsWith("/actuator/")
                || path.startsWith("/error");
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(RestBean.unauthorized("登录状态已过期，请重新登录！").asJsonString());
    }
}
