package org.easyweb4j.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认打印日志的线程组
 *
 * @author ChenLei(linxray @ gmail.com)
 * @date 2020/02/09
 * @since 1.0
 */
public class ExceptionPrintThreadGroup extends ThreadGroup {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionPrintThreadGroup.class);

  public ExceptionPrintThreadGroup(String name) {
    super(name);
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    LOGGER.error("Exception caught in thread: name={}", t.getName(), e);
    super.uncaughtException(t, e);
  }
}
