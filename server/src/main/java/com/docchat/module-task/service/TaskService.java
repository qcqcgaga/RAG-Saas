package com.docchat.module_task.service;

import com.docchat.common.response.PageResult;
import com.docchat.module_task.dto.TaskDetailResponse;
import com.docchat.module_task.dto.TaskResponse;
import com.docchat.module_task.entity.AsyncTask;

public interface TaskService {

    AsyncTask createTask(String taskType, Long documentId);

    PageResult<TaskResponse> listTasks(int page, int size);

    TaskDetailResponse getTask(Long taskId);

    TaskResponse retryTask(Long taskId);

    void updateTaskProgress(Long taskId, int progress);

    void updateTaskStatus(Long taskId, String status, String errorMessage);
}
