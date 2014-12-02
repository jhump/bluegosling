package com.apriori.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;



public class GeneratorTest {
   
   public static void main(String args[]) throws Exception {
      Set<Thread> threads = Collections.synchronizedSet(new HashSet<Thread>());
      CountDownLatch latch = new CountDownLatch(100);
      UncheckedGenerator<Integer, Integer> gen =
            Generator.create((Integer i, Generator.Output<Integer, Integer> o) -> {
               threads.add(Thread.currentThread());
               try {
                  while (true) {
                     i = o.yield(i);
                  }
               } catch (SequenceAbandonedException e) {
                  latch.countDown();
               }
            });

      for (int i = 0; i < 100; i++) {
         System.out.println("----");
         UncheckedSequence<Integer, Integer> seq = gen.start();
         for (int j = 0; j < 100; j++) {
            System.out.println(seq.next(i * 100 + j));
         }
      }
      
      while (latch.getCount() > 0) {
         if (!latch.await(1, TimeUnit.MILLISECONDS)) {
            System.gc();
         }
      }
      System.out.println("Done!");
      
      System.out.println("Used " + threads.size() + " threads");
   }
}
