package com.docchat.module_widget.repository;

import com.docchat.module_widget.entity.WidgetConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WidgetConfigRepository extends JpaRepository<WidgetConfig, Long> {

    Optional<WidgetConfig> findByWidgetToken(String widgetToken);

    Optional<WidgetConfig> findByTenantId(Long tenantId);
}
