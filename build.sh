#!/bin/bash

# HTTP Capture Java Agent 构建脚本

echo "🚀 正在构建 HTTP Capture Java Agent..."

# 清理之前的构建
mvn clean

# 编译和打包
mvn package -DskipTests -s maven-settings.xml

if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo "📦 Agent JAR 文件位于: target/http-capture-extension-1.0.0.jar"
    echo ""
    echo "🔧 使用方法："
    echo ""
    echo "方式一：独立使用（推荐）"
    echo "   java -javaagent:target/http-capture-extension-1.0.0.jar \\"
    echo "        -jar your-application.jar"
    echo ""
    echo "方式二：与 OpenTelemetry Java Agent 一起使用"
    echo "   java -javaagent:opentelemetry-javaagent.jar \\"
    echo "        -javaagent:target/http-capture-extension-1.0.0.jar \\"
    echo "        -Dotel.service.name=my-app \\"
    echo "        -jar your-application.jar"
    echo ""
    echo "📋 运行演示：./run-demo.sh"
else
    echo "❌ 构建失败！"
    exit 1
fi
