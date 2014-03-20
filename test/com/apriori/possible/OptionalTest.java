package com.apriori.possible;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test cases for {@link Optional}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: test serialization
public class OptionalTest extends AbstractPossibleTest {

   @Override
   protected Possible<String> valuePresent(String s) {
      return Optional.some(s);
   }

   @Override
   protected Possible<String> valueAbsent() {
      return Optional.none();
   }
   
   @Test public void nullsMeanAbsent() {
      checkAbsentValue(Optional.<String>of(null));

      // this converts present null into absent
      checkAbsentValue(valueAbsent().or(Reference.<String>setTo(null)));

      // and transform treats null results as absent
      checkAbsentValue(valuePresent("abc").transform((s) -> null));
   }

   @Test public void disallowSomeNull() {
      try {
         Optional.some(null);
         fail("expecting a NullPointerException");
      } catch (NullPointerException expected) {
      }
   }
   
   @Test public void asOptional() {
      checkPresentValue(Optional.asOptional(Reference.setTo("abc")), "abc");
      checkAbsentValue(Optional.asOptional(Reference.<String>unset()));
      
      // null means absent for Optional
      checkAbsentValue(Optional.asOptional(Reference.<String>setTo(null)));
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
