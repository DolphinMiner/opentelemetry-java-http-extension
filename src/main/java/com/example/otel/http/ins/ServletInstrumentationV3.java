package com.example.otel.http.ins;

import com.example.otel.http.ins.adapter.ServletAdapterImplV3;
import com.example.otel.http.ins.util.Pair;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static net.bytebuddy.matcher.ElementMatchers.named;
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
//        return AgentElementMatchers.hasSuperType(
//                namedOneOf("javax.servlet.http.HttpServlet"));
        return named("javax.servlet.http.HttpServlet");
    }

    @Override
    public void transform(TypeTransformer typeTransformer) {
        typeTransformer.applyAdviceToMethod(
                namedOneOf( "service")
                        .and(
                                ElementMatchers.takesArgument(
                                        0, named("javax.servlet.ServletRequest")))
                        .and(
                                ElementMatchers.takesArgument(
                                        1, named("javax.servlet.ServletResponse")))
                        .and(ElementMatchers.isPublic()),
                this.getClass().getName() + "$Servlet3Advice");
    }

    @SuppressWarnings("unused")
    public static class Servlet3Advice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) ServletRequest request,
                                   @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
            Pair<HttpServletRequest, HttpServletResponse> pair =
                    ServletAdviceHelper.onServiceEnter(ServletAdapterImplV3.getInstance(), request, response);

            if (pair == null) {
                return;
            }

            if (pair.getFirst() != null) {
                request = pair.getFirst();
            }

            if (pair.getSecond() != null) {
                response = pair.getSecond();
            }
        }

        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void onExit(@Advice.Argument(value = 0, readOnly = false) ServletRequest request,
                                  @Advice.Argument(value = 1, readOnly = false) ServletResponse response) {
             ServletAdviceHelper.onServiceExit(ServletAdapterImplV3.getInstance(), request, response);
        }
    }
}