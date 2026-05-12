package com.aigc.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建项目请求 DTO
 * 前端新建项目时发送，Service 层基于此创建 Project 实体
 */
@Data
public class ProjectCreateRequest {

    /** 项目名称：必填，最长 100 字符 */
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最长 100 字符")
    private String name;

    /** 封面图 URL（可选，创建时通常为空，保存后由前端更新） */
    private String cover;

    /** 项目分类（可选，如 short_drama / oral / ad 等） */
    private String category;

    /** 初始画布数据（可选，使用模板时携带，新建时通常为空） */
    private String canvasData;
}
