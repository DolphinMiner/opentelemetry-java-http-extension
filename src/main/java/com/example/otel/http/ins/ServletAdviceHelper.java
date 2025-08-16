package com.example.otel.http.ins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServletAdviceHelper {
    // 使用单例的 ObjectMapper，避免重复创建对象
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void onServiceEnter(Object servletRequest,
                                      Object servletResponse){
        try {
            System.out.println("============================onServiceEnter start============================");
            System.out.println(servletRequest);
            System.out.println(servletResponse);
            String traceId= Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId();
            String spanId = Java8BytecodeBridge.currentSpan().getSpanContext().getSpanId();
            System.out.println("traceId:" + traceId);
            System.out.println("spanId:" + spanId);
            System.out.println("============================onServiceEnter end============================");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void onServiceExit(Object servletRequest,
                                     Object servletResponse){
        try {
            System.out.println("============================onServiceExit start============================");
            
            // 提取请求信息
            Map<String, Object> requestInfo = extractRequestInfo(servletRequest);
            // 提取响应信息
            Map<String, Object> responseInfo = extractResponseInfo(servletResponse);
            
            // 序列化并打印
            String requestJson = OBJECT_MAPPER.writeValueAsString(requestInfo);
            String responseJson = OBJECT_MAPPER.writeValueAsString(responseInfo);
            
            System.out.println("Request Info: " + requestJson);
            System.out.println("Response Info: " + responseJson);
            
            String traceId= Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId();
            String spanId = Java8BytecodeBridge.currentSpan().getSpanContext().getSpanId();
            System.out.println("traceId:" + traceId);
            System.out.println("spanId:" + spanId);
            System.out.println("============================onServiceExit end============================");
        } catch (Exception e) {
            System.out.println("序列化过程中出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 提取请求信息
     */
    private static Map<String, Object> extractRequestInfo(Object servletRequest) {
        Map<String, Object> requestInfo = new HashMap<>();
        
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            
            requestInfo.put("method", request.getMethod());
            requestInfo.put("requestURI", request.getRequestURI());
            requestInfo.put("queryString", request.getQueryString());
            requestInfo.put("contentType", request.getContentType());
            requestInfo.put("contentLength", request.getContentLength());
            
            // 获取请求头
            Map<String, String> headers = new HashMap<>();
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
            requestInfo.put("headers", headers);
            
            // 尝试获取请求体
            try {
                if (request.getContentLength() > 0) {
                    BufferedReader reader = request.getReader();
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                    requestInfo.put("body", body.toString());
                } else {
                    requestInfo.put("body", "无请求体或请求体为空");
                }
            } catch (IOException e) {
                requestInfo.put("body", "无法读取请求体: " + e.getMessage());
            }
        } else {
            requestInfo.put("error", "不是 HttpServletRequest 类型");
        }
        
        return requestInfo;
    }
    
    /**
     * 提取响应信息
     */
    private static Map<String, Object> extractResponseInfo(Object servletResponse) {
        Map<String, Object> responseInfo = new HashMap<>();
        
        if (servletResponse instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            
            responseInfo.put("status", response.getStatus());
            responseInfo.put("contentType", response.getContentType());
            responseInfo.put("characterEncoding", response.getCharacterEncoding());
            
            // 获取响应头
            Map<String, String> headers = new HashMap<>();
            java.util.Collection<String> headerNames = response.getHeaderNames();
            for (String headerName : headerNames) {
                headers.put(headerName, response.getHeader(headerName));
            }
            responseInfo.put("headers", headers);
            
            // 注意：HttpServletResponse 的 body 通常无法直接获取
            // 因为响应可能还没有完全写入
            responseInfo.put("body", "响应体无法直接获取（通常需要自定义 ResponseWrapper）");
        } else {
            responseInfo.put("error", "不是 HttpServletResponse 类型");
        }
        
        return responseInfo;
    }
}
