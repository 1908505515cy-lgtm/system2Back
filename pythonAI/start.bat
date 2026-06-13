@echo off
chcp 65001 >nul
echo [启动脚本] 清理 8081 端口上的旧进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
    echo   杀掉 PID: %%a
    taskkill /F /PID %%a >nul 2>&1
)
timeout /t 1 /nobreak >nul
echo [启动脚本] 启动 Python AI 服务 (端口 8081)...
.venv\Scripts\python.exe main.py
