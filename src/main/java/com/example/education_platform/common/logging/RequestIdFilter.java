package com.example.education_platform.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id, puts it in the logging context, and returns it to the caller.
 *
 * <p>This is what turns a log file into something you can actually read: instead of interleaved
 * lines from a dozen simultaneous requests, you filter on one id and get exactly one request's
 * story. When a student reports an error, the id in the response header is enough to find it.
 *
 * <p>Runs first, before security, so that even a rejected request is traceable — a 401 nobody can
 * find in the logs is a 401 nobody can explain.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "requestId";
    public static final String HEADER = "X-Request-Id";

    private static final int MAX_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = sanitise(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // Threads are pooled and reused, so leaving this behind would tag the next request
            // with the previous one's id
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * An id supplied by a caller is accepted so a trace can span services, but never verbatim: a
     * header containing a newline would let anyone forge whole log lines, and one containing a
     * megabyte of text would make every line of the request unreadable. Anything outside a plain
     * id alphabet is dropped, and anything left empty gets a generated id instead.
     */
    private static String sanitise(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String cleaned = supplied.replaceAll("[^A-Za-z0-9._-]", "");
        if (cleaned.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
    }
}
