package com.helps.infra.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebSocketCorsFilter extends OncePerRequestFilter {

    @Value("${websocket.allowed-origins}")
    private String websocketAllowedOrigins;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().contains("/ws")) {

            String[] origins = websocketAllowedOrigins.split(",");
            String origin = request.getHeader("Origin");

            boolean originAllowed = false;
            if (origin != null) {
                for (String allowedOrigin : origins) {
                    if (allowedOrigin.trim().equals(origin)) {
                        originAllowed = true;
                        break;
                    }
                }
            }

            if (originAllowed) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            } else if (origins.length > 0) {
                response.setHeader("Access-Control-Allow-Origin", origins[0].trim());
            }

            response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");

            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}