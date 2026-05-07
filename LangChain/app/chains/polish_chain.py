"""文字润化链 —— 使用 LLM 对文本进行润色改写"""
from typing import Optional, List

from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

_POLISH_SYSTEM = """\
你是专业的文字润色专家，擅长对文字进行语言润化、改写，使其更流畅、专业、生动。

规则：
1. 保持原文核心意思和信息完整不变，不删减关键内容
2. 优化语言表达，使句子更流畅自然，消除语病和冗余
3. 如有参考上下文，结合上下文保持整体风格一致
4. 直接输出润化后的文字，不要添加任何解释、标注或引号
"""


def _build_llm(api_key: str, base_url: str, model_name: str) -> ChatOpenAI:
    return ChatOpenAI(
        model=model_name,
        openai_api_key=api_key,
        openai_api_base=base_url,
        temperature=0.7,
    )


def polish_text(text: str,
                context: Optional[List[dict]] = None,
                api_key: Optional[str] = None,
                base_url: Optional[str] = None,
                model_name: Optional[str] = None) -> str:
    if not api_key:
        raise ValueError("请传入模型 API Key（api_key）")
    if not base_url:
        raise ValueError("请传入模型 API 地址（base_url）")
    if not model_name:
        raise ValueError("请传入模型名称（model_name）")
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
    chain = tpl | _build_llm(api_key=api_key, base_url=base_url, model_name=model_name)
    result = chain.invoke({"text": text})
    return result.content
