package com.apriori.testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;


// TODO: doc
public class MoreAsserts {
   
   public interface Block {
      void run() throws Throwable;
   }

   public static <T extends Throwable> T assertThrows(Class<T> thrownType, Block block) {
      try {
         block.run();
      } catch (Throwable thrown) {
         assertTrue("Expecting a " + thrownType.getName() + " but caught "
               + thrown.getClass().getName(), thrownType.isInstance(thrown));
         return thrownType.cast(thrown);
      }
      fail("Expecting a "+ thrownType.getName() + " but nothing thrown");
      return null; // make compiler happy; we'll never actually reach this
   }
   
   public static <T> void assertNotEquals(T t1, T t2) {
      assertNotEquals(null, t1, t2);
   }

   public static <T> void assertNotEquals(String message, T t1, T t2) {
      assertFalse(message, Objects.equals(t1, t2));
   }
}
