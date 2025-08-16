package com.example.otel.http.ins;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.opentelemetry.javaagent.tooling.HelperInjector;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * This is a demo instrumentation which hooks into servlet invocation and modifies the http
 * response.
 */
@AutoService(InstrumentationModule.class)
public final class ServletV3InstrumentationModule extends InstrumentationModule {
    public ServletV3InstrumentationModule() {
        super("servlet-v3", "servlet-3");
    }

    @Override
    public List<String> getAdditionalHelperClassNames() {
        return Arrays.asList(
                "com.example.otel.http.ins.ServletAdviceHelper",
                "com.example.otel.http.ins.ResponseWrapper"
        );
    }
    /*
    We want this instrumentation to be applied after the standard servlet instrumentation.
    The latter creates a server span around http request.
    This instrumentation needs access to that server span.
     */
    @Override
    public int order() {
        return 1;
    }

    @Override
    public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
        return AgentElementMatchers.hasClassesNamed("javax.servlet.http.HttpServlet");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return singletonList(new ServletInstrumentationV3());
    }
}
