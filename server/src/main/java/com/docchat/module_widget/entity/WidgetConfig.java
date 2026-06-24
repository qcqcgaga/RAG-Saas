package com.docchat.module_widget.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "widget_configs")
public class WidgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    @Column(name = "brand_color", nullable = false, length = 7)
    private String brandColor;

    @Column(name = "welcome_message", nullable = false, length = 200)
    private String welcomeMessage;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "widget_token", nullable = false, unique = true, length = 64)
    private String widgetToken;

    @Column(nullable = false)
    private Short enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (brandColor == null) brandColor = "#1890ff";
        if (welcomeMessage == null) welcomeMessage = "你好，有什么可以帮你的？";
        if (enabled == null) enabled = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
