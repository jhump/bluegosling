package com.bluegosling.function;

// TODO: doc
@FunctionalInterface
public interface CharConsumer {

   void accept(char value);
   
   default CharConsumer andThen(CharConsumer after) {
      return (ch) -> { accept(ch); after.accept(ch); };
   }
}
