package com.example.otel.http.ins;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;


import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;

/**
 * ø
 * ServletInstrumentationV5 - 基于OpenTelemetry extension的Servlet 5.x增强器
 * 专门处理jakarta.servlet API (Servlet 5.x/6.x)
 *
 * @date 2022/03/03
 */

public class ServletInstrumentationV5 implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return AgentElementMatchers.hasSuperType(
                namedOneOf("jakarta.servlet.http.HttpServlet"));
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
        typeTransformer.applyAdviceToMethod(
                namedOneOf("doFilter", "service")
                        .and(
                                ElementMatchers.takesArgument(
                                        0, ElementMatchers.named("jakarta.servlet.http.HttpServletRequest")))
                        .and(
                                ElementMatchers.takesArgument(
                                        1, ElementMatchers.named("jakarta.servlet.http.HttpServletResponse")))
                        .and(ElementMatchers.isPublic()),
                this.getClass().getName() + "$Servlet5Advice");
    }

    @SuppressWarnings("unused")
    public static class Servlet5Advice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(
                @Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                @Advice.Argument(value = 1, readOnly = false) HttpServletRequest response) {

//            if (!(response instanceof jakarta.servlet.http.HttpServletResponse)) {
//                return;
//            }
            ServletAdviceHelper.onServiceEnter(request, response);

        }


        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void onExit(@Advice.Argument(value = 0, readOnly = false) HttpServletRequest request,
                                  @Advice.Argument(value = 1, readOnly = false) HttpServletResponse response) {
            ServletAdviceHelper.onServiceExit(request, response);
        }
    }

}