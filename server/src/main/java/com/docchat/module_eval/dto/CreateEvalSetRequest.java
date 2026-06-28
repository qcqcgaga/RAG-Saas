package com.docchat.module_eval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEvalSetRequest {

    @NotBlank(message = "评测集名称不能为空")
    @Size(min = 1, max = 100, message = "评测集名称1-100字符")
    private String name;

    @Size(max = 500, message = "描述最多500字符")
    private String description;
}
