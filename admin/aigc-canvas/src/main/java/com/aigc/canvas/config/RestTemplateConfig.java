package com.aigc.canvas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置：用于调用 LangChain Python 服务的 HTTP 客户端
 * 显式配置超时时间，避免使用默认（无超时，可能导致线程永久阻塞）
 */
@Configuration
public class RestTemplateConfig {

    /** 连接超时（毫秒）：等待建立 TCP 连接的最大时间，默认 5 秒 */
    @Value("${langchain.connect-timeout:5000}")
    private int connectTimeout;

    /**
     * 读取超时（毫秒）：等待服务器响应数据的最大时间，默认 120 秒
     * AI 生图/生视频是同步接口，需要较长等待时间（部分模型需要 30~60 秒）
     * 文字润化（polish）也可能需要 10~30 秒
     */
    @Value("${langchain.read-timeout:120000}")
    private int readTimeout;

    /**
     * 创建 RestTemplate Bean，注入 LangChainClient
     * 使用 SimpleClientHttpRequestFactory（JDK HttpURLConnection），轻量无需额外依赖
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
