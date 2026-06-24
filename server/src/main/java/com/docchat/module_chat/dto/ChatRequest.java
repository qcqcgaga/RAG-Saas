package com.docchat.module_chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 500, message = "问题1-500字符")
    private String question;
}
