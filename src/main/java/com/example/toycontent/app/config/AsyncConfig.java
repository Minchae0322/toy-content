package com.example.toycontent.app.config;


import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

  @Bean(name = "notificationExecutor")
  public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("notification-");
    // 제출 시점(커밋 직후 요청 스레드)의 trace 컨텍스트를 스냅샷해 워커 스레드에서 복원 —
    // 이게 없으면 @Async 경계에서 traceId가 끊겨 kafka send가 별개 트레이스로 떨어진다.
    executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 조회수 증가 이벤트 전용 풀 (FeedViewCountEventListener).
   *
   * <p>UPDATE 1건짜리 짧은 작업이라 스레드는 적게, 큐는 깊게 잡는다. 큐가 차면
   * CallerRuns로 요청 스레드에 되밀려 증가분 유실 없이 자연 역압이 걸린다.
   * 롤링 배포 시 큐 잔량이 조회수 유실이 되지 않도록 종료 시 큐 소진을 기다린다.
   * 큐 깊이·활성 스레드는 Boot의 TaskExecutor 자동 계측(executor.* 메트릭)으로 관측한다.
   */
  @Bean(name = "viewCountExecutor")
  public Executor viewCountExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(5000);
    executor.setThreadNamePrefix("view-count-");
    executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    executor.initialize();
    return executor;
  }
}
