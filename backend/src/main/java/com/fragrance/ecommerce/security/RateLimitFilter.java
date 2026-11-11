package com.fragrance.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int requestLimit;
    private final Duration window;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleaner");
        t.setDaemon(true);
        return t;
    });

    public RateLimitFilter(
        @Value("${app.rate-limit.max-requests:100}") int requestLimit,
        @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        if (requestLimit < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Rate limit configuration must be positive");
        }
        this.requestLimit = requestLimit;
        this.window = Duration.ofSeconds(windowSeconds);
        cleaner.scheduleAtFixedRate(this::evictExpired, windowSeconds, windowSeconds, TimeUnit.SECONDS);
    }

    private void evictExpired() {
        var now = Instant.now();
        windows.entrySet().removeIf(e -> now.isAfter(e.getValue().startedAt().plus(window)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var now = Instant.now();
        var key = request.getRemoteAddr();
        var window = windows.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.startedAt().plus(this.window))) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        if (window.count() > requestLimit) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(window.startedAt().plus(this.window).getEpochSecond() - now.getEpochSecond()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Too many requests\",\"detail\":\"Try again after the rate-limit window.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Window(Instant startedAt, int count) {}
}
