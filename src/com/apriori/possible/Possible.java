package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.Set;

// TODO: javadoc
public interface Possible<T> {
   boolean isPresent();
   Possible<T> or(Possible<T> alternate);
   <U> Possible<U> transform(Function<T, U> function);
   Possible<T> filter(Predicate<T> predicate);
   T get();
   T getOr(T alternate);
   <X extends Throwable> T getOr(X throwable) throws X;
   Set<T> asSet();
   <R> R visit(Visitor<T, R> visitor);
   
   interface Visitor<T, R> {
      R present(T t);
      R absent();
   }
}
