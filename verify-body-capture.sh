#!/bin/bash

echo "📋 验证HTTP Response Body录制功能..."

# Kill any existing processes
pkill -f demo-app
sleep 2

# Set up environment
export OTEL_SERVICE_NAME="demo-app"
export OTEL_TRACES_EXPORTER="logging"
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_JAVAAGENT_EXTENSIONS="target/http-capture-extension-1.0.0.jar"

echo "🚀 启动应用..."

# Start app and capture all output
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name="$OTEL_SERVICE_NAME" \
     -Dotel.traces.exporter="$OTEL_TRACES_EXPORTER" \
     -Dotel.javaagent.extensions="$OTEL_JAVAAGENT_EXTENSIONS" \
     -jar demo-app/target/demo-web-app-1.0.0.jar \
     --server.port=9090 > app_output.log 2>&1 &

APP_PID=$!
echo "应用已启动，PID: $APP_PID"

# Wait for startup
echo "⏳ 等待应用启动..."
sleep 8

echo ""
echo "🧪 发送测试请求..."

# Make a test request
RESPONSE=$(curl -s -w "HTTP_STATUS:%{http_code}" \
     -X POST \
     -H "Content-Type: application/json" \
     -d '{"message":"测试response body录制","user":"张三"}' \
     http://localhost:9090/api/echo)

echo "✅ 请求完成"
echo "响应: $RESPONSE"

# Wait for logs to be processed
echo ""
echo "⏳ 等待日志处理..."
sleep 5

# Stop the app
echo "🛑 停止应用..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo ""
echo "📊 分析结果..."

# Check for our extension loading
echo ""
echo "=== 1. 检查扩展是否被加载 ==="
if grep -q "custom-servlet-body-capture" app_output.log; then
    echo "✅ 自定义扩展已加载"
    grep "custom-servlet-body-capture" app_output.log
else
    echo "❌ 自定义扩展未找到"
fi

# Check for our instrumentation being applied
echo ""
echo "=== 2. 检查增强是否被应用 ==="
if grep -q "DispatcherServlet" app_output.log; then
    echo "✅ DispatcherServlet被找到"
    grep "DispatcherServlet" app_output.log | head -3
else
    echo "❌ DispatcherServlet增强未找到"
fi

# Check for span attributes with response body
echo ""
echo "=== 3. 检查span中的response body ==="
if grep -q "http.response.body" app_output.log; then
    echo "✅ 找到http.response.body属性！"
    grep "http.response.body" app_output.log
else
    echo "❌ 未找到http.response.body属性"
fi

# Check for our logging messages
echo ""
echo "=== 4. 检查我们的日志消息 ==="
if grep -q "Recorded request and response" app_output.log; then
    echo "✅ 找到我们的录制日志！"
    grep "Recorded request and response" app_output.log
else
    echo "❌ 未找到我们的录制日志"
fi

# Show any span logs that contain our test data
echo ""
echo "=== 5. 检查包含测试数据的span ==="
if grep -q "张三" app_output.log; then
    echo "✅ Span包含我们的测试数据！"
    grep -A 5 -B 5 "张三" app_output.log
else
    echo "❌ Span中未找到测试数据"
fi

# Final summary
echo ""
echo "=== 📋 最终结果 ==="
if grep -q "http.response.body" app_output.log && grep -q "张三" app_output.log; then
    echo "🎉 SUCCESS: HTTP Response Body录制成功！"
    echo "✅ 我们成功实现了将servlet response body放入OpenTelemetry span attributes中"
else
    echo "⚠️  需要进一步调试..."
    echo "💡 让我们查看完整的span输出:"
    grep -A 10 -B 5 "LoggingSpanExporter" app_output.log | tail -20
fi

echo ""
echo "📁 完整日志保存在: app_output.log"
