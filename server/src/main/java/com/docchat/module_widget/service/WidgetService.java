package com.docchat.module_widget.service;

import com.docchat.module_widget.dto.*;

public interface WidgetService {

    /** 通过 widget_token 获取组件配置（公开接口） */
    WidgetConfigResponse getConfigByToken(String token);

    /** 更新当前租户的组件配置（自动获取或创建） */
    WidgetConfigResponse updateConfig(UpdateWidgetConfigRequest request);

    /** 获取嵌入脚本代码 */
    EmbedScriptResponse getEmbedScript();

    /** 重新生成 widget_token */
    TokenResponse regenerateToken();
}
