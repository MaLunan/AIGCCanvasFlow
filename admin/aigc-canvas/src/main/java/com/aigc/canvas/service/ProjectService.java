package com.aigc.canvas.service;

import com.aigc.canvas.dto.ProjectCreateRequest;
import com.aigc.canvas.dto.ProjectUpdateRequest;
import com.aigc.canvas.dto.ProjectVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ProjectService {
    ProjectVO create(Long userId, ProjectCreateRequest request);
    ProjectVO getById(Long id, Long userId);
    Page<ProjectVO> page(Long userId, int current, int size, String category);
    ProjectVO update(Long id, Long userId, ProjectUpdateRequest request);
    void delete(Long id, Long userId);
    /** 保存画布数据 */
    void saveCanvas(Long id, Long userId, String canvasData);
}
