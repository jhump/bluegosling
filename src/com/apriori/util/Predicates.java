package com.apriori.util;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Numerous utility methods related to using {@link Predicate}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Functions
 */
//TODO: tests
public final class Predicates {
   private Predicates() {
   }

   // TODO: javadoc

   public static <T> Predicate<T> alwaysAccept() {
      return (o) -> true;
   }
   
   public static <T> Predicate<T> alwaysReject() {
      return (o) -> false;
   }

   public static <T, U> BiPredicate<T, U> alwaysAcceptBoth() {
      return (o1, o2) -> true;
   }
   
   public static <T, U> BiPredicate<T, U> alwaysRejectBoth() {
      return (o1, o2) -> false;
   }

   public static <T, U, V> TriPredicate<T, U, V> alwaysAcceptAll() {
      return (o1, o2, o3) -> true;
   }
   
   public static <T, U, V> TriPredicate<T, U, V> alwaysRejectAll() {
      return (o1, o2, o3) -> false;
   }

   public static <T> Predicate<T> isNull() {
      return (o) -> o == null;
   }

   public static <T> Predicate<T> notNull() {
      return (o) -> o != null;
   }

   public static <T> Predicate<T> isSameAs(T object) {
      return o -> o == object;
   }

   public static <T, U> BiPredicate<T, U> areSameObject() {
      return (o1, o2) -> o1 == o2;
   }
   
   public static <T> Predicate<T> everyOther() {
      return new Predicate<T>() {
         private boolean accept = true;

         @Override
         public boolean test(T t) {
            boolean ret = accept;
            accept = !accept;
            return ret;
         }
      };
   }
   
   public static <T> Predicate<T> every(int n) {
      if (n < 1) {
         throw new IllegalArgumentException();
      }
      return new Predicate<T>() {
         private int i;

         @Override
         public boolean test(T t) {
            boolean ret = i == 0;
            i++;
            if (i == n) {
               i = 0;
            }
            return ret;
         }
      };
   }

   // TODO: boolean arithmetic below accept var-args?
   
   /**
    * Returns a predicate that combines the results from two predicates using an XOR operation.
    * 
    * @param p1 the first predicate
    * @param p2 the second predicate
    * @return a predicate that returns {@code p1.test(input) ^ p2.test(input)}
    */
   public static <T> Predicate<T> xor(Predicate<? super T> p1, Predicate<? super T> p2) {
      return (o) -> p1.test(o) ^ p2.test(o);
   }

   // TODO: javadoc

   public static <T, U> BiPredicate<T, U> xor(BiPredicate<? super T, ? super U> p1,
         BiPredicate<? super T, ? super U> p2) {
      return (t, u) -> p1.test(t, u) ^ p2.test(t, u);
   }

   public static <T, U, V> TriPredicate<T, U, V> xor(
         TriPredicate<? super T, ? super U, ? super V> p1,
         TriPredicate<? super T, ? super U, ? super V> p2) {
      return (t, u, v) -> p1.test(t, u, v) ^ p2.test(t, u, v);
   }
   
   static boolean toPrimitive(Boolean b) {
      return b != null && b;
   }
   
   /**
    * Converts a function into a predicate. Functions return boxed booleans instead of primitives,
    * and thus can return a {@code null}. The returned predicate returns true if the function
    * returns true and false otherwise, so a {@code null} function result is considered false.
    * 
    * @param function a function
    * @return the specified function converted to a predicate
    */
   public static <T> Predicate<T> fromFunction(Function<T, Boolean> function) {
      return (o) -> toPrimitive(function.apply(o));
   }

   /**
    * Converts a bivariate function into a bivariate predicate. Functions return boxed booleans
    * instead of primitives, and thus can return a {@code null}. The returned predicate returns
    * true if the function returns true and false otherwise, so a {@code null} function result is
    * considered false.
    * 
    * @param function a function
    * @return the specified function converted to a predicate
    */
   public static <T1, T2> BiPredicate<T1, T2> fromFunction(BiFunction<T1, T2, Boolean> function) {
      return (o1, o2) -> toPrimitive(function.apply(o1, o2));
   }

   /**
    * Converts a three-argument function into a three-argument predicate. Functions return boxed
    * booleans instead of primitives, and thus can return a {@code null}. The returned predicate
    * returns true if the function returns true and false otherwise, so a {@code null} function
    * result is considered false.
    * 
    * @param function a function
    * @return the specified function converted to a predicate
    */
   public static <T1, T2, T3> TriPredicate<T1, T2, T3> fromFunction(
         TriFunction<T1, T2, T3, Boolean> function) {
      return (o1, o2, o3) -> toPrimitive(function.apply(o1, o2, o3));
   }
   
   // TODO: javadoc
   public static <I> BooleanSupplier curry(Predicate<? super I> predicate, I arg) {
      return () -> predicate.test(arg);
   }

   public static <I1, I2> BooleanSupplier curry(BiPredicate<? super I1, ? super I2> predicate,
         I1 arg1, I2 arg2) {
      return () -> predicate.test(arg1, arg2);
   }

   public static <I1, I2> Predicate<I2> curry(BiPredicate<? super I1, ? super I2> predicate,
         I1 arg1) {
      return (arg2) -> predicate.test(arg1, arg2);
   }

   public static <I1, I2, I3> BooleanSupplier curry(
         TriPredicate<? super I1, ? super I2, ? super I3> predicate, I1 arg1, I2 arg2, I3 arg3) {
      return () -> predicate.test(arg1, arg2, arg3);
   }
   
   public static <I1, I2, I3> Predicate<I3> curry(
         TriPredicate<? super I1, ? super I2, ? super I3> predicate, I1 arg1, I2 arg2) {
      return (arg3) -> predicate.test(arg1, arg2, arg3);
   }
   
   public static <I1, I2, I3> BiPredicate<I2, I3> curry(
         TriPredicate<? super I1, ? super I2, ? super I3> predicate, I1 arg1) {
      return (arg2, arg3) -> predicate.test(arg1, arg2, arg3);
   }
}
