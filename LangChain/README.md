# LangChain AIGC 服务

基于 **LangChain + FastAPI** 构建的 AI 内容生成后端服务，为 CanvasFlow 画布节点提供文字生图、文字生视频、图生视频三大能力。

---

## 功能规划

### 1. 文字生图（Text → Image）

| 功能点 | 说明 |
|--------|------|
| 提示词增强 | LangChain Chain 自动补全/优化用户输入的中文提示词，翻译并扩展为适合模型的英文 prompt |
| 多模型路由 | 根据风格参数自动选择模型（写实 → Flux，动漫 → NovelAI，通用 → DALL·E 3） |
| 参数控制 | 支持分辨率、宽高比、采样步数、CFG Scale、负向提示词 |
| 批量生成 | 单次请求生成 1~8 张，返回任务 ID 异步轮询 |
| 风格预设 | 写实、动漫、水墨、赛博朋克、电影感等内置风格 |

**支持模型**
- `DALL·E 3`（OpenAI API）
- `Flux 1.1 Pro`（fal.ai / Replicate）
- `Stable Diffusion XL`（本地 / Replicate）
- `Midjourney`（第三方代理 API）

---

### 2. 文字生视频（Text → Video）

| 功能点 | 说明 |
|--------|------|
| 分镜脚本拆解 | LangChain Agent 将长文本脚本拆解为逐镜提示词序列 |
| 提示词优化 | 自动为每个镜头补充运镜、光照、景深描述 |
| 时长控制 | 支持 3 秒 / 5 秒 / 10 秒片段生成 |
| 分辨率选择 | 720p / 1080p，横版 / 竖版 / 方形 |
| 多模型切换 | 按质量/速度需求选择不同模型 |

**支持模型**
- `可灵 2.0`（Kling API）
- `Wan 2.1`（阿里云 API）
- `CogVideoX-5B`（本地推理 / Replicate）
- `Hailuo MiniMax`（MiniMax API）
- `LTX-Video`（本地推理）

---

### 3. 图生视频（Image → Video）

| 功能点 | 说明 |
|--------|------|
| 运动控制 | 支持镜头推拉、左右平移、旋转等运动参数 |
| 运动强度 | 0~10 连续调节运动幅度 |
| 时长控制 | 3 / 5 秒片段 |
| 帧率设置 | 24fps / 30fps |
| 参考图上传 | 支持 URL 或 Base64 图片输入 |

**支持模型**
- `可灵 2.0 图生视频`（Kling API）
- `Wan 2.1 图生视频`（阿里云 API）
- `Stable Video Diffusion`（本地推理 / Replicate）
- `Runway Gen-3`（Runway API）

---

## 技术架构

```
LangChain/
├── app/
│   ├── main.py                  # FastAPI 入口，挂载路由、中间件
│   ├── api/
│   │   ├── t2i.py               # 文字生图路由
│   │   ├── t2v.py               # 文字生视频路由
│   │   └── i2v.py               # 图生视频路由
│   ├── chains/
│   │   ├── prompt_enhance.py    # LangChain 提示词增强链
│   │   ├── script_splitter.py   # 脚本拆镜 Agent
│   │   ├── t2i_chain.py         # 文字生图完整 Chain
│   │   ├── t2v_chain.py         # 文字生视频完整 Chain
│   │   └── i2v_chain.py         # 图生视频完整 Chain
│   ├── models/
│   │   ├── image_models.py      # 图像模型适配器（统一接口）
│   │   └── video_models.py      # 视频模型适配器（统一接口）
│   ├── tasks/
│   │   ├── worker.py            # Celery worker
│   │   └── task_store.py        # Redis 任务状态管理
│   └── utils/
│       ├── storage.py           # 文件上传（MinIO / OSS / S3）
│       └── image_utils.py       # 图片预处理（resize、格式转换）
├── requirements.txt
├── .env.example
└── README.md
```

---

## API 接口设计

### 文字生图

```
POST /api/v1/t2i/generate
{
  "prompt": "赛博朋克风格的城市夜景",
  "negative_prompt": "模糊, 低质量",
  "model": "flux",           // flux | dalle3 | sdxl | midjourney
  "style": "cyberpunk",      // 风格预设，可选
  "width": 1024,
  "height": 1024,
  "num_images": 1,
  "enhance_prompt": true     // 是否启用 LangChain 提示词增强
}

Response: { "task_id": "xxx" }
```

### 文字生视频

