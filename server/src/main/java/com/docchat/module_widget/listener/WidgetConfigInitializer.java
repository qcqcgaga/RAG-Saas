package com.docchat.module_widget.listener;

import com.docchat.common.event.TenantCreatedEvent;
import com.docchat.module_widget.entity.WidgetConfig;
import com.docchat.module_widget.repository.WidgetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WidgetConfigInitializer {

    private final WidgetConfigRepository widgetConfigRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        WidgetConfig config = new WidgetConfig();
        config.setTenantId(event.getTenantId());
        config.setWidgetToken(UUID.randomUUID().toString().replace("-", ""));
        config.setBrandColor("#1890ff");
        config.setWelcomeMessage("您好！有什么可以帮您？");
        config.setEnabled((short) 1);
        widgetConfigRepository.save(config);

        log.info("自动创建WidgetConfig: tenantId={}", event.getTenantId());
    }
}
