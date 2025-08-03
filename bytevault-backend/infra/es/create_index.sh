#!/bin/bash

# ES索引创建脚本
# 使用方法: ./create_index.sh [ES_HOST] [ES_PORT]
# 默认: localhost:9200

ES_HOST=${1:-localhost}
ES_PORT=${2:-9200}
ES_URL="http://${ES_HOST}:${ES_PORT}"

echo "正在连接到 Elasticsearch: ${ES_URL}"

# 检查ES是否可用
if ! curl -s "${ES_URL}/_cluster/health" > /dev/null; then
    echo "错误: 无法连接到 Elasticsearch (${ES_URL})"
    echo "请确保 Elasticsearch 已启动并可访问"
    exit 1
fi

echo "Elasticsearch 连接成功"

# 检查索引是否已存在
if curl -s "${ES_URL}/user_files" | grep -q "user_files"; then
    echo "警告: 索引 'user_files' 已存在"
    read -p "是否要删除现有索引并重新创建? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "删除现有索引..."
        curl -X DELETE "${ES_URL}/user_files"
        echo
    else
        echo "取消操作"
        exit 0
    fi
fi

echo "创建索引 'user_files'..."

# 创建索引
curl -X PUT "${ES_URL}/user_files" \
  -H 'Content-Type: application/json' \
  -d @index_mapping.json

echo
echo "索引创建完成!"

# 验证索引创建
echo "验证索引状态..."
curl -X GET "${ES_URL}/user_files/_mapping?pretty"

echo
echo "索引创建脚本执行完成!"
echo "可以使用以下命令验证索引:"
echo "curl -X GET '${ES_URL}/_cat/indices?v'" 