package com.example.otel.http.ins.adapter;


import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class ServletAdapterImplUtil {

    public static byte[] getUrlDecodeBytes(String contentType, byte[] originalBytes) {
        // x-www-form-urlencoded 需要解码原始http body中的内容 (eg: name=white+wan&id=123  name=white Wan&id=123)
        if (!StringUtils.isEmpty(contentType) && contentType.contains("x-www-form-urlencoded")) {
            String raw = new String(originalBytes, StandardCharsets.UTF_8);
            try {
                String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
                return decoded.getBytes(StandardCharsets.UTF_8);
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }
        return originalBytes;
    }
}
