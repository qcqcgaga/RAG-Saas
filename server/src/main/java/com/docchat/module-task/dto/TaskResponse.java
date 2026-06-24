package com.docchat.module_task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String taskType;
    private String status;
    private Short progress;
    private Short retryCount;
    private Short maxRetry;
}
