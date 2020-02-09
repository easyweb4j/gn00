package org.easyweb4j.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ExecutorsTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorsTest.class);

  @Test(expectedExceptions = {RejectedExecutionException.class})
  public void boundedExecutors() {
    ExecutorService executorService = BoundedExecutors.newBoundedExecutorService(2, 1);

    for (int i = 0; i < 3; i++) {
      executorService.execute(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOGGER.error("interrupted", e);
        }
      });
    }

  }

  @Test()
  public void boundedExecutorsNormal() throws InterruptedException {
    ExecutorService executorService = BoundedExecutors.newBoundedExecutorService(2, 1);

    for (int i = 0; i < 2; i++) {
      executorService.execute(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOGGER.error("interrupted", e);
        }
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(20000, TimeUnit.SECONDS);

  }

  @Test()
  public void boundedExecutorsThrowExceptionInThread() throws InterruptedException {
    ExecutorService executorService = BoundedExecutors.newBoundedExecutorService(2, 1);

    for (int i = 0; i < 2; i++) {
      executorService.execute(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOGGER.error("interrupted", e);
        }

        throw new RuntimeException("hello");
      });
    }

    executorService.shutdown();
    executorService.awaitTermination(20000, TimeUnit.SECONDS);

  }

}
