package com.apriori.util;

// TODO: doc
@FunctionalInterface
public interface FloatConsumer {

   void accept(float value);
   
   default FloatConsumer andThen(FloatConsumer after) {
      return (ch) -> { accept(ch); after.accept(ch); };
   }
}
