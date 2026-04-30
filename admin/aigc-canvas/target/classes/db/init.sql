CREATE DATABASE IF NOT EXISTS aigc_canvas DEFAULT CHARACTER SET utf8mb4;
USE aigc_canvas;

-- 项目表
CREATE TABLE IF NOT EXISTS t_project (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '项目ID',
    user_id      BIGINT NOT NULL COMMENT '用户ID',
    name         VARCHAR(100) NOT NULL COMMENT '项目名称',
    cover        VARCHAR(500) COMMENT '封面图URL',
    category     VARCHAR(50) COMMENT 'short_drama/oral/ad/mv/vlog/other',
    canvas_data  LONGTEXT COMMENT '画布JSON数据',
    frame_count  INT NOT NULL DEFAULT 0 COMMENT '分镜数量',
    status       TINYINT NOT NULL DEFAULT 0 COMMENT '0-草稿 1-已发布',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_user_category (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布项目表';

-- 模板表
CREATE TABLE IF NOT EXISTS t_template (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    name         VARCHAR(100) NOT NULL COMMENT '模板名称',
    category     VARCHAR(50) COMMENT 'short_drama/oral/ad/mv/vlog/edu/other',
    cover        VARCHAR(500) COMMENT '封面图URL',
    description  VARCHAR(500) COMMENT '模板描述',
    canvas_data  LONGTEXT COMMENT '模板画布JSON',
    use_count    INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    hot          TINYINT NOT NULL DEFAULT 0 COMMENT '0-普通 1-热门',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '0-下线 1-上线',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT NOT NULL DEFAULT 0,
    INDEX idx_category (category),
    INDEX idx_hot (hot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流模板表';

-- AI 模型表
CREATE TABLE IF NOT EXISTS t_ai_model (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型ID',
    name                VARCHAR(100) NOT NULL COMMENT '展示名称',
    provider            VARCHAR(50) COMMENT '供应商',
    type                VARCHAR(20) NOT NULL COMMENT 'video/image/audio',
    model_key           VARCHAR(100) NOT NULL UNIQUE COMMENT '模型API标识符',
    support_aspects     VARCHAR(200) COMMENT '支持比例，逗号分隔',
    support_durations   VARCHAR(100) COMMENT '支持时长，逗号分隔（秒）',
    support_resolutions VARCHAR(100) COMMENT '支持分辨率，逗号分隔',
    cost_points         INT NOT NULL DEFAULT 10 COMMENT '每次消耗算力点',
    description         VARCHAR(500) COMMENT '模型简介',
    status              TINYINT NOT NULL DEFAULT 1 COMMENT '0-下线 1-上线',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             TINYINT NOT NULL DEFAULT 0,
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型表';

-- 资产表
CREATE TABLE IF NOT EXISTS t_asset (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '资产ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    name        VARCHAR(200) NOT NULL COMMENT '资产名称',
    type        VARCHAR(50) NOT NULL DEFAULT 'other' COMMENT 'character/style/music/storyboard/other',
    url         VARCHAR(1000) NOT NULL COMMENT '文件URL',
    thumb       VARCHAR(1000) COMMENT '缩略图URL',
    file_size   BIGINT COMMENT '文件大小（字节）',
    ext         VARCHAR(20) COMMENT '文件扩展名',
    meta        JSON COMMENT '扩展元信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资产表';

-- 初始模型数据
INSERT IGNORE INTO t_ai_model (name, provider, type, model_key, support_aspects, support_durations, support_resolutions, cost_points, description) VALUES
('Kling 3.0',     'kuaishou', 'video', 'kling-v3',      '16:9,9:16,1:1',     '5,10',     '720p,1080p', 50,  '快手可灵 3.0，高质量视频生成'),
('Seedance 2.0',  'bytedance','video', 'seedance-v2',   '16:9,9:16,1:1,4:3', '5,10,15',  '720p,1080p', 60,  '字节跳动 Seedance 2.0'),
('Vidu 2.0',      'shengshu', 'video', 'vidu-v2',       '16:9,9:16,1:1',     '4,8',      '720p,1080p', 45,  '生数科技 Vidu 2.0'),
('Sora',          'openai',   'video', 'sora-v1',       '16:9,9:16,1:1',     '5,10,20',  '720p,1080p', 80,  'OpenAI Sora'),
('Runway Gen-3',  'runway',   'video', 'runway-gen3',   '16:9,9:16',         '5,10',     '720p,1080p', 55,  'Runway Gen-3 Alpha'),
('Pika 2.0',      'pika',     'video', 'pika-v2',       '16:9,9:16,1:1',     '3,5,10',   '720p,1080p', 40,  'Pika 2.0'),
('Flux 1.1 Pro',  'blackforest','image','flux-pro-1.1', '1:1,16:9,9:16,4:3,3:4', null, null,         5,   'Flux 1.1 Pro 图像生成'),
('DALL·E 3',      'openai',   'image', 'dalle-3',       '1:1,16:9,9:16',     null,       null,         8,   'OpenAI DALL·E 3'),
('Midjourney v6', 'midjourney','image','mj-v6',         '1:1,16:9,9:16,4:3', null,       null,         10,  'Midjourney v6'),
('Ideogram 2.0',  'ideogram', 'image', 'ideogram-v2',   '1:1,16:9,9:16',     null,       null,         6,   'Ideogram 2.0');

-- 初始模板数据
INSERT IGNORE INTO t_template (name, category, description, use_count, hot, status) VALUES
('悬疑短剧模板',   'short_drama', '经典悬疑反转结构，包含开场钩子、主线冲突、高潮反转三幕式框架', 12400, 1, 1),
('情感口播模板',   'oral',        '情感共鸣型口播，适合情感博主、心理账号快速产出内容', 8900, 1, 1),
('商品广告模板',   'ad',          '15-30秒电商广告，痛点展示→产品方案→行动号召结构', 21200, 1, 1),
('Vlog 旅行模板',  'vlog',        '旅行 Vlog 剪辑结构，含出发期待→过程体验→回忆总结节奏', 5600, 0, 1),
('知识分享模板',   'edu',         '知识科普型短视频，三段论讲解结构，适合教育类账号', 14100, 1, 1),
('古风 MV 模板',   'mv',          '古风音乐视频框架，画面与歌词节点对应，含转场提示', 9300, 0, 1),
('品牌宣传片模板', 'ad',          '60秒品牌故事结构，品牌起源→核心价值→产品场景→号召', 6800, 0, 1),
('搞笑段子模板',   'oral',        '搞笑反转段子结构，铺垫→发展→意外反转三节奏', 18700, 1, 1);
