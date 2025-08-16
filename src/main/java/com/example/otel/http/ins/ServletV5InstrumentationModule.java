package com.example.otel.http.ins;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * This is a demo instrumentation which hooks into servlet invocation and modifies the http
 * response.
 */
@AutoService(InstrumentationModule.class)
public final class ServletV5InstrumentationModule extends InstrumentationModule {
    public ServletV5InstrumentationModule() {
        super("servlet-v5", "servlet-3");
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
        return AgentElementMatchers.hasClassesNamed("jakarta.servlet.http.HttpServlet");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return singletonList(new ServletInstrumentationV5());
    }
}
