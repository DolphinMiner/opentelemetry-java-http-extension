package com.example.otel.http.ins;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 缓存Request Body的包装类 (Jakarta EE)
 */
public class CachedBodyJakartaServletRequest extends HttpServletRequestWrapper {
    private byte[] cachedBody;

    public CachedBodyJakartaServletRequest(HttpServletRequest request) throws IOException {
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
        return new CachedJakartaServletInputStream(cachedBody);
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
