package com.apriori.choice;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.util.Function;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utility methods and interfaces for working with {@link Choice}s. The basic {@link Choice}
 * interface (and its enclosed interfaces, like {@link Choice.Ops3}, etc.) contain abstract
 * operations for working with a choice out of <em>N</em> or more options. Additional interfaces
 * are included herein with more concrete choices. For example, {@link Choice.Ops3} contains
 * operations for choices that have three or more options; whereas the enclosed {@link Choices3}
 * extends those operations for choices that have <em>exactly</em> three options.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public class Choices {
   /**
    * A visitor for a choice with two options. When passed to {@link Choices2#visit(Choices.Visitor2)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Visitor2<A, B, R> {
      /**
       * Called when visiting a choice where the first option is present.
       *
       * @param a the value of the first option that was present
       * @return the result of visiting the option
       */
      R visitFirst(A a);

      /**
       * Called when visiting a choice where the second option is present.
       *
       * @param b the value of the second option that was present
       * @return the result of visiting the option
       */
      R visitSecond(B b);
   }
   
   /**
    * A visitor for a choice with three options. When passed to {@link Choices3#visit(Choices.Visitor3)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <C> the type of the choice's third option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Visitor3<A, B, C, R> extends Visitor2<A, B, R> {
      /**
       * Called when visiting a choice where the third option is present.
       *
       * @param c the value of the third option that was present
       * @return the result of visiting the option
       */
      R visitThird(C c);
   }

   /**
    * A visitor for a choice with four options. When passed to {@link Choices4#visit(Choices.Visitor4)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <C> the type of the choice's third option
    * @param <D> the type of the choice's fourth option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Visitor4<A, B, C, D, R> extends Visitor3<A, B, C, R> {
      /**
       * Called when visiting a choice where the fourth option is present.
       *
       * @param d the value of the fourth option that was present
       * @return the result of visiting the option
       */
      R visitFourth(D d);
   }

   /**
    * A visitor for a choice with five options. When passed to {@link Choices5#visit(Choices.Visitor5)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <C> the type of the choice's third option
    * @param <D> the type of the choice's fourth option
    * @param <E> the type of the choice's fifth option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Visitor5<A, B, C, D, E, R> extends Visitor4<A, B, C, D, R> {
      /**
       * Called when visiting a choice where the fifth option is present.
       *
       * @param e the value of the fifth option that was present
       * @return the result of visiting the option
       */
      R visitFifth(E e);
   }
   
   /**
    * Operations for a {@link Choice} that has exactly two options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Choices2<A, B> extends Choice<A, B> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link Visitor2#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(Visitor2<? super A, ? super B, R> visitor);
      
      /**
       * Adapts this choice of two options into a choice of three options. In the returned choice,
       * the third option will never be present. If the first option is present in this choice, it
       * will be present in the returned choice; same goes for the second option.
       *
       * @return a choice of three options with the same present option as this
       */
      <C> Choices3<A, B, C> expand();

      // co-variantly constraint return type to a Choices2
      @Override <T> Choices2<T, B> transformFirst(Function<? super A, ? extends T> function);

      // co-variantly constraint return type to a Choices2
      @Override <T> Choices2<A, T> transformSecond(Function<? super B, ? extends T> function);
   }

   public interface Choices3<A, B, C> extends Choice.Ops3<A, B, C> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link Visitor3#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(Visitor3<? super A, ? super B, ? super C, R> visitor);
      
      /**
       * Adapts this choice of three options into a choice of four options. In the returned choice,
       * the fourth option will never be present. If the first option is present in this choice, it
       * will be present in the returned choice; same goes for the second and third options.
       *
       * @return a choice of four options with the same present option as this
       */
      <D> Choices4<A, B, C, D> expand();
      
      // co-variantly constraint return type to a Choices3
      @Override <T> Choices3<T, B, C> transformFirst(Function<? super A, ? extends T> function);

      // co-variantly constraint return type to a Choices3
      @Override <T> Choices3<A, T, C> transformSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constraint return type to a Choices3
      @Override <T> Choices3<A, B, T> transformThird(Function<? super C, ? extends T> function);
   }

   public interface Choices4<A, B, C, D> extends Choice.Ops4<A, B, C, D> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link Visitor4#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(Visitor4<? super A, ? super B, ? super C, ? super D, R> visitor);

      /**
       * Adapts this choice of four options into a choice of five options. In the returned choice,
       * the fifth option will never be present. If the first option is present in this choice, it
       * will be present in the returned choice; same goes for the second, third, and fourth
       * options.
       *
       * @return a choice of five options with the same present option as this
       */
      <E> Choices5<A, B, C, D, E> expand();
      
      // co-variantly constraint return type to a Choices4
      @Override <T> Choices4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function);
      
      // co-variantly constraint return type to a Choices4
      @Override <T> Choices4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constraint return type to a Choices4
      @Override <T> Choices4<A, B, T, D> transformThird(Function<? super C, ? extends T> function);
      
      // co-variantly constraint return type to a Choices4
      @Override <T> Choices4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function);
   }

   public interface Choices5<A, B, C, D, E> extends Choice.Ops5<A, B, C, D, E> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link Visitor5#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(Visitor5<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor);
      
      // co-variantly constraint return type to a Choices5
      @Override <T> Choices5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function);
      
      // co-variantly constraint return type to a Choices5
      @Override <T> Choices5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constraint return type to a Choices5
      @Override <T> Choices5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function);
      
      // co-variantly constraint return type to a Choices5
      @Override <T> Choices5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function);
      
      // co-variantly constraint return type to a Choices5
      @Override <T> Choices5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function);
   }
   
   /**
    * Coalesces two options into a single result. This is the same as {@link Choices2#get()} except
    * that it can return a type other than {@code Object} thanks to additional type constraints.
    *
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(Choices2<? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }

   /**
    * Coalesces three options into a single result. This is the same as {@link Choices3#get()}
    * except that it can return a type other than {@code Object} thanks to additional type
    * constraints.
    *
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(Choices3<? extends T, ? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }

   /**
    * Coalesces four options into a single result. This is the same as {@link Choices2#get()} except
    * that it can return a type other than {@code Object} thanks to additional type constraints.
    *
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(Choices4<? extends T, ? extends T, ? extends T, ? extends T> choice) {
      @SuppressWarnings("unchecked") // all components extend T, so this will be safe
      T ret = (T) choice.get();
      return ret;
   }

   /**
    * Coalesces five options into a single result. This is the same as {@link Choices2#get()} except
    * that it can return a type other than {@code Object} thanks to additional type constraints.
    *
    * @param choice the choice to coalesce
    * @return the value of the choice's present option
    */
   public static <T> T coalesce(Choices5<? extends T, ? extends T, ? extends T, ? extends T, ? extends T> choice) {
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
