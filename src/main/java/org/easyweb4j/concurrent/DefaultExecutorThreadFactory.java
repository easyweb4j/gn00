package org.easyweb4j.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;

/**
 * 默认线程工厂，支持线程的优先级和名字自定义
 *
 * @author ChenLei(linxray @ gmail.com)
 * @date 2020/02/09
 * @since 1.0
 */
public class DefaultExecutorThreadFactory implements ThreadFactory {

  private static final ConcurrentHashMap<String, AtomicInteger> POOL_COUNTER_MAP =
    new ConcurrentHashMap<>();

  private AtomicInteger threadNumber;
  private int poolNumber;
  private String namePrefix;
  private int defaultThreadPriority;
  private ThreadGroup threadGroup;

  public DefaultExecutorThreadFactory(String namePrefix, int defaultThreadPriority) {
    threadNumber = new AtomicInteger(1);
    this.defaultThreadPriority = defaultThreadPriority;

    // inc pool number
    poolNumber = 1;
    AtomicInteger existingValue = POOL_COUNTER_MAP
      .putIfAbsent(namePrefix, new AtomicInteger(poolNumber));
    if (null != existingValue) {
      poolNumber = existingValue.incrementAndGet();
    }

    String poolName = namePrefix + "-" + poolNumber;
    this.namePrefix = poolName + "-thread-";

    threadGroup = new ExceptionPrintThreadGroup(poolName);
    threadGroup.setDaemon(false);
  }

  public DefaultExecutorThreadFactory() {
    new DefaultExecutorThreadFactory("pool", Thread.NORM_PRIORITY);
  }

  @Override
  public Thread newThread(Runnable r) {
    String threadName = namePrefix + threadNumber.getAndIncrement();
    int priority = this.defaultThreadPriority;
    if (ThreadMetaCustomizer.class.isAssignableFrom(r.getClass())) {
      ThreadMetaCustomizer threadMetaCustomizer = (ThreadMetaCustomizer) r;
      if (null != threadMetaCustomizer.priority()) {
        priority = threadMetaCustomizer.priority().intValue();
      }

      if (StringUtils.isNoneBlank(threadMetaCustomizer.name())) {
        threadName = threadName + "-" + threadMetaCustomizer.name();
      }
    }

    Thread t = new Thread(threadGroup, r, threadName, 0);
    t.setPriority(priority);
    return t;
  }
}
