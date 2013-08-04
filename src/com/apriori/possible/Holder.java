package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
// TODO: tests
public class Holder<T> implements Possible<T>, Serializable {
   
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

   @Override
   public Possible<T> or(Possible<T> alternate) {
      return isPresent ? this : alternate;
   }

   /**
    * {@inheritDoc}
    * 
    * The returned object will not be a {@link Holder} and thus not be mutable.
    */
   @Override
   public <U> Possible<U> transform(Function<T, U> function) {
      return isPresent
            ? Reference.set(function.apply(value))
            : Reference.<U>unset();
   }

   /**
    * {@inheritDoc}
    * 
    * The returned object will not be a {@link Holder} and thus not be mutable.
    */
   @Override
   public Possible<T> filter(Predicate<T> predicate) {
      return isPresent && predicate.apply(value)
            ? Reference.set(value)
            : Reference.<T>unset();
   }

   @Override
   public T get() {
      if (!isPresent) {
         throw new IllegalStateException();
      }
      return value;
   }

   @Override
   public T getOr(T alternate) {
      return isPresent ? value : alternate;
   }

   @Override
   public <X extends Throwable> T getOr(X throwable) throws X {
      if (!isPresent) {
         throw throwable;
      }
      return value;
   }

   @Override
   public Set<T> asSet() {
      return isPresent ? Collections.singleton(value) : Collections.<T>emptySet();
   }

   @Override
   public <R> R visit(Possible.Visitor<T, R> visitor) {
      return isPresent ? visitor.present(value) : visitor.absent();
   }
   
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Holder)) {
         return false;
      }
      Holder<?> other = (Holder<?>) o;
      return isPresent
            ? (value == null ? other.value == null : value.equals(other.value))
            : !other.isPresent;
   }
   
   @Override
   public int hashCode() {
      return Holder.class.hashCode() ^
            (isPresent ? (value != null ? value.hashCode() : 0) : -1);
   }
   
   @Override
   public String toString() {
      return isPresent ? "Holder: " + value : "Holder, cleared";
   }
}
