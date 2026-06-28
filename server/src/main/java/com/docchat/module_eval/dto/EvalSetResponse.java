package com.docchat.module_eval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalSetResponse {

    private Long id;
    private String name;
    private String description;
    private int pairCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
