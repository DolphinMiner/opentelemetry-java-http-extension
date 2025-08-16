package com.example.otel.http.ins;

import com.example.otel.http.ins.util.Pair;
import com.example.otel.http.ins.wrapper.CachedBodyRequestWrapperV3;
import com.example.otel.http.ins.wrapper.CachedBodyResponseWrapperV3;
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

    public static <TRequest, TResponse> Pair<TRequest, TResponse> onServiceEnter(
            ServletAdapter<TRequest, TResponse> adapter, Object servletRequest,
            Object servletResponse) {
        try {

            TRequest httpServletRequest = adapter.asHttpServletRequest(servletRequest);
            if (httpServletRequest == null) {
                return null;
            }

            TResponse httpServletResponse = adapter.asHttpServletResponse(servletResponse);
            if (httpServletResponse == null) {
                return null;
            }

            System.out.println("============================onServiceEnter start============================");
            System.out.println(servletRequest);
            System.out.println(servletResponse);
            String traceId = Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId();
            String spanId = Java8BytecodeBridge.currentSpan().getSpanContext().getSpanId();
            System.out.println("traceId:" + traceId);
            System.out.println("spanId:" + spanId);
            System.out.println("============================onServiceEnter end============================");
            // warp req res
            // if (ContextManager.needRecordOrReplay()) {}
            httpServletRequest = adapter.wrapRequest(httpServletRequest);
            httpServletResponse = adapter.wrapResponse(httpServletResponse);
            return Pair.of(httpServletRequest, httpServletResponse);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;

    }

    public static <TRequest, TResponse> void onServiceExit(
            ServletAdapter<TRequest, TResponse> adapter, Object servletRequest,
            Object servletResponse) {
        try {
            System.out.println("============================onServiceExit start============================");
            TRequest httpServletRequest = adapter.asHttpServletRequest(servletRequest);
            TResponse httpServletResponse = adapter.asHttpServletResponse(servletResponse);
            if (httpServletRequest == null || httpServletResponse == null) {
                return;
            }

            if (!adapter.wrapped(httpServletRequest, httpServletResponse)) {
                return;
            }

            // 提取request 和 response
            byte[] requestBytes = adapter.getRequestBytes(httpServletRequest);
            byte[] responseBytes = adapter.getResponseBytes(httpServletResponse);
            String reqJsonStr = OBJECT_MAPPER.writeValueAsString(new String(requestBytes));
            String resJsonStr = OBJECT_MAPPER.writeValueAsString(new String(responseBytes));

            System.out.println("request:" + reqJsonStr);
            System.out.println("response:" + resJsonStr);

            String traceId = Java8BytecodeBridge.currentSpan().getSpanContext().getTraceId();
            String spanId = Java8BytecodeBridge.currentSpan().getSpanContext().getSpanId();
            System.out.println("traceId:" + traceId);
            System.out.println("spanId:" + spanId);
            System.out.println("============================onServiceExit end============================");

            // 还原response
            adapter.copyBodyToResponse(httpServletResponse);
        } catch (Exception e) {
            System.out.println("序列化过程中出错: " + e.getMessage());
        }
    }
}
