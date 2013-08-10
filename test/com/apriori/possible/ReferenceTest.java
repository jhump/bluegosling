package com.apriori.possible;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import com.apriori.util.Function;

import org.junit.Test;

/**
 * Test cases for {@link Reference}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: test serialization
public class ReferenceTest extends AbstractPossibleTest {

   @Override
   protected Possible<String> valuePresent(String s) {
      return Reference.setTo(s);
   }

   @Override
   protected Possible<String> valueAbsent() {
      return Reference.unset();
   }
   
   @Test public void allowPresentNulls() {
      checkPresentValue(valuePresent(null), null);
      checkPresentValue(valueAbsent().or(Reference.<String>setTo(null)), null);
      checkPresentValue(valuePresent("abc").transform(new Function<String, String>() {
         @Override
         public String apply(String input) {
            return null;
         }
      }), null);
   }

   @Test public void asReference() {
      checkPresentValue(Reference.asReference(Optional.some("abc")), "abc");
      checkAbsentValue(Reference.asReference(Optional.<String>none()));
   }
   
   @Test public void equalsAndHashCode() {
      Possible<String> set1 = valuePresent("abc");
      Possible<String> set2 = valuePresent("xyz");
      Possible<String> setNull = valuePresent(null);
      Possible<String> unset = valueAbsent();
      
      assertEquals(set1, set1);
      assertEquals(set1, valuePresent("abc"));
      assertEquals(set1.hashCode(), valuePresent("abc").hashCode());

      assertEquals(set2, set2);
      assertEquals(set2, valuePresent("xyz"));
      assertEquals(set2.hashCode(), valuePresent("xyz").hashCode());

      assertEquals(setNull, setNull);
      assertEquals(setNull, valuePresent(null));
      assertEquals(setNull.hashCode(), valuePresent(null).hashCode());

      assertEquals(unset, unset);
      assertSame(unset, valueAbsent()); // use "same" not "equals" since unset ref is a singleton
      
      assertFalse(set1.equals(set2));
      assertFalse(set1.equals(setNull));
      assertFalse(set1.equals(unset));
      assertFalse(set2.equals(set1));
      assertFalse(set2.equals(setNull));
      assertFalse(set2.equals(unset));
      assertFalse(setNull.equals(set1));
      assertFalse(setNull.equals(set2));
      assertFalse(setNull.equals(unset));
      assertFalse(unset.equals(set1));
      assertFalse(unset.equals(set2));
      assertFalse(unset.equals(setNull));
   }
}
