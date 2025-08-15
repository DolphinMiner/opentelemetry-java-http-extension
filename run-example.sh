#!/bin/bash

# HTTP Capture Extension 示例运行脚本

AGENT_JAR="opentelemetry-javaagent.jar"
EXTENSION_JAR="target/http-capture-extension-1.0.0.jar"

echo "正在运行 HTTP Capture Extension 示例..."

# 检查 OpenTelemetry Java Agent 是否存在
if [ ! -f "$AGENT_JAR" ]; then
    echo "⚠️  未找到 OpenTelemetry Java Agent！"
    echo "请下载 opentelemetry-javaagent.jar 到当前目录："
    echo "wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.18.0/opentelemetry-javaagent.jar"
    exit 1
fi

# 检查扩展 JAR 是否存在
if [ ! -f "$EXTENSION_JAR" ]; then
    echo "⚠️  未找到扩展 JAR 文件！"
    echo "请先运行构建脚本: ./build.sh"
    exit 1
fi

# 检查应用程序 JAR 参数
if [ -z "$1" ]; then
    echo "用法: $0 <your-application.jar>"
    echo "示例: $0 my-spring-boot-app.jar"
    exit 1
fi

APP_JAR="$1"

if [ ! -f "$APP_JAR" ]; then
    echo "❌ 应用程序 JAR 文件未找到: $APP_JAR"
    exit 1
fi

echo "✅ 正在启动应用程序，HTTP 捕获功能已启用..."
echo "📄 HTTP 请求和响应将输出到控制台"
echo ""

# 启动应用程序
java -javaagent:$AGENT_JAR \
     -Dotel.javaagent.extensions=$EXTENSION_JAR \
     -Dotel.service.name=http-capture-demo \
     -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
     -jar $APP_JAR
