/**
 * Classes that extend and enhance the API provided by the {@code ScheduledExecutorService}
 * interface.
 * 
 * <p>The idea is to provide a richer API with introspection on status of scheduled tasks, ability
 * to pause recurring tasks, cancel individual instances of the task, and to inject better exception
 * handling when a task throws an exception.
 * 
 * <p><strong>NOTE:</strong> This package is still under construction. See TODOs in 
 * {@code BetterExecutorService.java}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.concurrent;