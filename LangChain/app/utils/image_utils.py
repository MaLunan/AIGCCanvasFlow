"""图片工具 —— base64 解码上传、尺寸校验、格式转换"""
import base64
import io

from app.utils.storage import upload_bytes


def ensure_image_url(image_url: str = "", image_base64: str = "", task_id: str = "") -> str:
    """
    统一处理图片输入：
    - 如果传了 image_url，直接返回
    - 如果传了 image_base64，解码后上传到存储，返回 URL
    """
    if image_url:
        return image_url

    if image_base64:
        # 处理 data:image/xxx;base64,... 格式
        if "," in image_base64:
            header, data = image_base64.split(",", 1)
            ext = "jpg"
            if "png" in header:
                ext = "png"
            elif "webp" in header:
                ext = "webp"
        else:
            data = image_base64
            ext = "jpg"

        image_bytes = base64.b64decode(data)
        filename = f"i2v/{task_id}/source.{ext}"
        return upload_bytes(image_bytes, filename)

    raise ValueError("必须提供 image_url 或 image_base64")


def resize_image(image_bytes: bytes, max_width: int = 1920, max_height: int = 1080) -> bytes:
    """将图片缩放到指定尺寸上限，保持宽高比"""
    from PIL import Image

    img = Image.open(io.BytesIO(image_bytes))
    img.thumbnail((max_width, max_height), Image.LANCZOS)

    buf = io.BytesIO()
    fmt = img.format or "JPEG"
    img.save(buf, format=fmt)
    return buf.getvalue()
