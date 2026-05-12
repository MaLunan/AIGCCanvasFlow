package com.aigc.canvas.service;

import com.aigc.canvas.dto.ProjectCreateRequest;
import com.aigc.canvas.dto.ProjectUpdateRequest;
import com.aigc.canvas.dto.ProjectVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 项目服务接口：定义项目 CRUD + 画布数据自动保存
 * 所有写操作均需校验项目归属（userId 匹配），防止越权操作
 * 实现类：ProjectServiceImpl
 */
public interface ProjectService {
    /** 创建新项目 */
    ProjectVO create(Long userId, ProjectCreateRequest request);
    /** 查询项目详情（校验归属） */
    ProjectVO getById(Long id, Long userId);
    /** 分页查询用户项目列表 */
    Page<ProjectVO> page(Long userId, int current, int size, String category);
    /** 更新项目基本信息（部分更新） */
    ProjectVO update(Long id, Long userId, ProjectUpdateRequest request);
    /** 删除项目（校验归属后硬删除/逻辑删除） */
    void delete(Long id, Long userId);
    /** 保存画布 JSON 数据（前端 1.5s 防抖自动触发） */
    void saveCanvas(Long id, Long userId, String canvasData);
}
