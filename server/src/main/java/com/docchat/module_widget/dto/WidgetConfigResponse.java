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
    private Short enabled;
}
