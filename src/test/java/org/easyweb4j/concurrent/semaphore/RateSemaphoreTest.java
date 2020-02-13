package org.easyweb4j.concurrent.semaphore;

import com.google.common.base.Stopwatch;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RateSemaphoreTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(RateSemaphoreTest.class);

  @Test(timeOut = 30_000)
  public void timeRateSemaphore() throws Exception {
    int threadSize = 20;
    int permits = 8;
    int seconds = 2;
    try (RateSemaphore rateSemaphore = Semaphores
      .newRateSemaphoreDuration(permits, "PT2S")) {

      List<Thread> threads = new LinkedList<>();
      CyclicBarrier cyclicBarrier = new CyclicBarrier(threadSize);

      long curMS = System.currentTimeMillis();
      for (int i = 0; i < threadSize; i++) {
        Thread t = newThread(rateSemaphore, cyclicBarrier, i);
        threads.add(t);
        t.start();
      }

      for (Thread t : threads) {
        t.join();
      }
      long duration = System.currentTimeMillis() - curMS;

      Assert.assertTrue(
        duration > ((Math.floor((double) threadSize / permits)) * seconds
          * 1000), "use " + duration);

      Thread.sleep(1000 * seconds * 2);

      // test reset
      threads.clear();
      cyclicBarrier = new CyclicBarrier(threadSize);
      curMS = System.currentTimeMillis();
      for (int i = 0; i < threadSize; i++) {
        Thread t = newThread(rateSemaphore, cyclicBarrier, i);
        threads.add(t);
        t.start();
      }

      for (Thread t : threads) {
        t.join();
      }

      duration = System.currentTimeMillis() - curMS;
      Assert.assertTrue(
        duration > ((Math.floor((double) threadSize / permits)) * seconds
          * 1000), "use " + duration);
    }

  }

  private Thread newThread(RateSemaphore rateSemaphore, CyclicBarrier cyclicBarrier, int i) {
    return new Thread(() -> {
      try {
        cyclicBarrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        LOGGER.error("await {}", i, e);
      }

      Stopwatch stopwatch = Stopwatch.createStarted();
      try {
        switch (i % 2) {
          case 0:
            rateSemaphore.acquire();
            break;
          case 1:
            if (!rateSemaphore.tryAcquire()) {
              rateSemaphore.acquire(1);
            }
            break;
          default:
            LOGGER.debug("no case in thread");
            break;
        }
      } catch (Exception e) {
        LOGGER.error("exception: {}", i, e);
      }

      LOGGER.debug("t {}={}", i, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    });
  }

  @Test(timeOut = 30_000)
  public void timeRateSemaphoreLongPeriod() throws Exception {
    int threadSize = 20;
    int permits = 40;
    int seconds = 2;
    try (RateSemaphore rateSemaphore = Semaphores
      .newRateSemaphorePeriod(permits, "P2W1D")) {

      List<Thread> threads = new LinkedList<>();
      CyclicBarrier cyclicBarrier = new CyclicBarrier(threadSize);

      long curMS = System.currentTimeMillis();
      for (int i = 0; i < threadSize; i++) {
        Thread t = newThread(rateSemaphore, cyclicBarrier, i);
        threads.add(t);
        t.start();
      }

      for (Thread t : threads) {
        t.join();
      }
      long duration = System.currentTimeMillis() - curMS;

      Assert.assertTrue(
        duration < seconds * 1000, "use " + duration);
    }

  }
}
