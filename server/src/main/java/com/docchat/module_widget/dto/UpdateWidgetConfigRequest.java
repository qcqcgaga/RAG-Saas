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

    @Size(max = 200, message = "欢迎语长度不能超过200字符")
    private String welcomeMessage;

    private String iconUrl;

    /** 启用状态：1-启用 0-禁用，前端可能传 boolean 也可能传 number */
    private Boolean enabled;
}
