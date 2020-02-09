package org.easyweb4j.concurrent;

/**
 * 用于线程信息定制
 *
 * @author ChenLei(linxray @ gmail.com)
 * @date 2020/02/09
 * @since 1.0
 */
public interface ThreadMetaCustomizer {

  /**
   * 线程优先级，如null，则默认
   *
   * @return 线程优先级
   */
  Integer priority();

  /**
   * 线程名字后缀
   *
   * @return 线程名字后缀
   */
  String name();
}
