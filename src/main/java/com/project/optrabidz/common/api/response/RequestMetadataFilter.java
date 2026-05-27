package com.project.optrabidz.common.api.response;

import com.project.optrabidz.common.observability.ObservabilityMdcKeys;
import com.project.optrabidz.common.observability.RequestIdProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestMetadataFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestIdProvider.resolveOrCreate(request);
        response.setHeader(RequestIdProvider.REQUEST_ID_HEADER, requestId);

        try {
            MDC.put(ObservabilityMdcKeys.REQUEST_ID, requestId);
            MDC.put(ObservabilityMdcKeys.METHOD, request.getMethod());
            MDC.put(ObservabilityMdcKeys.PATH, request.getRequestURI());
            MDC.put(ObservabilityMdcKeys.CLIENT_IP, request.getRemoteAddr());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ObservabilityMdcKeys.REQUEST_ID);
            MDC.remove(ObservabilityMdcKeys.METHOD);
            MDC.remove(ObservabilityMdcKeys.PATH);
            MDC.remove(ObservabilityMdcKeys.CLIENT_IP);
        }
    }
}
