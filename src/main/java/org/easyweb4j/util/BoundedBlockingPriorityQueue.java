package org.easyweb4j.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

/**
 * 通过包装PriorityQueue实现有边界的优先队列
 *
 * @param <E> 类型
 * @author ChenLei(linx @ gmail.com)
 * @date 2020/02/11
 * @since 1.0
 */
public class BoundedBlockingPriorityQueue<E> implements BlockingQueue<E> {

  private static class OptimisticReadSupplierResult<T> {

    boolean isReturn;
    T returnValue;

    public OptimisticReadSupplierResult(boolean isReturn, T returnValue) {
      this.isReturn = isReturn;
      this.returnValue = returnValue;
    }

    public OptimisticReadSupplierResult(T returnValue) {
      this.isReturn = false;
      this.returnValue = returnValue;
    }
  }

  private int capacity;
  private int size;
  private PriorityQueue<E> queue;
  private StampedLock lock;
  private ReentrantLock conditionLock;
  private Condition notEmptyCondition;
  private Condition notFullCondition;

  public BoundedBlockingPriorityQueue(int capacity) {
    this.capacity = capacity;
    this.size = 0;
    this.queue = new PriorityQueue<>(capacity);
    this.lock = new StampedLock();
    this.conditionLock = new ReentrantLock();
    this.notEmptyCondition = this.conditionLock.newCondition();
    this.notFullCondition = this.conditionLock.newCondition();
  }


  @Override
  public boolean add(E e) {
    return opsThenWrite(
      () -> {
        if ((size + 1) > capacity) {
          throw new IllegalStateException();
        }
        return new OptimisticReadSupplierResult(false, null);
      },

      () -> {
        size++;
        boolean res = queue.add(e);
        signalNotEmpty();
        return res;
      }
    );
  }

  @Override
  public boolean offer(E e) {
    return opsThenWrite(
      () -> new OptimisticReadSupplierResult((size + 1) > capacity, false),
      () -> {
        size++;
        boolean res = queue.add(e);
        signalNotEmpty();
        return res;
      }
    );
  }

  @Override
  public E remove() {
    return opsThenWrite(
      () -> {
        if (1 > size) {
          throw new NoSuchElementException();
        }
        return new OptimisticReadSupplierResult<E>(false, null);
      },
      () -> {
        size--;
        E remove = queue.remove();
        signalNotFull();
        return remove;
      }
    );
  }

  @Override
  public E poll() {
    return opsThenWrite(
      () -> new OptimisticReadSupplierResult<E>(1 > size, null),
      () -> {
        size--;
        E poll = queue.poll();
        signalNotFull();
        return poll;
      }
    );
  }

  @Override
  public E element() {
    return opsThenWrite(
      () -> {
        if (1 > size) {
          throw new NoSuchElementException();
        }
        return new OptimisticReadSupplierResult<E>(false, null);
      },
      () -> queue.element()
    );
  }

  @Override
  public E peek() {
    return opsThenWrite(
      () -> new OptimisticReadSupplierResult<E>(1 > size, null),
      () -> queue.peek()
    );
  }

  @Override
  public void put(E e) throws InterruptedException {
    long stamp;
    while (true) {
      stamp = lock.tryOptimisticRead();
      if (capacity <= size) {
        conditionLock.lockInterruptibly();
        try {
          notFullCondition.await();
        } finally {
          conditionLock.unlock();
        }
        continue;
      }

      long writeStamp = lock.tryConvertToWriteLock(stamp);
      if (0 != writeStamp) {
        stamp = writeStamp;
        break;
      }
    }

    try {
      size++;
      queue.offer(e);
      signalNotEmpty();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
    long stamp;
    while (true) {
      stamp = lock.tryOptimisticRead();
      if (capacity <= size) {
        conditionLock.lockInterruptibly();
        try {
          boolean await = notFullCondition.await(timeout, unit);
          if (!await) {
            return false;
          }
        } finally {
          conditionLock.unlock();
        }
        continue;
      }

      long writeStamp = lock.tryConvertToWriteLock(stamp);
      if (0 != writeStamp) {
        stamp = writeStamp;
        break;
      }
    }

    try {
      size++;
      queue.offer(e);
      signalNotEmpty();
    } finally {
      lock.unlockWrite(stamp);
    }
    return false;
  }

  @Override
  public E take() throws InterruptedException {
    long stamp;
    while (true) {
      stamp = lock.tryOptimisticRead();
      if (1 > size) {
        conditionLock.lockInterruptibly();
        try {
          notEmptyCondition.await();
        } finally {
          conditionLock.unlock();
        }
        continue;
      }

      long writeStamp = lock.tryConvertToWriteLock(stamp);
      if (0 != writeStamp) {
        stamp = writeStamp;
        break;
      }
    }

    try {
      size--;
      E poll = queue.poll();
      signalNotFull();
      return poll;
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long stamp;
    while (true) {
      stamp = lock.tryOptimisticRead();
      if (1 > size) {
        conditionLock.lockInterruptibly();
        try {
          boolean await = notEmptyCondition.await(timeout, unit);
          if (!await) {
            return null;
          }
        } finally {
          conditionLock.unlock();
        }
        continue;
      }

      long writeStamp = lock.tryConvertToWriteLock(stamp);
      if (0 != writeStamp) {
        stamp = writeStamp;
        break;
      }
    }

    try {
      size--;
      E poll = queue.poll();
      signalNotFull();
      return poll;
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @Override
  public int remainingCapacity() {
    return opsThenWrite(
      () -> new OptimisticReadSupplierResult<>(true, capacity - size),
      null
    );
  }

  @Override
  public boolean remove(Object o) {
    return opsThenWrite(
      () -> new OptimisticReadSupplierResult<>(1 > size, false),
      () -> {
        boolean remove = queue.remove(o);
        if (remove) {
          signalNotFull();
        }
        return remove;
      }
    );
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    long stamp = lock.readLock();
    try {
      return queue.containsAll(c);
    } finally {
      lock.unlockRead(stamp);
    }
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return false;
  }

  @Override
  public void clear() {

  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return false;
  }

  @Override
  public Iterator<E> iterator() {
    return null;
  }

  @Override
  public Object[] toArray() {
    return new Object[0];
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return null;
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    return 0;
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    return 0;
  }

  private <T extends Object> T opsThenWrite(Supplier<OptimisticReadSupplierResult<T>> opsJob,
    Supplier<T> writeJob) {
    long stamp = lock.tryOptimisticRead();
    OptimisticReadSupplierResult<T> opsRes = opsJob.get();
    if (opsRes.isReturn) {
      return opsRes.returnValue;
    }

    long writeStamp = lock.tryConvertToWriteLock(stamp);
    try {
      if (0 != writeStamp) {
        stamp = writeStamp;
      } else {
        stamp = lock.writeLock();
      }

      return writeJob.get();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  private void signalNotEmpty() {
    conditionLock.lock();
    try {
      notEmptyCondition.signal();
    } finally {
      conditionLock.unlock();
    }
  }

  private void signalNotFull() {
    conditionLock.lock();
    try {
      notFullCondition.signal();
    } finally {
      conditionLock.unlock();
    }
  }

  private void signalNotEmptyInterruptibly() throws InterruptedException {
    conditionLock.lockInterruptibly();
    try {
      notEmptyCondition.signal();
    } finally {
      conditionLock.unlock();
    }
  }

  private void signalNotFullInterruptibly() throws InterruptedException {
    conditionLock.lockInterruptibly();
    try {
      notFullCondition.signal();
    } finally {
      conditionLock.unlock();
    }
  }
}
