package com.docchat.module_widget.controller;

import com.docchat.common.response.R;
import com.docchat.module_widget.dto.*;
import com.docchat.module_widget.service.WidgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/widget")
@RequiredArgsConstructor
public class WidgetController {

    private final WidgetService widgetService;

    @GetMapping("/config")
    public R<WidgetConfigResponse> getConfig(@RequestParam String token) {
        return R.ok(widgetService.getConfigByToken(token));
    }

    @PutMapping("/config")
    public R<WidgetConfigResponse> updateConfig(@Valid @RequestBody UpdateWidgetConfigRequest request) {
        return R.ok(widgetService.updateConfig(request));
    }

    @GetMapping("/embed-script")
    public R<EmbedScriptResponse> getEmbedScript() {
        return R.ok(widgetService.getEmbedScript());
    }

    @PostMapping("/regenerate-token")
    public R<TokenResponse> regenerateToken() {
        return R.ok(widgetService.regenerateToken());
    }
}
