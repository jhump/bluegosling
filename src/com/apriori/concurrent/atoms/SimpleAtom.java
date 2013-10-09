package com.apriori.concurrent.atoms;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.concurrent.atomic.AtomicReference;

//TODO: javadoc
//TODO: tests
public class SimpleAtom<T> extends AbstractAtom<T> implements SynchronousAtom<T> {

   private final AtomicReference<T> value;
   
   public SimpleAtom() {
      value = new AtomicReference<T>();
   }
   
   public SimpleAtom(T value) {
      this.value = new AtomicReference<T>(value);
   }

   public SimpleAtom(T value, Predicate<? super T> validator) {
      super(validator);
      this.value = new AtomicReference<T>(value);
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
      notify(oldValue, newValue);
      return oldValue;
   }

   @Override
   public T apply(Function<? super T, ? extends T> function) {
      T oldValue;
      T newValue;
      while (true) {
         oldValue = value.get();
         newValue = function.apply(oldValue);
         validate(newValue);
         if (value.compareAndSet(oldValue, newValue)) {
            break;
         }
      }
      notify(oldValue, newValue);
      return newValue;
   }
}
