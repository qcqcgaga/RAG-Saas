package com.docchat.module_apikey.repository;

import com.docchat.module_apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * API Key 数据访问层
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /** 根据 Key 哈希查找（用于鉴权校验） */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /** 查找租户下指定状态的 Key 列表 */
    List<ApiKey> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, Integer status);

    /** 统计租户下指定状态的 Key 数量 */
    long countByTenantIdAndStatus(Long tenantId, Integer status);
}
