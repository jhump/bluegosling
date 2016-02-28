package com.bluegosling.choice;

import com.bluegosling.choice.Choice.OfFive;
import com.bluegosling.choice.Choice.OfFour;
import com.bluegosling.choice.Choice.OfThree;
import com.bluegosling.choice.Choice.OfTwo;
import com.bluegosling.concurrent.ListenableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utility methods and interfaces for working with {@link Choice}s. The basic {@link Choice}
 * interface (and its enclosed interfaces, like {@link Choice.Ops3}, etc.) contain abstract
 * operations for working with a choice out of <em>N</em> or more options. Additional interfaces
 * are included herein with more concrete choices. For example, {@link Choice.Ops3} contains
 * operations for choices that have three or more options; whereas the enclosed {@link OfThree}
 * extends those operations for choices that have <em>exactly</em> three options.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
//TODO: describe in doc why interfaces don't have contract* methods?
public class Choices {
   
   /**
    * Coalesces two options into a single result. This is the same as {@link OfTwo#get()} except
    * that it can return a type other than {@code Object} thanks to additional type constraints.
    *
    * @param <T> a super-type of both options in the given choice
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(OfTwo<? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }

   /**
    * Coalesces three options into a single result. This is the same as {@link OfThree#get()}
    * except that it can return a type other than {@code Object} thanks to additional type
    * constraints.
    *
    * @param <T> a super-type of all three options in the given choice
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(OfThree<? extends T, ? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }

   /**
    * Coalesces four options into a single result. This is the same as {@link OfTwo#get()} except
    * that it can return a type other than {@code Object} thanks to additional type constraints.
    *
    * @param <T> a super-type of all four options in the given choice
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(OfFour<? extends T, ? extends T, ? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }

   /**
    * Coalesces five options into a single result. This is the same as {@link OfTwo#get()} except
    * that it can return a type other than {@code Object} thanks to additional type constraints.
    *
    * @param <T> a super-type of all five options in the given choice
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(OfFive<? extends T, ? extends T, ? extends T, ? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }
   
   /**
    * Returns a view of the given future as a {@link Variant2}. Completed futures are inherently a
    * choice between a successful value and cause of failure. This allows conversion of the future
    * into an actual {@link Choice} object.
    *
    * @param future the completed future
    * @return a variant with the first option set to the future's successful result or the second
    *       option set to the cause of failure, depending on the future's actual disposition
    * @throws IllegalArgumentException if the given future is not yet done
    */
   public static <T> Variant2<T, Throwable> fromCompletedFuture(Future<T> future) {
      if (!future.isDone()) {
         throw new IllegalArgumentException("The given future has not yet completed.");
      }
      if (future instanceof ListenableFuture) {
         ListenableFuture<T> lFuture = (ListenableFuture<T>) future;
         if (lFuture.isSuccessful()) {
            return Variant2.withFirst(lFuture.getResult());
         } else if (lFuture.isFailed()) {
            return Variant2.withSecond(lFuture.getFailure());
         } else if (lFuture.isCancelled()) {
            return Variant2.<T, Throwable>withSecond(new CancellationException());
         } else {
            throw new AssertionError("Future is not in valid state");
         }
      } else {
         // future is done, but it is still possible for get() to throw InterruptedException
         boolean interrupted = true;
         try {
            while (true) {
               try {
                  return Variant2.withFirst(future.get());
               } catch (ExecutionException e) {
                  return Variant2.withSecond(e.getCause());
               } catch (CancellationException e) {
                  return Variant2.<T, Throwable>withSecond(e);
               } catch (InterruptedException e) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) {
               Thread.currentThread().interrupt();
            }
         }
      }
   }
}
