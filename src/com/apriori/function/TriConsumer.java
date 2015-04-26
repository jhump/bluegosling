package com.apriori.function;

// TODO: javadoc
@FunctionalInterface
public interface TriConsumer<T, U, V> {
   void accept(T input1, U input2, V input3);

   default TriConsumer<T, U, V> andThen(TriConsumer<T, U, V> after) {
      return (t, u, v) -> { accept(t, u, v); after.accept(t, u, v); };
   }
}
