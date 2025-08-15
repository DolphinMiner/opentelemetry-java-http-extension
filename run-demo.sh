#!/bin/bash

# OpenTelemetry HTTP Capture Extension 演示脚本

echo "🚀 准备运行 OpenTelemetry HTTP Capture Extension 演示..."

OTEL_AGENT_JAR="opentelemetry-javaagent.jar"
EXTENSION_JAR="target/http-capture-extension-1.0.0.jar"
DEMO_APP_JAR="demo-app/target/demo-web-app-1.0.0.jar"

# 检查 OpenTelemetry Agent JAR 是否存在
if [ ! -f "$OTEL_AGENT_JAR" ]; then
    echo "⚠️  未找到 OpenTelemetry Java Agent，正在下载..."
    wget -O "$OTEL_AGENT_JAR" "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.18.0/opentelemetry-javaagent.jar"
    if [ $? -ne 0 ]; then
        echo "❌ 下载 OpenTelemetry Java Agent 失败！"
        exit 1
    fi
fi

# 检查扩展 JAR 是否存在
if [ ! -f "$EXTENSION_JAR" ]; then
    echo "⚠️  未找到扩展 JAR 文件，正在构建..."
    ./build.sh
    if [ $? -ne 0 ]; then
        echo "❌ 构建失败！"
        exit 1
    fi
fi

# 检查演示应用是否存在，如果不存在则构建
if [ ! -f "$DEMO_APP_JAR" ]; then
    echo "⚠️  未找到演示应用，正在构建..."
    cd demo-app
    mvn clean package -DskipTests
    cd ..
    
    if [ ! -f "$DEMO_APP_JAR" ]; then
        echo "❌ 演示应用构建失败！"
        exit 1
    fi
fi

echo "✅ 正在启动演示应用，HTTP 捕获扩展已启用..."
echo "📊 HTTP 请求和响应数据将添加到 OpenTelemetry span attributes"
echo "🌐 应用将在 http://localhost:8080 启动"
echo ""
echo "📋 测试端点："
echo "   GET  http://localhost:8080/api/hello"
echo "   POST http://localhost:8080/api/echo"
echo "   GET  http://localhost:8080/api/external"
echo "   POST http://localhost:8080/api/submit"
echo ""
echo "📈 查看 traces 请配置 OTLP exporter 或查看控制台日志"
echo "按 Ctrl+C 停止应用"
echo ""

# 启动应用程序
echo "选择运行模式："
echo "1. 独立模式（仅 HTTP Capture Agent）"
echo "2. 组合模式（HTTP Capture + OpenTelemetry Agent）"
read -p "请选择 (1 或 2): " mode

if [ "$mode" = "1" ]; then
    echo "🚀 使用独立模式启动..."
    java -javaagent:$EXTENSION_JAR \
         -Dserver.port=8080 \
         -jar $DEMO_APP_JAR
elif [ "$mode" = "2" ]; then
    echo "🚀 使用组合模式启动..."
    java -javaagent:$OTEL_AGENT_JAR \
         -javaagent:$EXTENSION_JAR \
         -Dotel.service.name=http-capture-demo \
         -Dotel.traces.exporter=logging \
         -Dotel.logs.exporter=none \
         -Dotel.metrics.exporter=none \
         -Dserver.port=8080 \
         -jar $DEMO_APP_JAR
else
    echo "❌ 无效选择，默认使用独立模式"
    java -javaagent:$EXTENSION_JAR \
         -Dserver.port=8080 \
         -jar $DEMO_APP_JAR
fi
