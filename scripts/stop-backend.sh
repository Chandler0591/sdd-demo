#!/usr/bin/env bash
# 停止后端：用 jps 按 jar 名匹配 library-backend 的 java 进程（避免误杀 IDE 编译进程）
JAVA_HOME="${JAVA_HOME:-}"
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME/bin" ]; then
  for candidate in "/d/myapp/Java/jdk-17" "/mnt/d/myapp/Java/jdk-17"; do
    if [ -d "$candidate/bin" ]; then JAVA_HOME="$candidate"; break; fi
  done
fi
JPS="$JAVA_HOME/bin/jps.exe"
[ -f "$JPS" ] || JPS="$JAVA_HOME/bin/jps"

pids=$("$JPS" -l 2>/dev/null | grep -i 'library-backend' | awk '{print $1}')
if [ -z "$pids" ]; then
  echo "没有运行中的后端进程"
  exit 0
fi
for pid in $pids; do
  echo "停止后端进程 PID=$pid"
  # Git Bash 需 // 防 MSYS 路径转换；WSL 下 //F 失败则退回 /F；最后尝试 kill
  taskkill //F //PID "$pid" >/dev/null 2>&1 || taskkill.exe /F /PID "$pid" >/dev/null 2>&1 || kill -9 "$pid" 2>/dev/null || true
done
