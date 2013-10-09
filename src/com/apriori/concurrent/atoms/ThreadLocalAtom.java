package com.apriori.concurrent.atoms;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.atomic.AtomicReference;

// TODO: javadoc
// TODO: tests
public class ThreadLocalAtom<T> extends AbstractAtom<T> implements SynchronousAtom<T> {
   
   private final AtomicReference<T> rootValue;
   private final ThreadLocal<T> value = new ThreadLocal<T>() {
      @Override protected T initialValue() {
         return getRootValue();
      }
   };

   public ThreadLocalAtom() {
      rootValue = new AtomicReference<T>(null);
   }

   public ThreadLocalAtom(T rootValue) {
      this.rootValue = new AtomicReference<T>(rootValue);
   }

   public ThreadLocalAtom(T rootValue, Predicate<? super T> validator) {
      super(validator);
      this.rootValue = new AtomicReference<T>(rootValue);
   }

   @Override
   public T get() {
      return value.get();
   }
   
   @Override
   public T set(T newValue) {
      validate(newValue);
      T oldValue = value.get();
      value.set(newValue);
      return oldValue;
   }

   public T getRootValue() {
      return rootValue.get();
   }
   
   public T setRootValue(T newRootValue) {
      validate(newRootValue);
      T oldRootValue = rootValue.getAndSet(newRootValue);
      notify(oldRootValue, newRootValue);
      return oldRootValue;
   }

   @Override
   public T apply(Function<? super T, ? extends T> function) {
      T oldValue = value.get();
      T newValue = function.apply(oldValue);
      validate(newValue);
      value.set(newValue);
      return newValue;
   }
}
