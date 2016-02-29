package com.bluegosling.concurrent;

import com.bluegosling.concurrent.futures.fluent.CombiningFluentFuture;
import com.bluegosling.concurrent.futures.fluent.FluentFuture;
import com.bluegosling.tuples.NTuple;
import com.bluegosling.tuples.Pair;
import com.bluegosling.tuples.Quartet;
import com.bluegosling.tuples.Quintet;
import com.bluegosling.tuples.Trio;
import com.bluegosling.tuples.Tuple;
import com.bluegosling.tuples.Tuples;
import com.bluegosling.tuples.Unit;

import java.util.Arrays;

//TODO: javadoc
//TODO: tests
public class FutureTuples {
   public static <T> FluentFuture<Unit<T>> asUnit(FluentFuture<? extends T> future) {
      return future.map((o) -> Unit.create(o));
   }
   
   public static <T, U> FluentFuture<Pair<T, U>> asPair(FluentFuture<? extends T> futureT,
         FluentFuture<? extends U> futureU) {
      return futureT.combineWith(futureU, (t, u) -> Pair.<T, U>create(t, u));
   }

   public static <T, U, V> FluentFuture<Trio<T, U, V>> asTrio(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV) {
      return futureT.combineWith(futureU, futureV, (t, u, v) -> Trio.<T, U, V>create(t, u, v));
   }

   public static <T, U, V, W> FluentFuture<Quartet<T, U, V, W>> asQuartet(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV, FluentFuture<? extends W> futureW) {
      return new CombiningFluentFuture<Quartet<T,U, V, W>>(
            Arrays.asList(futureT, futureU, futureV, futureW)) {
         @Override protected Quartet<T, U, V, W> computeValue() {
            return Quartet.create(futureT.getResult(), futureU.getResult(),
                  futureV.getResult(), futureW.getResult());
         }
      };
   }

   public static <T, U, V, W, X> FluentFuture<Quintet<T, U, V, W, X>> asQuintet(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV, FluentFuture<? extends W> futureW,
         FluentFuture<? extends X> futureX) {
      return new CombiningFluentFuture<Quintet<T,U, V, W, X>>(
            Arrays.asList(futureT, futureU, futureV, futureW, futureX)) {
         @Override protected Quintet<T, U, V, W, X> computeValue() {
            return Quintet.create(futureT.getResult(), futureU.getResult(), futureV.getResult(),
                  futureW.getResult(), futureX.getResult());
         }
      };
   }

   public static <T, U, V, W, X> FluentFuture<NTuple<T, U, V, W, X>> asNTuple(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV, FluentFuture<? extends W> futureW,
         FluentFuture<? extends X> futureX, FluentFuture<?> futureY,
         FluentFuture<?>... futures) {
      FluentFuture<?> all[] = new FluentFuture<?>[futures.length + 6];
      all[0] = futureT;
      all[1] = futureU;
      all[2] = futureV;
      all[3] = futureW;
      all[4] = futureX;
      all[5] = futureY;
      System.arraycopy(futures, 0, all, 6, futures.length);
      return new CombiningFluentFuture<NTuple<T,U, V, W, X>>(Arrays.asList(all)) {
         @Override protected NTuple<T, U, V, W, X> computeValue() {
            Object args[] = new Object[futures.length];
            for (int i = 0, j = 6; j < all.length; i++, j++) {
               args[i] = all[j].getResult();
            }
            return NTuple.create(futureT.getResult(), futureU.getResult(), futureV.getResult(),
                  futureW.getResult(), futureX.getResult(), futureY.getResult(), args);
         }
      };
   }

   public static FluentFuture<Tuple> join(FluentFuture<?>... futures) {
      return join(Arrays.asList(futures));
   }
   
   public static FluentFuture<Tuple> join(Iterable<FluentFuture<?>> futures) {
      return FluentFuture.join(futures).map((list) -> Tuples.fromCollection(list));
   }
}
