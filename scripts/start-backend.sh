#!/usr/bin/env bash
# 启动后端，日志写入 backend/logs/backend.log
# 用法：
#   bash scripts/start-backend.sh            # Key 读环境变量 DASHSCOPE_API_KEY
#   bash scripts/start-backend.sh "sk-xxx"   # Key 仅注入本次进程，不落任何文件
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_DIR/backend"
JAR="$BACKEND_DIR/target/library-backend-0.1.0.jar"

if [ ! -f "$JAR" ]; then
  echo "找不到 $JAR，请先运行 bash scripts/build-backend.sh 打包"
  exit 1
fi

# API Key：参数优先，其次环境变量
if [ -n "$1" ]; then
  export DASHSCOPE_API_KEY="$1"
elif [ -z "$DASHSCOPE_API_KEY" ]; then
  echo "提示：未设置 DASHSCOPE_API_KEY，AI 识别功能将不可用（不影响其他功能）"
fi

# JDK 17：PATH 上的 java 是 JDK 8，必须显式指定（优先环境变量，其次自动探测 Git Bash / WSL 路径）
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME/bin" ]; then
  for candidate in "/d/myapp/Java/jdk-17" "/mnt/d/myapp/Java/jdk-17"; do
    if [ -d "$candidate/bin" ]; then JAVA_HOME="$candidate"; break; fi
  done
fi
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME/bin" ]; then
  echo "找不到 JDK 17，请设置 JAVA_HOME 环境变量（本机通常在 D:\\myapp\\Java\\jdk-17）"
  exit 1
fi
export JAVA_HOME

# 已有实例先停，避免端口冲突
bash "$SCRIPT_DIR/stop-backend.sh" || true

mkdir -p "$BACKEND_DIR/logs"
cd "$BACKEND_DIR"
if uname -r 2>/dev/null | grep -qi microsoft; then
  # WSL：经 powershell Start-Process 启动，脱离 WSL 进程树（会话关闭后进程不被终止），日志重定向到文件
  JAVA_WIN="$(wslpath -w "$JAVA_HOME/bin/java.exe")"
  BACKEND_WIN="$(wslpath -w "$BACKEND_DIR")"
  LOG_WIN="$(wslpath -w "$BACKEND_DIR/logs/backend.log")"
  ERR_WIN="$(wslpath -w "$BACKEND_DIR/logs/backend-error.log")"
  # powershell.exe 不在 WSL 的 PATH 上（在 WindowsPowerShell\v1.0 子目录），用完整路径
  PS_EXE="$(command -v powershell.exe 2>/dev/null || echo /mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe)"
  "$PS_EXE" -NoProfile -Command "Start-Process -FilePath '$JAVA_WIN' -ArgumentList '-jar','target\\library-backend-0.1.0.jar' -WorkingDirectory '$BACKEND_WIN' -RedirectStandardOutput '$LOG_WIN' -RedirectStandardError '$ERR_WIN' -WindowStyle Hidden"
else
  # Git Bash / 原生环境：nohup 后台启动
  nohup "$JAVA_HOME/bin/java.exe" -jar target/library-backend-0.1.0.jar > logs/backend.log 2>&1 &
fi
echo "后端已启动，日志：$BACKEND_DIR/logs/backend.log"
bash "$SCRIPT_DIR/check.sh"
