"""文字生图完整 Chain：提示词增强 → 模型调用 → 存储"""
import logging

from app.chains.prompt_enhance import enhance_t2i_prompt
from app.models.image_models import get_image_model
from app.tasks.task_store import update_task
from app.utils.storage import upload_from_url

logger = logging.getLogger(__name__)

_DEFAULT_IMAGE_MODEL_NAMES = {
    "dalle3": "dall-e-3",
}


class T2IChain:
    def run(
        self,
        task_id: str,
        prompt: str,
        model: str = "dalle3",
        style: str = "default",
        negative_prompt: str = "",
        width: int = 1024,
        height: int = 1024,
        num_images: int = 1,
        enhance_prompt: bool = True,
        **kwargs,
    ) -> str:
        llm_api_key = kwargs.pop("llm_api_key", "")
        llm_base_url = kwargs.pop("llm_base_url", "")
        llm_model_name = kwargs.pop("llm_model_name", "")

        logger.info("[T2I] task=%s enhance_llm model=%s base_url=%s",
                    task_id, llm_model_name or "(default)", llm_base_url or "(default)")

        # ── Step 1: 提示词增强 ──────────────────────────────────────────────
        update_task(task_id, progress=10, error="")
        if enhance_prompt and prompt:
            try:
                enhanced = enhance_t2i_prompt(
                    prompt, style=style,
                    api_key=llm_api_key, base_url=llm_base_url, model_name=llm_model_name,
                )
                final_prompt = enhanced.get("prompt", prompt)
                final_negative = enhanced.get("negative_prompt", negative_prompt)
            except Exception as e:
                raise RuntimeError(f"提示词增强失败: {e}") from e
        else:
            final_prompt = prompt
            final_negative = negative_prompt
        update_task(task_id, progress=30, error="")

        # ── Step 2: 模型生成 ────────────────────────────────────────────────
        img_api_key = kwargs.pop("img_api_key", "")
        img_base_url = kwargs.pop("img_base_url", "")
        img_model_name = kwargs.pop("img_model_name", "")

        update_task(task_id, progress=40, error="")
        try:
            image_model = get_image_model(model, api_key=img_api_key, base_url=img_base_url)
        except ValueError as e:
            raise RuntimeError(f"不支持的模型 [{model}]: {e}") from e

        try:
            update_task(task_id, progress=50)
            urls = image_model.generate(
                prompt=final_prompt,
                negative_prompt=final_negative,
                width=width,
                height=height,
                num_images=num_images,
                img_model_name=img_model_name or _DEFAULT_IMAGE_MODEL_NAMES.get(model, model),
                **kwargs,
            )
        except Exception as e:
            raise RuntimeError(f"模型 [{model}] 生成失败: {e}") from e

        if not urls:
            raise RuntimeError(f"模型 [{model}] 未返回任何图片")

        update_task(task_id, progress=70, error="")

        # ── Step 3: 持久化到对象存储 ────────────────────────────────────────
        try:
            stored_url = upload_from_url(urls[0], prefix=f"t2i/{task_id}")
        except Exception as e:
            raise RuntimeError(f"图片存储失败: {e}") from e

        update_task(task_id, progress=90, error="")
        return stored_url
