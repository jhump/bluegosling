package com.apriori.reflect;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * An object that scans an entire type hierarchy. It delegates to a given action for each type in
 * the hierarchy.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <R> result type of invoking the scanner
 * @param <P> parameter type for invoking the scanner (or {@code Void} if there is no parameter)
 * 
 * @see #builder()
 * @see #scanWith(Class, Object, BiFunction)
 */
public class ClassHierarchyScanner<R, P> {
   
   /**
    * Builder pattern for constructing {@link ClassHierarchyScanner} instances.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <R> result type of invoking the scanner
    * @param <P> parameter type for invoking the scanner (or {@code Void} if there is no parameter)
    * 
    * @see ClassHierarchyScanner#builder()
    */
   public static class Builder<R, P> {
      private BiFunction<Class<?>, P, R> fn;
      private boolean preOrder = true;
      private boolean earlyOut = true;
      private boolean includeInterfaces = true;
      
      Builder() {
      }
      
      /**
       * An action that is invoked for each class in the hierarchy.
       * 
       * @param function a function that accepts the visited class and the current context parameter
       *       and returns a result value
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> forEachClass(BiFunction<Class<?>, P, R> function) {
         this.fn = Objects.requireNonNull(function);
         return this;
      }

      /**
       * Indicates that a pre-order traversal is performed. This means that a class in the hierarchy
       * is visited before its ancestor types are visited.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> preOrderVisit() {
         this.preOrder = true;
         return this;
      }
      
      /**
       * Indicates that a post-order traversal is performed. This means that a class in the
       * hierarchy is visited after its ancestor types are visited.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> postOrderVisit() {
         this.preOrder = false;
         return this;
      }
      
      /**
       * Indicates that crawling should end as soon as visiting one of the types results in a
       * non-null value. The first non-null value returned by an action is the returned result from
       * the scan operation.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> scanUntilValueFound() {
         this.earlyOut = true;
         return this;
      }
      
      /**
       * Indicates that all types are scanned. Only the last result, from visiting the last type, is
       * returned.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> scanEverything() {
         this.earlyOut = false;
         return this;
      }
      
      /**
       * Indicates that all interfaces implemented by a type will be included in the scan.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> includingInterfaces() {
         this.includeInterfaces = true;
         return this;
      }
      
      /**
       * Indicates that interfaces are skipped during the scan. Only non-interface types - classes
       * and ancestor classes - are included in the scan.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> skippingInterfaces() {
         this.includeInterfaces = false;
         return this;
      }
      
      /**
       * Builds a scanner with the settings specified via this builder.
       * 
       * @return a type hierarchy crawler
       */
      public ClassHierarchyScanner<R, P> build() {
         if (fn == null) {
            throw new IllegalStateException("Visitor for each class never specified");
         }
         return new ClassHierarchyScanner<R, P>(fn, preOrder, earlyOut, includeInterfaces);
      }
   }
   
   /**
    * Creates a new builder. The default settings include pre-order traversal, stopping the scan on
    * the first non-null result, and including interfaces in the scan.
    * 
    * @return a new builder
    */
   public static <R, P> Builder<R, P> builder() {
      return new Builder<R, P>();
   }
   
   /**
    * Scans the hierarchy of the specified type using the specified action. The default settings
    * will be used: pre-order traversal, stopping the scan on the first non-null result, and
    * including interfaces in the scan.
    * 
    * @param clazz the type whose hierarchy will be scanned
    * @param param an optional parameter
    * @param action the action that is invoked for each type in the hierarchy
    * @return the first non-null result from the specified action
    */
   public static <R, P> R scanWith(Class<?> clazz, P param, BiFunction<Class<?>, P, R> action) {
      return ClassHierarchyScanner.<R, P>builder().forEachClass(action).build()
            .scan(clazz, param);
   }
   
   private final BiFunction<Class<?>, P, R> fn;
   private final boolean preOrder;
   private final boolean earlyOut;
   private final boolean includeInterfaces;
   
   ClassHierarchyScanner(BiFunction<Class<?>, P, R> fn, boolean preOrder, boolean earlyOut,
         boolean includeInterfaces) {
      this.fn = fn;
      this.preOrder = preOrder;
      this.earlyOut = earlyOut;
      this.includeInterfaces = includeInterfaces;
   }
   
   /**
    * Scans the hierarchy of the specified type. The parameter is passed to the underlying action
    * for each type visited. The result will either be the first non-null result from the underlying
    * action or the last result.
    * 
    * @param clazz a type
    * @param param an optional parameter
    * @return a result
    * 
    * @see Builder#scanEverything()
    * @see Builder#scanUntilValueFound() 
    */
   public R scan(Class<?> clazz, P param) {
      R ret = null;
      if (preOrder) {
         ret = fn.apply(clazz, param);
         if (ret != null && earlyOut) {
            return ret;
         }
      }
      // now go through super-classes
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null) {
         R val = scan(superClass, param);
         if (val != null && earlyOut) {
            return val;
         }
      }
      // and interfaces
      if (includeInterfaces) {
         for (Class<?> iface : clazz.getInterfaces()) {
            R val = scan(iface, param);
            if (val != null && earlyOut) {
               return val;
            }
         }
      }
      if (!preOrder) {
         ret = fn.apply(clazz, param);
      }
      return ret;
   }
}
