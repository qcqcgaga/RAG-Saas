package com.docchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * DocChat 应用入口
 *
 * 文档智能客服 SaaS — 面向独立开发者/小团队
 * 上传文档 → 自动切分向量化 → RAG 对话 → 嵌入聊天组件
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class DocChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocChatApplication.class, args);
    }
}
