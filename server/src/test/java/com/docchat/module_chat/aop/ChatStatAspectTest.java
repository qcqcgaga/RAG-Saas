package com.docchat.module_chat.aop;

import com.docchat.module_stat.service.StatService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 对话统计切面单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChatStatAspectTest {

    @Mock private StatService statService;
    @Mock private ProceedingJoinPoint joinPoint;
    @InjectMocks private ChatStatAspect chatStatAspect;

    @Test
    @DisplayName("aroundConverse - JWT鉴权时跳过用量记录")
    void aroundConverse_jwtAuth_skipsRecording() throws Throwable {
        AuthContext.set(1L, null, "JWT", "gpt-4o-mini");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = chatStatAspect.aroundConverse(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(statService, never()).recordUsage(any(), any(), anyString(), anyString(), anyInt(), anyInt());
        AuthContext.clear();
    }

    @Test
    @DisplayName("aroundConverse - API_KEY鉴权时记录用量")
    void aroundConverse_apiKeyAuth_recordsUsage() throws Throwable {
        AuthContext.set(1L, 5L, "API_KEY", "gpt-4o-mini");
        when(joinPoint.proceed()).thenReturn("result");
        doNothing().when(statService).recordUsage(any(), any(), anyString(), anyString(), anyInt(), anyInt());

        Object result = chatStatAspect.aroundConverse(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(statService).recordUsage(1L, 5L, "API_KEY", "gpt-4o-mini", 0, 0);
        AuthContext.clear();
    }

    @Test
    @DisplayName("aroundConverse - 用量记录失败不影响对话结果")
    void aroundConverse_recordingFailure_doesNotAffectResult() throws Throwable {
        AuthContext.set(1L, 5L, "API_KEY", "gpt-4o-mini");
        when(joinPoint.proceed()).thenReturn("result");
        doThrow(new RuntimeException("DB error")).when(statService)
                .recordUsage(any(), any(), anyString(), anyString(), anyInt(), anyInt());

        // 对话应正常返回
        Object result = chatStatAspect.aroundConverse(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(statService).recordUsage(1L, 5L, "API_KEY", "gpt-4o-mini", 0, 0);
        AuthContext.clear();
    }

    @Test
    @DisplayName("aroundConverse - 对话方法抛异常时正常传播")
    void aroundConverse_converseThrows_propagatesException() throws Throwable {
        AuthContext.set(1L, 5L, "API_KEY", "gpt-4o-mini");
        when(joinPoint.proceed()).thenThrow(new RuntimeException("LLM unavailable"));

        assertThatThrownBy(() -> chatStatAspect.aroundConverse(joinPoint))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("LLM unavailable");

        AuthContext.clear();
    }

    @Test
    @DisplayName("aroundConverse - 无AuthContext时不记录用量")
    void aroundConverse_noAuthContext_skipsRecording() throws Throwable {
        AuthContext.clear(); // 确保无上下文
        when(joinPoint.proceed()).thenReturn("result");

        Object result = chatStatAspect.aroundConverse(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(statService, never()).recordUsage(any(), any(), anyString(), anyString(), anyInt(), anyInt());
    }
}
