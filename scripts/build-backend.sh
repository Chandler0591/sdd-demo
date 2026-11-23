#!/usr/bin/env bash
# 打包后端：先停运行中的实例（jar 被进程锁定无法覆盖），再 mvn package 跳过测试
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_DIR/backend"

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

# 先停旧实例，避免 jar 文件锁定导致 repackage 失败
bash "$SCRIPT_DIR/stop-backend.sh" || true

if uname -r 2>/dev/null | grep -qi microsoft; then
  # WSL：DrvFs 下无 .exe 后缀的 java 不存在，mvn 脚本校验 JAVA_HOME 会失败，
  # 且经 cmd.exe 调 mvn.cmd 会按错误字符集读源码，改走 powershell 调 mvn.cmd（本会话验证可靠）
  PS_EXE="$(command -v powershell.exe 2>/dev/null || echo /mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe)"
  JAVA_HOME_WIN="$(wslpath -w "$JAVA_HOME")"
  BACKEND_WIN="$(wslpath -w "$BACKEND_DIR")"
  "$PS_EXE" -NoProfile -Command "cd '$BACKEND_WIN'; \$env:JAVA_HOME='$JAVA_HOME_WIN'; mvn -q package -DskipTests"
else
  cd "$BACKEND_DIR"
  mvn -q package -DskipTests
fi
echo "打包完成：$BACKEND_DIR/target/library-backend-0.1.0.jar"
