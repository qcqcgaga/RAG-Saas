package com.docchat.module_tenant.repository;

import com.docchat.module_tenant.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    long countByTenantId(Long tenantId);

    Page<User> findByTenantId(Long tenantId, Pageable pageable);
}
