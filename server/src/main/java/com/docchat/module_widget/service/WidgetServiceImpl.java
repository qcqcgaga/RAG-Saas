package com.docchat.module_widget.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_widget.dto.*;
import com.docchat.module_widget.entity.WidgetConfig;
import com.docchat.module_widget.repository.WidgetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetServiceImpl implements WidgetService {

    private final WidgetConfigRepository widgetConfigRepository;

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
            config.setEnabled(request.getEnabled());
        }

        config = widgetConfigRepository.save(config);
        log.info("组件配置已更新: tenantId={}", tenantId);
        return toConfigResponse(config);
    }

    @Override
    public EmbedScriptResponse getEmbedScript() {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        WidgetConfig config = widgetConfigRepository.findByTenantId(tenantId)
            .orElseThrow(() -> ErrorCode.WIDGET_NOT_FOUND.asBizException());

        String script = "<script src=\"https://cdn.docchat.com/widget.js\" data-token=\""
            + config.getWidgetToken() + "\"></script>";
        String previewUrl = "https://preview.docchat.com/" + config.getWidgetToken();

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
        WidgetConfig config = widgetConfigRepository.findByTenantId(tenantId)
            .orElseThrow(() -> ErrorCode.WIDGET_NOT_FOUND.asBizException());

        config.setWidgetToken(generateToken());
        widgetConfigRepository.save(config);

        log.info("Widget Token 已重新生成: tenantId={}", tenantId);
        return TokenResponse.builder().token(config.getWidgetToken()).build();
    }

    private WidgetConfig createDefaultConfig(Long tenantId) {
        return WidgetConfig.builder()
            .tenantId(tenantId)
            .widgetToken(generateToken())
            .build();
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private WidgetConfigResponse toConfigResponse(WidgetConfig config) {
        return WidgetConfigResponse.builder()
            .brandColor(config.getBrandColor())
            .welcomeMessage(config.getWelcomeMessage())
            .iconUrl(config.getIconUrl())
            .enabled(config.getEnabled())
            .build();
    }
}
