package com.docchat.module_widget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WidgetConfigResponse {

    private String brandColor;
    private String welcomeMessage;
    private String iconUrl;
    /** 启用状态：true-启用 false-禁用（序列化为 boolean 给前端） */
    private Boolean enabled;
}
