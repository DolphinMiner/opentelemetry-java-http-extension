package com.example.otel.http;

import java.io.*;
import java.net.*;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * 简单的 HTTP 服务器，用于测试 HTTP 捕获功能
 */
public class SimpleWebServer {
    
    public static void main(String[] args) throws IOException {
        int port = 8080;
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // 创建处理器
        server.createContext("/api/test", new TestHandler());
        server.createContext("/api/echo", new EchoHandler());
        
        // 设置执行器
        server.setExecutor(Executors.newFixedThreadPool(4));
        
        // 启动服务器
        server.start();
        
        System.out.println("🚀 测试服务器已启动，端口: " + port);
        System.out.println("📍 测试端点:");
        System.out.println("   GET  http://localhost:" + port + "/api/test");
        System.out.println("   POST http://localhost:" + port + "/api/echo");
        System.out.println("按 Ctrl+C 停止服务器");
    }
    
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{ \"message\": \"Hello from test endpoint!\", \"timestamp\": " 
                            + System.currentTimeMillis() + " }";
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // 读取请求体
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                String requestBody = baos.toString("UTF-8");
                
                String response = "{ \"echo\": " + requestBody + ", \"timestamp\": " 
                                + System.currentTimeMillis() + " }";
                
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "{ \"error\": \"Method not allowed\", \"method\": \"" 
                                + exchange.getRequestMethod() + "\" }";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}
