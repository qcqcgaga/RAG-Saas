package com.docchat.module_task.controller;

import com.docchat.common.response.PageResult;
import com.docchat.common.response.R;
import com.docchat.module_task.dto.TaskDetailResponse;
import com.docchat.module_task.dto.TaskResponse;
import com.docchat.module_task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public R<PageResult<TaskResponse>> listTasks(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return R.ok(taskService.listTasks(page, size));
    }

    @GetMapping("/{taskId}")
    public R<TaskDetailResponse> getTask(@PathVariable Long taskId) {
        return R.ok(taskService.getTask(taskId));
    }

    @PostMapping("/{taskId}/retry")
    public R<TaskResponse> retryTask(@PathVariable Long taskId) {
        return R.ok(taskService.retryTask(taskId));
    }
}
