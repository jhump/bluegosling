package com.apriori.concurrent;

/**
 * An enumeration of commonly used {@link ScheduleNextTaskPolicy} implementations.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public enum ScheduleNextTaskPolicies implements ScheduleNextTaskPolicy<Object, Object> {
   ALWAYS() {
      @Override
      public boolean shouldScheduleNext(ScheduledTask<? extends Object, ? extends Object> latest) {
         return true;
      }
   },
   NEVER() {
      @Override
      public boolean shouldScheduleNext(ScheduledTask<? extends Object, ? extends Object> latest) {
         return false;
      }
   },
   ON_SUCCESS() {
      @Override
      public boolean shouldScheduleNext(ScheduledTask<? extends Object, ? extends Object> latest) {
         return latest.succeeded();
      }
   },
   ON_FAILURE() {
      @Override
      public boolean shouldScheduleNext(ScheduledTask<? extends Object, ? extends Object> latest) {
         return latest.failed();
      }
   }
}