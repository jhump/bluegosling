package com.bluegosling.concurrent.extras;

import com.bluegosling.concurrent.fluent.CombiningFluentFuture;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.tuples.NTuple;
import com.bluegosling.tuples.Pair;
import com.bluegosling.tuples.Quadruple;
import com.bluegosling.tuples.Quintuple;
import com.bluegosling.tuples.Triple;
import com.bluegosling.tuples.Tuple;
import com.bluegosling.tuples.Tuples;
import com.bluegosling.tuples.Single;

import java.util.Arrays;

//TODO: javadoc
//TODO: tests
public class FutureTuples {
   public static <T> FluentFuture<Single<T>> asSingle(FluentFuture<? extends T> future) {
      return future.map((o) -> Single.of(o));
   }
   
   public static <T, U> FluentFuture<Pair<T, U>> asPair(FluentFuture<? extends T> futureT,
         FluentFuture<? extends U> futureU) {
      return futureT.combineWith(futureU, (t, u) -> Pair.<T, U>of(t, u));
   }

   public static <T, U, V> FluentFuture<Triple<T, U, V>> asTriple(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV) {
      return futureT.combineWith(futureU, futureV, (t, u, v) -> Triple.<T, U, V>of(t, u, v));
   }

   public static <T, U, V, W> FluentFuture<Quadruple<T, U, V, W>> asQuadruple(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV, FluentFuture<? extends W> futureW) {
      return new CombiningFluentFuture<Quadruple<T,U, V, W>>(
            Arrays.asList(futureT, futureU, futureV, futureW)) {
         @Override protected Quadruple<T, U, V, W> computeValue() {
            return Quadruple.of(futureT.getResult(), futureU.getResult(),
                  futureV.getResult(), futureW.getResult());
         }
      };
   }

   public static <T, U, V, W, X> FluentFuture<Quintuple<T, U, V, W, X>> asQuintuple(
         FluentFuture<? extends T> futureT, FluentFuture<? extends U> futureU,
         FluentFuture<? extends V> futureV, FluentFuture<? extends W> futureW,
         FluentFuture<? extends X> futureX) {
      return new CombiningFluentFuture<Quintuple<T,U, V, W, X>>(
            Arrays.asList(futureT, futureU, futureV, futureW, futureX)) {
         @Override protected Quintuple<T, U, V, W, X> computeValue() {
            return Quintuple.of(futureT.getResult(), futureU.getResult(), futureV.getResult(),
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
      return FluentFuture.join(futures).map(Tuples::fromCollection);
   }
}
