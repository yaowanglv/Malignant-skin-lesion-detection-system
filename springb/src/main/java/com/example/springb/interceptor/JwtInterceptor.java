package com.example.springb.interceptor;

import com.example.springb.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String ADMIN_PATH_PREFIX = "/admin";
    private static final String CONFIG_PATH_PREFIX = "/config";

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "未提供有效的认证信息");
            return false;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            sendError(response, 401, "Token 无效或已过期，请重新登录");
            return false;
        }

        Integer userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUsername", username);
        request.setAttribute("currentRole", role);

        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        if (requestPath.startsWith(ADMIN_PATH_PREFIX) && !"admin".equalsIgnoreCase(role)) {
            sendError(response, 403, "权限不足，无法访问该资源");
            return false;
        }

        if (isConfigWriteRequest(requestPath, request.getMethod()) && !"admin".equalsIgnoreCase(role)) {
            sendError(response, 403, "权限不足，无法访问该资源");
            return false;
        }

        return true;
    }

    private boolean isConfigWriteRequest(String path, String method) {
        return path.startsWith(CONFIG_PATH_PREFIX) && !"GET".equalsIgnoreCase(method);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", String.valueOf(status));
        result.put("msg", message);
        result.put("data", null);

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
