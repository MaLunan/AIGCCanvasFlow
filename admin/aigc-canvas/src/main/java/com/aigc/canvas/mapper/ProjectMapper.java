package com.aigc.canvas.mapper;

import com.aigc.canvas.entity.Project;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper：继承 MyBatis-Plus BaseMapper，自动提供 CRUD 方法
 * 无需编写 XML，常用操作（selectById/insert/updateById/deleteById/selectPage）均由框架生成
 * @TableLogic 注解保证逻辑删除自动生效（deleted=1 的记录不会被查询到）
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
