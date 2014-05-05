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
 * <p>If the value can only change from being absent to being present then the implementations in
 * this class are thread-safe. If, however, the value can also change from being present to absent,
 * the implementations here are racy and could throw spurious {@link NoSuchElementException}s.
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
            return self.isPresent() ? self.get() : alternate.get();
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
            return self.isPresent() && function.apply(self.get()).isPresent();
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
            return self.isPresent() && predicate.test(self.get());
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
      return isPresent() ? get() : alternate;
   }

   @Override
   public T orElseGet(Supplier<? extends T> supplier) {
      return isPresent() ? get() : supplier.get();
   }

   @Override
   public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwableSupplier) throws X {
      if (isPresent()) {
         return get();
      }
      throw throwableSupplier.get();
   }

   @Override
   public void ifPresent(Consumer<? super T> consumer) {
      if (isPresent()) {
         consumer.accept(get());
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
                        next = get();
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
      return isPresent() ? visitor.present(get()) : visitor.absent();
   }
}
