package com.bluegosling.concurrent.atoms;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bluegosling.tuples.Trio;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

// TODO: need moar!
public class AsynchronousAtomTest {
   
   @Test public void initial() {
      AsynchronousAtom<Integer> atom = new AsynchronousAtom<>();
      assertNull(atom.get());
      atom = new AsynchronousAtom<>(123);
      assertEquals(Integer.valueOf(123), atom.get());
      Predicate<Integer> validator = i -> i < 100;
      atom = new AsynchronousAtom<>(99, validator);
      assertEquals(Integer.valueOf(99), atom.get());
      // validator requires < 100
      assertThrows(IllegalArgumentException.class, () -> new AsynchronousAtom<>(100, validator));
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
   }
   
   @Test public void getSetUpdateAccumulate() throws Exception {
      AsynchronousAtom<String> atom = new AsynchronousAtom<>("abc");
      assertEquals("abc", atom.get());
      assertEquals("abc", atom.set("def").get());
      assertEquals("def", atom.get());
      assertEquals("DEF", atom.updateAndGet(String::toUpperCase).get());
      assertEquals("DEF", atom.get());
      assertEquals("DEF", atom.getAndUpdate(String::toLowerCase).get());
      assertEquals("def", atom.get());
      assertEquals("defabc", atom.accumulateAndGet("abc", String::concat).get());
      assertEquals("defabc", atom.get());
      assertEquals("defabc", atom.getAndAccumulate("XYZ", String::concat).get());
      assertEquals("defabcXYZ", atom.get());
   }
   
   @Test public void invalidMutations() throws Exception {
      Predicate<Integer> validator = i -> i != null && i < 100;
      AsynchronousAtom<Integer> atom = new AsynchronousAtom<>(0, validator);
      AtomicBoolean notified = new AtomicBoolean();
      atom.addWatcher((a, oldValue, newValue) -> notified.set(true));
      
      atom.set(90).get();
      assertEquals(Integer.valueOf(90), atom.get());

      notified.set(false);
      // validator requires < 100
      assertThrows(IllegalArgumentException.class, () -> atom.set(100));
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      
      Function<Integer, Integer> addNine = (i) -> i + 9;
      atom.updateAndGet(addNine).get();
      assertEquals(Integer.valueOf(99), atom.get());

      notified.set(false);
      // another nine would push it over 100, results in failed future
      ExecutionException e = assertThrows(ExecutionException.class,
            () -> atom.updateAndGet(addNine).get());
      assertSame(IllegalArgumentException.class, e.getCause().getClass());
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
      assertFalse(notified.get());
   }
   
   @Test public void watchers() throws Exception {
      List<Trio<Atom<? extends String>, String, String>> notices1 =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      List<Trio<Atom<? extends String>, String, String>> notices2 =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      Atom.Watcher<String> watcher1 = (atom, oldValue, newValue) ->
            notices1.add(Trio.create(atom, oldValue, newValue));
      Atom.Watcher<String> watcher2 = (atom, oldValue, newValue) ->
            notices2.add(Trio.create(atom, oldValue, newValue));

      AsynchronousAtom<String> atom = new AsynchronousAtom<>();
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
   
   protected void checkWatchers(AsynchronousAtom<String> atom, List<?>... noticesArray)
         throws Exception {
      atom.set("abc").get();
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Trio.create(atom, null, "abc")), notices);
         notices.clear();
      }
      
      Function<String,String> twice = i -> i + i;
      atom.updateAndGet(twice).get();
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Trio.create(atom, "abc", "abcabc")), notices);
         notices.clear();
      }
      
      atom.set(null).get(); // reset
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Trio.create(atom, "abcabc", null)), notices);
         notices.clear();
      }
   }
}
