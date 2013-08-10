package com.apriori.concurrent;

// TODO: javadoc
public interface Rescheduler {
   long scheduleNextStartTime(long priorStartTimeNanos);
}
