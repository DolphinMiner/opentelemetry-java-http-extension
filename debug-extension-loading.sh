#!/bin/bash

echo "🐛 Debug Extension Loading Test..."

# Kill any existing processes
pkill -f demo-app
sleep 2

# Set up environment with maximum debugging
export OTEL_SERVICE_NAME="demo-app"
export OTEL_TRACES_EXPORTER="logging"
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_JAVAAGENT_EXTENSIONS="target/http-capture-extension-1.0.0.jar"
export OTEL_JAVAAGENT_DEBUG="true"

echo "📦 Starting demo application with maximum debugging..."

# Start with comprehensive logging
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name="$OTEL_SERVICE_NAME" \
     -Dotel.traces.exporter="$OTEL_TRACES_EXPORTER" \
     -Dotel.logs.exporter="$OTEL_LOGS_EXPORTER" \
     -Dotel.metrics.exporter="$OTEL_METRICS_EXPORTER" \
     -Dotel.javaagent.debug=true \
     -Dotel.javaagent.extensions="$OTEL_JAVAAGENT_EXTENSIONS" \
     -Djava.util.logging.level=ALL \
     -jar demo-app/target/demo-web-app-1.0.0.jar \
     --server.port=9090 2>&1 | grep -E "(ServletInstrumentation|servlet-body|ERROR|Loading.*class)" | head -20 &

APP_PID=$!
echo "Demo app started with PID: $APP_PID"

# Wait for app to start and show loading info
echo "⏳ Waiting for application to start and show loading info..."
sleep 10

echo ""
echo "🧪 Making one simple request..."
curl -s -X POST \
     -H "Content-Type: application/json" \
     -d '{"simple":"test"}' \
     http://localhost:9090/api/echo > /dev/null

echo ""
echo "⏳ Waiting for logs..."
sleep 5

echo ""
echo "🛑 Stopping demo application..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo ""
echo "✨ Debug test completed!"
