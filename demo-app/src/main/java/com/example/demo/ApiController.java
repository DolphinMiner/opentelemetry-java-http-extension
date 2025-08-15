package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 演示 API 控制器
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from HTTP Capture Demo!");
        response.put("timestamp", System.currentTimeMillis());
        response.put("method", "GET");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        response.put("echo", requestData);
        response.put("timestamp", System.currentTimeMillis());
        response.put("method", "POST");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/external")
    public ResponseEntity<Map<String, Object>> callExternal() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Making external HTTP call...");
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            // 模拟外部 HTTP 调用
            URL url = new URL("https://httpbin.org/get");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "HTTP-Capture-Demo/1.0");
            
            int responseCode = connection.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();
            
            response.put("external_call", Map.of(
                "url", url.toString(),
                "status", responseCode,
                "response_preview", content.substring(0, Math.min(100, content.length())) + "..."
            ));
        } catch (Exception e) {
            response.put("external_call_error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Data submitted successfully");
        response.put("received_data", data);
        response.put("processed_at", System.currentTimeMillis());
        
        // 模拟一些处理
        if (data.containsKey("name")) {
            response.put("greeting", "Hello, " + data.get("name") + "!");
        }
        
        return ResponseEntity.ok(response);
    }
}
