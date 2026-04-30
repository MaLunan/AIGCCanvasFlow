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

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;
    private final ProjectMapper projectMapper;

    @Override
    public Page<TemplateVO> page(int current, int size, String category, Boolean hot) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<Template>()
                .eq(Template::getStatus, 1)
                .eq(StringUtils.hasText(category), Template::getCategory, category)
                .eq(hot != null, Template::getHot, hot ? 1 : 0)
                .orderByDesc(Template::getUseCount);
        Page<Template> dbPage = templateMapper.selectPage(new Page<>(current, size), wrapper);
        Page<TemplateVO> voPage = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        voPage.setRecords(dbPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public TemplateVO getById(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null || template.getStatus() != 1)
            throw new BusinessException(ResultCode.NOT_FOUND);
        return toVO(template);
    }

    @Override
    public Long useTemplate(Long templateId, Long userId, String projectName) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) throw new BusinessException(ResultCode.NOT_FOUND);

        // 增加使用次数
        template.setUseCount(template.getUseCount() + 1);
        templateMapper.updateById(template);

        // 基于模板创建新项目
        Project project = new Project();
        project.setUserId(userId);
        project.setName(StringUtils.hasText(projectName) ? projectName : template.getName());
        project.setCategory(template.getCategory());
        project.setCover(template.getCover());
        project.setCanvasData(template.getCanvasData());
        project.setFrameCount(0);
        project.setStatus(0);
        projectMapper.insert(project);
        return project.getId();
    }

    private TemplateVO toVO(Template t) {
        TemplateVO vo = new TemplateVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }
}
