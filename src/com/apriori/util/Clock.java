package com.apriori.util;

import java.util.concurrent.TimeUnit;

// TODO: javadoc
public interface Clock {
   long currentTimeMillis();
   long nanoTime();
   void sleep(long duration, TimeUnit unit);
   void sleepUntilMillis(long wakeTimeMillis);
   void sleepUntilNanoTime(long wakeNanoTime);
}
