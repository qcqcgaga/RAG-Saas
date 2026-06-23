package com.docchat.common.constant;

/**
 * 通用常量定义
 *
 * 只放真正跨 3+ 模块使用的常量，模块专属常量在模块内定义。
 */
public final class CommonConstant {

    private CommonConstant() {
        // 禁止实例化
    }

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 最大分页大小 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 文件上传最大大小（字节）— 50MB */
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    /** 允许的文件类型白名单 */
    public static final String[] ALLOWED_FILE_TYPES = {"pdf", "md", "txt"};

    /** JWT Token 请求头 */
    public static final String AUTH_HEADER = "Authorization";

    /** JWT Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求 ID MDC Key */
    public static final String REQUEST_ID_KEY = "requestId";

    /** 租户 ID MDC Key */
    public static final String TENANT_ID_KEY = "tenantId";
}
