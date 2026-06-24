package com.docchat.module_chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {
    private String type;  // "token" | "done" | "error"
    private String content;  // token 内容或错误信息
    private List<SourceReference> sources;  // 仅 done 事件有值
}
