package com.apriori.concurrent.atoms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.atoms.Transaction.UncheckedTask;
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
      TransactionalAtom<String> atom = create("abc", new Predicate<String>() {
         @Override public boolean test(String input) {
            return input.length() < 10;
         }
      });
      
      // simple
      ListenableFuture<String> result = atom.commute(String::toUpperCase);
      assertTrue(result.isDone());
      assertEquals("ABC", result.get());
      assertEquals("ABC", atom.get());
      
      // w/ watcher
      final List<Trio<Atom<? extends String>, String, String>> notices =
            new ArrayList<Trio<Atom<? extends String>, String, String>>();
      Atom.Watcher<String> watcher = new Atom.Watcher<String>() {
         @Override public void changed(Atom<? extends String> a, String oldValue, String newValue) {
            notices.add(
                  Trio.<Atom<? extends String>, String, String>create(a, oldValue, newValue));
         }
      };
      atom.addWatcher(watcher);
      Function<String, String> twice = (s) -> s + s;
      result = atom.commute(twice);
      assertEquals(Arrays.asList(Trio.create(atom, "ABC", "ABCABC")), notices);
      notices.clear();
      assertTrue(result.isDone());
      assertEquals("ABCABC", result.get());
      assertEquals("ABCABC", atom.get());
      
      // fails validation
      try {
         atom.commute(twice);
         fail("Expecting IllegalArgumentException but caught nothing");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals("ABCABC", atom.get()); // unchanged
      assertTrue(notices.isEmpty());
   }
   
   @Test public void pin() {
      TransactionalAtom<String> atom = create("abc");
      try {
         atom.pin();
         fail("Expecting IllegalStateException but caught nothing");
      } catch (IllegalStateException expected) {
      }
   }
   
   @Test public void getSetApply_inTransaction() {
      final TransactionalAtom<Integer> atom = create(1);
      for (final Boolean rollback : Arrays.asList(true, false)) {
         Transaction.execute(new UncheckedTask() {
            @Override public void execute(Transaction t) {
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
      Predicate<Integer> validator = new Predicate<Integer>() {
         @Override public boolean test(Integer input) {
            return input != null && input < 100;
         }
      };
      final SynchronousAtom<Integer> atom = create(0, validator);
      final AtomicBoolean notified = new AtomicBoolean();
      atom.addWatcher(new Atom.Watcher<Integer>() {
         @Override public void changed(Atom<? extends Integer> a, Integer oldValue, Integer newValue) {
            notified.set(true);
         }
      });
      
      atom.set(90);
      assertEquals(Integer.valueOf(90), atom.get());

      final AtomicBoolean pastStepOne = new AtomicBoolean();
      notified.set(false);
      try {
         Transaction.execute(new UncheckedTask() {
            @Override public void execute(Transaction t) {
               atom.set(44); // this will get rolled back due to exception
               pastStepOne.set(true);
               atom.set(100); // validator requires < 100
            }
         });
         fail("Expecting IllegalArgumentException but caught nothing");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      assertTrue(pastStepOne.get());
      
      final Function<Integer, Integer> addNine = new Function<Integer, Integer>() {
         @Override public Integer apply(Integer input) {
            return input + 9;
         }
      };

      notified.set(false);
      pastStepOne.set(false);
      try {
         Transaction.execute(new UncheckedTask() {
            @Override public void execute(Transaction t) {
               atom.apply(addNine); // this will get rolled back due to exception
               pastStepOne.set(true);
               atom.apply(addNine); // another nine would push it over 100
            }
         });
         fail("Expecting IllegalArgumentException but caught nothing");
      } catch (IllegalArgumentException expected) {
      }
      assertEquals(Integer.valueOf(90), atom.get()); // unchanged
      assertFalse(notified.get());
      assertTrue(pastStepOne.get());
   }

   @Test public void watchers_inTransaction() {
      final TransactionalAtom<String> atom = create("");
      final List<Trio<Atom<?>, String, String>> notices =
            new ArrayList<Trio<Atom<?>, String, String>>();
      atom.addWatcher(new Atom.Watcher<String>() {
         @Override public void changed(Atom<? extends String> a, String oldValue, String newValue) {
            notices.add(Trio.<Atom<?>, String, String>create(a, oldValue, newValue));
         }
      });
      final Function<String, String> twice = new Function<String, String>() {
         @Override public String apply(String input) {
            return input + input;
         }
      };
      
      Transaction.execute(new UncheckedTask() {
         @Override public void execute(Transaction t) {
            assertEquals("", atom.set("a"));
            assertEquals("a", atom.set(atom.get() + "bc"));
            assertEquals("abcabc", atom.apply(twice));
         }
      });
      assertEquals("abcabc", atom.get());
      // watchers only get the total delta from before-transaction to committed-value
      assertEquals(1, notices.size());
      assertEquals(Trio.create(atom,  "", "abcabc"), notices.get(0));
   }
   
   @Test public void commute_inTransaction() {
      final TransactionalAtom<Integer> atom = create(100);
      final List<Trio<Atom<?>, Integer, Integer>> notices =
            new ArrayList<Trio<Atom<?>, Integer, Integer>>();
      atom.addWatcher(new Atom.Watcher<Integer>() {
         @Override public void changed(Atom<? extends Integer> a, Integer oldValue, Integer newValue) {
            notices.add(Trio.<Atom<?>, Integer, Integer>create(a, oldValue, newValue));
         }
      });
      final Function<Integer, Integer> twice = new Function<Integer, Integer>() {
         @Override public Integer apply(Integer input) {
            return input << 1;
         }
      };
      
      // explicit rollback cancels the future
      final Holder<ListenableFuture<Integer>> commuteResult = Holder.create();
      Transaction.execute(new UncheckedTask() {
         @Override public void execute(Transaction t) {
            commuteResult.set(atom.commute(twice));
            t.rollback();
         }
      });
      assertTrue(commuteResult.get().isCancelled());
      assertEquals(Integer.valueOf(100), atom.get());
      assertTrue(notices.isEmpty());
      
      // failure (implicit rollback) also cancels the future
      commuteResult.clear();
      try {
         Transaction.execute(new UncheckedTask() {
            @Override public void execute(Transaction t) {
               commuteResult.set(atom.commute(twice));
               throw new IllegalStateException();
            }
         });
         fail("Expecting IllegalStateException but nothing caught");
      } catch (IllegalStateException expected) {
      }
      assertTrue(commuteResult.get().isCancelled());
      assertEquals(Integer.valueOf(100), atom.get());
      assertTrue(notices.isEmpty());

      // commit completes the future
      commuteResult.clear();
      final Holder<ListenableFuture<Integer>> commuteResultTwo = Holder.create();
      Transaction.execute(new UncheckedTask() {
         @Override public void execute(Transaction t) {
            commuteResult.set(atom.commute(twice));
            assertFalse(commuteResult.get().isDone());
            commuteResultTwo.set(atom.commute(twice));
            assertFalse(commuteResultTwo.get().isDone());
         }
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
