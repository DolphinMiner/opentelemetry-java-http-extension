package com.example.otel.http;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

/**
 * HTTP 捕获 Java Agent
 * 使用 OpenTelemetry API 将 HTTP 数据添加到 span attributes
 */
public class HttpCaptureAgent {

    /**
     * Java Agent 入口方法
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("🚀 HTTP Capture Agent starting...");
        
        new AgentBuilder.Default()
                // 配置 Servlet 拦截
                .type(ElementMatchers.hasSuperType(
                        ElementMatchers.named("javax.servlet.http.HttpServlet")
                                .or(ElementMatchers.named("jakarta.servlet.http.HttpServlet"))))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("service"))
                                .intercept(Advice.to(HttpServletAdvice.class)))
                
                // 配置 HTTP Client 拦截
                .type(ElementMatchers.hasSuperType(
                        ElementMatchers.named("java.net.HttpURLConnection")))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("connect"))
                                .intercept(Advice.to(HttpClientAdvice.class)))
                
                // 安装到 JVM
                .installOn(inst);
        
        System.out.println("✅ HTTP Capture Agent installed successfully");
    }
}
