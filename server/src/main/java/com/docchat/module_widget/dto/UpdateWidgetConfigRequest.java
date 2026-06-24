package com.docchat.module_widget.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWidgetConfigRequest {

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "品牌色必须为 #HEX 格式")
    private String brandColor;

    @Size(min = 1, max = 200, message = "欢迎语长度必须在1-200字符之间")
    private String welcomeMessage;

    private String iconUrl;

    private Short enabled;
}
