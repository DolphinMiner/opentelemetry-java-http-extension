package com.example.otel.http.ins;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * ServletInstrumentationV3 - 基于OpenTelemetry extension的Servlet 3.x增强器
 * 专门处理javax.servlet API (Servlet 3.x)
 *
 * @date 2022/03/03
 */
@AutoService(InstrumentationModule.class)
public class ServletInstrumentationV3 extends InstrumentationModule {
    private static final Logger logger = Logger.getLogger(ServletInstrumentationV3.class.getName());

    public ServletInstrumentationV3() {
        super("servlet-body-v3", "servlet-body-v3-1.0");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Collections.singletonList(new HttpServletV3Instrumentation());
    }

    private static class HttpServletV3Instrumentation implements TypeInstrumentation {
        @Override
        public ElementMatcher<TypeDescription> typeMatcher() {
            return implementsInterface(named("javax.servlet.Servlet"))
                    .and(not(implementsInterface(named("jakarta.servlet.Servlet"))));
        }

        @Override
        public void transform(TypeTransformer transformer) {
            transformer.applyAdviceToMethod(
                    named("service").and(takesArguments(2))
                            .and(takesArgument(0, named("javax.servlet.http.HttpServletRequest")))
                            .and(takesArgument(1, named("javax.servlet.http.HttpServletResponse"))),
                    HttpServletV3Instrumentation.class.getName() + "$ServiceAdvice"
            );
        }

        @SuppressWarnings("unused")
        public static class ServiceAdvice {
            @Advice.OnMethodEnter(suppress = Throwable.class)
            public static void onEnter(
                    @Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                    @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response,
                    @Advice.Local("wrappedRequest") CachedBodyHttpServletRequest wrappedRequest,
                    @Advice.Local("wrappedResponse") CachedBodyHttpServletResponse wrappedResponse) {
                try {
                    // 包装request和response以缓存body内容
                    wrappedRequest = new CachedBodyHttpServletRequest(request);
                    wrappedResponse = new CachedBodyHttpServletResponse(response);

                    // 替换原始的request和response
                    request = wrappedRequest;
                    response = wrappedResponse;
                    
                    logger.fine("ServletV3: Wrapped request and response for body capture");
                } catch (Exception e) {
                    logger.severe("ServletV3: Failed to wrap request/response: " + e.getMessage());
                }
            }

            @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
            public static void onExit(
                    @Advice.Local("wrappedRequest") CachedBodyHttpServletRequest wrappedRequest,
                    @Advice.Local("wrappedResponse") CachedBodyHttpServletResponse wrappedResponse,
                    @Advice.Thrown Throwable throwable) {
                try {
                    Context context = Context.current();
                    Span span = LocalRootSpan.fromContext(context);

                    if (span != null && wrappedRequest != null && wrappedResponse != null) {
                        // 记录请求信息
                        span.setAttribute("servlet.version", "3.x");
                        span.setAttribute("servlet.api", "javax.servlet");
                        
                        // 记录request body
                        String requestBody = wrappedRequest.getCachedBody();
                        if (requestBody != null && !requestBody.trim().isEmpty()) {
                            span.setAttribute("http.request.body", requestBody);
                        }

                        // 记录response body
                        String responseBody = wrappedResponse.getCachedBody();
                        if (responseBody != null && !responseBody.trim().isEmpty()) {
                            span.setAttribute("http.response.body", responseBody);
                        }

                        // 记录content types
                        String requestContentType = wrappedRequest.getContentType();
                        if (requestContentType != null) {
                            span.setAttribute("http.request.content_type", requestContentType);
                        }

                        String responseContentType = wrappedResponse.getContentType();
                        if (responseContentType != null) {
                            span.setAttribute("http.response.content_type", responseContentType);
                        }

                        // 记录请求头信息
                        Enumeration<String> headerNames = wrappedRequest.getHeaderNames();
                        if (headerNames != null) {
                            while (headerNames.hasMoreElements()) {
                                String headerName = headerNames.nextElement();
                                String headerValue = wrappedRequest.getHeader(headerName);
                                if (headerValue != null) {
                                    span.setAttribute("http.request.header." + headerName.toLowerCase(), headerValue);
                                }
                            }
                        }

                        logger.info("ServletV3: Recorded request and response data for span: " + span.getSpanContext().getSpanId());
                    }
                } catch (Exception e) {
                    logger.severe("ServletV3: Failed to record request/response data: " + e.getMessage());
                }
            }
        }
    }


}