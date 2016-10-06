package com.bluegosling.possible;

import static com.bluegosling.function.Predicates.alwaysAccept;
import static com.bluegosling.function.Predicates.alwaysReject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

/**
 * Test cases for {@link Holder}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: test serialization, apply, applyFilter
public class HolderTest extends AbstractPossibleTest {

   @Override
   protected Holder<String> valuePresent(String s) {
      return Holder.create(s);
   }

   @Override
   protected Holder<String> valueAbsent() {
      return Holder.create();
   }

   @Override
   public void map() {
      super.map();
      Function<String, String> function = (s) -> s + ":xyz";
      // values that come back are supposed to be immutable snapshots, not Holders
      assertFalse(valuePresent("abc").map(function) instanceof Holder);
      assertFalse(valueAbsent().map(function) instanceof Holder);
   }

   @Override
   @Test public void filter() {
      // values that come back are supposed to be immutable snapshots, not Holders
      assertFalse(valuePresent("abc").filter(alwaysAccept()) instanceof Holder);
      assertFalse(valuePresent("abc").filter(alwaysReject()) instanceof Holder);
      assertFalse(valueAbsent().filter(alwaysAccept()) instanceof Holder);
      assertFalse(valueAbsent().filter(alwaysReject()) instanceof Holder);
   }
   
   @Test public void allowPresentNulls() {
      checkPresentValue(valuePresent(null), null);
      checkPresentValue(valueAbsent().or(Holder.<String>create(null)), null);
      checkPresentValue(valuePresent("abc").map((s) -> null), null);
   }

   
   @Test public void setAndClear() {
      // start with value
      Holder<String> h = valuePresent("abc");
      Set<String> set = h.asSet(); // set changes as underlying Holder does
      
      h.set("xyz");
      checkPresentValue(h, "xyz");
      checkSingletonSet(set, "xyz");
      assertEquals(valuePresent("xyz"), h);
      assertEquals(valuePresent("xyz").hashCode(), h.hashCode());
      
      h.set(null);
      checkPresentValue(h, null);
      checkSingletonSet(set, null);
      assertEquals(valuePresent(null), h);
      assertEquals(valuePresent(null).hashCode(), h.hashCode());
      
      h.clear();
      checkAbsentValue(h);
      checkEmptySet(set);
      assertEquals(valueAbsent(), h);
      assertEquals(valueAbsent().hashCode(), h.hashCode());
      
      h.set("abc");
      checkPresentValue(h, "abc");
      checkSingletonSet(set, "abc");
      assertEquals(valuePresent("abc"), h);
      assertEquals(valuePresent("abc").hashCode(), h.hashCode());
      
      // start absent
      h = valueAbsent();
      set = h.asSet();
      checkAbsentValue(h);
      
      h.set("abc");
      checkPresentValue(h, "abc");
      checkSingletonSet(set, "abc");
      assertEquals(valuePresent("abc"), h);
      assertEquals(valuePresent("abc").hashCode(), h.hashCode());
      
      h.clear();
      checkAbsentValue(h);
      checkEmptySet(set);
      assertEquals(valueAbsent(), h);
      assertEquals(valueAbsent().hashCode(), h.hashCode());
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
      assertEquals(unset, valueAbsent());
      assertNotSame(unset, valueAbsent()); // absent not singleton w/ Holder
      
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
   
   @Test public void mutateViaSet() {
      // start with present value
      Holder<String> h = valuePresent("abc");
      Set<String> set = h.asSet();

      assertTrue(set.remove("abc"));
      checkAbsentValue(h);

      assertTrue(set.add("xyz"));
      checkPresentValue(h, "xyz");

      assertFalse(set.add("xyz")); // returns false since value already contained
      checkPresentValue(h, "xyz");

      try {
         // adding second value not allowed
         set.add("abc");
         fail("expecting an IllegalStateException");
      } catch (IllegalStateException expected) {
         checkPresentValue(h, "xyz");
      }

      set.clear();
      checkAbsentValue(h);

      // start with absent value
      h = valueAbsent();
      set = h.asSet();
      
      assertTrue(set.add("xyz"));
      checkPresentValue(h, "xyz");
      
      set.clear();
      checkAbsentValue(h);
   }
   
   @Test public void asSetIterator() {
      Holder<String> h = valuePresent("abc");
      Set<String> set = h.asSet();
      
      Iterator<String> iter = set.iterator();
      try {
         // never fetched an item so nothing to remove
         iter.remove();
         fail("expecting an IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      assertEquals("abc", iter.next()); // consume single element
      iter.remove(); // so we can remove it
      checkAbsentValue(h);
      try {
         // item already removed, can't do it again
         iter.remove();
         fail("expecting an IllegalStateException");
      } catch (IllegalStateException expected) {
      }
      
      // hasNext can detect concurrent modification
      set.add("abc");
      iter = set.iterator();
      assertTrue(iter.hasNext());
      h.clear();
      try {
         iter.hasNext();
         fail("expecting a ConcurrentModificationException");
      } catch (ConcurrentModificationException expected) {
      }
      // next can detect concurrent modification
      set.add("abc");
      iter = set.iterator();
      assertTrue(iter.hasNext());
      h.clear();
      try {
         iter.next();
         fail("expecting a ConcurrentModificationException");
      } catch (ConcurrentModificationException expected) {
      }
      // remove can detect concurrent modification
      set.add("abc");
      iter = set.iterator();
      assertTrue(iter.hasNext());
      assertEquals("abc", iter.next());
      h.clear();
      try {
         iter.remove();
         fail("expecting a ConcurrentModificationException");
      } catch (ConcurrentModificationException expected) {
      }
   }
}
