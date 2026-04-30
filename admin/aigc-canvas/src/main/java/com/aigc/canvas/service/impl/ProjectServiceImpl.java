package com.aigc.canvas.service.impl;

import com.aigc.canvas.dto.ProjectCreateRequest;
import com.aigc.canvas.dto.ProjectUpdateRequest;
import com.aigc.canvas.dto.ProjectVO;
import com.aigc.canvas.entity.Project;
import com.aigc.canvas.mapper.ProjectMapper;
import com.aigc.canvas.service.ProjectService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    @Override
    public ProjectVO create(Long userId, ProjectCreateRequest request) {
        Project project = new Project();
        BeanUtils.copyProperties(request, project);
        project.setUserId(userId);
        project.setStatus(0);
        project.setFrameCount(0);
        projectMapper.insert(project);
        return toVO(project);
    }

    @Override
    public ProjectVO getById(Long id, Long userId) {
        Project project = getAndCheck(id, userId);
        return toVO(project);
    }

    @Override
    public Page<ProjectVO> page(Long userId, int current, int size, String category) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .eq(StringUtils.hasText(category), Project::getCategory, category)
                .orderByDesc(Project::getUpdateTime);
        Page<Project> dbPage = projectMapper.selectPage(new Page<>(current, size), wrapper);
        Page<ProjectVO> voPage = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        voPage.setRecords(dbPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public ProjectVO update(Long id, Long userId, ProjectUpdateRequest request) {
        Project project = getAndCheck(id, userId);
        if (StringUtils.hasText(request.getName())) project.setName(request.getName());
        if (StringUtils.hasText(request.getCover())) project.setCover(request.getCover());
        if (StringUtils.hasText(request.getCategory())) project.setCategory(request.getCategory());
        if (request.getFrameCount() != null) project.setFrameCount(request.getFrameCount());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        projectMapper.updateById(project);
        return toVO(project);
    }

    @Override
    public void delete(Long id, Long userId) {
        getAndCheck(id, userId);
        projectMapper.deleteById(id);
    }

    @Override
    public void saveCanvas(Long id, Long userId, String canvasData) {
        Project project = getAndCheck(id, userId);
        project.setCanvasData(canvasData);
        projectMapper.updateById(project);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Project getAndCheck(Long id, Long userId) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!project.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        return project;
    }

    private ProjectVO toVO(Project p) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }
}
