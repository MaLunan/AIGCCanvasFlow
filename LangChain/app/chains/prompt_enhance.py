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


def _build_llm() -> ChatOpenAI:
    """构建 ChatOpenAI 实例（temperature=0.7 保证创意性但不过于随机）"""
    return ChatOpenAI(
        model=settings.openai_model,
        openai_api_key=settings.openai_api_key,
        openai_api_base=settings.openai_base_url,
        temperature=0.7,
    )


def enhance_t2i_prompt(user_input: str, style: str = "default") -> dict:
    """
    增强文字生图提示词，返回 {prompt, negative_prompt}。
    流程：将用户中文描述 + 风格关键词 → LangChain Chain → JSON 解析 → 英文专业提示词
    """
    style_hint = _T2I_STYLE_HINTS.get(style, _T2I_STYLE_HINTS["default"])
    # 将风格名和风格关键词合并，一起注入 prompt 模板
    combined_style = f"{style}, {style_hint}"

    # LangChain LCEL 管道：prompt 模板 | LLM 调用 | JSON 输出解析
    chain = _enhance_prompt | _build_llm() | JsonOutputParser()
    result = chain.invoke({"user_input": user_input, "style": combined_style})
    return result


def enhance_t2v_prompt(user_input: str) -> dict:
    """增强文字生视频提示词，返回 {prompt, negative_prompt}"""
    prompt_tpl = ChatPromptTemplate.from_messages([
        ("system", _T2V_ENHANCE_SYSTEM),
        ("human", "用户描述：{user_input}"),
    ])
    chain = prompt_tpl | _build_llm() | JsonOutputParser()
    return chain.invoke({"user_input": user_input})
