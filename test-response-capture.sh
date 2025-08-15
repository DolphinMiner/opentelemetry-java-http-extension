#!/bin/bash

# Test script to verify HTTP response body capture
echo "🚀 Starting HTTP Response Body Capture Test..."

# Set up environment
export OTEL_SERVICE_NAME="demo-app"
export OTEL_TRACES_EXPORTER="logging"
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_LOG_LEVEL="DEBUG"

# Enable our extensions
export OTEL_JAVAAGENT_EXTENSIONS="target/http-capture-extension-1.0.0.jar"

echo "📦 Starting demo application with OpenTelemetry..."
echo "Using extension: $OTEL_JAVAAGENT_EXTENSIONS"

# Start the demo app with our extension
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name="$OTEL_SERVICE_NAME" \
     -Dotel.traces.exporter="$OTEL_TRACES_EXPORTER" \
     -Dotel.logs.exporter="$OTEL_LOGS_EXPORTER" \
     -Dotel.metrics.exporter="$OTEL_METRICS_EXPORTER" \
     -Dotel.javaagent.debug=true \
     -Dotel.javaagent.extensions="$OTEL_JAVAAGENT_EXTENSIONS" \
     -jar demo-app/target/demo-web-app-1.0.0.jar \
     --server.port=8080 &

APP_PID=$!
echo "Demo app started with PID: $APP_PID"

# Wait for app to start
echo "⏳ Waiting for application to start..."
sleep 10

# Test endpoints and capture responses
echo "🧪 Testing endpoints..."

echo ""
echo "=== Test 1: GET /api/hello ==="
curl -s http://localhost:8080/api/hello | jq .

echo ""
echo "=== Test 2: POST /api/echo ==="
curl -s -H "Content-Type: application/json" \
     -d '{"name":"test","message":"hello world"}' \
     http://localhost:8080/api/echo | jq .

echo ""
echo "=== Test 3: POST /api/submit ==="
curl -s -H "Content-Type: application/json" \
     -d '{"name":"John","age":30,"city":"Beijing"}' \
     http://localhost:8080/api/submit | jq .

echo ""
echo "✅ Tests completed! Check the console output above for span traces with http.response.body attributes."
echo "❗ Look for log entries containing 'http.response.body' and 'ServletV3' or 'ServletV5' messages."

# Clean up
echo ""
echo "🛑 Stopping demo application..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo "✨ Test completed!"
