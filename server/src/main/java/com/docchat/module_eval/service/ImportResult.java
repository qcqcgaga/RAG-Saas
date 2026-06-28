package com.docchat.module_eval.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 批量导入结果
 */
@Data
@AllArgsConstructor
public class ImportResult {
    /** 成功导入的数量 */
    private int imported;
    /** 导入后总数 */
    private int total;
}
