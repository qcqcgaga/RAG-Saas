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
public class EvalPairResponse {

    private Long id;
    private Long evalSetId;
    private String question;
    private String expectedDocument;
    private int sortOrder;
    private OffsetDateTime createdAt;
}
