package com.aigc.canvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Canvas 服务启动类
 * 提供：项目管理 / 画布保存 / 模板 / AI 模型 / 资产上传 / AI Agent（生图/生视频/润化）
 * 通过 Nacos 注册，网关转发 /canvas/** 路径到本服务
 * scanBasePackages = "com.aigc" 确保扫描到 aigc-common 中的组件
 */
@SpringBootApplication(scanBasePackages = "com.aigc")
public class CanvasApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanvasApplication.class, args);
    }
}
