import logging
from typing import List
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from .config import MODEL_NAME, PORT, HOST
from .agent import run_chat_sync, run_chat_stream

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("ai_service")

app = FastAPI(
    title="智慧医养守护平台 - AI 健康助理微服务",
    description="基于 LangChain + 硅基流动 DeepSeek-V3.2 + 联网搜索工具 + LangSmith 监控链路",
    version="1.0.0"
)

# 跨域配置（允许前端 Vite 开发服务器 5173 与后端端口直连）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class ChatMessage(BaseModel):
    role: str = Field(..., description="角色: user 或 assistant")
    content: str = Field(..., description="对话消息文本")

class ChatRequest(BaseModel):
    messages: List[ChatMessage] = Field(..., description="多轮对话历史列表")

@app.get("/api/ai/health")
@app.get("/health")
async def health_check():
    """服务健康检查端点"""
    return {
        "status": "UP",
        "service": "smart-care-ai",
        "model": MODEL_NAME,
        "langsmith_tracing": True
    }

@app.post("/api/ai/chat")
async def chat_stream(request: ChatRequest):
    """
    SSE 流式输出问答端点。
    返回 text/event-stream 协议数据，支持实时打字机吐字与 Tool 状态通知。
    """
    if not request.messages:
        raise HTTPException(status_code=400, detail="消息内容不能为空")

    history = [{"role": m.role, "content": m.content} for m in request.messages]
    return StreamingResponse(
        run_chat_stream(history),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )

@app.post("/api/ai/chat/sync")
async def chat_sync(request: ChatRequest):
    """同步问答端点（一次性返回完整回答与工具调用状态）"""
    if not request.messages:
        raise HTTPException(status_code=400, detail="消息内容不能为空")

    history = [{"role": m.role, "content": m.content} for m in request.messages]
    result = await run_chat_sync(history)
    return {
        "code": "OK",
        "data": result
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("ai_service.main:app", host=HOST, port=PORT, reload=False)
