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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@AutoService(InstrumentationModule.class)
public class ServletInstrumentation extends InstrumentationModule {
    private static final Logger logger = LoggerFactory.getLogger(ServletInstrumentation.class);

    public ServletInstrumentation() {
        super("servlet-body", "servlet-body-1.0");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Collections.singletonList(new HttpServletInstrumentation());
    }

    private static class HttpServletInstrumentation implements TypeInstrumentation {
        @Override
        public ElementMatcher<TypeDescription> typeMatcher() {
            return implementsInterface(named("javax.servlet.http.HttpServlet"))
                    .or(implementsInterface(named("javax.servlet.Servlet")));
        }

        @Override
        public void transform(TypeTransformer transformer) {
            transformer.applyAdviceToMethod(
                    named("service").and(takesArguments(2)),
                    HttpServletInstrumentation.class.getName() + "$ServiceAdvice"
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
                } catch (Exception e) {
                    logger.error("Failed to wrap request/response", e);
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

                        logger.debug("Recorded request and response bodies for span: {}", span.getSpanContext().getSpanId());
                    }
                } catch (Exception e) {
                    logger.error("Failed to record request/response bodies", e);
                }
            }
        }
    }

    // 缓存Request Body的包装类
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            cacheRequestBody();
        }

        private void cacheRequestBody() throws IOException {
            ServletInputStream inputStream = super.getInputStream();
            if (inputStream != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }

                cachedBody = baos.toByteArray();
            } else {
                cachedBody = new byte[0];
            }
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new CachedServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncoding()));
        }

        public String getCachedBody() {
            if (cachedBody == null || cachedBody.length == 0) {
                return null;
            }

            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = StandardCharsets.UTF_8.name();
            }

            try {
                return new String(cachedBody, encoding);
            } catch (UnsupportedEncodingException e) {
                return new String(cachedBody, StandardCharsets.UTF_8);
            }
        }
    }

    // 缓存Response Body的包装类
    private static class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {
        private ByteArrayOutputStream cachedBody;
        private ServletOutputStream outputStream;
        private PrintWriter writer;

        public CachedBodyHttpServletResponse(HttpServletResponse response) {
            super(response);
            cachedBody = new ByteArrayOutputStream();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (outputStream == null) {
                outputStream = new CachedServletOutputStream(super.getOutputStream(), cachedBody);
            }
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (writer == null) {
                String encoding = getCharacterEncoding();
                if (encoding == null) {
                    encoding = StandardCharsets.UTF_8.name();
                }
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), encoding));
            }
            return writer;
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            if (outputStream != null) {
                outputStream.flush();
            }
            super.flushBuffer();
        }

        public String getCachedBody() {
            if (cachedBody.size() == 0) {
                return null;
            }

            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = StandardCharsets.UTF_8.name();
            }

            try {
                return cachedBody.toString(encoding);
            } catch (UnsupportedEncodingException e) {
                return cachedBody.toString();
            }
        }
    }

    // 自定义ServletInputStream实现
    private static class CachedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] cachedBody) {
            this.inputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(javax.servlet.ReadListener readListener) {
            throw new UnsupportedOperationException("ReadListener not supported");
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }

    // 自定义ServletOutputStream实现
    private static class CachedServletOutputStream extends ServletOutputStream {
        private ServletOutputStream originalOutputStream;
        private ByteArrayOutputStream cachedBody;

        public CachedServletOutputStream(ServletOutputStream originalOutputStream, ByteArrayOutputStream cachedBody) {
            this.originalOutputStream = originalOutputStream;
            this.cachedBody = cachedBody;
        }

        @Override
        public boolean isReady() {
            return originalOutputStream.isReady();
        }

        @Override
        public void setWriteListener(javax.servlet.WriteListener writeListener) {
            originalOutputStream.setWriteListener(writeListener);
        }

        @Override
        public void write(int b) throws IOException {
            originalOutputStream.write(b);
            cachedBody.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            originalOutputStream.write(b);
            cachedBody.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            originalOutputStream.write(b, off, len);
            cachedBody.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            originalOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
            originalOutputStream.close();
        }
    }
}