package com.apriori.function;

// TODO: doc
@FunctionalInterface
public interface ShortConsumer {

   void accept(short value);
   
   default ShortConsumer andThen(ShortConsumer after) {
      return (b) -> { accept(b); after.accept(b); };
   }
}
