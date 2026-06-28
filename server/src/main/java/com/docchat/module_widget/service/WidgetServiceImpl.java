package com.docchat.module_widget.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_widget.dto.*;
import com.docchat.module_widget.entity.WidgetConfig;
import com.docchat.module_widget.repository.WidgetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetServiceImpl implements WidgetService {

    private final WidgetConfigRepository widgetConfigRepository;

    @Value("${docchat.widget.base-url:http://localhost:8080}")
    private String widgetBaseUrl;

    @Override
    public WidgetConfigResponse getConfigByToken(String token) {
        WidgetConfig config = widgetConfigRepository.findByWidgetToken(token)
            .orElseThrow(() -> ErrorCode.WIDGET_TOKEN_INVALID.asBizException());
        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public WidgetConfigResponse updateConfig(UpdateWidgetConfigRequest request) {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        WidgetConfig config = widgetConfigRepository.findByTenantId(tenantId)
            .orElseGet(() -> createDefaultConfig(tenantId));

        if (request.getBrandColor() != null) {
            config.setBrandColor(request.getBrandColor());
        }
        if (request.getWelcomeMessage() != null) {
            config.setWelcomeMessage(request.getWelcomeMessage());
        }
        if (request.getIconUrl() != null) {
            config.setIconUrl(request.getIconUrl());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled() ? (short) 1 : (short) 0);
        }

        config = widgetConfigRepository.save(config);
        log.info("组件配置已更新: tenantId={}", tenantId);
        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public EmbedScriptResponse getEmbedScript() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        WidgetConfig config = widgetConfigRepository.findByTenantId(tenantId)
            .orElseGet(() -> widgetConfigRepository.save(createDefaultConfig(tenantId)));

        // 嵌入脚本：data-api-key 同时用于获取配置和对话鉴权
        String script = "<script src=\"" + widgetBaseUrl + "/widget.js\" "
            + "data-api-key=\"" + config.getWidgetToken() + "\" "
            + "data-api-url=\"" + widgetBaseUrl + "\"></script>";
        String previewUrl = widgetBaseUrl + "/preview/" + config.getWidgetToken();

        return EmbedScriptResponse.builder()
            .script(script)
            .previewUrl(previewUrl)
            .build();
    }

    @Override
    @Transactional
    public TokenResponse regenerateToken() {
        if (!SecurityUtil.isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        Long tenantId = SecurityUtil.getCurrentTenantId();
        if (tenantId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "租户信息缺失，请重新登录");
        }
        WidgetConfig config = widgetConfigRepository.findByTenantId(tenantId)
            .orElseGet(() -> widgetConfigRepository.save(createDefaultConfig(tenantId)));

        config.setWidgetToken(generateToken());
        widgetConfigRepository.save(config);

        log.info("Widget Token 已重新生成: tenantId={}", tenantId);
        return TokenResponse.builder().token(config.getWidgetToken()).build();
    }

    private WidgetConfig createDefaultConfig(Long tenantId) {
        WidgetConfig config = new WidgetConfig();
        config.setTenantId(tenantId);
        config.setWidgetToken(generateToken());
        // @PrePersist 会设置 brandColor/welcomeMessage/enabled 默认值
        return config;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private WidgetConfigResponse toConfigResponse(WidgetConfig config) {
        return WidgetConfigResponse.builder()
            .brandColor(config.getBrandColor())
            .welcomeMessage(config.getWelcomeMessage())
            .iconUrl(config.getIconUrl())
            .enabled(config.getEnabled() != null && config.getEnabled() == 1)
            .build();
    }
}
