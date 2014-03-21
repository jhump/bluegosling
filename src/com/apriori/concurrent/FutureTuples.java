package com.apriori.concurrent;

import com.apriori.concurrent.ListenableFutures.CombiningFuture;
import com.apriori.tuples.NTuple;
import com.apriori.tuples.Pair;
import com.apriori.tuples.Quartet;
import com.apriori.tuples.Quintet;
import com.apriori.tuples.Trio;
import com.apriori.tuples.Tuple;
import com.apriori.tuples.Tuples;
import com.apriori.tuples.Unit;

import java.util.Arrays;

//TODO: javadoc
//TODO: tests
public class FutureTuples {
   public static <T> ListenableFuture<Unit<T>> asUnit(ListenableFuture<? extends T> future) {
      return future.transform((o) -> Unit.create(o));
   }
   
   public static <T, U> ListenableFuture<Pair<T, U>> asPair(ListenableFuture<? extends T> futureT,
         ListenableFuture<? extends U> futureU) {
      CombiningFuture<Pair<T, U>> futurePair =
            new CombiningFuture<Pair<T,U>>(Arrays.asList(futureT, futureU)) {
               @Override Pair<T, U> computeValue() {
                  return Pair.create(futureT.getResult(), futureU.getResult());
               }
            };
      return futurePair;
   }

   public static <T, U, V> ListenableFuture<Trio<T, U, V>> asTrio(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV) {
      CombiningFuture<Trio<T, U, V>> futureTrio =
            new CombiningFuture<Trio<T,U, V>>(Arrays.asList(futureT, futureU, futureV)) {
               @Override Trio<T, U, V> computeValue() {
                  return Trio.create(futureT.getResult(), futureU.getResult(), futureV.getResult());
               }
            };
      return futureTrio;
   }

   public static <T, U, V, W> ListenableFuture<Quartet<T, U, V, W>> asQuartet(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV, ListenableFuture<? extends W> futureW) {
      CombiningFuture<Quartet<T, U, V, W>> futureQuartet =
            new CombiningFuture<Quartet<T,U, V, W>>(Arrays.asList(futureT, futureU, futureV, futureW)) {
               @Override Quartet<T, U, V, W> computeValue() {
                  return Quartet.create(futureT.getResult(), futureU.getResult(), futureV.getResult(),
                        futureW.getResult());
               }
            };
      return futureQuartet;
   }

   public static <T, U, V, W, X> ListenableFuture<Quintet<T, U, V, W, X>> asQuintet(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV, ListenableFuture<? extends W> futureW,
         ListenableFuture<? extends X> futureX) {
      CombiningFuture<Quintet<T, U, V, W, X>> futureQuintet =
            new CombiningFuture<Quintet<T,U, V, W, X>>(Arrays.asList(futureT, futureU, futureV,
                  futureW, futureX)) {
               @Override Quintet<T, U, V, W, X> computeValue() {
                  return Quintet.create(futureT.getResult(), futureU.getResult(), futureV.getResult(),
                        futureW.getResult(), futureX.getResult());
               }
            };
      return futureQuintet;
   }

   public static <T, U, V, W, X> ListenableFuture<NTuple<T, U, V, W, X>> asNTuple(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV, ListenableFuture<? extends W> futureW,
         ListenableFuture<? extends X> futureX, ListenableFuture<?> futureY,
         ListenableFuture<?>... futures) {
      ListenableFuture<?> all[] = new ListenableFuture<?>[futures.length + 6];
      all[0] = futureT;
      all[1] = futureU;
      all[2] = futureV;
      all[3] = futureW;
      all[4] = futureX;
      all[5] = futureY;
      System.arraycopy(futures, 0, all, 6, futures.length);
      CombiningFuture<NTuple<T, U, V, W, X>> futureNTuple =
            new CombiningFuture<NTuple<T,U, V, W, X>>(Arrays.asList(all)) {
               @Override NTuple<T, U, V, W, X> computeValue() {
                  Object args[] = new Object[futures.length];
                  for (int i = 0, j = 6; j < all.length; i++, j++) {
                     args[i] = all[j].getResult();
                  }
                  return NTuple.create(futureT.getResult(), futureU.getResult(), futureV.getResult(),
                        futureW.getResult(), futureX.getResult(), futureY.getResult(), args);
               }
            };
      return futureNTuple;
   }

   public static ListenableFuture<Tuple> join(ListenableFuture<?>... futures) {
      return join(Arrays.asList(futures));
   }
   
   public static ListenableFuture<Tuple> join(Iterable<ListenableFuture<?>> futures) {
      return ListenableFutures.join(futures).transform((list) -> Tuples.fromCollection(list));
   }
}
