package org.easyweb4j.concurrent.semaphore.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.StampedLock;
import org.easyweb4j.concurrent.semaphore.RateSemaphore;

/**
 * 时间相关的速率信号量,支持秒级，分钟级，小时级，日级别, 周级别，自然月级别，自然年级别
 *
 * @author Ray(linxray @ gmail.com)
 * @date 2020/02/12
 * @since 1.0
 */
public class TimeRateSemaphore implements RateSemaphore {

  private LocalDateTime resetThreshold;
  private StampedLock resetMSLock;
  private Semaphore internalSemaphore;

  private int maxPermits;
  private TemporalAmount rateAmount;

  public TimeRateSemaphore(int maxPermits, TemporalAmount rateAmount) {
    this.rateAmount = rateAmount;
    this.maxPermits = maxPermits;
    init();
  }

  /**
   * 默认秒级别
   *
   * @param maxPermits 最大允许量
   */
  public TimeRateSemaphore(int maxPermits) {
    this.maxPermits = maxPermits;
    rateAmount = Duration.ofSeconds(1);
    init();
  }

  private void init() {
    resetThreshold = LocalDateTime.now();
    internalSemaphore = new Semaphore(maxPermits);
    resetMSLock = new StampedLock();
  }

  @Override
  public void acquire() throws InterruptedException {
    resetSemaphoreIfRequire();
    internalSemaphore.acquire();
  }

  @Override
  public void acquire(int permits) throws InterruptedException {
    resetSemaphoreIfRequire();
    internalSemaphore.acquire(permits);
  }

  @Override
  public void release() {
    internalSemaphore.release();
  }

  @Override
  public void release(int permits) {
    internalSemaphore.release(permits);
  }

  @Override
  public boolean tryAcquire() {
    resetSemaphoreIfRequire();
    return internalSemaphore.tryAcquire();
  }

  @Override
  public boolean tryAcquire(int permits) {
    resetSemaphoreIfRequire();
    return internalSemaphore.tryAcquire(permits);
  }

  private void resetSemaphoreIfRequire() {
    long stamp = resetMSLock.tryOptimisticRead();
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(resetThreshold)) {
      long writeStamp = resetMSLock.tryConvertToWriteLock(stamp);
      if (0 == writeStamp) {
        return;
      }
      stamp = writeStamp;
      try {
        resetThreshold = LocalDateTime.now().plus(rateAmount);
      } finally {
        resetMSLock.unlockWrite(stamp);
      }
    }
  }

}
