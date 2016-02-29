package com.bluegosling.generator;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.DeadlockException;
import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.generator.Generator;
import com.bluegosling.generator.Sequence;
import com.bluegosling.generator.SequenceAbandonedException;
import com.bluegosling.generator.SequenceFinishedException;
import com.bluegosling.generator.UncheckedGenerator;
import com.bluegosling.generator.UncheckedSequence;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class GeneratorTest {
   
   @Test public void nonEmptySequence() {
      // basic test case that verifies that values are properly communicated from consumer to
      // producer and back
      List<Integer> valsFromConsumer = new ArrayList<>();
      List<Integer> valsFromProducer = new ArrayList<>();
      UncheckedGenerator<Integer, Integer> gen = Generator.create((i, out) -> {
         valsFromConsumer.add(i);
         while (i < 100) {
            i = out.yield(i + i);
            valsFromConsumer.add(i);
         }
      });
      
      UncheckedSequence<Integer, Integer> seq = gen.start();
      int i = 0;
      while (true) {
         try {
            i = seq.next(i + 1);
            valsFromProducer.add(i);
         } catch (SequenceFinishedException e) {
            break;
         }
      }
      // subsequent attempts on sequence throw same exception
      assertThrows(SequenceFinishedException.class, () -> seq.next());
      
      assertEquals(Arrays.asList(1, 3, 7, 15, 31, 63, 127), valsFromConsumer);
      assertEquals(Arrays.asList(2, 6, 14, 30, 62, 126), valsFromProducer);
   }
   
   @Test public void emptySequence() {
      UncheckedGenerator<Integer, Integer> gen = Generator.create((i, out) -> {});
      UncheckedSequence<Integer, Integer> seq = gen.start();
      assertThrows(SequenceFinishedException.class, () -> seq.next());
      assertThrows(SequenceFinishedException.class, () -> seq.next(1));
   }
   
   @Test public void sequenceThrows() {
      Generator<Integer, Void, IOException> gen = new Generator<Integer, Void, IOException>() {
         @Override
         protected void run(Void v, Output<Integer, Void> out) throws IOException {
            throw new IOException();
         }
      };
      
      Sequence<Integer, Void, IOException> seq = gen.start();
      IOException e = assertThrows(IOException.class, () -> seq.next());
      // subsequent queries of sequence throw same exception
      assertSame(e, assertThrows(IOException.class, () -> seq.next()));
   }

   @Test public void sequenceAbandoned() throws Exception {
      CountDownLatch latch = new CountDownLatch(10);
      UncheckedGenerator<Integer, Integer> gen = Generator.create((i, out) -> {
         try {
            // unending generator
            while (true) {
               i = out.yield(i);
            }
         } catch (SequenceAbandonedException e) {
            // we'll catch this when the abandoned sequence is GC'ed
            latch.countDown();
         }
      });

      PrimitiveIterator.OfInt iter = IntStream.range(0, 100).iterator();
      for (int i = 0; i < 10; i++) {
         UncheckedSequence<Integer, Integer> seq = gen.start();
         for (int j = 0; j < 10; j++) {
            assertEquals(iter.next(), seq.next(i * 10 + j));
         }
      }
      
      // 10 sequences are now abandoned. Make sure they all clean up after themselves.
      while (latch.getCount() > 0) {
         if (!latch.await(1, TimeUnit.MILLISECONDS)) {
            System.gc();
         }
      }
   }
   
   @Test public void asIterable() {
      UncheckedGenerator<Integer, Void> gen = Generator.create(out -> {
         int i1 = 0;
         int i2 = 1;
         // generate 10 fibonacci numbers
         for (int i = 0; i < 10; i++) {
            int f = i1 + i2;
            out.yield(f);
            i1 = i2;
            i2 = f;
         }
      });
      List<Integer> fibs = new ArrayList<>();
      for (Integer i : gen.asIterable()) {
         fibs.add(i);
      }
      assertEquals(Arrays.asList(1, 2, 3, 5, 8, 13, 21, 34, 55, 89), fibs);
   }
   
   @Test public void customExecutor() {
      AtomicReference<Thread> thread = new AtomicReference<Thread>();
      Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
         @Override
         public Thread newThread(Runnable r) {
            Thread ret = new Thread(r);
            assertTrue(thread.compareAndSet(null, ret));
            return ret;
         }
      });
      
      UncheckedGenerator<Thread, Void> gen =
            Generator.create(o -> o.yield(Thread.currentThread()), executor);
      for (int i = 0; i < 10; i++) {
         UncheckedSequence<Thread, Void> seq = gen.start();
         assertSame(thread.get(), seq.next());
         // exhaust sequence so that single thread is re-used w/out waiting for finalizer to run
         assertThrows(SequenceFinishedException.class, () -> seq.next());
      }
   }

   @Test public void sameThreadExecutorThrowsDeadlock() {
      UncheckedGenerator<String, Void> gen =
            Generator.create(o -> o.yield("abc"), SameThreadExecutor.get());
      UncheckedSequence<String, Void> seq = gen.start();
      // the one yielded value will never be seen because generator thread should detect deadlock
      // condition and abort
      assertThrows(DeadlockException.class, () -> seq.next());
   }

   @Test public void cantUseSequenceSimultaneouslyFromMultipleThreads() throws Exception {
      CountDownLatch waiting = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(1);
      Generator<Integer, Void, InterruptedException> gen =
            new Generator<Integer, Void, InterruptedException>() {
               @Override
               protected void run(Void v, Output<Integer, Void> o) throws InterruptedException {
                  waiting.countDown();
                  done.await();
                  o.yield(0);
               }
            };
      Sequence<Integer, Void, InterruptedException> seq = gen.start();
      FutureTask<Integer> f = new FutureTask<>(() -> seq.next());
      new Thread(f).start();
      waiting.await();
      // other thread now waiting on producer, so this is invalid concurrent usage
      assertThrows(ConcurrentModificationException.class, () -> seq.next());
      done.countDown();
      // other thread got the value successfully
      assertEquals(0, f.get().intValue());
   }

   @Test public void canSafelyTransferSequenceFromOneThreadToAnother() throws Exception {
      UncheckedGenerator<Integer, Void> gen = Generator.create(o -> {
         for (int i = 0; i < 10; i++) {
            o.yield(i);
         }
      });
      UncheckedSequence<Integer, Void> seq = gen.start();
      // a different thread consumes each value, but none at the same time
      for (int i = 0; i < 10; i++) {
         FutureTask<Integer> f = new FutureTask<>(() -> seq.next());
         new Thread(f).start();
         assertEquals(i, f.get().intValue());
      }
   }

   @Test public void sequenceDoesntSwallowInterruptions() throws Exception {
      // single-use thread that will terminate when the generator finishes
      Executor executor = r -> new Thread(r).start();
      
      Generator<Thread, Void, InterruptedException> gen =
            new Generator<Thread, Void, InterruptedException>(executor) {
               @Override
               protected void run(Void v, Output<Thread, Void> out)
                     throws InterruptedException {
                  while (true) {
                     // do something interruptible
                     Thread.sleep(1);
                     out.yield(Thread.currentThread());
                  }
               }
            };
      Sequence<Thread, Void, InterruptedException> seq = gen.start();
      Thread th = seq.next();
      th.interrupt();
      // make sure interrupt doesn't kill thread (Output.yield is not interruptible)
      th.join(200); 
      assertTrue(th.isAlive());
      // when Output.yield returns, it restores interrupt status, which causes the producer
      // to be interrupted
      assertThrows(InterruptedException.class, () -> seq.next());
      // now thread will terminate
      th.join(200);
      assertFalse(th.isAlive());
   }
}
