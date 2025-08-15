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

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * 直接针对我们的ApiController进行body捕获
 * 这样避免与servlet的复杂性和类加载问题
 */
@AutoService(InstrumentationModule.class)
public class WorkingBodyCapture extends InstrumentationModule {
    private static final Logger logger = Logger.getLogger(WorkingBodyCapture.class.getName());

    public WorkingBodyCapture() {
        super("working-body-capture", "working-1.0");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Collections.singletonList(new ApiControllerInstrumentation());
    }

    public static class ApiControllerInstrumentation implements TypeInstrumentation {
        @Override
        public ElementMatcher<TypeDescription> typeMatcher() {
            return named("com.example.demo.ApiController");
        }

        @Override
        public void transform(TypeTransformer transformer) {
            transformer.applyAdviceToMethod(
                    named("echo"),
                    ApiControllerInstrumentation.class.getName() + "$EchoMethodAdvice"
            );
        }

        public static class EchoMethodAdvice {
            @Advice.OnMethodEnter(suppress = Throwable.class)
            public static void onEnter(@Advice.Argument(0) Object requestBody) {
                try {
                    logger.info("WorkingBodyCapture: Echo method called with request: " + requestBody);
                    
                    Context context = Context.current();
                    Span span = LocalRootSpan.fromContext(context);
                    
                    if (span != null && requestBody != null) {
                        String bodyStr = requestBody.toString();
                        span.setAttribute("http.request.body", bodyStr);
                        span.setAttribute("custom.request.capture", "true");
                        
                        logger.info("WorkingBodyCapture: Added http.request.body to span: " + bodyStr);
                    }
                } catch (Exception e) {
                    logger.severe("WorkingBodyCapture: Failed to capture request body: " + e.getMessage());
                }
            }

            @Advice.OnMethodExit(suppress = Throwable.class)
            public static void onExit(@Advice.Return Object responseBody) {
                try {
                    logger.info("WorkingBodyCapture: Echo method returning: " + responseBody);
                    
                    Context context = Context.current();
                    Span span = LocalRootSpan.fromContext(context);
                    
                    if (span != null && responseBody != null) {
                        String bodyStr = responseBody.toString();
                        span.setAttribute("http.response.body", bodyStr);
                        span.setAttribute("custom.response.capture", "true");
                        
                        logger.info("WorkingBodyCapture: Added http.response.body to span: " + bodyStr);
                    }
                } catch (Exception e) {
                    logger.severe("WorkingBodyCapture: Failed to capture response body: " + e.getMessage());
                }
            }
        }
    }
}
