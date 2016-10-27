package com.bluegosling.concurrent.atoms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bluegosling.tuples.Triple;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

public class ThreadLocalAtomTest extends AbstractSynchronousAtomTest {
   
   @Override
   protected <T> ThreadLocalAtom<T> create() {
      return new ThreadLocalAtom<T>();
   }

   @Override
   protected <T> ThreadLocalAtom<T> create(T initialValue) {
      return new ThreadLocalAtom<T>(initialValue);
   }

   @Override
   protected <T> ThreadLocalAtom<T> create(T initialValue, Predicate<T> validator) {
      return new ThreadLocalAtom<T>(initialValue, validator);
   }

   @Override
   protected void checkWatchers(SynchronousAtom<String> atom, List<?>... noticesArray) {
      // normal mutations do not generate notices
      atom.set("abc");
      for (List<?> notices : noticesArray) {
         assertTrue(notices.isEmpty());
      }

      atom.set(null);
      for (List<?> notices : noticesArray) {
         assertTrue(notices.isEmpty());
      }
      
      atom.updateAndGet((s) -> s + s);
      for (List<?> notices : noticesArray) {
         assertTrue(notices.isEmpty());
      }
      
      // only changes to root value generate notices
      ThreadLocalAtom<String> tla = (ThreadLocalAtom<String>) atom;
      tla.setRootValue("abc");
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Triple.of(tla, null, "abc")), notices);
         notices.clear();
      }
      tla.setRootValue(null);
      for (List<?> notices : noticesArray) {
         assertEquals(Arrays.asList(Triple.of(tla, "abc", null)), notices);
         notices.clear();
      }
   }
   
   @Test public void getSetRoot() throws Exception {
      final ThreadLocalAtom<String> atom = create("abc");
      assertEquals("abc", atom.getRootValue());
      assertEquals(atom.getRootValue(), atom.get());
      atom.set("def");
      assertEquals("abc", atom.getRootValue());
      
      atom.setRootValue("ABC");
      assertEquals("ABC", atom.getRootValue());
      assertEquals("def", atom.get()); // root changes only impact new threads
      
      Callable<String> getRootValue = () -> {
         assertEquals(atom.getRootValue(), atom.get());
         atom.set("something else");
         return atom.getRootValue();
      };
      FutureTask<String> future1 = new FutureTask<String>(getRootValue);
      new Thread(future1).start();
      assertEquals("ABC", future1.get());
      // changes in other thread don't impact this one:
      assertEquals("def", atom.get());
      
      atom.setRootValue("eek!");
      FutureTask<String> future2 = new FutureTask<String>(getRootValue);
      new Thread(future2).start();
      assertEquals("eek!", future2.get());
      assertEquals("def", atom.get());
   }
}
