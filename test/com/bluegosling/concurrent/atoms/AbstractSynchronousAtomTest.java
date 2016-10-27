package com.bluegosling.concurrent.atoms;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bluegosling.tuples.Triple;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Abstract test class with general test cases for all implementations of {@link SynchronousAtom}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractSynchronousAtomTest {

   protected abstract <T> SynchronousAtom<T> create();
   protected abstract <T> SynchronousAtom<T> create(T initialValue);
   protected abstract <T> SynchronousAtom<T> create(T initialValue, Predicate<T> validator);
   
   @Test public void initial() {
      SynchronousAtom<Integer> atom = create();
      assertNull(atom.get());
      atom = create(123);
      assertEquals(Integer.valueOf(123), atom.get());
      Predicate<Integer> validator = i -> i < 100;
      atom = create(99, validator);
      assertEquals(Integer.valueOf(99), atom.get());
      // validator requires < 100
      assertThrows(IllegalArgumentException.class, () -> create(100, validator));
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
   }
   
   @Test public void getSetUpdateAccumulate() {
      SynchronousAtom<String> atom = create("abc");
      assertEquals("abc", atom.get());
      assertEquals("abc", atom.set("def"));
      assertEquals("def", atom.get());
      assertEquals("DEF", atom.updateAndGet(String::toUpperCase));
      assertEquals("DEF", atom.get());
      assertEquals("DEF", atom.getAndUpdate(String::toLowerCase));
      assertEquals("def", atom.get());
      assertEquals("defabc", atom.accumulateAndGet("abc", String::concat));
      assertEquals("defabc", atom.get());
      assertEquals("defabc", atom.getAndAccumulate("XYZ", String::concat));
      assertEquals("defabcXYZ", atom.get());
   }
   
   @Test public void invalidMutations() {
      Predicate<Integer> validator = i -> i != null && i < 100;
      SynchronousAtom<Integer> atom = create(0, validator);
      AtomicBoolean notified = new AtomicBoolean();
      atom.addWatcher((a, oldValue, newValue) -> notified.set(true));
      
      atom.set(90);
      assertEquals(Integer.valueOf(90), atom.get());

      notified.set(false);
      // validator requires < 100
      assertThrows(IllegalArgumentException.class, () -> atom.set(100));
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      
      Function<Integer, Integer> addNine = (i) -> i + 9;
      atom.updateAndGet(addNine);
      assertEquals(Integer.valueOf(99), atom.get());

      notified.set(false);
      // another nine would push it over 100
      assertThrows(IllegalArgumentException.class, () -> atom.updateAndGet(addNine));
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
      assertFalse(notified.get());
   }
   
   @Test public void watchers() {
      List<Triple<Atom<? extends String>, String, String>> notices1 =
            new ArrayList<Triple<Atom<? extends String>, String, String>>();
      List<Triple<Atom<? extends String>, String, String>> notices2 =
            new ArrayList<Triple<Atom<? extends String>, String, String>>();
      Atom.Watcher<String> watcher1 = (atom, oldValue, newValue) ->
            notices1.add(Triple.of(atom, oldValue, newValue));
      Atom.Watcher<String> watcher2 = (atom, oldValue, newValue) ->
            notices2.add(Triple.of(atom, oldValue, newValue));

      SynchronousAtom<String> atom = create();
      atom.addWatcher(watcher1);
      checkWatchers(atom, notices1);
      assertTrue(notices2.isEmpty());
      
      // now w/ multiple watchers
      atom.addWatcher(watcher2);
      checkWatchers(atom, notices1, notices2);
      
      // remove one
      atom.removeWatcher(watcher1);
      checkWatchers(atom, notices2);
      assertTrue(notices1.isEmpty());
   }
   
   protected void checkWatchers(SynchronousAtom<String> atom, List<?>... noticesArray) {
      atom.set("abc");
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Triple.of(atom, null, "abc")), notices);
         notices.clear();
      }
      
      Function<String,String> twice = i -> i + i;
      atom.updateAndGet(twice);
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Triple.of(atom, "abc", "abcabc")), notices);
         notices.clear();
      }
      
      atom.set(null); // reset
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Triple.of(atom, "abcabc", null)), notices);
         notices.clear();
      }
   }
}
