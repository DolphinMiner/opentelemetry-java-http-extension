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
    
    // 使用 ThreadLocal 来存储每个线程的 ResponseWrapper
    private static final ThreadLocal<ResponseWrapper> responseWrapperHolder = new ThreadLocal<>();

    public static void onServiceEnter(Object servletRequest,
                                      Object servletResponse){
        try {
            System.out.println("============================onServiceEnter start============================");
            System.out.println(servletRequest);
            System.out.println(servletResponse);
            
            // 如果是 HttpServletResponse，创建 ResponseWrapper 并存储
            if (servletResponse instanceof HttpServletResponse) {
                HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
                ResponseWrapper wrapper = new ResponseWrapper(httpResponse);
                responseWrapperHolder.set(wrapper);
                
                // 将包装后的响应传递给后续处理
                // 注意：这里我们需要修改原始的 servletResponse 引用
                // 但由于 Java 是值传递，我们需要其他方式来实现
                System.out.println("已创建 ResponseWrapper，响应体将被捕获");
            }
            
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
            
            // 尝试获取响应信息，优先使用 ResponseWrapper
            Map<String, Object> responseInfo;
            ResponseWrapper wrapper = responseWrapperHolder.get();
            if (wrapper != null) {
                responseInfo = extractResponseInfoFromWrapper(wrapper);
                // 清理 ThreadLocal
                responseWrapperHolder.remove();
            } else {
                responseInfo = extractResponseInfo(servletResponse);
            }
            
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
     * 从 ResponseWrapper 提取响应信息
     */
    private static Map<String, Object> extractResponseInfoFromWrapper(ResponseWrapper wrapper) {
        Map<String, Object> responseInfo = new HashMap<>();
        
        try {
            responseInfo.put("status", wrapper.getStatus());
            responseInfo.put("contentType", wrapper.getContentType());
            responseInfo.put("characterEncoding", wrapper.getCharacterEncoding());
            
            // 获取响应头
            Map<String, String> headers = new HashMap<>();
            java.util.Collection<String> headerNames = wrapper.getHeaderNames();
            for (String headerName : headerNames) {
                headers.put(headerName, wrapper.getHeader(headerName));
            }
            responseInfo.put("headers", headers);
            
            // 获取响应体内容
            String responseBody = wrapper.getCapturedContent();
            if (responseBody != null && !responseBody.isEmpty()) {
                responseInfo.put("body", responseBody);
                responseInfo.put("bodySize", wrapper.getContentSize());
            } else {
                responseInfo.put("body", "响应体为空");
                responseInfo.put("bodySize", 0);
            }
            
            responseInfo.put("source", "ResponseWrapper 捕获");
            
        } catch (Exception e) {
            responseInfo.put("error", "从 ResponseWrapper 提取信息时出错: " + e.getMessage());
        }
        
        return responseInfo;
    }
    
    /**
     * 从标准 HttpServletResponse 提取响应信息
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
            
            responseInfo.put("body", "使用标准 HttpServletResponse，无法获取响应体内容");
            responseInfo.put("note", "建议在 onServiceEnter 中创建 ResponseWrapper 来获取完整的响应体内容");
            responseInfo.put("source", "标准 HttpServletResponse");
        } else {
            responseInfo.put("error", "不是 HttpServletResponse 类型");
        }
        
        return responseInfo;
    }
    
    /**
     * 获取当前线程的 ResponseWrapper（供外部使用）
     */
    public static ResponseWrapper getCurrentResponseWrapper() {
        return responseWrapperHolder.get();
    }
    
    /**
     * 清理当前线程的 ResponseWrapper（供外部使用）
     */
    public static void clearCurrentResponseWrapper() {
        responseWrapperHolder.remove();
    }
}
