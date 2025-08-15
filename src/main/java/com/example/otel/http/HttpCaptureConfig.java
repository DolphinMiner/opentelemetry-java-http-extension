package com.example.otel.http;

import java.io.InputStream;
import java.util.Properties;

/**
 * HTTP 捕获配置管理类
 */
public class HttpCaptureConfig {
    
    private static final Properties properties = new Properties();
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        try {
            InputStream inputStream = HttpCaptureConfig.class
                    .getResourceAsStream("/http-capture.properties");
            if (inputStream != null) {
                properties.load(inputStream);
                inputStream.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading HTTP capture configuration: " + e.getMessage());
        }
    }
    
    public static boolean isRequestBodyCaptureEnabled() {
        return Boolean.parseBoolean(properties.getProperty("http.capture.request.body.enabled", "true"));
    }
    
    public static boolean isResponseBodyCaptureEnabled() {
        return Boolean.parseBoolean(properties.getProperty("http.capture.response.body.enabled", "true"));
    }
    
    public static int getRequestBodyMaxSize() {
        return Integer.parseInt(properties.getProperty("http.capture.request.body.max.size", "10240"));
    }
    
    public static int getResponseBodyMaxSize() {
        return Integer.parseInt(properties.getProperty("http.capture.response.body.max.size", "10240"));
    }
    
    public static boolean isOutboundCaptureEnabled() {
        return Boolean.parseBoolean(properties.getProperty("http.capture.outbound.enabled", "true"));
    }
    
    public static String getLogLevel() {
        return properties.getProperty("http.capture.log.level", "INFO");
    }
}
