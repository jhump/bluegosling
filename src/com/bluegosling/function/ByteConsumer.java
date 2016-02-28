package com.bluegosling.function;

// TODO: doc
@FunctionalInterface
public interface ByteConsumer {

   void accept(byte value);
   
   default ByteConsumer andThen(ByteConsumer after) {
      return (b) -> { accept(b); after.accept(b); };
   }
}
