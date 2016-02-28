package com.bluegosling.function;

// TODO: doc
@FunctionalInterface
public interface BooleanConsumer {

   void accept(boolean value);
   
   default BooleanConsumer andThen(BooleanConsumer after) {
      return (b) -> { accept(b); after.accept(b); };
   }
}
