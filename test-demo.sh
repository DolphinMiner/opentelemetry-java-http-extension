#!/bin/bash

# HTTP Capture Agent 测试脚本

BASE_URL="http://localhost:8080"

echo "🧪 开始测试 HTTP Capture Agent..."
echo "请确保演示应用正在运行 (./run-demo.sh)"
echo ""

# 等待一秒
sleep 1

echo "1️⃣ 测试 GET 请求..."
curl -s "$BASE_URL/api/hello" | jq '.' 2>/dev/null || curl -s "$BASE_URL/api/hello"
echo -e "\n"

echo "2️⃣ 测试 POST 请求..."
curl -s -X POST "$BASE_URL/api/echo" \
     -H "Content-Type: application/json" \
     -d '{"message": "Hello World", "user": "测试用户"}' | jq '.' 2>/dev/null || curl -s -X POST "$BASE_URL/api/echo" \
     -H "Content-Type: application/json" \
     -d '{"message": "Hello World", "user": "测试用户"}'
echo -e "\n"

echo "3️⃣ 测试外部 HTTP 调用..."
curl -s "$BASE_URL/api/external" | jq '.' 2>/dev/null || curl -s "$BASE_URL/api/external"
echo -e "\n"

echo "4️⃣ 测试数据提交..."
curl -s -X POST "$BASE_URL/api/submit" \
     -H "Content-Type: application/json" \
     -d '{"name": "张三", "email": "zhangsan@example.com", "message": "这是一个测试消息"}' | jq '.' 2>/dev/null || curl -s -X POST "$BASE_URL/api/submit" \
     -H "Content-Type: application/json" \
     -d '{"name": "张三", "email": "zhangsan@example.com", "message": "这是一个测试消息"}'
echo -e "\n"

echo "✅ 测试完成！请检查运行演示应用的终端，查看 HTTP 捕获输出。"
