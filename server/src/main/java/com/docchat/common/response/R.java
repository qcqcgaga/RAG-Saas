package com.docchat.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应封装
 *
 * 所有 API 响应必须使用此类封装，确保前端收到统一的 JSON 结构：
 * {
 *   "code": 0,        // 0 表示成功，非 0 表示失败
 *   "msg": "success", // 提示信息
 *   "data": {...}     // 业务数据（失败时为 null）
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        return new R<>(0, "success", data);
    }

    public static <T> R<T> ok() {
        return new R<>(0, "success", null);
    }

    public static <T> R<T> fail(String errorCode, String errorMessage) {
        return new R<>(errorCode.hashCode(), errorMessage, null);
    }
}
