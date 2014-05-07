package com.apriori.possible;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An abstract base class for possible values that are dynamic, meaning whether a value is present
 * or not can change over the life of the value. Possible values that are immutable should not
 * extend this base class as they can be implemented much more efficiently due to their unchanging
 * state.
 * 
 * <p>The default implementations in this base class are thread-safe and suitable for sub-classes
 * that support concurrent usage. Where appropriate, this is handled by first checking the abstract
 * {@link #isPresent()} before calling {@link #get()}. However, {@code get()} could still throw if
 * the value suddenly becomes absent. So the implementations catch {@link NoSuchElementException}
 * and then proceed as if {@code isPresent()} had returned false.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public abstract class AbstractDynamicPossible<T> implements Possible<T> {

   /**
    * Returns a possible that reflects either the value of this possible or that of the given
    * alternate, depending on whether or not this possible is present.
    */
   @Override
   public Possible<T> or(Possible<T> alternate) {
      AbstractDynamicPossible<T> self = this;
      return new AbstractDynamicPossible<T>() {
         @Override
         public boolean isPresent() {
            return self.isPresent() || alternate.isPresent();
         }

         @Override
         public T get() {
            if (self.isPresent()) {
               try {
                  return self.get();
               } catch (NoSuchElementException e) {
                  // in case there was a race and the value became absent, fall through
               }
            }
            return alternate.get();
         }
      };
   }

   /**
    * Returns a possible that reflects the result of applying the given function to this possible
    * value, if present.
    * 
    * <p>At any given moment, the returned possible is present if this possible is present.
    * Similarly, for any given moment, the returned possible's value is the result of applying the
    * given function to the value of this possible, if present. Since this possible's value is
    * dynamic, the result of applying the function is not memo-ized. So the function may be invoked
    * repeatedly by usages of the returned possible.
    */
   @Override
   public <U> Possible<U> map(Function<? super T, ? extends U> function) {
      AbstractDynamicPossible<T> self = this;
      return new AbstractDynamicPossible<U>() {
         @Override
         public boolean isPresent() {
            return self.isPresent();
         }

         @Override
         public U get() {
            return function.apply(self.get());
         }
      };
   }

   /**
    * Returns a possible that reflects the result of applying the given function to this possible
    * value, if present.
    * 
    * <p>At any given moment, the value of the returned possible will reflect the result of
    * applying the given function to the value of this possible at that same moment. Just like with
    * {@link #map(Function)}, the given function may be invoked repeatedly by usages of the returned
    * possible.
    */
   @Override
   public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
      AbstractDynamicPossible<T> self = this;
      return new AbstractDynamicPossible<U>() {
         @Override
         public boolean isPresent() {
            if (self.isPresent()) {
               boolean reallyPresent = true;
               T t = null;
               try {
                  t = self.get();
               } catch (NoSuchElementException e) {
                  // in case there was a race and the value became absent
                  reallyPresent = false;
               }
               if (reallyPresent) {
                  return function.apply(t).isPresent();
               }
            }
            return false;
         }

         @Override
         public U get() {
            return function.apply(self.get()).get();
         }
      };
   }

   /**
    * Returns a possible that reflects the result of applying the given predicate to this possible
    * value, if present.
    * 
    * <p>At any given moment, the returned possible is present if and only if this possible is
    * present and this possible's value matches the given predicate. Since this possible's value is
    * dynamic, the result of applying the predicate is not memo-ized. So the predicate may be
    * tested repeatedly by usages of the returned possible.
    */
   @Override
   public Possible<T> filter(Predicate<? super T> predicate) {
      AbstractDynamicPossible<T> self = this;
      return new AbstractDynamicPossible<T>() {
         @Override
         public boolean isPresent() {
            if (self.isPresent()) {
               boolean reallyPresent = true;
               T t = null;
               try {
                  t = self.get();
               } catch (NoSuchElementException e) {
                  // in case there was a race and the value became absent
                  reallyPresent = false;
               }
               if (reallyPresent) {
                  return predicate.test(t);
               }
            }
            return false;
         }

         @Override
         public T get() {
            T result = self.get();
            if (predicate.test(result)) {
               return result;
            }
            throw new NoSuchElementException();
         }
      };
   }

   @Override
   public T orElse(T alternate) {
      if (isPresent()) {
         try {
            return get();
         } catch (NoSuchElementException e) {
            // in case there was a race and the value became absent, fall through
         }
      }
      return alternate;
   }

   @Override
   public T orElseGet(Supplier<? extends T> supplier) {
      if (isPresent()) {
         try {
            return get();
         } catch (NoSuchElementException e) {
            // in case there was a race and the value became absent, fall through
         }
      }
      return supplier.get();
   }

   @Override
   public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwableSupplier) throws X {
      if (isPresent()) {
         try {
            return get();
         } catch (NoSuchElementException e) {
            // in case there was a race and the value became absent, fall through
         }
      }
      throw throwableSupplier.get();
   }

   @Override
   public void ifPresent(Consumer<? super T> consumer) {
      if (isPresent()) {
         boolean reallyPresent = true;
         T t = null;
         try {
            t = get();
         } catch (NoSuchElementException e) {
            // in case there was a race and the value became absent
            reallyPresent = false;
         }
         if (reallyPresent) {
            consumer.accept(t);
         }
      }
   }

   /**
    * Returns a view of this possible as a set. At any given moment, the set may be an empty set
    * or a singleton set, based on whether or not this possible is present.
    */
   @Override
   public Set<T> asSet() {
      return new AbstractSet<T>() {
         @Override
         public Iterator<T> iterator() {
            return new Iterator<T>() {
               boolean foundNext;
               boolean hasNext;
               T next;
               
               private void findNext() {
                  if (!foundNext) {
                     hasNext = isPresent();
                     if (hasNext) {
                        try {
                           next = get();
                        } catch (NoSuchElementException e) {
                           // in case there was a race and the value became absent
                           hasNext = false;
                           next = null;
                        }
                     } else {
                        next = null;
                     }
                     foundNext = true;
                  }
               }
               
               @Override
               public boolean hasNext() {
                  findNext();
                  return hasNext;
               }

               @Override
               public T next() {
                  findNext();
                  if (hasNext) {
                     // no next after consuming the single present value 
                     hasNext = false;
                     T ret = next;
                     next = null;
                     return ret;
                  }
                  throw new NoSuchElementException();
               }

               @Override
               public void remove() {
                  throw new UnsupportedOperationException();
               }
            };
         }

         @Override
         public int size() {
            return isPresent() ? 1 : 0;
         }
      };
   }

   @Override
   public <R> R visit(Visitor<? super T, R> visitor) {
      if (isPresent()) {
         boolean reallyPresent = true;
         T t = null;
         try {
            t = get();
         } catch (NoSuchElementException e) {
            // in case there was a race and the value became absent
            reallyPresent = false;
         }
         if (reallyPresent) {
            return visitor.present(t);
         }
      }
      return visitor.absent();
   }
}
