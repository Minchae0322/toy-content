package com.example.toycontent.app.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final long SLOW_THRESHOLD_MS = 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            int status = res.getStatus();
            String method = req.getMethod();
            String uri = req.getRequestURI();

            if (status >= 500) {
                log.error("[HTTP] {} {} {} - {}ms", method, uri, status, elapsed);
            } else if (status >= 400) {
                log.warn("[HTTP] {} {} {} - {}ms", method, uri, status, elapsed);
            } else if (elapsed > SLOW_THRESHOLD_MS) {
                log.warn("[HTTP-SLOW] {} {} {} - {}ms", method, uri, status, elapsed);
            } else {
                log.info("[HTTP] {} {} {} - {}ms", method, uri, status, elapsed);
            }
        }
    }
}
