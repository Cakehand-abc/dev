import json
import logging
import re
import time
import uuid
from typing import AsyncGenerator, Dict, Any, List
from langchain_openai import ChatOpenAI
from langchain_core.messages import (
    SystemMessage,
    HumanMessage,
    AIMessage,
    ToolMessage,
    BaseMessage
)
from .config import (
    SILICONFLOW_API_KEY,
    SILICONFLOW_BASE_URL,
    MODEL_NAME
)
from .tools import web_search

logger = logging.getLogger("ai_service.agent")

SYSTEM_PROMPT = """你是由“智慧医养守护平台”赋能的专属 AI 智能健康顾问（小养助手）。
你的核心使命是为平台上的老年人、家属、社区网格员及基层医护人员提供专业、温暖、可靠的健康咨询与养老建议。

【你的核心职责与知识领域】：
1. 老年人常见慢性病管理：如高血压、冠心病、糖尿病、高血脂的日常监测指标、饮食宜忌与科学作息。
2. 平台智能硬件守护：辅助分析智能腕表上报的生理指标（心率异常、血压偏高、体温异常、夜间活动告警等），给出初筛建议。
3. 居家养老安全与急救：如居家防跌倒改造要点、中暑或低血糖突发自救指南、紧急就医流程指引。
4. 遇到需要查询【最新医疗健康资讯】、【最新养老优待政策】或【具体医学新药/新指南研究】时，请主动调用 `web_search` 工具联网检索权威资料，并在回答中恰当融合。

【回答规范】：
- 语气温暖亲切、通俗易懂、层次分明（善用加粗与列表序号）。
- 严谨声明：你提供的建议为健康宣教与养生指导，不能替代临床专科医生的确诊；若出现严重胸闷、呼吸困难、急性意识不清等危急情况，请即刻呼叫 120。
"""

def clean_output(text: str) -> str:
    """清理模型可能携带的特殊标记或内嵌标签"""
    if not text:
        return ""
    text = re.sub(r"<\|.*?\|>", "", text)
    return text.strip()

def get_llm(streaming: bool = True) -> ChatOpenAI:
    return ChatOpenAI(
        model=MODEL_NAME,
        api_key=SILICONFLOW_API_KEY,
        base_url=SILICONFLOW_BASE_URL,
        temperature=0.7,
        streaming=streaming,
        timeout=60
    )

def parse_input_messages(chat_history: List[Dict[str, str]]) -> List[BaseMessage]:
    messages: List[BaseMessage] = [SystemMessage(content=SYSTEM_PROMPT)]
    for msg in chat_history:
        role = msg.get("role", "user")
        content = msg.get("content", "")
        if role == "user":
            messages.append(HumanMessage(content=content))
        elif role == "assistant":
            messages.append(AIMessage(content=content))
    return messages

