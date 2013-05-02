package com.apriori.reflect;

// TODO: javadoc
public class ClassHierarchyCrawler<R, P> implements ClassVisitor<R, P> {
   
   public static class Builder<R, P> {
      private ClassVisitor<R, P> visitor;
      private boolean preOrder = true;
      private boolean earlyOut = true;
      private boolean includeInterfaces = true;
      
      Builder() {
      }
      
      public Builder<R, P> forEachClass(ClassVisitor<R, P> aVisitor) {
         if (aVisitor == null) {
            throw new NullPointerException();
         }
         this.visitor = aVisitor;
         return this;
      }
      
      public Builder<R, P> preOrderVisit() {
         this.preOrder = true;
         return this;
      }
      
      public Builder<R, P> postOrderVisit() {
         this.preOrder = false;
         return this;
      }
      
      public Builder<R, P> crawlUntilValueFound() {
         this.earlyOut = true;
         return this;
      }
      
      public Builder<R, P> crawlEverything() {
         this.earlyOut = false;
         return this;
      }
      
      public Builder<R, P> includingInterfaces() {
         this.includeInterfaces = true;
         return this;
      }
      
      public Builder<R, P> skippingInterfaces() {
         this.includeInterfaces = false;
         return this;
      }
      
      public ClassHierarchyCrawler<R, P> build() {
         if (visitor == null) {
            throw new IllegalStateException("Visitor for each class never specified");
         }
         return new ClassHierarchyCrawler<R, P>(visitor, preOrder, earlyOut, includeInterfaces);
      }
   }
   
   public static <R, P> Builder<R, P> builder() {
      return new Builder<R, P>();
   }
   
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
