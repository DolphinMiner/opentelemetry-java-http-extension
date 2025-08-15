package com.example.otel.http.ins;

import jakarta.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 自定义ServletOutputStream实现 (Jakarta EE)
 */
public class CachedJakartaServletOutputStream extends ServletOutputStream {
    private ServletOutputStream originalOutputStream;
    private ByteArrayOutputStream cachedBody;

    public CachedJakartaServletOutputStream(ServletOutputStream originalOutputStream, ByteArrayOutputStream cachedBody) {
        this.originalOutputStream = originalOutputStream;
        this.cachedBody = cachedBody;
    }

    @Override
    public boolean isReady() {
        return originalOutputStream.isReady();
    }

    @Override
    public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
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
