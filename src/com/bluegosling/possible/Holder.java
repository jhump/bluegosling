package com.bluegosling.possible;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@linkplain Possible possible} value that is mutable. The contained value can be set or
 * cleared programmatically. So the state of whether a value is present can change. This class
 * allows values, when present, to be {@code null}.
 * 
 * <p>This is similar to a mutable reference, not unlike {@link AtomicReference}. But this class
 * can distinguish between a {@code null} value and one that is not present. Also note that
 * instances of this class are <em>not</em> thread-safe.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the held value
 */
public final class Holder<T> extends AbstractDynamicPossible<T> implements Serializable {
   
   private static final long serialVersionUID = -1290017626307767440L;

   /**
    * Creates a new instance whose value is initially absent.
    * 
    * @return a new holder with no held value
    */
   public static <T> Holder<T> create() {
      return new Holder<T>();
   }

   /**
    * Creates a new instance that holds the specified value.
    * 
    * @param t the held value
    * @return a new instance that holds the specified value
    */
   public static <T> Holder<T> create(T t) {
      return new Holder<T>(t);
   }
   
   private T value;
   private boolean isPresent;
   
   private Holder() {
   }

   private Holder(T t) {
      value = t;
      isPresent = true;
   }
   
   /**
    * Sets the held value.
    * 
    * @param t the new held value
    * @return the previously held value or {@code null} if no value was present
    */
   public T set(T t) {
      T ret = value;
      value = t;
      isPresent = true;
      return ret;
   }
   
   /**
    * Clears the held value.
    * 
    * @return the previously held value or {@code null} if no value was present
    */
   public T clear() {
      T ret = value;
      value = null;
      isPresent = false;
      return ret;
   }

   @Override
   public boolean isPresent() {
      return isPresent;
   }
   
   // TODO: tests for apply(...)
   
   /**
    * Applies the specified function to the held value, if present. If no value is present, nothing
    * is done. Upon return, the held value will be the function's result.
    *
    * @param function the function to apply
    */
   public void apply(Function<? super T, ? extends T> function) {
      if (isPresent) {
         value = function.apply(value);
      }
   }
   
   /**
    * Applies the specified predicate to the held value, if present. If no value is present, nothing
    * is done. Upon return, a held value will only be present if a held value was initially present,
    * and it matched the given predicate.
    *
    * @param predicate the predicate to apply
    */
   public void apply(Predicate<? super T> predicate) {
      if (isPresent && !predicate.test(value)) {
         isPresent = false;
         value = null;
      }
   }

   @Override
   public T get() {
      if (!isPresent) {
         throw new NoSuchElementException();
      }
      return value;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>The returned set, being a view of this possible value, is mutable. If it is empty then the
    * held value is absent and a call to {@link Set#add(Object)} will effectively set the held
    * value. If a held value is already present, however, trying to add an item to the set will
    * throw an {@link IllegalStateException} since only a single held value is permitted. If a held
    * value is present, it can be removed from the set, effectively clearing the held value.
    * 
    * <p>As the held value is changed via calls to {@link #set(Object)} or {@link #clear()}, the
    * set will also change. If the held value is changed directly through the {@link Holder}
    * interface while a set iteration is in progress, the iterator will throw a
    * {@link ConcurrentModificationException}. Since this class is not thread-safe, do not count on
    * this exception for correctness. It is just a best effort to fail-fast in the case of what is
    * clearly a programming error.
    */
   @SuppressWarnings("synthetic-access") // anonymous sub-class accesses private fields
   @Override
   public Set<T> asSet() {
      return new AbstractSet<T>() {
         
         @Override
         public boolean add(T t) {
            if (isPresent) {
               if (value == null ? t == null : value.equals(t)) {
                  return false;
               }
               throw new IllegalStateException("Holder cannot contain more than one element");
            } else {
               set(t);
            }
            return true;
         }
         
         @Override
         public int size() {
            return isPresent ? 1 : 0;
         }
         
         @Override
         public Iterator<T> iterator() {
            return new Iterator<T>() {
               private final boolean hasNext = isPresent;
               private final T next = isPresent ? value : null;
               private boolean consumed;
               private boolean removed;
               
               private void checkMod() {
                  if (hasNext != isPresent || (hasNext && next != value)) {
                     throw new ConcurrentModificationException();
                  }
               }
               
               @Override
               public boolean hasNext() {
                  if (removed) {
                     return false;
                  }
                  checkMod();
                  return hasNext && !consumed;
               }

               @Override
               public T next() {
                  checkMod();
                  if (consumed || !hasNext) {
                     throw new NoSuchElementException();
                  }
                  consumed = true;
                  return next;
               }

               @Override
               public void remove() {
                  if (!consumed || removed) {
                     throw new IllegalStateException();
                  }
                  checkMod();
                  Holder.this.clear();
                  removed = true;
               }
            };
         }
      };
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Holder)) {
         return false;
      }
      Holder<?> other = (Holder<?>) o;
      return isPresent
            ? other.isPresent && (value == null ? other.value == null : value.equals(other.value))
            : !other.isPresent;
   }
   
   @Override
   public int hashCode() {
      return Holder.class.hashCode() ^
            (isPresent ? (value == null ? 0 : value.hashCode()) : -1);
   }
   
   @Override
   public String toString() {
      return isPresent ? "Holder: " + value : "Holder, cleared";
   }
}
