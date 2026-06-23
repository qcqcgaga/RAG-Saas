package com.docchat.common.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页结果封装
 *
 * 用于列表查询接口的分页响应：
 * {
 *   "list": [...],    // 当前页数据
 *   "total": 100,     // 总记录数
 *   "page": 1,        // 当前页码（从 1 开始）
 *   "size": 20        // 每页大小
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }
}
