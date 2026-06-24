package com.docchat.module_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {
    private String documentName;
    private int chunkIndex;
    private String content;
    private double score;
}
