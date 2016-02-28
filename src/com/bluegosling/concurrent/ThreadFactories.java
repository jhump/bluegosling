package com.bluegosling.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods for creating thread factories.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class ThreadFactories {
   private ThreadFactories() {
   }

   /**
    * Returns a thread factory that creates threads belonging to the given group and named after the
    * given group. Thread names are in the format {@code "threadgroupname-1"}. The suffix
    * ({@code "-1"} in the example) contains a unique identifier so that each thread has a distinct
    * name. The identifier starts at 1 for the first thread created, 2 for the second thread, and
    * so on.
    *
    * @param threadGroup the group to which created threads belong
    * @return a thread factory that creates threads belonging to the given group and named after the
    *       given group
    */
   public static ThreadFactory newGroupingThreadFactory(ThreadGroup threadGroup) {
      return groupingThreadFactory(threadGroup, threadGroup.getName(), false);
   }

   /**
    * Returns a thread factory that creates threads belonging to the given group and with the given
    * name prefix. Thread names are in the format {@code "prefix-1"}. The suffix ({@code "-1"} in
    * the example) contains a unique identifier so that each thread has a distinct name. The
    * identifier starts at 1 for the first thread created, 2 for the second thread, and so on.
    *
    * @param threadGroup the group to which created threads belong
    * @param threadNamePrefix the prefix used to create thread names
    * @return a thread factory that creates threads belonging to the given group and wit the given
    *       name prefix
    */
   public static ThreadFactory newGroupingThreadFactory(ThreadGroup threadGroup,
         String threadNamePrefix) {
      return groupingThreadFactory(threadGroup, threadNamePrefix, false);
   }

   /**
    * Returns a thread factory that creates threads belonging to the given group and with the given
    * name prefix. Thread names are in the format {@code "prefix-1"}. The suffix ({@code "-1"} in
    * the example) contains a unique identifier so that each thread has a distinct name. The
    * identifier starts at 1 for the first thread created, 2 for the second thread, and so on.
    * 
    * <p>A new thread group is created with the given name.
    *
    * @param threadGroupName the name of the new group to which created threads belong
    * @return a thread factory that creates threads belonging to the given group and wit the given
    *       name prefix
    */
   public static ThreadFactory newGroupingThreadFactory(String threadGroupName) {
      return groupingThreadFactory(new ThreadGroup(threadGroupName), threadGroupName, false);
   }

   /**
    * Returns a thread factory that creates daemon threads belonging to the given group and named
    * after the given group. Thread names are in the format {@code "threadgroupname-1"}. The suffix
    * ({@code "-1"} in the example) contains a unique identifier so that each thread has a distinct
    * name. The identifier starts at 1 for the first thread created, 2 for the second thread, and
    * so on.
    *
    * @param threadGroup the group to which created threads belong
    * @return a thread factory that creates threads belonging to the given group and named after the
    *       given group
    */   public static ThreadFactory newGroupingDaemonThreadFactory(ThreadGroup threadGroup) {
      return groupingThreadFactory(threadGroup, threadGroup.getName(), true);
   }

    /**
     * Returns a thread factory that creates daemon threads belonging to the given group and with
     * the given name prefix. Thread names are in the format {@code "prefix-1"}. The suffix
     * ({@code "-1"} in the example) contains a unique identifier so that each thread has a distinct
     * name. The identifier starts at 1 for the first thread created, 2 for the second thread, and
     * so on.
     *
     * @param threadGroup the group to which created threads belong
     * @param threadNamePrefix the prefix used to create thread names
     * @return a thread factory that creates threads belonging to the given group and wit the given
     *       name prefix
     */
   public static ThreadFactory newGroupingDaemonThreadFactory(ThreadGroup threadGroup,
         String threadNamePrefix) {
      return groupingThreadFactory(threadGroup, threadNamePrefix, true);
   }

   /**
    * Returns a thread factory that creates daemon threads belonging to the given group and with the
    * given name prefix. Thread names are in the format {@code "prefix-1"}. The suffix ({@code "-1"}
    * in the example) contains a unique identifier so that each thread has a distinct name. The
    * identifier starts at 1 for the first thread created, 2 for the second thread, and so on.
    * 
    * <p>A new thread group is created with the given name.
    *
    * @param threadGroupName the name of the new group to which created threads belong
    * @return a thread factory that creates threads belonging to the given group and wit the given
    *       name prefix
    */
   public static ThreadFactory newGroupingDaemonThreadFactory(String threadGroupName) {
      return groupingThreadFactory(new ThreadGroup(threadGroupName), threadGroupName, true);
   }
   
   private static ThreadFactory groupingThreadFactory(ThreadGroup threadGroup,
         String threadNamePrefix, boolean isDaemon) {
      return new ThreadFactory() {
         private final AtomicInteger id = new AtomicInteger();
         
         @Override
         public Thread newThread(Runnable r) {
            String name = threadNamePrefix + "-" + id.incrementAndGet(); 
            Thread t = new Thread(threadGroup, r, name);
            t.setDaemon(isDaemon);
            return t;
         }
         
         @Override
         public String toString() {
            String groupName = threadGroup.getName();
            StringBuilder sb = new StringBuilder();
            sb.append(ThreadFactory.class.getSimpleName()).append(": ")
               .append(groupName);
            if (!groupName.equals(threadNamePrefix)) {
               sb.append("; ").append(threadNamePrefix);
            }
            if (isDaemon) {
               sb.append(" (daemon)");
            }
            return sb.toString();
         }
      };
   }
}
