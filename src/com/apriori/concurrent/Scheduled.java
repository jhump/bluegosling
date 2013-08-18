package com.apriori.concurrent;

import java.util.Comparator;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

//TODO: javadoc
public interface Scheduled extends Delayed {
   long getScheduledNanoTime();
   
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
            return nanos1 > nanos2 ? 1 : (nanos1 < nanos2 ? -1 : 0);
         }
         return nanos1 > nanos2 ? 1 : (nanos1 < nanos2 ? -1 : 0);
      }
   };
}
