"""脚本拆镜 Agent —— 将长文本脚本拆解为逐镜头提示词序列"""
from typing import List

from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from app.config import settings

_SPLIT_SYSTEM = """\
你是专业的影视分镜师和 AI 提示词工程师。

任务：将用户提供的视频脚本拆分为独立的分镜序列，每个分镜用于 AI 视频生成。

规则：
1. 根据场景/情节自然断点拆分，每个分镜建议 3~10 秒
2. 每个分镜需要包含：主体、动作、场景、镜头类型、光照
3. 将中文描述转化为英文提示词
4. 保持角色和场景的连贯性
5. 严格输出 JSON 数组

输出格式：
[
  {{
    "index": 1,
    "duration": 5,
    "prompt": "英文提示词",
    "negative_prompt": "负向提示词",
    "scene_desc": "中文场景描述（给用户看的）"
  }},
  ...
]
"""


def split_script(script: str, target_duration: int = 5) -> List[dict]:
    """
    将脚本文本拆解为分镜列表

    Args:
        script: 用户输入的脚本文本
        target_duration: 每个分镜目标时长（秒），用于提示模型控制粒度

    Returns:
        分镜列表，每个元素包含 index / duration / prompt / negative_prompt / scene_desc
    """
    llm = ChatOpenAI(
        model=settings.openai_model,
        openai_api_key=settings.openai_api_key,
        openai_api_base=settings.openai_base_url,
        temperature=0.5,
    )

    prompt_tpl = ChatPromptTemplate.from_messages([
        ("system", _SPLIT_SYSTEM),
        ("human", "脚本：\n{script}\n\n每个分镜目标时长：{duration} 秒"),
    ])

    chain = prompt_tpl | llm | JsonOutputParser()
    shots: List[dict] = chain.invoke({"script": script, "duration": target_duration})
    return shots
