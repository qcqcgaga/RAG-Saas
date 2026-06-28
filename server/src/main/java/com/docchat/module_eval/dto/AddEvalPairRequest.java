package com.docchat.module_eval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddEvalPairRequest {

    @NotBlank(message = "问题不能为空")
    @Size(min = 1, max = 500, message = "问题1-500字符")
    private String question;

    @NotBlank(message = "期望文档不能为空")
    @Size(min = 1, max = 255, message = "期望文档1-255字符")
    private String expectedDocument;
}
