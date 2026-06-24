package com.docchat.module_task.service;

import com.docchat.module_task.entity.AsyncTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskQueueService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_PREFIX = "docchat:task:queue:";
    private static final String LOCK_PREFIX = "docchat:lock:task:";
    private static final long LOCK_TIMEOUT_MINUTES = 30;

    /** 推入任务队列 */
    public void pushTask(AsyncTask task) {
        String queueKey = QUEUE_PREFIX + task.getTaskType();
        String payload = serializeTask(task);
        redisTemplate.opsForList().leftPush(queueKey, payload);
        log.info("任务已入队: taskId={}, type={}", task.getId(), task.getTaskType());
    }

    /** 从队列弹出任务 */
    public AsyncTask popTask(String taskType) {
        String queueKey = QUEUE_PREFIX + taskType;
        String payload = redisTemplate.opsForList().rightPop(queueKey);
        if (payload == null) {
            return null;
        }
        return deserializeTask(payload);
    }

    /** 获取分布式锁 */
    public boolean acquireLock(Long taskId) {
        String lockKey = LOCK_PREFIX + taskId;
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", LOCK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(acquired);
    }

    /** 释放分布式锁 */
    public void releaseLock(Long taskId) {
        String lockKey = LOCK_PREFIX + taskId;
        redisTemplate.delete(lockKey);
    }

    private String serializeTask(AsyncTask task) {
        try {
            return objectMapper.writeValueAsString(task);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("任务序列化失败", e);
        }
    }

    private AsyncTask deserializeTask(String payload) {
        try {
            return objectMapper.readValue(payload, AsyncTask.class);
        } catch (JsonProcessingException e) {
            log.error("任务反序列化失败: {}", payload, e);
            return null;
        }
    }
}
