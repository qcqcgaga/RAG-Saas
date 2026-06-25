package com.docchat.module_task.service;

import com.docchat.common.exception.BizException;
import com.docchat.common.response.ErrorCode;
import com.docchat.common.util.SecurityUtil;
import com.docchat.module_task.dto.TaskDetailResponse;
import com.docchat.module_task.dto.TaskResponse;
import com.docchat.module_task.entity.AsyncTask;
import com.docchat.module_task.repository.AsyncTaskRepository;
import com.docchat.common.response.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private AsyncTaskRepository taskRepository;
    @Mock private TaskQueueService taskQueueService;
    @InjectMocks private TaskServiceImpl taskService;

    private AsyncTask buildTask(Long id, Long tenantId, String status, short retryCount) {
        return AsyncTask.builder()
            .id(id).tenantId(tenantId).documentId(1L)
            .taskType("CHUNK_AND_EMBED").status(status)
            .progress((short) 50).maxRetry((short) 3).retryCount(retryCount)
            .build();
    }

    @Test
    @DisplayName("createTask - 创建任务并推入队列")
    void createTask_createsAndPushes() {
        AsyncTask task = buildTask(1L, 100L, "PENDING", (short) 0);
        when(taskRepository.save(any())).thenReturn(task);

        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L);
            AsyncTask result = taskService.createTask("CHUNK_AND_EMBED", 1L);
            assertThat(result.getStatus()).isEqualTo("PENDING");
            verify(taskQueueService).pushTask(any());
        }
    }

    @Test
    @DisplayName("getTask - 正常获取任务")
    void getTask_found() {
        AsyncTask task = buildTask(1L, 100L, "COMPLETED", (short) 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L);
            TaskDetailResponse resp = taskService.getTask(1L);
            assertThat(resp.getId()).isEqualTo(1L);
            assertThat(resp.getStatus()).isEqualTo("COMPLETED");
        }
    }

    @Test
    @DisplayName("getTask - 任务不存在抛TASK_NOT_FOUND")
    void getTask_notFound_throws() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L);
            assertThatThrownBy(() -> taskService.getTask(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                    .isEqualTo(ErrorCode.TASK_NOT_FOUND.getCode()));
        }
    }

    @Test
    @DisplayName("getTask - 跨租户访问抛FORBIDDEN")
    void getTask_crossTenant_throws() {
        AsyncTask task = buildTask(1L, 200L, "COMPLETED", (short) 0); // 属于租户200
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L); // 当前租户100
            assertThatThrownBy(() -> taskService.getTask(1L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                    .isEqualTo(ErrorCode.FORBIDDEN.getCode()));
        }
    }

    @Test
    @DisplayName("retryTask - FAILED任务可重试")
    void retryTask_failedTask_canRetry() {
        AsyncTask task = buildTask(1L, 100L, "FAILED", (short) 1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L);
            TaskResponse resp = taskService.retryTask(1L);
            assertThat(resp.getStatus()).isEqualTo("PENDING");
            verify(taskQueueService).pushTask(any());
        }
    }

    @Test
    @DisplayName("retryTask - 非FAILED任务抛TASK_NOT_FAILED")
    void retryTask_notFailed_throws() {
        AsyncTask task = buildTask(1L, 100L, "COMPLETED", (short) 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L);
            assertThatThrownBy(() -> taskService.retryTask(1L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                    .isEqualTo(ErrorCode.TASK_NOT_FAILED.getCode()));
        }
    }

    @Test
    @DisplayName("retryTask - 超过最大重试次数抛TASK_MAX_RETRY_EXCEEDED")
    void retryTask_maxRetryExceeded_throws() {
        AsyncTask task = buildTask(1L, 100L, "FAILED", (short) 3); // maxRetry=3
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        try (MockedStatic<SecurityUtil> sec = mockStatic(SecurityUtil.class)) {
            sec.when(SecurityUtil::getCurrentTenantId).thenReturn(100L);
            assertThatThrownBy(() -> taskService.retryTask(1L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                    .isEqualTo(ErrorCode.TASK_MAX_RETRY_EXCEEDED.getCode()));
        }
    }

    @Test
    @DisplayName("updateTaskProgress - 进度值上限为100")
    void updateTaskProgress_clampedToMax100() {
        AsyncTask task = buildTask(1L, 100L, "PROCESSING", (short) 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenReturn(task);

        taskService.updateTaskProgress(1L, 150);
        verify(taskRepository).save(argThat(t -> t.getProgress() == 100));
    }

    @Test
    @DisplayName("updateTaskProgress - 进度值下限为0")
    void updateTaskProgress_clampedToMin0() {
        AsyncTask task = buildTask(2L, 100L, "PROCESSING", (short) 0);
        when(taskRepository.findById(2L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenReturn(task);

        taskService.updateTaskProgress(2L, -10);
        verify(taskRepository).save(argThat(t -> t.getProgress() == 0));
    }

    @Test
    @DisplayName("updateTaskProgress - 不存在的任务静默忽略")
    void updateTaskProgress_nonExistent_silentIgnore() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());
        taskService.updateTaskProgress(999L, 50);
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTaskStatus - COMPLETED状态设置progress=100")
    void updateTaskStatus_completed_setsProgress100() {
        AsyncTask task = buildTask(1L, 100L, "PROCESSING", (short) 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskService.updateTaskStatus(1L, "COMPLETED", null);
        verify(taskRepository).save(argThat(t ->
            t.getProgress() == 100 && t.getCompletedAt() != null
        ));
    }

    @Test
    @DisplayName("updateTaskStatus - PROCESSING状态设置startedAt")
    void updateTaskStatus_processing_setsStartedAt() {
        AsyncTask task = buildTask(1L, 100L, "PENDING", (short) 0);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskService.updateTaskStatus(1L, "PROCESSING", null);
        verify(taskRepository).save(argThat(t -> t.getStartedAt() != null));
    }
}
