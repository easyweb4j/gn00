package org.easyweb4j.concurrent.semaphore;

import java.time.Duration;
import java.time.Period;
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

  public static final RateSemaphore newRateSemaphoreDuration(int permits, String rateAmount) {
    return new TimeRateSemaphore(permits, Duration.parse(rateAmount));
  }

  public static final RateSemaphore newRateSemaphorePeriod(int permits, String rateAmount) {
    return new TimeRateSemaphore(permits, Period.parse(rateAmount));
  }
}
