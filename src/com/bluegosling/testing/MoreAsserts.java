package com.bluegosling.testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;

/**
 * Useful test assertions.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MoreAsserts {
   
   /**
    * Like a {@link Runnable}, except it's allowed to throw anything.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Block {
      void run() throws Throwable;
   }

   /**
    * Assert that the given block throws. The type of exception thrown must be an instance of the
    * given type or else the assertion fails. If the block does not throw anything, the assertion
    * also fails.
    *
    * @param thrownType the expected type of what is thrown
    * @param block the block that should throw
    * @return the exception thrown by the given block
    */
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
   
   /**
    * Asserts that the given two values are not equal.
    *
    * @param t1 a value
    * @param t2 another value
    */
   public static <T> void assertNotEquals(T t1, T t2) {
      assertNotEquals(null, t1, t2);
   }

   /**
    * Asserts that the given two values are not equal.
    *
    * @param message the error message if the assertion fails
    * @param t1 a value
    * @param t2 another value
    */
   public static <T> void assertNotEquals(String message, T t1, T t2) {
      assertFalse(message, Objects.equals(t1, t2));
   }
}
