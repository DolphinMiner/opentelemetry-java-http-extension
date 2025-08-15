package com.example.otel.http;

import io.opentelemetry.api.trace.Span;
import net.bytebuddy.asm.Advice;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

/**
 * HTTP Servlet 拦截建议类
 * 将 HTTP 数据添加到当前活动的 OpenTelemetry span
 */
public class HttpServletAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(0) ServletRequest request,
            @Advice.Argument(1) ServletResponse response) {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            try {
                addRequestAttributes(httpRequest);
            } catch (Exception e) {
                // 静默处理错误，避免影响业务逻辑
            }
        }
    }

    @Advice.OnMethodExit
    public static void onExit(
            @Advice.Argument(0) ServletRequest request,
            @Advice.Argument(1) ServletResponse response) {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            try {
                addResponseAttributes(httpRequest, httpResponse);
            } catch (Exception e) {
                // 静默处理错误，避免影响业务逻辑
            }
        }
    }

    private static void addRequestAttributes(HttpServletRequest request) {
        if (!HttpCaptureConfig.isRequestBodyCaptureEnabled()) {
            return;
        }

        Span currentSpan = Span.current();
        if (!currentSpan.getSpanContext().isValid()) {
            return;
        }

        try {
            // 添加请求头信息
            StringBuilder headersBuilder = new StringBuilder();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                if (headersBuilder.length() > 0) {
                    headersBuilder.append(", ");
                }
                headersBuilder.append(headerName).append("=").append(headerValue);
            }
            
            if (headersBuilder.length() > 0) {
                currentSpan.setAttribute("http.request.headers", headersBuilder.toString());
            }

            // 添加查询参数
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.trim().isEmpty()) {
                currentSpan.setAttribute("http.request.query_string", queryString);
            }

            // 添加请求体（仅对有请求体的方法）
            String method = request.getMethod();
            if ("POST".equalsIgnoreCase(method) || 
                "PUT".equalsIgnoreCase(method) ||
                "PATCH".equalsIgnoreCase(method)) {
                
                String requestBody = getRequestBody(request);
                if (requestBody != null && !requestBody.trim().isEmpty()) {
                    // 限制请求体长度
                    int maxSize = HttpCaptureConfig.getRequestBodyMaxSize();
                    if (requestBody.length() > maxSize) {
                        requestBody = requestBody.substring(0, maxSize) + "... [truncated]";
                    }
                    currentSpan.setAttribute("http.request.body", requestBody);
                }
            }

        } catch (Exception e) {
            currentSpan.setAttribute("http.request.capture_error", e.getMessage());
        }
    }

    private static void addResponseAttributes(HttpServletRequest request, HttpServletResponse response) {
        if (!HttpCaptureConfig.isResponseBodyCaptureEnabled()) {
            return;
        }

        Span currentSpan = Span.current();
        if (!currentSpan.getSpanContext().isValid()) {
            return;
        }

        try {
            // 添加响应头信息
            StringBuilder headersBuilder = new StringBuilder();
            for (String headerName : response.getHeaderNames()) {
                String headerValue = response.getHeader(headerName);
                if (headersBuilder.length() > 0) {
                    headersBuilder.append(", ");
                }
                headersBuilder.append(headerName).append("=").append(headerValue);
            }
            
            if (headersBuilder.length() > 0) {
                currentSpan.setAttribute("http.response.headers", headersBuilder.toString());
            }

            // 添加内容类型
            String contentType = response.getContentType();
            if (contentType != null) {
                currentSpan.setAttribute("http.response.content_type", contentType);
            }


        } catch (Exception e) {
            currentSpan.setAttribute("http.response.capture_error", e.getMessage());
        }
    }

    private static String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line).append("\n");
        }
        return body.toString();
    }
}
