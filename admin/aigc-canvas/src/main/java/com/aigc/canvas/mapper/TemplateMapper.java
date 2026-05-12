package com.aigc.canvas.mapper;

import com.aigc.canvas.entity.Template;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板 Mapper：继承 BaseMapper，提供自动 CRUD
 * 模板是平台数据（管理员维护），用户只有读权限
 */
@Mapper
public interface TemplateMapper extends BaseMapper<Template> {
}
