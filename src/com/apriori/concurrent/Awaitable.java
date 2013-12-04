package com.apriori.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * An object that represents a future event.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Awaitable {
   /**
    * Awaits the future event, blocking the current thread until the event occurs.
    *
    * @throws InterruptedException if this thread is interrupted while waiting for the event
    */
   void await() throws InterruptedException;
   
   /**
    * Awaits the future event, blocking up to the specified amount of time until the event occurs.
    *
    * @param limit the maximum amount of time to wait
    * @param unit the unit of {@code limit}
    * @return true if the event occurred or false if the time limit was encountered first
    * @throws InterruptedException if this thread is interrupted while waiting for the event
    */
   boolean await(long limit, TimeUnit unit) throws InterruptedException;
   
   /**
    * Returns true if the event has occurred.
    *
    * @return true if the event has occurred; false otherwise
    */
   boolean isDone();
}
