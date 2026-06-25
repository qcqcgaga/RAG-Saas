package com.docchat.module_task.repository;

import com.docchat.module_task.entity.AsyncTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsyncTaskRepository extends JpaRepository<AsyncTask, Long> {

    Page<AsyncTask> findByTenantId(Long tenantId, Pageable pageable);

    Page<AsyncTask> findByTenantIdAndStatus(Long tenantId, String status, Pageable pageable);
}
