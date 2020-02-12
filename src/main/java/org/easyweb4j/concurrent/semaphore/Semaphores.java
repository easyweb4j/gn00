package org.easyweb4j.concurrent.semaphore;

import java.time.temporal.TemporalAmount;
import org.easyweb4j.concurrent.semaphore.impl.TimeRateSemaphore;

/**
 * Semaphores factory
 *
 * @author Ray(linxray @ gmail.com)
 * @date 2020/02/13
 * @since 1.0
 */
public abstract class Semaphores {

  private Semaphores() {
  }

  public static final RateSemaphore newRateSemaphore(int permits) {
    return new TimeRateSemaphore(permits);
  }

  public static final RateSemaphore newRateSemaphore(int permits, TemporalAmount timeAmount) {
    return new TimeRateSemaphore(permits, timeAmount);
  }
}
