"""图生视频完整 Chain：图片预处理 → 提交任务 → 轮询结果 → 存储"""
import time

from app.chains.t2v_chain import _POLL_INTERVAL, _POLL_TIMEOUT
from app.models.video_models import get_video_model
from app.tasks.task_store import update_task
from app.utils.image_utils import ensure_image_url
from app.utils.storage import upload_from_url


class I2VChain:
    def run(
        self,
        task_id: str,
        image_url: str = "",
        image_base64: str = "",
        prompt: str = "",
        model: str = "kling",
        duration: int = 5,
        motion_strength: float = 0.5,
        fps: int = 24,
        **kwargs,
    ) -> str:
        # Step 1: 处理图片输入（base64 → 上传为 URL）
        update_task(task_id, progress=15)
        final_image_url = ensure_image_url(
            image_url=image_url,
            image_base64=image_base64,
            task_id=task_id,
        )

        # Step 2: 提交图生视频任务
        update_task(task_id, progress=25)
        video_model = get_video_model(model)
        platform_task_id = video_model.submit(
            mode="i2v",
            image_url=final_image_url,
            prompt=prompt,
            duration=duration,
            motion_strength=motion_strength,
            **kwargs,
        )

        # Step 3: 轮询
        result_url = self._poll(task_id, video_model, platform_task_id)

        # Step 4: 存储
        update_task(task_id, progress=90)
        stored_url = upload_from_url(result_url, prefix=f"i2v/{task_id}", ext="mp4")
        return stored_url

    @staticmethod
    def _poll(task_id: str, model, platform_task_id: str) -> str:
        deadline = time.time() + _POLL_TIMEOUT
        progress = 30

        while time.time() < deadline:
            result = model.query(platform_task_id)
            if result["status"] == "succeeded":
                return result["result_url"]
            if result["status"] == "failed":
                raise RuntimeError(f"图生视频失败: {result.get('error')}")

            progress = min(progress + 3, 85)
            update_task(task_id, progress=progress)
            time.sleep(_POLL_INTERVAL)

        raise TimeoutError("图生视频超时，请稍后重试")
