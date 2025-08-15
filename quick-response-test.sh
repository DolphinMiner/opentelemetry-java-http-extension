#!/bin/bash

echo "🚀 Quick HTTP Response Body Capture Test..."

# Kill any existing processes
pkill -f demo-app
sleep 2

# Set up environment  
export OTEL_SERVICE_NAME="demo-app"
export OTEL_TRACES_EXPORTER="logging"
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_JAVAAGENT_EXTENSIONS="target/http-capture-extension-1.0.0.jar"

echo "📦 Starting demo application on port 9090..."

# Start the demo app with our extension on different port
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name="$OTEL_SERVICE_NAME" \
     -Dotel.traces.exporter="$OTEL_TRACES_EXPORTER" \
     -Dotel.logs.exporter="$OTEL_LOGS_EXPORTER" \
     -Dotel.metrics.exporter="$OTEL_METRICS_EXPORTER" \
     -Dotel.javaagent.extensions="$OTEL_JAVAAGENT_EXTENSIONS" \
     -jar demo-app/target/demo-web-app-1.0.0.jar \
     --server.port=9090 &

APP_PID=$!
echo "Demo app started with PID: $APP_PID"

# Wait for app to start
echo "⏳ Waiting for application to start..."
sleep 8

echo ""
echo "🧪 Testing POST endpoint with JSON response..."
curl -s -X POST \
     -H "Content-Type: application/json" \
     -d '{"test":"data","message":"hello world"}' \
     http://localhost:9090/api/echo

echo ""
echo ""
echo "⏳ Waiting a moment for span to be processed..."
sleep 3

echo ""
echo "🛑 Stopping demo application..."
kill $APP_PID
wait $APP_PID 2>/dev/null

echo ""
echo "✨ Test completed!"
echo ""
echo "📋 Please check the output above for:"
echo "   1. Span logs containing 'http.response.body' attributes"
echo "   2. 'ServletV3' or 'ServletV5' logging messages"
echo "   3. 'Recorded request and response data for span' messages"
