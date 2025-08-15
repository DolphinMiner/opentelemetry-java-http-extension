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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * 简化的Response Body捕获实现
 * 直接拦截HttpServletResponse.getWriter()方法
 */
@AutoService(InstrumentationModule.class)
public class SimpleResponseBodyCapture extends InstrumentationModule {
    private static final Logger logger = Logger.getLogger(SimpleResponseBodyCapture.class.getName());

    public SimpleResponseBodyCapture() {
        super("simple-response-body-capture", "simple-1.0");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Collections.singletonList(new ResponseWriterInstrumentation());
    }

    public static class ResponseWriterInstrumentation implements TypeInstrumentation {
        @Override
        public ElementMatcher<TypeDescription> typeMatcher() {
            return hasSuperType(named("javax.servlet.http.HttpServletResponse"));
        }

        @Override
        public void transform(TypeTransformer transformer) {
            transformer.applyAdviceToMethod(
                    named("getWriter"),
                    ResponseWriterInstrumentation.class.getName() + "$GetWriterAdvice"
            );
        }

        public static class GetWriterAdvice {
            @Advice.OnMethodExit(suppress = Throwable.class)
            public static void onExit(@Advice.Return(readOnly = false) PrintWriter writer,
                                     @Advice.This HttpServletResponse response) {
                try {
                    if (writer != null) {
                        // 创建一个捕获写入内容的PrintWriter包装器
                        CapturingPrintWriter capturingWriter = new CapturingPrintWriter(writer);
                        writer = capturingWriter;
                        
                        logger.info("SimpleResponseBodyCapture: Wrapped PrintWriter for response body capture");
                        
                        // 在请求结束时记录body（这里用一个简单的Hook模拟）
                        // 实际实现中需要在适当的地方调用
                        recordResponseBody(capturingWriter.getCapturedContent());
                    }
                } catch (Exception e) {
                    logger.severe("Failed to wrap PrintWriter: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 将捕获的response body记录到span中
     */
    private static void recordResponseBody(String responseBody) {
        try {
            Context context = Context.current();
            Span span = LocalRootSpan.fromContext(context);
            
            if (span != null && responseBody != null && !responseBody.trim().isEmpty()) {
                span.setAttribute("http.response.body", responseBody);
                span.setAttribute("custom.response.capture", "true");
                
                Logger.getLogger(SimpleResponseBodyCapture.class.getName())
                      .info("SimpleResponseBodyCapture: Recorded response body: " + responseBody.substring(0, Math.min(100, responseBody.length())));
            }
        } catch (Exception e) {
            Logger.getLogger(SimpleResponseBodyCapture.class.getName())
                  .severe("Failed to record response body: " + e.getMessage());
        }
    }

    /**
     * 捕获写入内容的PrintWriter包装器
     */
    public static class CapturingPrintWriter extends PrintWriter {
        private final PrintWriter original;
        private final StringWriter capturedContent;

        public CapturingPrintWriter(PrintWriter original) {
            super(new StringWriter());
            this.original = original;
            this.capturedContent = new StringWriter();
        }

        @Override
        public void write(String s) {
            original.write(s);
            capturedContent.write(s);
        }

        @Override
        public void write(char[] buf, int off, int len) {
            original.write(buf, off, len);
            capturedContent.write(buf, off, len);
        }

        @Override
        public void write(int c) {
            original.write(c);
            capturedContent.write(c);
        }

        @Override
        public void print(String s) {
            original.print(s);
            capturedContent.write(s);
        }

        @Override
        public void println(String s) {
            original.println(s);
            capturedContent.write(s + "\n");
        }

        @Override
        public void flush() {
            original.flush();
            // 在flush时记录body
            recordResponseBody(getCapturedContent());
        }

        @Override
        public void close() {
            // 在close时记录body
            recordResponseBody(getCapturedContent());
            original.close();
        }

        public String getCapturedContent() {
            return capturedContent.toString();
        }
    }
}
