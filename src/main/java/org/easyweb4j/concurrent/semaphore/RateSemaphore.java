package org.easyweb4j.concurrent.semaphore;

/**
 * 速率控制信号量
 *
 * @author Ray(linxray @ gmail.com)
 * @date 2020/02/12
 * @since 1.0
 */
public interface RateSemaphore {

  /**
   * 阻塞式的获取信号量
   *
   * @throws InterruptedException
   */
  void acquire() throws InterruptedException;

  void acquire(int permits) throws InterruptedException;

  /**
   * 释放信号量
   */
  void release();

  void release(int permits);

  /**
   * 尝试获取信号量，非阻塞
   *
   * @return true成功获取，false获取失败
   */
  boolean tryAcquire();

  boolean tryAcquire(int permits);

}
