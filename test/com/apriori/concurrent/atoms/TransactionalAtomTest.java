package com.apriori.concurrent.atoms;

import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.possible.Holder;
import com.apriori.tuples.Trio;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Test cases for {@link TransactionalAtom}. This class has mostly simple tests when using such an
 * atom outside of a transaction.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TransactionalAtomTest extends AbstractSynchronousAtomTest {

   @Override
   protected <T> TransactionalAtom<T> create() {
      return new TransactionalAtom<T>();
   }

   @Override
   protected <T> TransactionalAtom<T> create(T initialValue) {
      return new TransactionalAtom<T>(initialValue);
   }

   @Override
   protected <T> TransactionalAtom<T> create(T initialValue, Predicate<T> validator) {
      return new TransactionalAtom<T>(initialValue, validator);
   }
   
   @Test public void commute() throws Exception {
      TransactionalAtom<String> atom = create("abc", i -> i.length() < 10);
      
      // simple
      ListenableFuture<String> result = atom.commute(String::toUpperCase);
      assertTrue(result.isDone());
      assertEquals("ABC", result.get());
      assertEquals("ABC", atom.get());
      
      // w/ watcher
      List<Trio<Atom<? extends String>, String, String>> notices =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      Atom.Watcher<String> watcher = (a, oldValue, newValue) ->
            notices.add(Trio.create(a, oldValue, newValue));
      atom.addWatcher(watcher);
      Function<String, String> twice = (s) -> s + s;
      result = atom.commute(twice);
      assertEquals(Arrays.asList(Trio.create(atom, "ABC", "ABCABC")), notices);
      notices.clear();
      assertTrue(result.isDone());
      assertEquals("ABCABC", result.get());
      assertEquals("ABCABC", atom.get());
      
      // fails validation
      assertThrows(IllegalArgumentException.class, () -> atom.commute(twice));
      assertEquals("ABCABC", atom.get()); // unchanged
      assertTrue(notices.isEmpty());
   }
   
   @Test public void pin() {
      TransactionalAtom<String> atom = create("abc");
      // cannot pin outside of transaction
      assertThrows(IllegalStateException.class, () -> atom.pin());
   }
   
   @Test public void getSetApply_inTransaction() {
      TransactionalAtom<Integer> atom = create(1);
      for (final Boolean rollback : Arrays.asList(true, false)) {
         Transaction.execute(t -> {
            assertEquals(Integer.valueOf(1), atom.get());
            assertEquals(Integer.valueOf(1), atom.set(123));
            assertEquals(Integer.valueOf(123), atom.get());
            assertEquals(Integer.valueOf(223), atom.apply(new Function<Integer, Integer>() {
               @Override public Integer apply(Integer input) {
                  return input + 100;
               }
            }));
            if (rollback) {
               t.rollback();
            }
         });
         if (rollback) {
            assertEquals(Integer.valueOf(1), atom.get());
         } else {
            assertEquals(Integer.valueOf(223), atom.get());
         }
      }
   }
   
   @Test public void invalidMutations_inTransaction() {
      Predicate<Integer> validator = i -> i != null && i < 100;
      SynchronousAtom<Integer> atom = create(0, validator);
      AtomicBoolean notified = new AtomicBoolean();
      atom.addWatcher((a, oldValue, newValue) -> notified.set(true));
      
      atom.set(90);
      assertEquals(Integer.valueOf(90), atom.get());

      AtomicBoolean pastStepOne = new AtomicBoolean();
      notified.set(false);
      assertThrows(IllegalArgumentException.class, () -> Transaction.execute(t -> {
         atom.set(44); // this will get rolled back due to exception
         pastStepOne.set(true);
         atom.set(100); // validator requires < 100
      }));
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      assertTrue(pastStepOne.get());
      
      Function<Integer, Integer> addNine = i -> i + 9;

      notified.set(false);
      pastStepOne.set(false);
      assertThrows(IllegalArgumentException.class, () -> Transaction.execute(t -> {
         atom.apply(addNine); // this will get rolled back due to exception
         pastStepOne.set(true);
         atom.apply(addNine); // another nine would push it over 100
      }));
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      assertTrue(pastStepOne.get());
   }

   @Test public void watchers_inTransaction() {
      TransactionalAtom<String> atom = create("");
      List<Trio<Atom<?>, String, String>> notices =
            new ArrayList<Trio<Atom<?>, String, String>>();
      atom.addWatcher((a, oldValue, newValue) ->
            notices.add(Trio.create(a, oldValue, newValue)));
      Function<String, String> twice = i -> i + i;
      
      Transaction.execute(t -> {
         assertEquals("", atom.set("a"));
         assertEquals("a", atom.set(atom.get() + "bc"));
         assertEquals("abcabc", atom.apply(twice));
      });
      assertEquals("abcabc", atom.get());
      // watchers only get the total delta from before-transaction to committed-value
      assertEquals(1, notices.size());
      assertEquals(Trio.create(atom,  "", "abcabc"), notices.get(0));
   }
   
   @Test public void commute_inTransaction() {
      TransactionalAtom<Integer> atom = create(100);
      List<Trio<Atom<?>, Integer, Integer>> notices =
            new ArrayList<Trio<Atom<?>, Integer, Integer>>();
      atom.addWatcher((a, oldValue, newValue) -> notices.add(Trio.create(a, oldValue, newValue)));
      Function<Integer, Integer> twice = i -> i << 1;
      
      // explicit rollback cancels the future
      Holder<ListenableFuture<Integer>> commuteResult = Holder.create();
      Transaction.execute(t -> {
         commuteResult.set(atom.commute(twice));
         t.rollback();
      });
      assertTrue(commuteResult.get().isCancelled());
      assertEquals(Integer.valueOf(100), atom.get());
      assertTrue(notices.isEmpty());
      
      // failure (implicit rollback) also cancels the future
      commuteResult.clear();
      assertThrows(IllegalStateException.class, () -> Transaction.execute(t -> {
         commuteResult.set(atom.commute(twice));
         throw new IllegalStateException();
      }));
      assertTrue(commuteResult.get().isCancelled());
      assertEquals(Integer.valueOf(100), atom.get());
      assertTrue(notices.isEmpty());

      // commit completes the future
      commuteResult.clear();
      Holder<ListenableFuture<Integer>> commuteResultTwo = Holder.create();
      Transaction.execute(t -> {
         commuteResult.set(atom.commute(twice));
         assertFalse(commuteResult.get().isDone());
         commuteResultTwo.set(atom.commute(twice));
         assertFalse(commuteResultTwo.get().isDone());
      });
      assertEquals(Integer.valueOf(200), commuteResult.get().getResult());
      assertEquals(Integer.valueOf(400), commuteResultTwo.get().getResult());
      assertEquals(Integer.valueOf(400), atom.get());
      // got a notice this time
      assertEquals(1, notices.size());
      assertEquals(Trio.create(atom, 100, 400), notices.get(0));
   }

   @Test public void pin_inTransaction() {
      // TODO
   }
   
   @Test public void newComponent() {
      // TODO
   }
}
