package org.easyweb4j.helper;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.collections4.CollectionUtils;
import org.easyweb4j.helper.batch.BatchHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BatchTest {

  @Test
  public void loopByIndex() {
    AtomicInteger atomicInteger = new AtomicInteger(0);
    BatchHelper.loopByIndex(10, (step, batchSize) -> {
      Assert.assertEquals(batchSize, Integer.valueOf(10));
      if (20 <= step) {
        return null;
      }
      HashSet<Integer> integers = new HashSet<>();
      for (int i = 0; i < step + 1; i++) {
        integers.add(i);
      }

      return integers;

    }, (val) -> {
      Assert.assertTrue(CollectionUtils.isNotEmpty(val));
      atomicInteger.incrementAndGet();
    });

    Assert.assertEquals(atomicInteger.get(), 20);
  }

  @Test
  public void loopByIndexNoConsumer() {
    BatchHelper.loopByIndex(-100, (step, batchSize) -> {
      Assert.assertEquals(batchSize, Integer.valueOf(1000));
      if (20 <= step) {
        return null;
      }
      HashSet<Integer> integers = new HashSet<>();
      for (int i = 0; i < step + 1; i++) {
        integers.add(i);
      }

      return integers;

    }, null);
  }
}
