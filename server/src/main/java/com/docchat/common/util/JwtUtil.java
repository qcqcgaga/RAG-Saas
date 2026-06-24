package com.docchat.common.util;

import com.docchat.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(
            jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /** 生成 JWT Token */
    public String generateToken(Long userId, Long tenantId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("tenantId", tenantId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }

    /** 解析 Token，返回 Claims */
    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /** 从 Token 获取 userId */
    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /** 从 Token 获取 tenantId */
    public Long getTenantId(Claims claims) {
        return claims.get("tenantId", Long.class);
    }

    /** 从 Token 获取 role */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    /** 验证 Token 是否有效 */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
