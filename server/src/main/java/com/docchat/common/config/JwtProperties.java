package com.docchat.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "docchat.jwt")
public class JwtProperties {
    /** 密钥，从环境变量注入 */
    private String secret = "changeme-use-env-var-in-prod";
    /** Token 有效期（秒），默认 24h */
    private long expiration = 86400;
}
