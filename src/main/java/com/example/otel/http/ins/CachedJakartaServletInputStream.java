package com.example.otel.http.ins;

import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 自定义ServletInputStream实现 (Jakarta EE)
 */
public class CachedJakartaServletInputStream extends ServletInputStream {
    private ByteArrayInputStream inputStream;

    public CachedJakartaServletInputStream(byte[] cachedBody) {
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
    public void setReadListener(jakarta.servlet.ReadListener readListener) {
        throw new UnsupportedOperationException("ReadListener not supported");
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
}
