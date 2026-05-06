"""文字生图完整 Chain：提示词增强 → 模型调用 → 存储"""
from app.chains.prompt_enhance import enhance_t2i_prompt
from app.models.image_models import get_image_model
from app.tasks.task_store import update_task
from app.utils.storage import upload_from_url


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
        """
        执行文字生图流水线，返回第一张图的最终 URL
        """
        # Step 1: 提示词增强
        update_task(task_id, progress=20)
        if enhance_prompt and prompt:
            enhanced = enhance_t2i_prompt(prompt, style=style)
            final_prompt = enhanced.get("prompt", prompt)
            final_negative = enhanced.get("negative_prompt", negative_prompt)
        else:
            final_prompt = prompt
            final_negative = negative_prompt

        # Step 2: 模型生成
        update_task(task_id, progress=40)
        image_model = get_image_model(model)
        urls = image_model.generate(
            prompt=final_prompt,
            negative_prompt=final_negative,
            width=width,
            height=height,
            num_images=num_images,
            **kwargs,
        )

        if not urls:
            raise RuntimeError("模型未返回任何图片")

        # Step 3: 持久化到对象存储（可选，url 有效期有限）
        update_task(task_id, progress=80)
        stored_url = upload_from_url(urls[0], prefix=f"t2i/{task_id}")

        return stored_url
