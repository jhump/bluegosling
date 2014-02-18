package com.apriori.concurrent.atoms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.apriori.tuples.Trio;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
      Predicate<Integer> validator = new Predicate<Integer>() {
         @Override
         public boolean test(Integer input) {
            return input < 100;
         }
      };
      atom = create(99, validator);
      assertEquals(Integer.valueOf(99), atom.get());
      try {
         create(100, validator); // validator requires < 100
         fail("Expecting IllegalArgumentException but caught nothing");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
   }
   
   @Test public void getSetApply() {
      SynchronousAtom<String> atom = create("abc");
      assertEquals("abc", atom.get());
      assertEquals("abc", atom.set("def"));
      assertEquals("def", atom.get());
      assertEquals("DEF", atom.apply(new Function<String, String>() {
         @Override
         public String apply(String input) {
            return input.toUpperCase();
         }
      }));
      assertEquals("DEF", atom.get());
   }
   
   @Test public void invalidMutations() {
      Predicate<Integer> validator = new Predicate<Integer>() {
         @Override
         public boolean test(Integer input) {
            return input != null && input < 100;
         }
      };
      SynchronousAtom<Integer> atom = create(0, validator);
      final AtomicBoolean notified = new AtomicBoolean();
      atom.addWatcher(new Atom.Watcher<Integer>() {
         @Override
         public void changed(Atom<? extends Integer> a, Integer oldValue, Integer newValue) {
            notified.set(true);
         }
      });
      
      atom.set(90);
      assertEquals(Integer.valueOf(90), atom.get());

      notified.set(false);
      try {
         atom.set(100); // validator requires < 100
         fail("Expecting IllegalArgumentException but caught nothing");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      
      Function<Integer, Integer> addNine = new Function<Integer, Integer>() {
         @Override
         public Integer apply(Integer input) {
            return input + 9;
         }
      };
      atom.apply(addNine);
      assertEquals(Integer.valueOf(99), atom.get());

      notified.set(false);
      try {
         atom.apply(addNine); // another nine would push it over 100
         fail("Expecting IllegalArgumentException but caught nothing");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(Integer.valueOf(99), atom.get()); // unchanged
      assertFalse(notified.get());
   }
   
   @Test public void watchers() {
      final List<Trio<Atom<? extends String>, String, String>> notices1 =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      final List<Trio<Atom<? extends String>, String, String>> notices2 =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      Atom.Watcher<String> watcher1 = new Atom.Watcher<String>() {
         @Override
         public void changed(Atom<? extends String> atom, String oldValue, String newValue) {
            notices1.add(
                  Trio.<Atom<? extends String>, String, String>create(atom, oldValue, newValue));
         }
      };
      Atom.Watcher<String> watcher2 = new Atom.Watcher<String>() {
         @Override
         public void changed(Atom<? extends String> atom, String oldValue, String newValue) {
            notices2.add(
                  Trio.<Atom<? extends String>, String, String>create(atom, oldValue, newValue));
         }
      };

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
   
   @SuppressWarnings("unchecked")
   protected void checkWatchers(SynchronousAtom<String> atom, List<?>... noticesArray) {
      atom.set("abc");
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Trio.create(atom, null, "abc")), notices);
         notices.clear();
      }
      
      Function<String,String> twice = new Function<String, String>() {
         @Override
         public String apply(String input) {
            return input + input;
         }
      };
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
