// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Provides a simplistic structure for running many threads in parallel during a test. This
 * could possibly be useful for performance testing but is also useful in validating that
 * classes are properly thread-safe.
 * 
 * <p>Note that this class should only be used from a single thread, ideally from an individual
 * {@code TestCase}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MultipleAccessTestHelper {

   /**
    * Represents one or more accessor threads. It consists of a {@link Runnable}, which is
    * executed repeatedly by a thread, and a count, which is the number of threads performing
    * the same type of access.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Accessor {
      private int count;
      private Runnable operation;
      
      Accessor(int count, Runnable operation) {
         this.count = count;
         this.operation = operation;
      }
      
      public int count() {
         return count;
      }
      
      public Runnable operation() {
         return operation;
      }
   }

   /** The total number of accessor threads that will be started. */
   private int numThreads;
   
   /** The accessors that will be executed. */
   private Collection<Accessor> accessors = new ArrayList<Accessor>();

   /** A latch that is used to synchronize the start of access. */
   CountDownLatch threadReady;
   
   /** A latch that is used to signal that accessor threads should stop. */
   CountDownLatch threadStop;
   
   /** A latch that is used to wait for all threads to complete. */
   CountDownLatch threadDone;
   
   /**
    * A collection of thread failures. This collection will include any uncaught
    * exceptions thrown by accessor threads. Note that these exceptions will also
    * abort the execution of an accessor thread, so this collection will never
    * contain more objects than there are accessor threads.
    */
   Collection<Throwable> threadFailures;
   
   /**
    * A thread that runs the logic for a single accessor. It simply invokes a
    * {@link Runnable} repeatedly until signaled to stop.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SingleAccessThread extends Thread {
      
      private Runnable singleOperation;
      
      SingleAccessThread(Runnable singleOperation) {
         this.singleOperation = singleOperation;
         this.setDaemon(true); // don't let abandoned thread hold up the whole VM
      }

      @Override
      public void run() {
         threadReady.countDown();
         try {
            threadReady.await();
            do {
               singleOperation.run();
            } while (!threadStop.await(0, TimeUnit.SECONDS));
         } catch (Throwable t) {
            threadFailures.add(t);
         } finally {
            threadDone.countDown();
         }
      }
   }
   
   /**
    * Adds accessors. When this helper object runs the accessor threads, it will start
    * the specified number of threads, each running the specified logic over and over
    * until instructed to stop.
    * 
    * <p>The operation that is executed should be very fast. If it does not execute very
    * quickly, it could cause the execution of the threads to fail with an exception about
    * accessor threads not terminating in a timely manner.
    *
    * @param count the number of threads that will execute this operation
    * @param operation the operation that will be performed repeatedly
    * @return this (for method chaining)
    */
   public MultipleAccessTestHelper addAccessors(int count, Runnable operation) {
      accessors.add(new Accessor(count, operation));
      numThreads += count;
      return this;
   }

   /**
    * Starts accessor threads and lets them run for the specified period of time.
    *
    * @param duration the duration for which to let threads execute
    * @param unit the time unit for {@code duration}
    * @return a collection of uncaught exceptions (empty if all threads completed normally)
    * @throws InterruptedException if this thread is interrupted while waiting on the accessor
    *       threads to complete
    */
   public Collection<Throwable> run(int duration, TimeUnit unit) throws InterruptedException {
      threadReady = new CountDownLatch(numThreads);
      threadStop = new CountDownLatch(1);
      threadDone = new CountDownLatch(numThreads);
      threadFailures = Collections.synchronizedCollection(new ArrayList<Throwable>());
      
      try {
         // create all of the threads
         for (Accessor accessor : accessors) {
            for (int i = 0; i < accessor.count(); i++) {
               new SingleAccessThread(accessor.operation()).start();
            }
         }
         // and then let threads execute
         
         // more than 5 seconds? something's wrong...
         if (!threadReady.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException(
                  "Concurrent access threads failed to become ready after 5 seconds");
         }
         // wait for specified duration or until they all terminate
         if (!threadDone.await(duration, unit)) {
            // have to tell threads to stop
            threadStop.countDown();
            // more than 5 seconds? something's wrong...
            if (!threadDone.await(5, TimeUnit.SECONDS)) {
               throw new RuntimeException(
                     "Concurrent access threads failed to finish after 5 seconds");
            }
         }
         
         return threadFailures;
         
      } finally {
         // try to tell any running threads to stop
         if (threadStop.getCount() > 0) {
            threadStop.countDown();
         }
      }
   }
}
