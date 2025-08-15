#!/bin/bash

echo "🔍 专门测试HTTP Request Body捕获..."

# Kill any existing processes
pkill -f demo-app
sleep 2

# Set up environment
export OTEL_SERVICE_NAME="demo-app"
export OTEL_TRACES_EXPORTER="logging"
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_JAVAAGENT_EXTENSIONS="target/http-capture-extension-1.0.0.jar"

# 使用更详细的logging来调试
export OTEL_JAVAAGENT_LOGGING=application

echo "🚀 启动应用 (调试模式)..."

# Start app with debug logging
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name="$OTEL_SERVICE_NAME" \
     -Dotel.traces.exporter="$OTEL_TRACES_EXPORTER" \
     -Dotel.javaagent.extensions="$OTEL_JAVAAGENT_EXTENSIONS" \
     -Dotel.javaagent.logging=application \
     -jar demo-app/target/demo-web-app-1.0.0.jar \
     --server.port=8888 > test_output.log 2>&1 &

APP_PID=$!
echo "应用已启动，PID: $APP_PID，端口: 8888"

# Wait for startup
echo "⏳ 等待应用启动..."
sleep 10

echo ""
echo "🧪 发送测试请求到 /api/echo..."

# Make a test request with specific data we can search for
RESPONSE=$(curl -s -w "HTTP_STATUS:%{http_code}" \
     -X POST \
     -H "Content-Type: application/json" \
     -d '{"test_request":"这是request body测试","user_id":12345,"action":"verify_capture"}' \
     http://localhost:8888/api/echo)

echo "✅ 请求完成"
echo "响应: $RESPONSE"

# Wait for logs to be processed
echo ""
echo "⏳ 等待日志处理..."
sleep 3

# Stop the app
echo "🛑 停止应用..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo ""
echo "📊 分析结果..."

# Check for our extension being loaded
echo ""
echo "=== 1. 检查WorkingBodyCapture扩展加载 ==="
if grep -q "working-body-capture" test_output.log; then
    echo "✅ WorkingBodyCapture扩展已加载"
    grep "working-body-capture" test_output.log
else
    echo "❌ WorkingBodyCapture扩展未找到"
fi

# Check for our ApiController instrumentation
echo ""
echo "=== 2. 检查ApiController增强 ==="
if grep -q "ApiController" test_output.log; then
    echo "✅ ApiController被增强"
    grep "ApiController" test_output.log
else
    echo "❌ ApiController增强未找到"
fi

# Check for our logging messages about body capture
echo ""
echo "=== 3. 检查body捕获日志 ==="
if grep -q "WorkingBodyCapture" test_output.log; then
    echo "✅ 找到body捕获日志！"
    grep "WorkingBodyCapture" test_output.log
else
    echo "❌ 未找到body捕获日志"
fi

# Check for http.request.body in span attributes
echo ""
echo "=== 4. 检查span中的http.request.body ==="
if grep -q "http.request.body" test_output.log; then
    echo "🎉 SUCCESS: 找到http.request.body属性！"
    grep -A 2 -B 2 "http.request.body" test_output.log
else
    echo "❌ 未找到http.request.body属性"
fi

# Check for our test data in spans
echo ""
echo "=== 5. 检查span中的测试数据 ==="
if grep -q "这是request body测试" test_output.log; then
    echo "🎉 SUCCESS: Span包含我们的测试数据！"
    grep -A 5 -B 5 "这是request body测试" test_output.log
else
    echo "❌ Span中未找到测试数据"
fi

# Check for both request and response body attributes
echo ""
echo "=== 6. 检查complete body attributes ==="
REQUEST_BODY_FOUND=$(grep -c "http.request.body" test_output.log)
RESPONSE_BODY_FOUND=$(grep -c "http.response.body" test_output.log)

echo "找到的http.request.body: $REQUEST_BODY_FOUND 次"
echo "找到的http.response.body: $RESPONSE_BODY_FOUND 次"

if [ "$REQUEST_BODY_FOUND" -gt 0 ] && [ "$RESPONSE_BODY_FOUND" -gt 0 ]; then
    echo "🎉 COMPLETE SUCCESS: Request和Response body都被成功捕获！"
elif [ "$REQUEST_BODY_FOUND" -gt 0 ]; then
    echo "🎉 PARTIAL SUCCESS: Request body被成功捕获！"
elif [ "$RESPONSE_BODY_FOUND" -gt 0 ]; then
    echo "🎉 PARTIAL SUCCESS: Response body被成功捕获！"
else
    echo "❌ Request和Response body都未被捕获"
fi

# Show recent spans with our attributes
echo ""
echo "=== 7. 显示包含我们属性的span ==="
grep -A 10 -B 5 "AttributesMap.*http.*body" test_output.log | tail -20

echo ""
echo "📁 完整日志保存在: test_output.log"

# Final summary
echo ""
echo "=== 📋 最终总结 ==="
if grep -q "http.request.body" test_output.log; then
    echo "✅ HTTP Request Body捕获: 成功"
else
    echo "❌ HTTP Request Body捕获: 失败"
fi

if grep -q "http.response.body" test_output.log; then
    echo "✅ HTTP Response Body捕获: 成功"
else
    echo "❌ HTTP Response Body捕获: 失败"
fi
