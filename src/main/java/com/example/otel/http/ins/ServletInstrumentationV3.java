package com.example.otel.http.ins;

import com.example.otel.http.ins.util.ServletAdviceHelper;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

/**
 * ServletInstrumentationV3 - 基于OpenTelemetry extension的Servlet 3.x增强器
 * 专门处理javax.servlet API (Servlet 3.x)
 *
 * @date 2022/03/03
 */
public class ServletInstrumentationV3 implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return AgentElementMatchers.hasSuperType(
                namedOneOf("javax.servlet.Filter", "javax.servlet.http.HttpServlet"));
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
        typeTransformer.applyAdviceToMethod(
                namedOneOf("doFilter", "service")
                        .and(
                                ElementMatchers.takesArgument(
                                        0, ElementMatchers.named("javax.servlet.ServletRequest")))
                        .and(
                                ElementMatchers.takesArgument(
                                        1, ElementMatchers.named("javax.servlet.ServletResponse")))
                        .and(ElementMatchers.isPublic()),
                this.getClass().getName() + "$Servlet3Advice");
    }

    @SuppressWarnings("unused")
    public static class Servlet3Advice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(@Advice.Argument(value = 0) ServletRequest request,
                                   @Advice.Argument(value = 1) ServletResponse response) {
            ServletAdviceHelper.onServiceEnter(request, response);
//            if (!(response instanceof HttpServletResponse)) {
//                return;
//            }
//
//            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
//            if (!httpServletResponse.containsHeader("X-server-id")) {
//            }
        }

        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void onExit(@Advice.Argument(value = 0, readOnly = false) ServletRequest request,
                                  @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
            ServletAdviceHelper.onServiceExit(request, response);
        }
    }
}