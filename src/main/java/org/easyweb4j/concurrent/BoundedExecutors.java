package org.easyweb4j.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 限制的线程池，对线程池的大小，队列可灵活限制，线程优先级，线程名称可根据线程自定义
 *
 * @author ChenLei(linxray @ gmail.com)
 * @date 2020/02/09
 * @since 1.0
 */
public abstract class BoundedExecutors {

  private BoundedExecutors() {

  }

  public static final ExecutorService newBoundedExecutorService(
    int maximumQueueSize,
    int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    String threadGroupNamePrefix,
    int threadPriority
  ) {
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
      corePoolSize,
      maximumPoolSize,
      keepAliveTime,
      unit,
      new ArrayBlockingQueue<Runnable>(maximumQueueSize),
      new DefaultExecutorThreadFactory(threadGroupNamePrefix, threadPriority)
    );

    threadPoolExecutor.prestartAllCoreThreads();
    return threadPoolExecutor;
  }

  public static final ExecutorService newBoundedExecutorService(
    int maximumQueueSize,
    int poolSize,
    String threadGroupNamePrefix,
    int threadPriority
  ) {
    return newBoundedExecutorService(
      maximumQueueSize,
      poolSize,
      poolSize,
      60,
      TimeUnit.SECONDS,
      threadGroupNamePrefix,
      threadPriority
    );
  }

  public static final ExecutorService newBoundedExecutorService(
    int maximumQueueSize,
    int corePoolSize,
    int maximumPoolSize,
    String threadGroupNamePrefix
  ) {
    return newBoundedExecutorService(
      maximumQueueSize,
      corePoolSize,
      maximumPoolSize,
      60,
      TimeUnit.SECONDS,
      threadGroupNamePrefix,
      Thread.NORM_PRIORITY
    );
  }

  public static final ExecutorService newBoundedExecutorService(
    int maximumQueueSize,
    int poolSize
  ) {
    return newBoundedExecutorService(
      maximumQueueSize,
      poolSize,
      poolSize,
      60,
      TimeUnit.SECONDS,
      "bounded-pool",
      Thread.NORM_PRIORITY
    );
  }
}
