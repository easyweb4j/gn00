package org.easyweb4j.concurrent.semaphore.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import org.easyweb4j.concurrent.semaphore.RateSemaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 时间相关的速率信号量,支持秒级，分钟级，小时级，日级别, 周级别，自然月级别，自然年级别
 *
 * @author Ray(linxray @ gmail.com)
 * @date 2020/02/12
 * @since 1.0
 */
public class TimeRateSemaphore implements RateSemaphore {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeRateSemaphore.class);

  private static class ReleaseSemaphore implements Runnable {

    private StampedLock hasBgThreadStartedLock = new StampedLock();
    private boolean hasBgThreadStarted = false;
    private volatile boolean isClose = false;
    private TimeRateSemaphore timeRateSemaphore;

    public ReleaseSemaphore(TimeRateSemaphore timeRateSemaphore) {
      this.timeRateSemaphore = timeRateSemaphore;
    }

    @Override
    public void run() {
      LOGGER.debug("bg release thread start");
      if (isClose) {
        LOGGER.debug("bg release thread die of close");
        return;
      }

      long stamp = hasBgThreadStartedLock.writeLock();
      try {
        if (!timeRateSemaphore.hasSemaphoreRequireLastPeriod()) {
          hasBgThreadStarted = false;
          LOGGER.debug("bg release thread die of idle");
          return;
        }

        // schedule again
        timeRateSemaphore.startSchedule();
        hasBgThreadStarted = true;
      } finally {
        hasBgThreadStartedLock.unlockWrite(stamp);

        timeRateSemaphore.resetInternalSemaphore();
      }

    }

    void close() {
      isClose = true;
    }

    void startBgReleaseThreadIfRequire() {
      long stamp = hasBgThreadStartedLock.tryOptimisticRead();
      if (hasBgThreadStarted) {
        return;
      }

      long wstamp = hasBgThreadStartedLock.tryConvertToWriteLock(stamp);
      if (0 != wstamp) {
        stamp = wstamp;
      } else {
        return;
      }

      LOGGER.debug("require schedule");
      try {
        timeRateSemaphore.startSchedule();
        hasBgThreadStarted = true;
      } finally {
        hasBgThreadStartedLock.unlockWrite(stamp);

      }

    }
  }

  private Semaphore internalSemaphore;

  private int maxPermits;
  private TemporalAmount rateAmount;
  private ScheduledExecutorService executorService;
  private ReleaseSemaphore releaseSemaphore;

  public TimeRateSemaphore(int maxPermits, TemporalAmount temporalAmount) {
    this.rateAmount = temporalAmount;
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
    internalSemaphore = new Semaphore(maxPermits);
    executorService = Executors.newScheduledThreadPool(1);

    releaseSemaphore = new ReleaseSemaphore(this);
  }

  @Override
  public void acquire() throws InterruptedException {
    resetBgReleaseThread();
    internalSemaphore.acquire();
  }


  @Override
  public void acquire(int permits) throws InterruptedException {
    resetBgReleaseThread();
    internalSemaphore.acquire(permits);
  }

  private void resetBgReleaseThread() {
    releaseSemaphore.startBgReleaseThreadIfRequire();
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
    resetBgReleaseThread();
    return internalSemaphore.tryAcquire();
  }

  @Override
  public boolean tryAcquire(int permits) {
    resetBgReleaseThread();
    return internalSemaphore.tryAcquire(permits);
  }

  @Override
  public void close() throws Exception {
    releaseSemaphore.close();
  }

  private void resetInternalSemaphore() {
    LOGGER.debug("reset semaphore");
    internalSemaphore.drainPermits();
    internalSemaphore.release(maxPermits);
  }

  private void startSchedule() {
    LOGGER.debug("start schedule exe");
    long deadline = LocalDateTime.now().plus(rateAmount).atZone(ZoneId.systemDefault()).toInstant()
      .toEpochMilli();
    long now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()
      .toEpochMilli();
    long delay = deadline - now;
    executorService.schedule(
      releaseSemaphore,
      delay > 0 ? delay : 0,
      TimeUnit.MILLISECONDS
    );
  }

  private boolean hasSemaphoreRequireLastPeriod() {
    return internalSemaphore.getQueueLength() > 0;
  }


}
