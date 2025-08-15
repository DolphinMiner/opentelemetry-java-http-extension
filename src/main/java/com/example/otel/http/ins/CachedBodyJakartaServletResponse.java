package com.example.otel.http.ins;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 缓存Response Body的包装类 (Jakarta EE)
 */
public class CachedBodyJakartaServletResponse extends HttpServletResponseWrapper {
    private ByteArrayOutputStream cachedBody;
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public CachedBodyJakartaServletResponse(HttpServletResponse response) {
        super(response);
        cachedBody = new ByteArrayOutputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CachedJakartaServletOutputStream(super.getOutputStream(), cachedBody);
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
