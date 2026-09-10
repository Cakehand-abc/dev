@echo off
chcp 65001 >nul
echo 正在启动智慧医养 AI 微服务 (FastAPI + LangChain + DeepSeek-V3.2)...
python -m uvicorn ai_service.main:app --host 0.0.0.0 --port 8000 --reload
pause
