package com.bluegosling.possible;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.Optional;

/**
 * Test cases for {@link Optional}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: test serialization
public class OptionalsTest extends AbstractPossibleTest {

   @Override
   protected Possible<String> valuePresent(String s) {
      return Optionals.toPossible(Optional.of(s));
   }

   @Override
   protected Possible<String> valueAbsent() {
      return Optionals.toPossible(Optional.empty());
   }
   
   @Test public void nullsMeanAbsent() {
      // this converts present null into absent
      checkAbsentValue(valueAbsent().or(Reference.setTo(null)));

      // and map treats null results as absent
      checkAbsentValue(valuePresent("abc").map((s) -> null));
   }

   // TODO: move
   @Test public void notNull() {
      checkPresentValue(Possible.notNull(Reference.setTo("abc")), "abc");
      checkAbsentValue(Possible.notNull(Reference.unset()));
      
      // null means absent for Optional
      checkAbsentValue(Possible.notNull(Reference.setTo(null)));
   }
   
   @Test public void equalsAndHashCode() {
      Possible<String> set1 = valuePresent("abc");
      Possible<String> set2 = valuePresent("xyz");
      Possible<String> unset = valueAbsent();
      
      assertEquals(set1, set1);
      assertEquals(set1, valuePresent("abc"));
      assertEquals(set1.hashCode(), valuePresent("abc").hashCode());

      assertEquals(set2, set2);
      assertEquals(set2, valuePresent("xyz"));
      assertEquals(set2.hashCode(), valuePresent("xyz").hashCode());

      assertEquals(unset, unset);
      assertSame(unset, valueAbsent()); // use "same" not "equals" since none is a singleton
      
      assertFalse(set1.equals(set2));
      assertFalse(set1.equals(unset));
      assertFalse(set2.equals(set1));
      assertFalse(set2.equals(unset));
      assertFalse(unset.equals(set1));
      assertFalse(unset.equals(set2));
   }
}
