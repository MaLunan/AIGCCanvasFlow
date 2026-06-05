"""提示词增强链 —— 中文输入 → 高质量英文 prompt + negative_prompt"""
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.config import settings

_ENHANCE_SYSTEM = """\
你是专业的 AI 绘画提示词工程师，擅长将用户的中文描述转化为高质量的 Stable Diffusion / Flux / DALL·E 提示词。

规则：
1. 将输入翻译并扩展为详细的英文提示词（prompt），包含主体、场景、光照、构图、风格、质量词
2. 生成负向提示词（negative_prompt），排除常见瑕疵
3. 根据指定风格（style）调整描述侧重点
4. 严格输出 JSON，不要输出其他内容

输出格式：
{{"prompt": "...", "negative_prompt": "..."}}
"""

_ENHANCE_HUMAN = "用户描述：{user_input}\n风格：{style}"

_enhance_prompt = ChatPromptTemplate.from_messages([
    ("system", _ENHANCE_SYSTEM),
    ("human", _ENHANCE_HUMAN),
])

# 风格预设关键词表：每种风格对应一组引导性英文词，拼接到用户 prompt 后
# 作用：在 LLM 增强提示词时提供风格方向锚点，减少歧义
_T2I_STYLE_HINTS = {
    "realistic":   "photorealistic, ultra detailed, 8k, cinematic lighting",
    "anime":       "anime style, cel shading, vibrant colors, studio ghibli",
    "cyberpunk":   "cyberpunk, neon lights, rain, dark city, sci-fi",
    "ink":         "Chinese ink painting, brush strokes, minimalist, elegant",
    "cinematic":   "cinematic, film grain, anamorphic lens, depth of field",
    "default":     "high quality, detailed",
}

_T2V_ENHANCE_SYSTEM = """\
你是专业的 AI 视频提示词工程师。将用户的中文描述转化为适合视频生成模型的英文提示词。

要求：
1. 描述主体动作、场景、光照
2. 加入镜头运动描述（如：slow push in / pan left / static shot）
3. 加入时间感描述（如：golden hour / night / dawn）
4. 严格输出 JSON

输出格式：
{{"prompt": "...", "negative_prompt": "..."}}
"""


_UNSUPPORTED_CHAT_MODEL_KEYWORDS = (
    "seedream",
    "dall-e",
    "dalle",
    "flux",
    "sdxl",
    "stable-diffusion",
    "kling",
    "wanx",
    "minimax",
)


def _looks_like_generation_model(model_name: str) -> bool:
    normalized = (model_name or "").lower()
    return any(keyword in normalized for keyword in _UNSUPPORTED_CHAT_MODEL_KEYWORDS)


def _build_llm(api_key: str = "", base_url: str = "", model_name: str = "") -> ChatOpenAI:
    """构建 ChatOpenAI 实例（优先用传入参数，fallback 到 .env 配置）"""
    selected_model = model_name or settings.openai_model or "gpt-4o"
    if _looks_like_generation_model(selected_model):
        raise ValueError(
            f"提示词增强需要文本/聊天模型，当前模型 [{selected_model}] 是图像或视频生成模型；"
            "请把它填到 img_model_name，llm_model_name 留空或改为 doubao-pro / gpt-4o 等聊天模型。"
        )
    return ChatOpenAI(
        model=selected_model,
        openai_api_key=api_key or settings.openai_api_key,
        openai_api_base=base_url or settings.openai_base_url,
        temperature=0.7,
        request_timeout=30,
        max_retries=2,
    )


def enhance_t2i_prompt(user_input: str, style: str = "default",
                       api_key: str = "", base_url: str = "", model_name: str = "") -> dict:
    """增强文字生图提示词，返回 {prompt, negative_prompt}"""
    style_hint = _T2I_STYLE_HINTS.get(style, _T2I_STYLE_HINTS["default"])
    combined_style = f"{style}, {style_hint}"

    chain = _enhance_prompt | _build_llm(api_key, base_url, model_name) | JsonOutputParser()
    result = chain.invoke({"user_input": user_input, "style": combined_style})
    return result


def enhance_t2v_prompt(user_input: str,
                       api_key: str = "", base_url: str = "", model_name: str = "") -> dict:
    """增强文字生视频提示词，返回 {prompt, negative_prompt}"""
    prompt_tpl = ChatPromptTemplate.from_messages([
        ("system", _T2V_ENHANCE_SYSTEM),
        ("human", "用户描述：{user_input}"),
    ])
    chain = prompt_tpl | _build_llm(api_key, base_url, model_name) | JsonOutputParser()
    return chain.invoke({"user_input": user_input})
