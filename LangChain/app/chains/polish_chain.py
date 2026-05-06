"""文字润化链 —— 使用 LLM 对文本进行润色改写"""
from typing import Optional, List

from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.config import settings

_POLISH_SYSTEM = """\
你是专业的文字润色专家，擅长对文字进行语言润化、改写，使其更流畅、专业、生动。

规则：
1. 保持原文核心意思和信息完整不变，不删减关键内容
2. 优化语言表达，使句子更流畅自然，消除语病和冗余
3. 如有参考上下文，结合上下文保持整体风格一致
4. 直接输出润化后的文字，不要添加任何解释、标注或引号
"""


def _build_llm() -> ChatOpenAI:
    return ChatOpenAI(
        model=settings.openai_model,
        openai_api_key=settings.openai_api_key,
        openai_api_base=settings.openai_base_url,
        temperature=0.7,
    )


def polish_text(text: str, context: Optional[List[dict]] = None) -> str:
    """
    润化文本。
    :param text: 待润化文字
    :param context: 上游节点上下文，格式 [{"label": "...", "content": "..."}]
    :return: 润化后的文字
    """
    context_hint = ""
    if context:
        items = "\n".join(
            f"[{c.get('label', '节点')}]: {c.get('content', '')}"
            for c in context
            if c.get("content")
        )
        if items:
            context_hint = f"\n\n参考上下文（仅供风格参考，不要在输出中引用）：\n{items}"

    tpl = ChatPromptTemplate.from_messages([
        ("system", _POLISH_SYSTEM + context_hint),
        ("human", "{text}"),
    ])
    chain = tpl | _build_llm()
    result = chain.invoke({"text": text})
    return result.content
