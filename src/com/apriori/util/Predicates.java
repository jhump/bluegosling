package com.apriori.util;

import java.util.Objects;
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
   private static Predicate<Object> ACCEPT_ALL = (o) -> true;

   private static Predicate<Object> REJECT_ALL = (o) -> false;

   private static BiPredicate<Object, Object> ACCEPT_ALL2 = (o1, o2) -> true;

   private static BiPredicate<Object, Object> REJECT_ALL2 = (o1, o2) -> false;

   private static TriPredicate<Object, Object, Object> ACCEPT_ALL3 = (o1, o2, o3) -> true;

   private static TriPredicate<Object, Object, Object> REJECT_ALL3 = (o1, o2, o3) -> false;

   private static Predicate<Object> IS_NULL = (o) -> o == null;

   private static Predicate<Object> NOT_NULL = (o) -> o != null;

   @SuppressWarnings("unchecked")
   public static <T> Predicate<T> alwaysAccept() {
      return (Predicate<T>) ACCEPT_ALL;
   }
   
   @SuppressWarnings("unchecked")
   public static <T> Predicate<T> alwaysReject() {
      return (Predicate<T>) REJECT_ALL;
   }

   @SuppressWarnings("unchecked")
   public static <T, U> BiPredicate<T, U> alwaysAcceptBoth() {
      return (BiPredicate<T, U>) ACCEPT_ALL2;
   }
   
   @SuppressWarnings("unchecked")
   public static <T, U> BiPredicate<T, U> alwaysRejectBoth() {
      return (BiPredicate<T, U>) REJECT_ALL2;
   }

   @SuppressWarnings("unchecked")
   public static <T, U, V> TriPredicate<T, U, V> alwaysAcceptAll() {
      return (TriPredicate<T, U, V>) ACCEPT_ALL3;
   }
   
   @SuppressWarnings("unchecked")
   public static <T, U, V> TriPredicate<T, U, V> alwaysRejectAll() {
      return (TriPredicate<T, U, V>) REJECT_ALL3;
   }

   @SuppressWarnings("unchecked")
   public static <T> Predicate<T> isNull() {
      return (Predicate<T>) IS_NULL;
   }

   @SuppressWarnings("unchecked")
   public static <T> Predicate<T> notNull() {
      return (Predicate<T>) NOT_NULL;
   }

   public static <T> Predicate<T> isEqualTo(T object) {
      return curry(Objects::equals, object);
   }

   public static <T, U> BiPredicate<T, U> areSameObject() {
      return (o1, o2) -> o1 == o2;
   }

   // TODO: boolean arithmetic below accept var-args?
   
   /**
    * Returns a predicate that combines the results from two predicates using an AND operation. The
    * operation is short-circuited so that the second predicate will not be invoked if the first
    * predicate returns false.
    * 
    * @param p1 the first predicate
    * @param p2 the second predicate
    * @return a predicate that returns {@code p1.test(input) && p2.test(input)}
    */
   public static <T> Predicate<T> and(Predicate<? super T> p1, Predicate<? super T> p2) {
      return (o) -> p1.test(o) && p2.test(o);
   }

   /**
    * Returns a predicate that combines the results from two predicates using an OR operation. The
    * operation is short-circuited so that the second predicate will not be invoked if the first
    * predicate returns true.
    * 
    * @param p1 the first predicate
    * @param p2 the second predicate
    * @return a predicate that returns {@code p1.test(input) || p2.test(input)}
    */
   public static <T> Predicate<T> or(Predicate<? super T> p1, Predicate<? super T> p2) {
      return (o) -> p1.test(o) || p2.test(o);
   }
   
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
   
   /**
    * Returns a predicate that negates the results of the specified predicate.
    * 
    * @param p a predicate
    * @return a predicate that returns {@code !p.test(input)}
    */
   public static <T> Predicate<T> not(Predicate<? super T> p) {
      return (o) -> !p.test(o);
   }

   // TODO: [Bi,Tri]Predicate versions of above boolean arithmetic combinations

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
