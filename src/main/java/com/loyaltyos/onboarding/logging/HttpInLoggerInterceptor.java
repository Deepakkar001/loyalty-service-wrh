package com.loyaltyos.onboarding.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Logs inbound HTTP requests once per request with handler + status + duration.
 * Body logging is intentionally omitted to avoid secrets/PII leakage.
 */
public class HttpInLoggerInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpInLoggerInterceptor.class);
    private static final String ATTR_START_NS = HttpInLoggerInterceptor.class.getName() + ".startNs";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_NS, System.nanoTime());

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null ? uri : (uri + "?" + query);

        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp == null || remoteIp.isBlank()) remoteIp = request.getRemoteAddr();

        String handlerName = "unknown";
        if (handler instanceof HandlerMethod hm) {
            handlerName = hm.getBeanType().getSimpleName() + "#" + hm.getMethod().getName();
        }

        // ENTER style (similar to many gateway/service loggers)
        log.info("ENTER {} {} handler={} remoteIp={}", method, path, handlerName, remoteIp);
        return true;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        @Nullable Exception ex
    ) {
        long start = 0L;
        Object v = request.getAttribute(ATTR_START_NS);
        if (v instanceof Long l) start = l;
        long durMs = start == 0L ? -1 : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null ? uri : (uri + "?" + query);

        String remoteIp = request.getHeader("X-Forwarded-For");
        if (remoteIp == null || remoteIp.isBlank()) remoteIp = request.getRemoteAddr();

        String handlerName = "unknown";
        if (handler instanceof HandlerMethod hm) {
            handlerName = hm.getBeanType().getSimpleName() + "#" + hm.getMethod().getName();
        }

        int status = response.getStatus();

        if (ex != null) {
            log.warn("EXIT  {} {} status={} durationMs={} handler={} error={}",
                method, path, status, durMs, handlerName, ex.getClass().getSimpleName());
            return;
        }
        log.info("EXIT  {} {} status={} durationMs={} handler={}", method, path, status, durMs, handlerName);
    }
}

