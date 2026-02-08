package com.example.demo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class RequestCounterFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestCounterFilter.class);

    private static final String HOSTNAME = resolveHostname();

    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Use AtomicInteger to prevent race conditions (thread-safe)
    private static final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String userAgent = req.getHeader("User-Agent");

        // 1. Identify AWS Health Checks
        // The ELB (Elastic Load Balancer) hits your '/' path every few seconds.
        // We check the User-Agent to see if it's the AWS Health Checker.
        boolean isHealthCheck = (userAgent != null && userAgent.contains("ELB-HealthChecker"));

        // 2. Increment counter ONLY if it's a real request (optional)
        // If you want to keep the 81,000+ count, remove the 'if' check.
        // If you only want to count real people, keep the 'if' check.
        int currentCount;
        if (!isHealthCheck) {
            currentCount = counter.incrementAndGet();
        } else {
            currentCount = counter.get(); // Just get current value without incrementing
        }

        // 3. Build the full path (URI + Query String)
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String path = req.getRequestURI();
        String query = req.getQueryString();
        String fullPath = (query != null) ? path + "?" + query : path;

        // 4. Log the details
        // Added 'type' to the log so you can see if it's a USER or a HEALTH_CHECK
        log.info("host={}, type={}, time={}, path={}, counter={}, ua={}",
                HOSTNAME,
                isHealthCheck ? "HEALTH_CHECK" : "USER_VISIT",
                timestamp,
                fullPath,
                currentCount,
                userAgent);

        chain.doFilter(request, response);
    }

    private static String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown-host";
        }
    }
}