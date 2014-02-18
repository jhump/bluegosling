package com.apriori.reflect;

/**
 * A {@link ClassVisitor} that crawls an entire type hierarchy. It delegates to another visitor for
 * each type in the hierarchy.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <R> result type of invoking the crawler
 * @param <P> parameter type for invoking the crawler (or {@code Void} if there is no parameter)
 * 
 * @see #builder()
 * @see #crawlWith(Class, Object, ClassVisitor)
 */
public class ClassHierarchyCrawler<R, P> implements ClassVisitor<R, P> {
   
   /**
    * Builder pattern for constructing {@link ClassHierarchyCrawler} instances.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <R> result type of invoking the crawler
    * @param <P> parameter type for invoking the crawler (or {@code Void} if there is no parameter)
    * 
    * @see ClassHierarchyCrawler#builder()
    */
   public static class Builder<R, P> {
      private ClassVisitor<R, P> visitor;
      private boolean preOrder = true;
      private boolean earlyOut = true;
      private boolean includeInterfaces = true;
      
      Builder() {
      }
      
      /**
       * A visitor that is invoked for each class in the hierarchy.
       * 
       * @param aVisitor a visitor
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> forEachClass(ClassVisitor<R, P> aVisitor) {
         if (aVisitor == null) {
            throw new NullPointerException();
         }
         this.visitor = aVisitor;
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
       * non-null value. The first non-null value returned by a visitor is the returned result from
       * the crawl.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> crawlUntilValueFound() {
         this.earlyOut = true;
         return this;
      }
      
      /**
       * Indicates that all types are crawled. Only the last result, from visiting the last type, is
       * returned.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> crawlEverything() {
         this.earlyOut = false;
         return this;
      }
      
      /**
       * Indicates that all interfaces implemented by a type will be included in the crawl.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> includingInterfaces() {
         this.includeInterfaces = true;
         return this;
      }
      
      /**
       * Indicates that interfaces are skipped during the crawl. Only non-interface types - classes
       * and ancestor classes - are included in the crawl.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> skippingInterfaces() {
         this.includeInterfaces = false;
         return this;
      }
      
      /**
       * Builds a crawler with the settings specified via this builder.
       * 
       * @return a type hierarchy crawler
       */
      public ClassHierarchyCrawler<R, P> build() {
         if (visitor == null) {
            throw new IllegalStateException("Visitor for each class never specified");
         }
         return new ClassHierarchyCrawler<R, P>(visitor, preOrder, earlyOut, includeInterfaces);
      }
   }
   
   /**
    * Creates a new builder. The default settings include pre-order traversal, stopping the crawl on
    * the first non-null result, and including interfaces in the crawl.
    * 
    * @return a new builder
    */
   public static <R, P> Builder<R, P> builder() {
      return new Builder<R, P>();
   }
   
   /**
    * Crawls the hierarchy of the specified type using the specified visitor. The default settings
    * will be used: pre-order traversal, stopping the crawl on the first non-null result, and
    * including interfaces in the crawl.
    * 
    * @param clazz the type whose hierarchy will be crawled
    * @param param an optional parameter
    * @param visitor the visitor that is invoked for each type in the hierarchy
    * @return the first non-null result from the specified visitor
    */
   public static <R, P> R crawlWith(Class<?> clazz, P param, ClassVisitor<R, P> visitor) {
      return ClassHierarchyCrawler.<R, P> builder().forEachClass(visitor).build().visit(clazz, param);
   }
   
   private final ClassVisitor<R, P> visitor;
   private final boolean preOrder;
   private final boolean earlyOut;
   private final boolean includeInterfaces;
   
   ClassHierarchyCrawler(ClassVisitor<R, P> visitor, boolean preOrder, boolean earlyOut, boolean includeInterfaces) {
      this.visitor = visitor;
      this.preOrder = preOrder;
      this.earlyOut = earlyOut;
      this.includeInterfaces = includeInterfaces;
   }
   
   /**
    * Crawls the hierarchy of the specified type. The parameter is passed to the underlying visitor
    * for each type visited. The result will either be the first non-null result from the underlying
    * visitor or the last result.
    * 
    * @param clazz a type
    * @param param an optional parameter
    * @return a result
    * 
    * @see Builder#crawlEverything()
    * @see Builder#crawlUntilValueFound() 
    */
   @Override
   public R visit(Class<?> clazz, P param) {
      R ret = null;
      if (preOrder) {
         ret = visitor.visit(clazz, param);
         if (ret != null && earlyOut) {
            return ret;
         }
      }
      // now go through super-classes
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null) {
         R val = visit(superClass, param);
         if (val != null && earlyOut) {
            return val;
         }
      }
      // and interfaces
      if (includeInterfaces) {
         for (Class<?> iface : clazz.getInterfaces()) {
            R val = visit(iface, param);
            if (val != null && earlyOut) {
               return val;
            }
         }
      }
      if (!preOrder) {
         ret = visitor.visit(clazz, param);
      }
      return ret;
   }
}
