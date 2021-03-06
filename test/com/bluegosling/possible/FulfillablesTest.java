package com.bluegosling.possible;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;

/**
 * Test cases for {@link Fulfillables}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FulfillablesTest extends AbstractPossibleTest {

   @Override
   protected Fulfillable<String> valuePresent(String s) {
      return Fulfillable.fulfilled(s);
   }

   @Override
   protected Fulfillable<String> valueAbsent() {
      return Fulfillable.create();
   }

   @Test public void fulfill() {
      // already fulfilled cannot be fulfilled again
      Fulfillable<String> f = valuePresent("abc");
      Set<String> set = f.asSet();
      checkSingletonSet(set, "abc");
      assertFalse(f.fulfill("xyz"));
      checkPresentValue(f, "abc"); // value not changed
      checkSingletonSet(set, "abc");

      // initially unfulfilled
      f = valueAbsent();
      set = f.asSet();
      checkEmptySet(set);
      assertTrue(f.fulfill("xyz"));
      checkSingletonSet(set, "xyz"); // change is visible through set view
      checkPresentValue(f, "xyz");
      assertFalse(f.fulfill("abc")); // can only fulfill once
      checkPresentValue(f, "xyz");
      checkSingletonSet(set, "xyz");
   }
}
