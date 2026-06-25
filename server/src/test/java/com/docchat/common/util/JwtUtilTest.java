package com.docchat.common.util;

import com.docchat.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String SECRET = "test-secret-key-must-be-at-least-32-chars!!";

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpiration(86400);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    @DisplayName("生成并解析Token - 完整往返")
    void generateAndParseToken_roundtrip() {
        String token = jwtUtil.generateToken(1L, 100L, "ADMIN");
        Claims claims = jwtUtil.parseToken(token);

        assertThat(jwtUtil.getUserId(claims)).isEqualTo(1L);
        assertThat(jwtUtil.getTenantId(claims)).isEqualTo(100L);
        assertThat(jwtUtil.getRole(claims)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("不同参数生成不同Token")
    void generateToken_differentParams_differentTokens() {
        String token1 = jwtUtil.generateToken(1L, 100L, "ADMIN");
        String token2 = jwtUtil.generateToken(2L, 100L, "MEMBER");
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("validateToken - 有效Token返回true")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, 100L, "ADMIN");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken - 畸形Token返回false")
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.valid.token")).isFalse();
    }

    @Test
    @DisplayName("validateToken - 空字符串返回false")
    void validateToken_emptyToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken - 篡改Token返回false")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(1L, 100L, "ADMIN");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken - null返回false")
    void validateToken_nullToken_returnsFalse() {
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("getUserId - 正确提取userId")
    void getUserId_correctExtraction() {
        String token = jwtUtil.generateToken(42L, 100L, "ADMIN");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(jwtUtil.getUserId(claims)).isEqualTo(42L);
    }

    @Test
    @DisplayName("getTenantId - 正确提取tenantId")
    void getTenantId_correctExtraction() {
        String token = jwtUtil.generateToken(1L, 999L, "MEMBER");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(jwtUtil.getTenantId(claims)).isEqualTo(999L);
    }

    @Test
    @DisplayName("getRole - 正确提取role")
    void getRole_correctExtraction() {
        String token = jwtUtil.generateToken(1L, 100L, "MEMBER");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(jwtUtil.getRole(claims)).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("parseToken - 不抛异常表示签名验证通过")
    void parseToken_validToken_noException() {
        String token = jwtUtil.generateToken(1L, 100L, "ADMIN");
        assertThatNoException().isThrownBy(() -> jwtUtil.parseToken(token));
    }

    @Test
    @DisplayName("过期Token验证失败")
    void validateToken_expiredToken_returnsFalse() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpiration(0); // 立即过期
        JwtUtil expiringUtil = new JwtUtil(props);

        String token = expiringUtil.generateToken(1L, 100L, "ADMIN");
        assertThat(expiringUtil.validateToken(token)).isFalse();
    }
}
