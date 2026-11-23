#!/usr/bin/env bash
# 健康检查：GET /api/health（同时验证 Web 与 MySQL 连接）
# 启动中的 Spring Boot 需要数秒就绪，最多重试 5 次（每次间隔 3 秒）
for i in 1 2 3 4 5; do
  resp=$(curl -s -m 10 http://127.0.0.1:8080/api/health)
  if echo "$resp" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    echo "后端健康：$resp"
    exit 0
  fi
  [ "$i" -lt 5 ] && sleep 3
done
echo "后端未就绪：http://127.0.0.1:8080/api/health 无响应（$resp）"
exit 1