```
POST /api/v1/t2v/generate
{
  "prompt": "一只猫在草地上奔跑，阳光明媚",
  "model": "kling",          // kling | wan | cogvideox | minimax
  "duration": 5,             // 秒
  "resolution": "1080p",
  "aspect_ratio": "16:9",    // 16:9 | 9:16 | 1:1
  "enhance_prompt": true
}

Response: { "task_id": "xxx" }
```

### 图生视频

```
POST /api/v1/i2v/generate
{
  "image_url": "https://...",  // 或 "image_base64": "..."
  "prompt": "镜头缓慢向前推进",  // 可选运动描述
  "model": "kling",             // kling | wan | svd | runway
  "duration": 5,
  "motion_strength": 6,         // 0~10
  "fps": 24
}

Response: { "task_id": "xxx" }
```

### 任务查询（通用）

```
GET /api/v1/tasks/{task_id}

Response:
{
  "task_id": "xxx",
  "status": "pending | processing | succeeded | failed",
  "progress": 60,            // 0~100
  "result_url": "https://...",  // status=succeeded 时返回
  "error": null
}
```

---

## 核心 LangChain 设计

### 提示词增强链（PromptEnhanceChain）

```
用户输入（中文）
    ↓  TranslationChain（GPT-4o）
英文 prompt
    ↓  EnhancementChain（添加风格/光照/镜头语言）
增强后的 prompt
    ↓  NegativePromptChain（自动生成负向提示词）
最终 (prompt, negative_prompt)
```

### 脚本拆镜 Agent（ScriptSplitterAgent）

```
长文本脚本
    ↓  TextSplitter（按场景断句）
场景列表
    ↓  SceneAnalysisChain（为每个场景生成镜头描述）
[镜头1, 镜头2, ..., 镜头N]
    ↓  PromptEnhanceChain（逐镜提示词增强）
最终镜头序列
```

---

## 快速启动

### 环境要求

- Python >= 3.10
- [uv](https://docs.astral.sh/uv/) 包管理器（安装：`curl -LsSf https://astral.sh/uv/install.sh | sh`）
- Redis（Celery 消息队列）

### 启动步骤

```bash
# 1. 进入项目
cd LangChain

# 2. 创建虚拟环境
uv venv --python 3.10

# 3. 安装依赖
uv sync

# 4. 配置环境变量
cp .env.example .env
# 编辑 .env 填入各平台的 API Key（至少需要 OPENAI_API_KEY）

# 5. 确保 Redis 已启动（默认连接 redis://localhost:6379/0）
# brew services start redis   # macOS

# 6. 启动服务（二选一）

# 方式 A：两个终端分别启动
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8888   # 终端 1：API 服务
uv run celery -A app.tasks.worker.celery_app worker --loglevel=info --concurrency=4  # 终端 2：Worker

# 方式 B：一键同时启动
uv run honcho start
```

启动后访问 **http://localhost:8000/docs** 查看 Swagger API 文档。

---

## 环境变量

```bash
# .env（复制自 .env.example）
# ── OpenAI ─────────────────────────────────
OPENAI_API_KEY=sk-...
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o

# ── 可灵 Kling ─────────────────────────────
KLING_ACCESS_KEY=
KLING_SECRET_KEY=

# ── 万象 Wan ───────────────────────────────
DASHSCOPE_API_KEY=

# ── Replicate ──────────────────────────────
REPLICATE_API_TOKEN=

# ── Runway ─────────────────────────────────
RUNWAY_API_KEY=

# ── MiniMax ────────────────────────────────
MINIMAX_API_KEY=
MINIMAX_GROUP_ID=

# ── 对象存储（MinIO / S3）───────────────────
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=
MINIO_SECRET_KEY=
MINIO_BUCKET=aigc-outputs

# ── Redis / Celery ─────────────────────────
REDIS_URL=redis://localhost:6379/0

# ── 应用 ───────────────────────────────────
DEBUG=false
TASK_EXPIRE_SECONDS=86400
```

---

## 开发计划

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| P0 | FastAPI 骨架 + 任务队列（Celery + Redis）| 高 |
| P0 | 提示词增强链（LangChain + GPT-4o）| 高 |
| P0 | 文字生图：DALL·E 3 + Flux 接入 | 高 |
| P1 | 文字生视频：可灵 + Wan 接入 | 高 |
| P1 | 图生视频：可灵图生视频接入 | 高 |
| P2 | 脚本拆镜 Agent | 中 |
| P2 | 多模型路由策略（按质量/速度/费用）| 中 |
| P3 | 本地模型推理（SDXL / CogVideoX / SVD）| 低 |
| P3 | 存储对接（MinIO / 阿里云 OSS）| 低 |
