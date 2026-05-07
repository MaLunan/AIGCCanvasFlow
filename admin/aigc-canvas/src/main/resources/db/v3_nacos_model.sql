-- ============================================================
-- V3: 模型广场迁移至 Nacos — t_user_model_library 增加 model_key
-- ============================================================
USE aigc_canvas;

-- 1. 新增 model_key 列（平台模型标识，对应 Nacos aigc.models.{key}）
ALTER TABLE t_user_model_library
    ADD COLUMN model_key VARCHAR(100) NULL COMMENT '平台模型 key（Nacos model_key），自定义模型为 NULL' AFTER model_id;

-- 2. 基于 model_key 的唯一约束（替代原 model_id 唯一约束）
ALTER TABLE t_user_model_library
    ADD UNIQUE KEY uk_user_model_key (user_id, model_key, deleted);

-- 3. 如有历史数据，可按需从 t_ai_model 回填 model_key（可选）
-- UPDATE t_user_model_library ul
--     JOIN t_ai_model am ON ul.model_id = am.id
-- SET ul.model_key = am.model_key
-- WHERE ul.is_custom = 0 AND ul.deleted = 0;
