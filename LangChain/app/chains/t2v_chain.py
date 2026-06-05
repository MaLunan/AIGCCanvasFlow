"""文字生视频完整 Chain：提示词增强 → 提交任务 → 轮询结果 → 存储"""
import time

from app.chains.prompt_enhance import enhance_t2v_prompt
from app.models.video_models import get_video_model
from app.tasks.task_store import update_task
from app.utils.storage import upload_from_url

_POLL_INTERVAL = 5
_POLL_TIMEOUT  = 600


class T2VChain:
    def run(
        self,
        task_id: str,
        prompt: str,
        model: str = "kling",
        duration: int = 5,
        resolution: str = "1080p",
        aspect_ratio: str = "16:9",
        enhance_prompt: bool = True,
        **kwargs,
    ) -> str:
        llm_api_key = kwargs.pop("llm_api_key", "")
        llm_base_url = kwargs.pop("llm_base_url", "")
        llm_model_name = kwargs.pop("llm_model_name", "")

        # ── Step 1: 提示词增强 ──────────────────────────────────────────────
        update_task(task_id, progress=5, error="")
        if enhance_prompt and prompt:
            try:
                enhanced = enhance_t2v_prompt(
                    prompt,
                    api_key=llm_api_key, base_url=llm_base_url, model_name=llm_model_name,
                )
                final_prompt   = enhanced.get("prompt", prompt)
                final_negative = enhanced.get("negative_prompt", "")
            except Exception as e:
                raise RuntimeError(f"提示词增强失败: {e}") from e
        else:
            final_prompt   = prompt
            final_negative = ""
        update_task(task_id, progress=15, error="")

        # ── Step 2: 提交视频生成任务 ────────────────────────────────────────
        try:
            video_model = get_video_model(model)
        except ValueError as e:
            raise RuntimeError(f"不支持的视频模型 [{model}]: {e}") from e

        update_task(task_id, progress=20, error="")
        try:
            platform_task_id = video_model.submit(
                mode="t2v",
                prompt=final_prompt,
                negative_prompt=final_negative,
                duration=duration,
                aspect_ratio=aspect_ratio,
                resolution=_resolution_str(resolution),
                **kwargs,
            )
        except Exception as e:
            raise RuntimeError(f"提交视频任务失败 [{model}]: {e}") from e

        # ── Step 3: 轮询直到完成 ────────────────────────────────────────────
        result_url = self._poll(task_id, video_model, platform_task_id)

        # ── Step 4: 存储 ────────────────────────────────────────────────────
        update_task(task_id, progress=85, error="")
        try:
            stored_url = upload_from_url(result_url, prefix=f"t2v/{task_id}", ext="mp4")
        except Exception as e:
            raise RuntimeError(f"视频存储失败: {e}") from e

        update_task(task_id, progress=95, error="")
        return stored_url

    @staticmethod
    def _poll(task_id: str, model, platform_task_id: str) -> str:
        deadline = time.time() + _POLL_TIMEOUT
        progress = 25

        while time.time() < deadline:
            result = model.query(platform_task_id)
            if result["status"] == "succeeded":
                return result["result_url"]
            if result["status"] == "failed":
                raise RuntimeError(f"视频生成失败: {result.get('error')}")

            progress = min(progress + 3, 80)
            update_task(task_id, progress=progress, error="")
            time.sleep(_POLL_INTERVAL)

        raise TimeoutError("视频生成超时，请稍后重试")


def _resolution_str(resolution: str) -> str:
    mapping = {
        "1080p": "1920*1080",
        "720p":  "1280*720",
        "480p":  "854*480",
    }
    return mapping.get(resolution, "1280*720")
