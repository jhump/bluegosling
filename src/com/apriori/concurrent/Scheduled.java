package com.apriori.concurrent;

import java.util.Comparator;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * A specialization of {@link Delayed} that allows for more accurate comparisons between the time
 * at which instances are scheduled.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Scheduled extends Delayed {
   /**
    * Returns the time at which this instance is scheduled, in {@linkplain System#nanoTime() system
    * nanos}.
    *
    * @return the time at which this instance is scheduled
    */
   long getScheduledNanoTime();
   
   /**
    * An accurate and precise comparator of {@link Delayed} objects that will compare the scheduled
    * nano-time if both objects are instances of {@link Scheduled}.
    * 
    * <p>Comparing objects just based on delay is inherently racy at very fine precision since
    * nanoseconds pass between the method calls to query the objects' delay. So it may be
    * impossible to otherwise determine if two delayed objects are actually scheduled for precisely
    * the same instant in time. Comparing absolute scheduled times allows for such precise
    * comparisons.
    */
   Comparator<Delayed> COMPARATOR = new Comparator<Delayed>() {
      @Override
      public int compare(Delayed o1, Delayed o2) {
         long nanos1;
         long nanos2;
         if (o1 instanceof Scheduled && o2 instanceof Scheduled) {
            nanos1 = ((Scheduled) o1).getScheduledNanoTime();
            nanos2 = ((Scheduled) o2).getScheduledNanoTime();
         } else {
            nanos1 = o1.getDelay(TimeUnit.NANOSECONDS);
            nanos2 = o2.getDelay(TimeUnit.NANOSECONDS);
         }
         return Long.compare(nanos1, nanos2);
      }
   };
}
