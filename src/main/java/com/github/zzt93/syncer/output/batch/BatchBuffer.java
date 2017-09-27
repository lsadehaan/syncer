package com.github.zzt93.syncer.output.batch;

import com.github.zzt93.syncer.common.ThreadSafe;
import com.github.zzt93.syncer.config.pipeline.output.PipelineBatch;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zzt
 */
public class BatchBuffer<T> {

  private final int limit;
  private final ConcurrentLinkedDeque<T> deque = new ConcurrentLinkedDeque<>();
  private final AtomicInteger estimateSize = new AtomicInteger(0);
  private final Class<T> clazz;

  public BatchBuffer(PipelineBatch batch, Class<T> aClass) {
    limit = batch.getSize();
    clazz = aClass;
  }

  public boolean add(T data) {
    deque.addLast(data);
    estimateSize.incrementAndGet();
    return true;
  }

  public boolean addAll(List<T> data) {
    boolean res = deque.addAll(data);
    estimateSize.addAndGet(data.size());
    return res;
  }

  @ThreadSafe(safe = {ConcurrentLinkedDeque.class, AtomicInteger.class})
  public T[] flushIfReachSizeLimit() {
    if (estimateSize.getAndUpdate(x -> x >= limit ? x - limit : x) > limit) {
      T[] res = (T[]) Array.newInstance(clazz, limit);
      for (int i = 0; i < limit; i++) {
        res[i] = deque.removeFirst();
      }
      return res;
    }
    return null;
  }

  public T[] flush() {
    ArrayList<T> res = new ArrayList<>();
    if (estimateSize.getAndUpdate(x -> 0) > 0) {
      while (!deque.isEmpty()) {
        res.add(deque.removeFirst());
      }
    }
    return res.toArray((T[]) Array.newInstance(clazz, 0));
  }
}
