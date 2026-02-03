package com.example.demo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    private static final Logger log =
            LoggerFactory.getLogger(RequestCounterFilter.class);

    private static final String HOSTNAME = resolveHostname();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static int counter=0;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String timestamp = LocalDateTime.now().format(FORMATTER);
        counter++;
        String path = req.getRequestURI();
        if (req.getQueryString() != null) {
                path += "?" + req.getQueryString();
            }
        log.info("host={}, time={}, path={}, counter={}",
                HOSTNAME,
                timestamp,
                path,
                counter);

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