package com.docchat.module_task.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.response.PageResult;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_task.dto.TaskDetailResponse;
import com.docchat.module_task.dto.TaskResponse;
import com.docchat.module_task.entity.AsyncTask;
import com.docchat.module_task.repository.AsyncTaskRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final AsyncTaskRepository taskRepository;
    private final TaskQueueService taskQueueService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public AsyncTask createTask(String taskType, Long documentId) {
        Long tenantId = SecurityUtil.getCurrentTenantId();

        AsyncTask task = AsyncTask.builder()
            .tenantId(tenantId)
            .documentId(documentId)
            .taskType(taskType)
            .status("PENDING")
            .progress((short) 0)
            .maxRetry((short) 3)
            .retryCount((short) 0)
            .build();

        task = taskRepository.save(task);
        taskQueueService.pushTask(task);

        log.info("创建异步任务: taskId={}, type={}, documentId={}", task.getId(), taskType, documentId);
        return task;
    }

    @Override
    public PageResult<TaskResponse> listTasks(int page, int size) {
        Long tenantId = SecurityUtil.getCurrentTenantId();
        Page<AsyncTask> taskPage = taskRepository.findByTenantId(
            tenantId, PageRequest.of(page - 1, size)
        );

        var list = taskPage.getContent().stream()
            .map(this::toTaskResponse)
            .toList();

        return PageResult.of(list, taskPage.getTotalElements(), page, size);
    }

    @Override
    public TaskDetailResponse getTask(Long taskId) {
        AsyncTask task = findTaskAndCheckOwnership(taskId);
        return toTaskDetailResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse retryTask(Long taskId) {
        AsyncTask task = findTaskAndCheckOwnership(taskId);

        if (!"FAILED".equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_NOT_FAILED);
        }

        if (task.getRetryCount() >= task.getMaxRetry()) {
            throw new BizException(ErrorCode.TASK_MAX_RETRY_EXCEEDED);
        }

        task.setRetryCount((short) (task.getRetryCount() + 1));
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task.setProgress((short) 0);
        taskRepository.save(task);

        taskQueueService.pushTask(task);

        log.info("任务重试: taskId={}, retryCount={}", taskId, task.getRetryCount());
        return toTaskResponse(task);
    }

    @Override
    @Transactional
    public void updateTaskProgress(Long taskId, int progress) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setProgress((short) Math.min(100, Math.max(0, progress)));
            taskRepository.save(task);
        });
    }

    @Override
    @Transactional
    public void updateTaskStatus(Long taskId, String status, String errorMessage) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            if ("PROCESSING".equals(status)) {
                task.setStartedAt(OffsetDateTime.now());
            } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                task.setCompletedAt(OffsetDateTime.now());
                if ("COMPLETED".equals(status)) {
                    task.setProgress((short) 100);
                }
            }
            taskRepository.save(task);
        });
    }

    @Override
    @Transactional
    public void deleteTasksByDocumentId(Long documentId) {
        taskRepository.deleteByDocumentId(documentId);
        entityManager.flush();
        log.info("已删除文档关联的异步任务: documentId={}", documentId);
    }

    private AsyncTask findTaskAndCheckOwnership(Long taskId) {
        AsyncTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new BizException(ErrorCode.TASK_NOT_FOUND));

        Long currentTenantId = SecurityUtil.getCurrentTenantId();
        if (!task.getTenantId().equals(currentTenantId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return task;
    }

    private TaskResponse toTaskResponse(AsyncTask task) {
        return TaskResponse.builder()
            .id(task.getId())
            .taskType(task.getTaskType())
            .status(task.getStatus())
            .progress(task.getProgress())
            .retryCount(task.getRetryCount())
            .maxRetry(task.getMaxRetry())
            .build();
    }

    private TaskDetailResponse toTaskDetailResponse(AsyncTask task) {
        return TaskDetailResponse.builder()
            .id(task.getId())
            .documentId(task.getDocumentId())
            .documentName(null)  // 需要 join 查询，后续在 Controller 层补充
            .taskType(task.getTaskType())
            .status(task.getStatus())
            .progress(task.getProgress())
            .retryCount(task.getRetryCount())
            .maxRetry(task.getMaxRetry())
            .errorMessage(task.getErrorMessage())
            .startedAt(task.getStartedAt())
            .completedAt(task.getCompletedAt())
            .createdAt(task.getCreatedAt())
            .build();
    }
}
