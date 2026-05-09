"""文字润化/生成链 —— 支持纯文本润色，以及携带图片上下文的视觉理解"""
import base64
import logging
from typing import Optional, List
from urllib.parse import urlparse

import httpx
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from openai import RateLimitError
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type, before_sleep_log

logger = logging.getLogger(__name__)

_IMAGE_EXTS = ('.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp')

_POLISH_SYSTEM = """\
你是专业的文字处理助手，擅长文字润色改写，也能根据图片生成描述。

规则：
1. 若收到图片，请根据图片内容完成用户指令
2. 若仅有文字，保持原文核心意思不变，优化语言表达使其更流畅自然
3. 如有参考上下文，结合上下文保持整体风格一致
4. 直接输出结果，不要添加任何解释、标注或引号
"""


def _normalize_base_url(url: str) -> str:
    """若 URL 没有路径部分，自动追加 /v1（OpenAI SDK 规范）。
    例：https://api.deepseek.com → https://api.deepseek.com/v1
    已有路径的不变：https://open.bigmodel.cn/api/paas/v4 → 不变"""
    url = url.rstrip('/')
    parsed = urlparse(url)
    if not parsed.path or parsed.path == '/':
        url += '/v1'
    return url


@retry(
    retry=retry_if_exception_type(RateLimitError),
    wait=wait_exponential(multiplier=1, min=2, max=30),
    stop=stop_after_attempt(4),
    before_sleep=before_sleep_log(logger, logging.WARNING),
    reraise=True,
)
def _invoke_with_retry(llm, messages):
    return llm.invoke(messages)


def _build_llm(api_key: str, base_url: str, model_name: str) -> ChatOpenAI:
    return ChatOpenAI(
        model=model_name,
        openai_api_key=api_key,
        openai_api_base=_normalize_base_url(base_url),
        temperature=0.7,
    )


def _is_image_url(content: str) -> bool:
    if not content.startswith(('http://', 'https://')):
        return False
    path = content.lower().split('?')[0]
    return any(path.endswith(ext) for ext in _IMAGE_EXTS)


def _fetch_image_as_base64(url: str) -> tuple[str, str]:
    """下载图片，返回 (base64字符串, mime_type)。
    Python 服务与 Spring Boot 同机运行，可访问 localhost URL。"""
    resp = httpx.get(url, timeout=15, follow_redirects=True)
    resp.raise_for_status()
    mime = resp.headers.get('Content-Type', 'image/jpeg').split(';')[0].strip()
    b64 = base64.b64encode(resp.content).decode()
    return b64, mime


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

    llm = _build_llm(api_key=api_key, base_url=base_url, model_name=model_name)

    # 拆分图片上下文和文本上下文
    image_items = []
    text_items = []
    for c in (context or []):
        content = c.get("content", "")
        if not content:
            continue
        if _is_image_url(content):
            image_items.append(c)
        else:
            text_items.append(c)

    # 构建 system 消息
    system_content = _POLISH_SYSTEM
    if text_items:
        hints = "\n".join(
            f"[{c.get('label', '节点')}]: {c.get('content', '')}"
            for c in text_items
        )
        system_content += f"\n\n参考上下文：\n{hints}"

    if image_items:
        # 视觉路径：将图片转为 base64 内嵌到 human 消息
        human_parts: list = [{"type": "text", "text": text}]
        for img in image_items:
            try:
                b64, mime = _fetch_image_as_base64(img["content"])
                human_parts.append({
                    "type": "image_url",
                    "image_url": {"url": f"data:{mime};base64,{b64}"},
                })
            except Exception as e:
                # 图片下载失败时降级为文字说明
                human_parts.append({
                    "type": "text",
                    "text": f"[图片获取失败: {img.get('label', '')}，错误: {e}]",
                })

        messages = [
            SystemMessage(content=system_content),
            HumanMessage(content=human_parts),
        ]
        try:
            result = _invoke_with_retry(llm, messages)
        except Exception as e:
            err = str(e)
            if 'image_url' in err or 'image' in err.lower():
                raise ValueError(
                    f"当前模型「{model_name}」不支持图片输入，"
                    "请在模型库中切换为支持视觉的模型（如 gpt-4o、qwen-vl-plus、glm-4v 等）"
                ) from e
            raise
    else:
        # 纯文本路径
        messages = [
            SystemMessage(content=system_content),
            HumanMessage(content=text),
        ]
        result = _invoke_with_retry(llm, messages)

    return result.content
