import logging
from langchain_core.tools import tool

logger = logging.getLogger("ai_service.tools")

@tool
def web_search(query: str) -> str:
    """
    用于在互联网上搜索最新医疗健康资讯、养老政策、疾病诊疗指南、药物常识以及时事信息。
    当长者或医护人员咨询包含时效性、最新医学进展、日常饮食调理方案等需要权威联网资料时调用。
    """
    logger.info(f"Executing web_search for query: {query}")
    try:
        from duckduckgo_search import DDGS
        # 针对健康养老主题优化关键词检索
        results = list(DDGS().text(query, max_results=4))
        if not results:
            return f"未能检索到与【{query}】相关的有效互联网信息，将根据现有医学与养老常识为您解答。"

        formatted_snippets = []
        for i, item in enumerate(results, 1):
            title = item.get("title", "网页来源")
            snippet = item.get("body", "")
            href = item.get("href", "")
            formatted_snippets.append(f"【来源 {i}】{title}\n摘要：{snippet}\n链接：{href}")

        return "\n\n".join(formatted_snippets)
    except Exception as e:
        logger.warning(f"Web search error for '{query}': {e}")
        return f"联网检索服务短暂波动（{str(e)}），请根据既有医学与康养专业知识继续提供严谨建议。"
