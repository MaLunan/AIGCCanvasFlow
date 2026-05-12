package com.aigc.canvas.service.impl;

import com.aigc.canvas.dto.TemplateVO;
import com.aigc.canvas.entity.Project;
import com.aigc.canvas.entity.Template;
import com.aigc.canvas.mapper.ProjectMapper;
import com.aigc.canvas.mapper.TemplateMapper;
import com.aigc.canvas.service.TemplateService;
import com.aigc.common.enums.ResultCode;
import com.aigc.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模板服务实现：平台模板的查询和使用
 * 模板数据由管理员维护，用户只可查看和使用，不能修改
 */
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;
    // 使用模板时需要插入新项目
    private final ProjectMapper projectMapper;

    /**
     * 分页查询模板列表（只返回 status=1 的上线模板）
     * - category 过滤：可选，按内容类型筛选
     * - hot 过滤：可选，true=只显示热门模板
     * - 按使用次数倒序排列（热门模板排在前面）
     */
    @Override
    public Page<TemplateVO> page(int current, int size, String category, Boolean hot) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(Template::getStatus, 1)  // 只查上线模板
                .eq(StringUtils.hasText(category), Template::getCategory, category)
                // hot 有值时才加过滤，Boolean → int 转换（true=1，false=0）
                .eq(hot != null, Template::getHot, hot ? 1 : 0)
                .orderByDesc(Template::getUseCount); // 使用次数多的排在前面
        Page<Template> dbPage = templateMapper.selectPage(new Page<>(current, size), wrapper);
        Page<TemplateVO> voPage = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        voPage.setRecords(dbPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 获取模板详情（含 canvasData 字段）
     * 下线的模板（status != 1）视为不存在，返回 NOT_FOUND
     */
    @Override
    public TemplateVO getById(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null || template.getStatus() != 1)
            throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(template);
    }

    /**
     * 使用模板创建新项目
     * 流程：
     * 1. 模板使用次数 +1（热度统计）
     * 2. 复制模板的 canvasData 到新项目
     * 3. 返回新项目 ID，前端直接跳转到画布编辑页
     *
     * @param projectName 新项目名称（可选，默认使用模板名称）
     * @return 新创建项目的 ID
     */
    @Override
    public Long useTemplate(Long templateId, Long userId, String projectName) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) throw new BusinessException(ResultCode.NOT_FOUND);

        // 模板使用次数 +1，用于热门排序
        template.setUseCount(template.getUseCount() + 1);
        templateMapper.updateById(template);

        // 基于模板 canvasData 创建新项目
        Project project = new Project();
        project.setUserId(userId);
        // 项目名称：用户传入的名称 > 模板名称
        project.setName(StringUtils.hasText(projectName) ? projectName : template.getName());
        project.setCategory(template.getCategory());
        project.setCover(template.getCover());
        project.setCanvasData(template.getCanvasData()); // 复制画布数据
        project.setFrameCount(0);  // 新项目帧数从 0 开始（前端会重新统计）
        project.setStatus(0);      // 草稿状态
        projectMapper.insert(project);
        return project.getId();    // 返回新项目 ID，前端用于跳转 /canvas?projectId=xxx
    }

    /** Template Entity → TemplateVO 转换 */
    private TemplateVO toVO(Template t) {
        TemplateVO vo = new TemplateVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }
}
