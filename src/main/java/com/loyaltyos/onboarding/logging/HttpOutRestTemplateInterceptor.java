package com.loyaltyos.onboarding.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Logs outbound HTTP calls made via RestTemplate and propagates X-Request-Id.
 * Intentionally does NOT log request/response bodies by default.
 */
public class HttpOutRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpOutRestTemplateInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long start = System.nanoTime();
        URI uri = request.getURI();

        // Propagate correlation id if present.
        String traceId = MDC.get(CorrelationIdFilter.MDC_TRACE_ID);
        if (traceId != null && !traceId.isBlank()) {
            HttpHeaders headers = request.getHeaders();
            if (!headers.containsKey(CorrelationIdFilter.HEADER_REQUEST_ID)) {
                headers.add(CorrelationIdFilter.HEADER_REQUEST_ID, traceId);
            }
        }

        String method = request.getMethod() == null ? "?" : request.getMethod().name();
        String host = uri.getHost();
        String path = uri.getPath();

        log.info("event=HTTP_OUT phase=request method={} url={} host={} path={}", method, uri, host, path);
        try {
            ClientHttpResponse res = execution.execute(request, body);
            long durMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("event=HTTP_OUT phase=response method={} url={} status={} durationMs={}",
                method, uri, res.getStatusCode().value(), durMs);
            return res;
        } catch (Exception ex) {
            long durMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.warn("event=HTTP_OUT phase=error method={} url={} durationMs={} error={}",
                method, uri, durMs, ex.getClass().getSimpleName());
            throw ex;
        }
    }
}

