package org.easyweb4j.util;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BoundedBlockingPriorityQueueTest {

  private static final Logger LOGGER = LoggerFactory
    .getLogger(BoundedBlockingPriorityQueueTest.class);

  @Test
  public void multipleThreads() throws InterruptedException {

    int producerSize = 30;
    int consumerSize = 24;
    int producerAddCount = 112300;
    int consumerCount = 45600;
    int tolThreads = producerSize + consumerSize;
    Assert.assertTrue(producerSize * producerAddCount > consumerSize * consumerCount);
    BlockingQueue<Integer> queue = new BoundedBlockingPriorityQueue<>(
      producerSize * producerAddCount - consumerSize * consumerCount / 3);
    CyclicBarrier cyclicBarrier = new CyclicBarrier(tolThreads);
    List<Thread> threads = new LinkedList<>();

    newProducer(producerSize, threads, cyclicBarrier, producerAddCount, queue);
    newConsumer(consumerSize, threads, cyclicBarrier, consumerCount, queue);

    for (Thread thread : threads) {
      thread.join();
    }

    Assert.assertTrue(queue.size() > 0);
    Assert
      .assertEquals(producerSize * producerAddCount - consumerSize * consumerCount, queue.size());

  }

  private void newConsumer(int consumerSize, List<Thread> threads, CyclicBarrier cyclicBarrier,
    int size, BlockingQueue<Integer> queue) {
    for (int i = 0; i < consumerSize; i++) {
      Thread thread = new Thread(() -> {
        try {
          cyclicBarrier.await();
        } catch (InterruptedException e) {
          LOGGER.error("consumer", e);
        } catch (BrokenBarrierException e) {
          LOGGER.error("consumer", e);
        }

        int mode = 0;
        for (int j = 0; j < size; j++) {
          try {
            switch (mode) {
              case 0:
                queue.take();
                break;
              case 1:
                Integer poll = queue.poll();
                if (null == poll) {
                  mode--;
                  j--;
                  continue;
                }
                break;
              case 2:
                queue.peek();
                j--;
                break;
              case 3:
                Integer poll1 = queue.poll(2, TimeUnit.SECONDS);
                if (null == poll1) {
                  mode--;
                  j--;
                  continue;
                }
                break;
              case 5:
                LinkedList<Integer> objects = new LinkedList<>();
                queue.drainTo(objects);
                queue.addAll(objects);
                queue.take();
                break;

              case 6:
                queue.remainingCapacity();
                queue.take();
                break;

              case 7:
                try {
                  queue.remove();
                } catch (NoSuchElementException e) {
                  j--;
                  mode--;
                }
                break;
              case 8:
                queue.toArray();
                queue.take();
                break;

              default:
                mode = 0;
                j--;
                continue;
            }

            mode++;
          } catch (Exception e) {
            LOGGER.error("consumer", e);
          }
        }

      });
      thread.start();
      threads.add(thread);
    }
  }

  private void newProducer(int producerSize, List<Thread> threads, CyclicBarrier cyclicBarrier,
    int producerAddCount, BlockingQueue<Integer> queue) {
    for (int i = 0; i < producerSize; i++) {
      Thread thread = new Thread(() -> {
        try {
          cyclicBarrier.await();
        } catch (InterruptedException e) {
          LOGGER.error("producer", e);
        } catch (BrokenBarrierException e) {
          LOGGER.error("producer", e);
        }

        int mode = 0;
        for (int j = 0; j < producerAddCount; j++) {
          try {
            switch (mode) {
              case 0:
                try {
                  queue.add(RandomUtils.nextInt());
                } catch (IllegalStateException e) {
                  j--;
                  continue;
                }
                break;
              case 1:
                queue.put(RandomUtils.nextInt());
                break;
              case 2:
                queue.offer(RandomUtils.nextInt());
                break;
              case 3:
                boolean offer = queue.offer(RandomUtils.nextInt(), 5, TimeUnit.SECONDS);
                if (!offer) {
                  j--;
                  continue;
                }
                break;

              default:
                mode = 0;
                j--;
                continue;
            }

            mode++;
          } catch (Exception e) {
            LOGGER.error("producer", e);
          }
        }

      });
      thread.start();
      threads.add(thread);
    }
  }

  @Test
  public void priorityTest() {
    BlockingQueue<Integer> queue = new BoundedBlockingPriorityQueue<>(3);
    queue.add(2);
    queue.add(1);
    queue.add(10);

    Assert.assertEquals(queue.poll().intValue(), 1);
    Assert.assertEquals(queue.poll().intValue(), 2);
    Assert.assertEquals(queue.poll().intValue(), 10);

    queue = new BoundedBlockingPriorityQueue<>(3, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return o2.compareTo(o1);
      }
    });

    queue.add(2);
    queue.add(1);
    queue.add(10);

    Assert.assertEquals(queue.poll().intValue(), 10);
    Assert.assertEquals(queue.poll().intValue(), 2);
    Assert.assertEquals(queue.poll().intValue(), 1);
  }
}
