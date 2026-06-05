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


# ─── OpenAI 兼容图像模型（DALL·E 3 / Seedream / 任意兼容接口）─────────────
class OpenAIImageModel(BaseImageModel):
    SEEDREAM_MIN_PIXELS = 3_686_400
    SEEDREAM_DEFAULT_SIZE = "1920x1920"

    def __init__(self, api_key=None, base_url=None):
        self._client = OpenAI(
            api_key=api_key or settings.openai_api_key,
            base_url=base_url or settings.openai_base_url,
        )

    def generate(self, prompt, negative_prompt="", width=1024, height=1024,
                 num_images=1, quality="standard", style="vivid",
                 img_model_name="dall-e-3", img_size=None, **kwargs) -> List[str]:
        size = img_size or f"{width}x{height}"
        if self._is_seedream_model(img_model_name) and width * height < self.SEEDREAM_MIN_PIXELS:
            size = self.SEEDREAM_DEFAULT_SIZE

        urls = []
        count = min(num_images, 4)
        for _ in range(count):
            resp = self._client.images.generate(
                model=img_model_name,
                prompt=prompt,
                n=1,
                size=size,
                quality=quality,
                style=style,
            )
            urls.append(resp.data[0].url)
        return urls

    @staticmethod
    def _is_seedream_model(model_name: str) -> bool:
        return "seedream" in (model_name or "").lower()


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
    "dalle3":  OpenAIImageModel,
    "flux":    FluxModel,
    "sdxl":    SDXLModel,
}


def get_image_model(model_name: str, api_key=None, base_url=None) -> BaseImageModel:
    cls = _MODEL_REGISTRY.get(model_name)
    if not cls:
        raise ValueError(f"不支持的图像模型: {model_name}，可选: {list(_MODEL_REGISTRY.keys())}")
    if cls == OpenAIImageModel:
        return cls(api_key=api_key, base_url=base_url)
    return cls()
