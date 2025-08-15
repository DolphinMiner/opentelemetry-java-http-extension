#!/bin/bash

echo "🚀 Debug HTTP Response Body Capture Test..."

# Kill any existing processes
pkill -f demo-app
sleep 2

# Set up environment with verbose logging
export OTEL_SERVICE_NAME="demo-app"
export OTEL_TRACES_EXPORTER="logging"
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_JAVAAGENT_EXTENSIONS="target/http-capture-extension-1.0.0.jar"
export OTEL_JAVAAGENT_DEBUG="true"

echo "📦 Starting demo application with DEBUG logging..."

# Start the demo app with our extension and more debug info
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name="$OTEL_SERVICE_NAME" \
     -Dotel.traces.exporter="$OTEL_TRACES_EXPORTER" \
     -Dotel.logs.exporter="$OTEL_LOGS_EXPORTER" \
     -Dotel.metrics.exporter="$OTEL_METRICS_EXPORTER" \
     -Dotel.javaagent.debug=true \
     -Dotel.javaagent.extensions="$OTEL_JAVAAGENT_EXTENSIONS" \
     -Djava.util.logging.config.file=logging.properties \
     -jar demo-app/target/demo-web-app-1.0.0.jar \
     --server.port=9090 2>&1 | grep -E "(Loading instrumentation|ServletInstrumentation|servlet-body|ERROR|WARN)" &

APP_PID=$!
echo "Demo app started with PID: $APP_PID"

# Wait for app to start
echo "⏳ Waiting for application to start..."
sleep 8

echo ""
echo "🧪 Testing POST endpoint..."
curl -s -X POST \
     -H "Content-Type: application/json" \
     -d '{"test":"data"}' \
     http://localhost:9090/api/echo > /dev/null

echo ""
echo "⏳ Waiting for logs..."
sleep 3

echo ""
echo "🛑 Stopping demo application..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo ""
echo "✨ Debug test completed!"
