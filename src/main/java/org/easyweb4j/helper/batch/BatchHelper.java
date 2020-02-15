package org.easyweb4j.helper.batch;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 批处理帮助类
 *
 * @author Ray(linxray @ gmail.com)
 * @date 2020/02/15
 * @since 1.0
 */
public abstract class BatchHelper {

  /**
   * 轮寻生产者直至生产完毕，消费者可空
   *
   * @param supplier 生产者，如停止则返回null
   * @param consumer 消费者
   * @param <E>      对象类
   * @param <T>      集合类，结果集
   * @return false表示参数异常，true表示正常退出
   */
  public static final <E extends Object, T extends Set<E>> boolean loop(
    Supplier<T> supplier, Consumer<T> consumer
  ) {
    if (null == supplier) {
      return false;
    }

    T result;
    while (null != (result = supplier.get())) {
      if (null != consumer) {
        consumer.accept(result);
      }
    }

    return true;
  }

  /**
   * 整数相加轮寻生产者消费
   *
   * @param batchSize 批次大小
   * @param supplier  生产者，第一个参数是当前轮寻批次,批次从0开始，第二个参数是batchSize
   * @param consumer  消费者
   * @param <E>       生产对象
   * @param <T>       生产集合
   * @return false表示参数异常，true表示正常退出
   */
  public static final <E extends Object, T extends Set<E>> boolean loopByIndex(
    int batchSize,
    BiFunction<Integer, Integer, T> supplier, Consumer<T> consumer
  ) {
    int actualBatchSize = 1 > batchSize ? 1000 : batchSize;
    final Integer[] step = new Integer[]{Integer.valueOf(0)};

    return loop(
      () -> supplier.apply(step[0]++, actualBatchSize),
      consumer
    );
  }

}
