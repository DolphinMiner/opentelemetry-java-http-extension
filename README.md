# HTTP Capture Java Agent for OpenTelemetry

这是一个独立的 Java Agent，用于捕获 HTTP 请求和响应体数据并将其添加到 OpenTelemetry span 的 attributes 中。可以与 OpenTelemetry Java Agent 一起使用，实现更丰富的 HTTP 监控。

## 功能特性

- ✅ **无缝集成**: 扩展现有的 OpenTelemetry HTTP 监控，无需替换
- ✅ **Span Attributes**: 将 HTTP 数据添加到现有 span 的 attributes 中
- ✅ **入站请求捕获**: 捕获 Servlet 请求头、查询参数、请求体
- ✅ **入站响应捕获**: 捕获 Servlet 响应头、内容类型
- ✅ **出站请求捕获**: 捕获 HttpURLConnection 请求信息
- ✅ **出站响应捕获**: 捕获 HttpURLConnection 响应状态和头信息
- ✅ **可配置选项**: 支持开关和大小限制配置
- ✅ **错误处理**: 静默处理捕获错误，不影响业务逻辑

## 快速开始

### 1. 构建扩展

```bash
./build.sh
```

### 2. 运行应用程序

有两种使用方式：

#### 方式一：独立使用（推荐）
```bash
java -javaagent:target/http-capture-extension-1.0.0.jar \
     -jar your-application.jar
```

#### 方式二：与 OpenTelemetry Java Agent 一起使用
```bash
# 先下载 OpenTelemetry Java Agent
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.18.0/opentelemetry-javaagent.jar

# 同时使用两个 agent
java -javaagent:opentelemetry-javaagent.jar \
     -javaagent:target/http-capture-extension-1.0.0.jar \
     -Dotel.service.name=my-app \
     -Dotel.traces.exporter=logging \
     -jar your-application.jar
```

### 4. 运行演示应用

```bash
# 启动演示应用（自动下载 OTel Agent）
./run-demo.sh

# 在另一个终端运行测试
./test-demo.sh
```

## 项目结构

```
opentelemetry-java-http-extension/
├── src/
│   ├── main/
│   │   ├── java/com/example/otel/http/
│   │   │   ├── HttpCaptureExtension.java       # OpenTelemetry 扩展主类
│   │   │   ├── HttpServletInstrumentation.java # Servlet 增强器
│   │   │   ├── HttpClientInstrumentation.java  # HTTP 客户端增强器
│   │   │   └── HttpCaptureConfig.java          # 配置管理
│   │   └── resources/
│   │       ├── META-INF/services/              # SPI 服务注册
│   │       └── http-capture.properties         # 配置文件
│   └── test/
│       └── java/com/example/otel/http/
│           └── SimpleWebServer.java            # 测试服务器
├── demo-app/                                   # 演示应用
│   ├── pom.xml                                 # Spring Boot 演示应用
│   └── src/main/java/com/example/demo/
│       ├── DemoApplication.java                # 主应用类
│       └── ApiController.java                  # API 控制器
├── pom.xml                                     # Maven 配置
├── build.sh                                   # 构建脚本
├── run-standalone.sh                          # 独立运行脚本
├── run-demo.sh                                # 演示运行脚本
├── test-demo.sh                               # 演示测试脚本
└── README.md                                  # 文档
```

## 配置选项

编辑 `src/main/resources/http-capture.properties` 文件：

```properties
# 是否启用 HTTP 请求体捕获
http.capture.request.body.enabled=true

# 是否启用 HTTP 响应体捕获
http.capture.response.body.enabled=true

# 请求体最大捕获大小（字节）
http.capture.request.body.max.size=10240

# 响应体最大捕获大小（字节）
http.capture.response.body.max.size=10240

# 是否启用出站 HTTP 请求捕获
http.capture.outbound.enabled=true

# 日志级别
http.capture.log.level=INFO
```

## Span Attributes 示例

当启用扩展后，HTTP 数据将作为 attributes 添加到 OpenTelemetry span 中：

### 入站 HTTP 请求 Attributes
```json
{
  "http.request.headers": "Content-Type=application/json, User-Agent=curl/7.68.0, Accept=*/*",
  "http.request.query_string": "param1=value1&param2=value2",
  "http.request.body": "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}"
}
```

### 入站 HTTP 响应 Attributes
```json
{
  "http.response.headers": "Content-Type=application/json, Content-Length=67",
  "http.response.content_type": "application/json"
}
```

### 出站 HTTP 请求 Attributes
```json
{
  "http.client.method": "GET",
  "http.client.url": "https://api.example.com/data",
  "http.client.request.headers": "User-Agent=Java/11.0.16, Accept=application/json"
}
```

### 出站 HTTP 响应 Attributes
```json
{
  "http.client.response.status_code": 200,
  "http.client.response.status_message": "OK",
  "http.client.response.content_type": "application/json",
  "http.client.response.content_length": 1234,
  "http.client.response.headers": "Content-Type=application/json, Date=Wed, 01 Jan 2024 12:00:00 GMT"
}
```

### 错误处理 Attributes
```json
{
  "http.request.capture_error": "IOException: Stream closed",
  "http.response.capture_error": "IllegalStateException: Response already committed"
}
```

## 测试

启动测试服务器：

```bash
# 编译测试服务器
mvn test-compile exec:java -Dexec.mainClass="com.example.otel.http.SimpleWebServer" -Dexec.classpathScope="test"
```

在另一个终端测试：

```bash
# 测试 GET 请求
curl http://localhost:8080/api/test

# 测试 POST 请求
curl -X POST http://localhost:8080/api/echo \
     -H "Content-Type: application/json" \
     -d '{"message": "Hello World"}'
```

## 依赖要求

- Java 8+
- Maven 3.6+
- OpenTelemetry Java Agent 2.18.0+
- Spring Boot 2.7+ (仅演示应用需要)

## 开发说明

### 核心组件

1. **HttpCaptureExtension**: OpenTelemetry 扩展主入口类
2. **HttpServletInstrumentation**: 扩展 Servlet 监控的增强器
3. **HttpClientInstrumentation**: 扩展 HTTP 客户端监控的增强器
4. **HttpCaptureConfig**: 配置参数管理

### 工作原理

该扩展通过 OpenTelemetry Java Agent Extension API 工作：

1. **扩展注册**: 通过 SPI 机制注册到 OpenTelemetry Java Agent
2. **字节码增强**: 使用 Byte Buddy 增强 Servlet 和 HTTP 客户端类
3. **Span 集成**: 获取当前活动的 OpenTelemetry span
4. **Attributes 添加**: 将捕获的 HTTP 数据作为 attributes 添加到 span
5. **错误隔离**: 捕获过程中的错误不会影响业务逻辑

### 扩展注册

通过 Java SPI 机制注册扩展：
- `META-INF/services/io.opentelemetry.javaagent.extension.AgentExtension`

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
