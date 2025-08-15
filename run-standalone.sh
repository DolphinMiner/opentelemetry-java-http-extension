#!/bin/bash

# HTTP Capture Agent 独立运行脚本

AGENT_JAR="target/http-capture-extension-1.0.0.jar"

echo "🚀 正在启动 HTTP Capture Agent..."

# 检查 Agent JAR 是否存在
if [ ! -f "$AGENT_JAR" ]; then
    echo "⚠️  未找到 Agent JAR 文件！"
    echo "请先运行构建脚本: ./build.sh"
    exit 1
fi

# 检查应用程序 JAR 参数
if [ -z "$1" ]; then
    echo "用法: $0 <your-application.jar> [additional-jvm-args]"
    echo "示例: $0 my-spring-boot-app.jar"
    echo "示例: $0 my-app.jar -Xmx1g -Dserver.port=8080"
    exit 1
fi

APP_JAR="$1"
shift  # 移除第一个参数，剩余的作为 JVM 参数

if [ ! -f "$APP_JAR" ]; then
    echo "❌ 应用程序 JAR 文件未找到: $APP_JAR"
    exit 1
fi

echo "✅ 正在启动应用程序，HTTP 捕获功能已启用..."
echo "📄 HTTP 请求和响应将输出到控制台"
echo "🔧 Agent: $AGENT_JAR"
echo "📱 App: $APP_JAR"
echo ""

# 启动应用程序
java -javaagent:$AGENT_JAR \
     "$@" \
     -jar $APP_JAR
