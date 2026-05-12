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

/**
 * 项目服务实现：画布项目的 CRUD + 画布数据保存
 * 所有写操作均通过 getAndCheck 校验项目归属，防止越权
 */
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;

    /**
     * 创建新项目
     * status=0 表示草稿，frameCount=0 初始无节点
     * MyBatis-Plus 的 MetaObjectFillHandler 会自动填充 createTime/updateTime
     */
    @Override
    public ProjectVO create(Long userId, ProjectCreateRequest request) {
        Project project = new Project();
        BeanUtils.copyProperties(request, project); // 将请求 DTO 属性复制到实体
        project.setUserId(userId);
        project.setStatus(0);       // 0=草稿状态
        project.setFrameCount(0);   // 初始节点数为 0
        projectMapper.insert(project);
        return toVO(project);
    }

    /**
     * 根据 ID 查询项目（同时校验归属）
     * 项目不存在返回 NOT_FOUND，不属于当前用户返回 FORBIDDEN
     */
    @Override
    public ProjectVO getById(Long id, Long userId) {
        Project project = getAndCheck(id, userId);
        return toVO(project);
    }

    /**
     * 分页查询用户项目列表
     * - 仅查询当前用户的项目（eq userId）
     * - category 有值时追加过滤条件，无值则查全部
     * - 按 updateTime 倒序，最近编辑的项目排在前面
     */
    @Override
    public Page<ProjectVO> page(Long userId, int current, int size, String category) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                // StringUtils.hasText 为 true 时才加 category 条件（条件构造器的动态 SQL 技巧）
                .eq(StringUtils.hasText(category), Project::getCategory, category)
                .orderByDesc(Project::getUpdateTime);
        Page<Project> dbPage = projectMapper.selectPage(new Page<>(current, size), wrapper);
        // 将 Entity Page 转为 VO Page（避免泄露数据库字段）
        Page<ProjectVO> voPage = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        voPage.setRecords(dbPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 更新项目基本信息（部分更新，null 字段不覆盖）
     * 只更新非空字段，保留原有值
     */
    @Override
    public ProjectVO update(Long id, Long userId, ProjectUpdateRequest request) {
        Project project = getAndCheck(id, userId);
        if (StringUtils.hasText(request.getName()))     project.setName(request.getName());
        if (StringUtils.hasText(request.getCover()))    project.setCover(request.getCover());
        if (StringUtils.hasText(request.getCategory())) project.setCategory(request.getCategory());
        if (request.getFrameCount() != null)  project.setFrameCount(request.getFrameCount());
        if (request.getStatus() != null)      project.setStatus(request.getStatus());
        projectMapper.updateById(project);
        return toVO(project);
    }

    /** 删除项目（先校验归属，防止越权删除他人项目） */
    @Override
    public void delete(Long id, Long userId) {
        getAndCheck(id, userId);  // 仅做归属校验，不使用返回值
        projectMapper.deleteById(id);
    }

    /**
     * 保存画布数据（自动保存）
     * canvasData 为 VueFlow 序列化的 JSON 字符串，直接存储到 canvas_data 字段
     * updateTime 由 MyBatis-Plus 自动更新，前端用于显示"最后保存时间"
     */
    @Override
    public void saveCanvas(Long id, Long userId, String canvasData) {
        Project project = getAndCheck(id, userId);
        project.setCanvasData(canvasData);
        projectMapper.updateById(project);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * 查询并校验项目归属：项目不存在抛 NOT_FOUND，不属于 userId 抛 FORBIDDEN
     * 所有涉及项目操作的方法都应先调用此方法，统一处理权限校验
     */
    private Project getAndCheck(Long id, Long userId) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!project.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        return project;
    }

    /** Entity → VO 转换（BeanUtils 属性名映射复制） */
    private ProjectVO toVO(Project p) {
        ProjectVO vo = new ProjectVO();
        BeanUtils.copyProperties(p, vo);
        return vo;
    }
}
