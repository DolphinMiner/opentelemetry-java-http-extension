package com.example.otel.http;

import io.opentelemetry.api.trace.Span;
import net.bytebuddy.asm.Advice;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * HTTP 客户端拦截建议类
 * 将出站 HTTP 数据添加到当前活动的 OpenTelemetry span
 */
public class HttpClientAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This HttpURLConnection connection) {
        if (!HttpCaptureConfig.isOutboundCaptureEnabled()) {
            return;
        }

        Span currentSpan = Span.current();
        if (!currentSpan.getSpanContext().isValid()) {
            return;
        }

        try {
            addOutboundRequestAttributes(currentSpan, connection);
        } catch (Exception e) {
            // 静默处理错误
        }
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.This HttpURLConnection connection) {
        if (!HttpCaptureConfig.isOutboundCaptureEnabled()) {
            return;
        }

        Span currentSpan = Span.current();
        if (!currentSpan.getSpanContext().isValid()) {
            return;
        }

        try {
            addOutboundResponseAttributes(currentSpan, connection);
        } catch (Exception e) {
            // 静默处理错误
        }
    }

    private static void addOutboundRequestAttributes(Span span, HttpURLConnection connection) {
        try {
            // 添加请求方法
            span.setAttribute("http.client.method", connection.getRequestMethod());
            
            // 添加请求 URL
            span.setAttribute("http.client.url", connection.getURL().toString());
            
            // 添加请求头
            StringBuilder headersBuilder = new StringBuilder();
            for (String key : connection.getRequestProperties().keySet()) {
                if (key != null) {
                    String value = connection.getRequestProperty(key);
                    if (headersBuilder.length() > 0) {
                        headersBuilder.append(", ");
                    }
                    headersBuilder.append(key).append("=").append(value);
                }
            }
            
            if (headersBuilder.length() > 0) {
                span.setAttribute("http.client.request.headers", headersBuilder.toString());
            }

        } catch (Exception e) {
            span.setAttribute("http.client.request.capture_error", e.getMessage());
        }
    }

    private static void addOutboundResponseAttributes(Span span, HttpURLConnection connection) {
        try {
            // 添加响应状态码
            try {
                int responseCode = connection.getResponseCode();
                span.setAttribute("http.client.response.status_code", responseCode);
                
                String responseMessage = connection.getResponseMessage();
                if (responseMessage != null) {
                    span.setAttribute("http.client.response.status_message", responseMessage);
                }
            } catch (IOException e) {
                span.setAttribute("http.client.response.status_error", e.getMessage());
            }

            // 添加响应头
            StringBuilder headersBuilder = new StringBuilder();
            try {
                for (String key : connection.getHeaderFields().keySet()) {
                    if (key != null) {
                        String value = connection.getHeaderField(key);
                        if (headersBuilder.length() > 0) {
                            headersBuilder.append(", ");
                        }
                        headersBuilder.append(key).append("=").append(value);
                    }
                }
                
                if (headersBuilder.length() > 0) {
                    span.setAttribute("http.client.response.headers", headersBuilder.toString());
                }
            } catch (Exception e) {
                span.setAttribute("http.client.response.headers_error", e.getMessage());
            }

            // 添加内容类型和长度
            String contentType = connection.getContentType();
            if (contentType != null) {
                span.setAttribute("http.client.response.content_type", contentType);
            }

            int contentLength = connection.getContentLength();
            if (contentLength >= 0) {
                span.setAttribute("http.client.response.content_length", contentLength);
            }

        } catch (Exception e) {
            span.setAttribute("http.client.response.capture_error", e.getMessage());
        }
    }
}
