"""图像模型适配器 —— 统一接口封装 DALL·E 3 / Flux / SDXL"""
import base64
import httpx
from abc import ABC, abstractmethod
from typing import List

from openai import OpenAI

from app.config import settings


class BaseImageModel(ABC):
    @abstractmethod
    def generate(
        self,
        prompt: str,
        negative_prompt: str = "",
        width: int = 1024,
        height: int = 1024,
        num_images: int = 1,
        **kwargs,
    ) -> List[str]:
        """返回生成图片的 URL 列表"""


# ─── DALL·E 3 ─────────────────────────────────────────────────────────────────
class DallE3Model(BaseImageModel):
    def __init__(self):
        # 复用 OpenAI 客户端实例，支持通过 openai_base_url 指向兼容代理
        self._client = OpenAI(
            api_key=settings.openai_api_key,
            base_url=settings.openai_base_url,
        )

    def generate(self, prompt, negative_prompt="", width=1024, height=1024,
                 num_images=1, quality="standard", style="vivid", **kwargs) -> List[str]:
        # DALL·E 3 只支持三种固定尺寸，不支持任意分辨率；不匹配则默认 1024×1024
        size_map = {
            (1024, 1024): "1024x1024",
            (1792, 1024): "1792x1024",  # 横版
            (1024, 1792): "1024x1792",  # 竖版
        }
        size = size_map.get((width, height), "1024x1024")

        # DALL·E 3 单次 API 只能生成 1 张（n=1 限制），需要循环多次获取多张
        urls = []
        count = min(num_images, 4)  # 最多 4 张（成本控制）
        for _ in range(count):
            resp = self._client.images.generate(
                model="dall-e-3",
                prompt=prompt,
                n=1,
                size=size,
                quality=quality,  # standard / hd
                style=style,      # vivid（鲜艳）/ natural（自然）
            )
            urls.append(resp.data[0].url)
        return urls


# ─── Flux（via Replicate）──────────────────────────────────────────────────────
class FluxModel(BaseImageModel):
    MODEL_ID = "black-forest-labs/flux-1.1-pro"

    def generate(self, prompt, negative_prompt="", width=1024, height=1024,
                 num_images=1, steps=28, guidance=3.5, **kwargs) -> List[str]:
        import replicate

        output = replicate.run(
            self.MODEL_ID,
            input={
                "prompt": prompt,
                "width": width,
                "height": height,
                "num_outputs": min(num_images, 4),
                "num_inference_steps": steps,
                "guidance": guidance,
            },
        )
        return [str(url) for url in output]


# ─── Stable Diffusion XL（via Replicate）─────────────────────────────────────
class SDXLModel(BaseImageModel):
    MODEL_ID = "stability-ai/sdxl:39ed52f2319f9b46c...latest"

    def generate(self, prompt, negative_prompt="", width=1024, height=1024,
                 num_images=1, steps=30, cfg_scale=7.5, **kwargs) -> List[str]:
        import replicate

        output = replicate.run(
            self.MODEL_ID,
            input={
                "prompt": prompt,
                "negative_prompt": negative_prompt,
                "width": width,
                "height": height,
                "num_outputs": min(num_images, 4),
                "num_inference_steps": steps,
                "guidance_scale": cfg_scale,
            },
        )
        return [str(url) for url in output]


# ─── 工厂函数 ──────────────────────────────────────────────────────────────────
_MODEL_REGISTRY = {
    "dalle3":  DallE3Model,
    "flux":    FluxModel,
    "sdxl":    SDXLModel,
}


def get_image_model(model_name: str) -> BaseImageModel:
    """工厂函数：根据 model_name 字符串实例化对应的图像模型适配器"""
    cls = _MODEL_REGISTRY.get(model_name)
    if not cls:
        raise ValueError(f"不支持的图像模型: {model_name}，可选: {list(_MODEL_REGISTRY.keys())}")
    return cls()
