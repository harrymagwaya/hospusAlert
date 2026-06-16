package com.shanalert.hospitalalert.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.shanalert.hospitalalert.config.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
        // Registering JavaTimeModule to handle LocalDateTime properly in JSON
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // Get the user's current identity from the security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> data = new HashMap<>();
        data.put("status", HttpServletResponse.SC_FORBIDDEN);
        data.put("error", "Forbidden");
        data.put("timestamp", LocalDateTime.now(ZoneId.of("UTC")));
        data.put("path", request.getServletPath());

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            // Context-aware error message for your specific roles
            data.put("message", "Access denied. Your role '" + userPrincipal.getUserRole() + "' does not have permission for this action.");
            data.put("user_email", userPrincipal.getEmail());
            data.put("user_status", userPrincipal.getStatus());
        } else {
            data.put("message", "Anonymous user: You do not have permission to access this resource. Please log in.");
        }

        // Convert the Map to JSON and write it to the response body
        String jsonResponse = objectMapper.writeValueAsString(data);
        response.getWriter().write(jsonResponse);
    }
}