async def run_chat_stream(chat_history: List[Dict[str, str]]) -> AsyncGenerator[str, None]:
    """
    遵循 zhibai-insight-ai 标准 SSE 协议下发事件流：
    event: started -> data: {"trace_id": "..."}
    event: delta -> data: {"text": "..."} (首字毫秒级即时吐出)
    event: tool_start -> data: {"tool": "web_search", "query": "..."}
    event: tool_done -> data: {"tool": "web_search", "query": "..."}
    event: done -> data: {"elapsed_ms": ..., "trace_id": "..."}
    """
    start_time = time.time()
    trace_id = f"trace_{uuid.uuid4().hex[:12]}"
    messages = parse_input_messages(chat_history)
    llm = get_llm(streaming=True)
    llm_with_tools = llm.bind_tools([web_search])

    yield f"event: started\ndata: {json.dumps({'trace_id': trace_id}, ensure_ascii=False)}\n\n"

    accumulated_tool_calls = {}
    try:
        # 第一轮直接以流式启动，首字即时喷射（首字延迟控制在 800ms~1.2s 内）
        async for chunk in llm_with_tools.astream(messages):
            # 累积模型工具调用片段
            if chunk.tool_call_chunks:
                for tc_chunk in chunk.tool_call_chunks:
                    idx = tc_chunk.get("index", 0)
                    if idx not in accumulated_tool_calls:
                        accumulated_tool_calls[idx] = {
                            "id": tc_chunk.get("id") or "",
                            "name": tc_chunk.get("name") or "",
                            "args": tc_chunk.get("args") or ""
                        }
                    else:
                        if tc_chunk.get("id"):
                            accumulated_tool_calls[idx]["id"] += tc_chunk["id"]
                        if tc_chunk.get("name"):
                            accumulated_tool_calls[idx]["name"] += tc_chunk["name"]
                        if tc_chunk.get("args"):
                            accumulated_tool_calls[idx]["args"] += tc_chunk["args"]

            # 如果没有工具调用，直接作为增量文本下发（真正实现首字无阻塞极速呈现）
            if chunk.content and not accumulated_tool_calls:
                yield f"event: delta\ndata: {json.dumps({'text': chunk.content}, ensure_ascii=False)}\n\n"

        # 如果检测到触发了联网搜索 Tool
        if accumulated_tool_calls:
            tool_calls_list = []
            for tc in accumulated_tool_calls.values():
                args_dict = {}
                try:
                    args_dict = json.loads(tc["args"])
                except Exception:
                    args_dict = {"query": tc["args"]}
                tool_calls_list.append({
                    "id": tc["id"] or uuid.uuid4().hex,
                    "name": tc["name"] or "web_search",
                    "args": args_dict
                })

            messages.append(AIMessage(content="", tool_calls=tool_calls_list))

            # 执行工具调用并下发状态事件
            for tc in tool_calls_list:
                tool_name = tc["name"]
                tool_args = tc["args"]
                query = tool_args.get("query", "")
                yield f"event: tool_start\ndata: {json.dumps({'tool': tool_name, 'query': query}, ensure_ascii=False)}\n\n"

                tool_result = await web_search.ainvoke(tool_args)

                yield f"event: tool_done\ndata: {json.dumps({'tool': tool_name, 'query': query}, ensure_ascii=False)}\n\n"
                messages.append(ToolMessage(content=tool_result, tool_call_id=tc["id"]))

            # 结合检索结果，以纯流式下发最终生成的答复 Token
            async for chunk in llm.astream(messages):
                if chunk.content:
                    yield f"event: delta\ndata: {json.dumps({'text': chunk.content}, ensure_ascii=False)}\n\n"

        elapsed_ms = int((time.time() - start_time) * 1000)
        yield f"event: done\ndata: {json.dumps({'elapsed_ms': elapsed_ms, 'trace_id': trace_id}, ensure_ascii=False)}\n\n"

    except Exception as e:
        logger.exception("Streaming error: %s", e)
        yield f"event: error\ndata: {json.dumps({'message': str(e), 'code': 'INTERNAL_ERROR'}, ensure_ascii=False)}\n\n"

async def run_chat_sync(chat_history: List[Dict[str, str]]) -> Dict[str, Any]:
    """同步问答执行逻辑（供备用）"""
    messages = parse_input_messages(chat_history)
    llm = get_llm(streaming=False)
    llm_with_tools = llm.bind_tools([web_search])

    tool_used = False
    search_queries = []

    for round_idx in range(3):
        response = await llm_with_tools.ainvoke(messages)
        if not response.tool_calls:
            return {
                "reply": clean_output(response.content),
                "tool_used": tool_used,
                "queries": search_queries
            }

        messages.append(response)
        for tc in response.tool_calls:
            if tc["name"] == "web_search":
                tool_used = True
                query = tc["args"].get("query", "")
                if query and query not in search_queries:
                    search_queries.append(query)
                tool_result = await web_search.ainvoke(tc["args"])
                messages.append(ToolMessage(content=tool_result, tool_call_id=tc["id"]))

    final_res = await llm.ainvoke(messages)
    return {
        "reply": clean_output(final_res.content),
        "tool_used": tool_used,
        "queries": search_queries
    }
