-- ============================================================
-- V2: 模型广场 & 用户模型库
-- ============================================================
USE aigc_canvas;

-- 1. 为 t_ai_model 补充展示字段（此脚本只执行一次，字段不存在时运行）
ALTER TABLE t_ai_model
    ADD COLUMN category VARCHAR(20)  NULL COMMENT '中文分类：视频/图像/音频/文本' AFTER description,
    ADD COLUMN tags     VARCHAR(200) NULL COMMENT '标签，逗号分隔' AFTER category,
    ADD COLUMN icon     VARCHAR(10)  NULL COMMENT 'emoji 图标' AFTER tags,
    ADD COLUMN color    VARCHAR(20)  NULL COMMENT '主题色 hex' AFTER icon;

-- 2. 更新已有模型数据
UPDATE t_ai_model SET category='视频', tags='文生视频,高质量', icon='🎬', color='#ff6b6b' WHERE model_key='kling-v3';
UPDATE t_ai_model SET category='视频', tags='文生视频,高质量', icon='🎬', color='#646cff' WHERE model_key='seedance-v2';
UPDATE t_ai_model SET category='视频', tags='文生视频,长视频', icon='🎬', color='#42b883' WHERE model_key='vidu-v2';
UPDATE t_ai_model SET category='视频', tags='文生视频,物理真实',icon='🎬', color='#7fd1f5' WHERE model_key='sora-v1';
UPDATE t_ai_model SET category='视频', tags='图生视频,商业级', icon='🎬', color='#f5c542' WHERE model_key='runway-gen3';
UPDATE t_ai_model SET category='视频', tags='文生视频,创意',   icon='🎬', color='#ff9966' WHERE model_key='pika-v2';
UPDATE t_ai_model SET category='图像', tags='文生图,超写实',   icon='🖼️', color='#d07ff5' WHERE model_key='flux-pro-1.1';
UPDATE t_ai_model SET category='图像', tags='文生图,精准理解', icon='🖼️', color='#7fd1f5' WHERE model_key='dalle-3';
UPDATE t_ai_model SET category='图像', tags='艺术风格,美学',   icon='🖼️', color='#f5c542' WHERE model_key='mj-v6';
UPDATE t_ai_model SET category='图像', tags='文字渲染,设计',   icon='🖼️', color='#42b883' WHERE model_key='ideogram-v2';

-- 3. 补充新模型（type 增加 text）
INSERT IGNORE INTO t_ai_model (name, provider, type, model_key, cost_points, description, category, tags, icon, color) VALUES
('Runway Gen-4',     'runway',     'video', 'runway-gen4',   60,  'Runway 第四代模型，商业创作首选',            '视频', '图生视频,商业级',  '🎬', '#f5c542'),
('MiniMax Video-01', 'minimax',    'video', 'minimax-v1',    45,  'MiniMax 出品，中文提示词理解优秀',            '视频', '文生视频,中文友好','🎬', '#ff9966'),
('WanVideo 2.1',     'alibaba',    'video', 'wan-2.1',       35,  '阿里巴巴开源视频模型，本地部署友好',           '视频', '开源,文生视频',    '🎬', '#42b883'),
('CogVideoX 2.0',    'zhipu',      'video', 'cogvideox-2',   40,  '智谱 AI 开源视频模型，高帧率流畅输出',         '视频', '开源,高帧率',      '🎬', '#d07ff5'),
('Stable Diffusion XL','stability','image', 'sdxl',           4,  '最流行开源图像模型，支持 LoRA 精细控制',       '图像', '开源,定制化',      '🖼️', '#ff9966'),
('Recraft V3',       'recraft',    'image', 'recraft-v3',     6,  '矢量图与品牌设计专用模型',                    '图像', '设计,SVG',         '🖼️', '#ff6b6b'),
('ElevenLabs TTS',   'elevenlabs', 'audio', 'elevenlabs-tts', 3,  '最自然的 AI 语音合成，支持 30+ 语言',         '音频', 'TTS,多语言',       '🔊', '#646cff'),
('Suno v4',          'suno',       'audio', 'suno-v4',        8,  'AI 音乐生成领导者，完整歌曲一键生成',          '音频', '音乐生成,AI作曲',  '🎵', '#42b883'),
('Udio',             'udio',       'audio', 'udio-v1',        8,  '专业级 AI 音乐创作，风格多样',                 '音频', '音乐生成,高质量',  '🎵', '#f5c542'),
('GPT-4o',           'openai',     'text',  'gpt-4o',        15,  'OpenAI 最强多模态模型，理解与生成均衡',        '文本', '多模态,对话',      '💬', '#7fd1f5'),
('Claude 3.7 Sonnet','anthropic',  'text',  'claude-3.7',    12,  '超强推理能力，长文本处理首选',                 '文本', '推理,写作',        '💬', '#ff9966'),
('Gemini 2.5 Pro',   'google',     'text',  'gemini-2.5',    15,  'Google 最新旗舰，代码与多模态能力卓越',        '文本', '多模态,代码',      '💬', '#42b883'),
('Qwen Max',         'alibaba',    'text',  'qwen-max',      10,  '阿里通义千问旗舰，中文理解能力业界领先',       '文本', '中文,长上下文',    '💬', '#ff6b6b'),
('DeepSeek R2',      'deepseek',   'text',  'deepseek-r2',    8,  '国产开源推理模型，数学与逻辑能力突出',         '文本', '推理,开源',        '💬', '#d07ff5');

-- 4. 创建用户模型库表
CREATE TABLE IF NOT EXISTS t_user_model_library (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    model_id     BIGINT       NULL     COMMENT '关联 t_ai_model.id，自定义模型时为 NULL',
    is_custom    TINYINT      NOT NULL DEFAULT 0 COMMENT '0-平台模型 1-自定义模型',
    name         VARCHAR(100) NULL     COMMENT '模型名称（冗余）',
    category     VARCHAR(20)  NULL     COMMENT '分类',
    description  VARCHAR(500) NULL     COMMENT '描述',
    api_endpoint VARCHAR(500) NULL     COMMENT '自定义模型 API 地址',
    api_key      VARCHAR(500) NULL     COMMENT '自定义模型 API Key',
    icon         VARCHAR(10)  NULL     COMMENT 'emoji 图标',
    color        VARCHAR(20)  NULL     COMMENT '主题色',
    enabled      TINYINT      NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time  DATETIME     NULL     DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_model (user_id, model_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户模型库';
