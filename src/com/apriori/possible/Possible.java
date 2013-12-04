package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;
import com.apriori.util.Source;

import java.util.Set;

/**
 * A possible value. A value might be present, but it may not be. Some implementations may allow a
 * "present" value to be null. Others may treat null the same as an absent value. Some
 * implementations may be mutable and provide additional operations for setting or clearing the
 * value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the possible value
 */
public interface Possible<T> {
   /**
    * Returns true if a value is present.
    * 
    * @return true if a value is present; false otherwise.
    */
   boolean isPresent();
   
   /**
    * Returns the current possible value if present or the specified value if not.
    * 
    * @param alternate an alternate value
    * @return returns the current possible value if present or the alternate if not
    */
   Possible<T> or(Possible<T> alternate);
   
   /**
    * Returns the current possible value, transformed by the specified function, if present.
    * 
    * @param function the function used to transform the value
    * @return returns the current value, transformed by the specified function, if present;
    *       otherwise returns a possible value that is absent
    */
   <U> Possible<U> transform(Function<? super T, ? extends U> function);
   
   /**
    * Filters the current value per the specified predicate. If a value is present and it matches
    * the specified predicate then it is returned. Otherwise a possible value that is absent is
    * returned.
    * 
    * @param predicate the predicated used to test the value
    * @return returns the current value if present and it matches the specified predicate; otherwise
    *       returns a possible value that is absent
    */
   Possible<T> filter(Predicate<? super T> predicate);
   
   /**
    * Gets the contained value if present.
    * 
    * @return the contained value
    * @throws IllegalStateException if a value is not present
    */
   T get();
   
   /**
    * Gets the contained value if present or the specified value if not.
    * 
    * @param alternate the alternate value
    * @return the contained value if present or the alternate if not
    */
   T getOr(T alternate);
   
   /**
    * Gets the contained value or throws the specified exception if not
    * 
    * @param throwable the exception to throw if a value is not present
    * @return the contained value
    * @throws X if a value is not present
    */
   <X extends Throwable> T getOrThrow(X throwable) throws X;
   
   /**
    * Gets the contained value or gets an exception from the specified source and throws it
    * 
    * @param throwable a source of the exception to throw if a value is not present
    * @return the contained value
    * @throws X if a value is not present
    */
   <X extends Throwable> T getOrThrow(Source<X> throwableSource) throws X;
   
   /**
    * Returns a view of this possible value as a set. If a value is present, a singleton set with
    * that value is returned. Otherwise, an empty set is returned.
    * 
    * @return a singleton set with the contained value, if present; an empty set otherwise
    */
   Set<T> asSet();
   
   /**
    * Invokes the appropriate method on the specified visitor. Returns the value that the visitor
    * produces.
    * 
    * @param visitor a visitor
    * @return the value returned by the visitor
    */
   <R> R visit(Visitor<T, R> visitor);
   
   /**
    * A visitor of possible values. When visited, one of the methods will be invoked, depending on
    * whether or not a value is present.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the possible value
    * @param <R> the type returned by the visitor
    */
   interface Visitor<T, R> {
      /**
       * Visits a possible value where a value is present.
       * 
       * @param t the contained value
       * @return the result of visiting the contained value
       */
      R present(T t);
      
      /**
       * Visits a possible value where no value is actually present.
       * 
       * @return the result of visiting a possible value where no value is actually present
       */
      R absent();
   }
}
