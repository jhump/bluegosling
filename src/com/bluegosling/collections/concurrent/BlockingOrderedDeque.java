package com.bluegosling.collections.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.bluegosling.collections.OrderedDeque;

public interface BlockingOrderedDeque<E> extends BlockingQueue<E>, OrderedDeque<E> {
   
   E takeFirst() throws InterruptedException;
   
   E takeLast() throws InterruptedException;
   
   E pollFirst(long timeout, TimeUnit unit) throws InterruptedException;
   
   E pollLast(long timeout, TimeUnit unit) throws InterruptedException;
   
   @Override
   default E take() throws InterruptedException {
      return takeFirst();
   }
   
   @Override
   default E poll(long timeout, TimeUnit unit) throws InterruptedException {
      return pollFirst(timeout, unit);
   }
}
