@echo off
chcp 65001 >nul
cls

echo ========================================
echo   YOLO 皮肤病变检测服务启动脚本
echo ========================================
echo.

python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Python，请确保 Python 已安装并添加到环境变量
    pause
    exit /b 1
)

echo [信息] 正在启动检测服务...
echo [信息] 服务地址: http://localhost:2026
echo [信息] 权重目录: %~dp0models
echo [信息] 按 Ctrl+C 停止服务
echo.

cd /d "%~dp0"
python detect-api-skin.py

echo.
echo [信息] 服务已停止
pause
