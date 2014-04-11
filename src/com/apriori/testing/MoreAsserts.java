package com.apriori.testing;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// TODO: doc
public class MoreAsserts {
   
   public interface Block {
      void run() throws Throwable;
   }

   public static <T extends Throwable> T assertThrows(Class<T> thrownType, Block block) {
      try {
         block.run();
         fail("Expecting a "+ thrownType.getName() + " but nothing thrown");
         return null; // make compiler happy; we'll never actually reach this
      } catch (Throwable thrown) {
         assertTrue("Expecting a " + thrownType.getName() + " but caught "
               + thrown.getClass().getName(), thrownType.isInstance(thrown));
         return thrownType.cast(thrown);
      }
   }
}
