package com.apriori.concurrent.atoms;

import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.apriori.tuples.Trio;

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
   
   @Test public void getSetApply() {
      SynchronousAtom<String> atom = create("abc");
      assertEquals("abc", atom.get());
      assertEquals("abc", atom.set("def"));
      assertEquals("def", atom.get());
      assertEquals("DEF", atom.apply(String::toUpperCase));
      assertEquals("DEF", atom.get());
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
      atom.apply(addNine);
      assertEquals(Integer.valueOf(99), atom.get());

      notified.set(false);
      // another nine would push it over 100
      assertThrows(IllegalArgumentException.class, () -> atom.apply(addNine));
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
      assertFalse(notified.get());
   }
   
   @Test public void watchers() {
      List<Trio<Atom<? extends String>, String, String>> notices1 =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      List<Trio<Atom<? extends String>, String, String>> notices2 =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      Atom.Watcher<String> watcher1 = (atom, oldValue, newValue) ->
            notices1.add(Trio.create(atom, oldValue, newValue));
      Atom.Watcher<String> watcher2 = (atom, oldValue, newValue) ->
            notices2.add(Trio.create(atom, oldValue, newValue));

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
         assertEquals(Arrays.asList(Trio.create(atom, null, "abc")), notices);
         notices.clear();
      }
      
      Function<String,String> twice = i -> i + i;
      atom.apply(twice);
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Trio.create(atom, "abc", "abcabc")), notices);
         notices.clear();
      }
      
      atom.set(null); // reset
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Trio.create(atom, "abcabc", null)), notices);
         notices.clear();
      }
   }
}
