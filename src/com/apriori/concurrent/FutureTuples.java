package com.apriori.concurrent;

import static com.apriori.concurrent.ListenableFutures.addCallback;

import com.apriori.concurrent.ListenableFutures.CombiningFuture;
import com.apriori.concurrent.ListenableFutures.CombiningVisitor;
import com.apriori.possible.Fulfillable;
import com.apriori.possible.Fulfillables;
import com.apriori.tuples.NTuple;
import com.apriori.tuples.Pair;
import com.apriori.tuples.Quartet;
import com.apriori.tuples.Quintet;
import com.apriori.tuples.Trio;
import com.apriori.tuples.Tuple;
import com.apriori.tuples.Tuples;
import com.apriori.tuples.Unit;
import com.apriori.util.Function;

import java.util.Arrays;
import java.util.List;

//TODO: javadoc
//TODO: tests
public class FutureTuples {
   public static <T> ListenableFuture<Unit<T>> asUnit(ListenableFuture<? extends T> future) {
      return ListenableFutures.transform(future, new Function<T, Unit<T>>() {
         @Override public Unit<T> apply(T t) {
            return Unit.create(t);
         }
      });
   }
   
   public static <T, U> ListenableFuture<Pair<T, U>> asPair(ListenableFuture<? extends T> futureT,
         ListenableFuture<? extends U> futureU) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-arg is safe
      CombiningFuture<Pair<T, U>> futurePair =
            new CombiningFuture<Pair<T,U>>(Arrays.asList(futureT, futureU)) {
               @Override Pair<T, U> computeValue() {
                  return Pair.create(t.get(), u.get());
               }
            };
      addCallback(futureT, new CombiningVisitor<T>(t, futurePair));
      addCallback(futureU, new CombiningVisitor<U>(u, futurePair));
      return futurePair;
   }

   public static <T, U, V> ListenableFuture<Trio<T, U, V>> asTrio(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-arg is safe
      CombiningFuture<Trio<T, U, V>> futureTrio =
            new CombiningFuture<Trio<T,U, V>>(Arrays.asList(futureT, futureU, futureV)) {
               @Override Trio<T, U, V> computeValue() {
                  return Trio.create(t.get(), u.get(), v.get());
               }
            };
      addCallback(futureT, new CombiningVisitor<T>(t, futureTrio));
      addCallback(futureU, new CombiningVisitor<U>(u, futureTrio));
      addCallback(futureV, new CombiningVisitor<V>(v, futureTrio));
      return futureTrio;
   }

   public static <T, U, V, W> ListenableFuture<Quartet<T, U, V, W>> asQuartet(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV, ListenableFuture<? extends W> futureW) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      final Fulfillable<W> w = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-arg is safe
      CombiningFuture<Quartet<T, U, V, W>> futureQuartet =
            new CombiningFuture<Quartet<T,U, V, W>>(Arrays.asList(futureT, futureU, futureV, futureW)) {
               @Override Quartet<T, U, V, W> computeValue() {
                  return Quartet.create(t.get(), u.get(), v.get(), w.get());
               }
            };
      addCallback(futureT, new CombiningVisitor<T>(t, futureQuartet));
      addCallback(futureU, new CombiningVisitor<U>(u, futureQuartet));
      addCallback(futureV, new CombiningVisitor<V>(v, futureQuartet));
      addCallback(futureW, new CombiningVisitor<W>(w, futureQuartet));
      return futureQuartet;
   }

   public static <T, U, V, W, X> ListenableFuture<Quintet<T, U, V, W, X>> asQuintet(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV, ListenableFuture<? extends W> futureW,
         ListenableFuture<? extends X> futureX) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      final Fulfillable<W> w = Fulfillables.create();
      final Fulfillable<X> x = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-arg is safe
      CombiningFuture<Quintet<T, U, V, W, X>> futureQuintet =
            new CombiningFuture<Quintet<T,U, V, W, X>>(Arrays.asList(futureT, futureU, futureV,
                  futureW, futureX)) {
               @Override Quintet<T, U, V, W, X> computeValue() {
                  return Quintet.create(t.get(), u.get(), v.get(), w.get(), x.get());
               }
            };
      addCallback(futureT, new CombiningVisitor<T>(t, futureQuintet));
      addCallback(futureU, new CombiningVisitor<U>(u, futureQuintet));
      addCallback(futureV, new CombiningVisitor<V>(v, futureQuintet));
      addCallback(futureW, new CombiningVisitor<W>(w, futureQuintet));
      addCallback(futureX, new CombiningVisitor<X>(x, futureQuintet));
      return futureQuintet;
   }

   public static <T, U, V, W, X> ListenableFuture<NTuple<T, U, V, W, X>> asNTuple(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV, ListenableFuture<? extends W> futureW,
         ListenableFuture<? extends X> futureX, ListenableFuture<?> futureY,
         ListenableFuture<?>... futures) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      final Fulfillable<W> w = Fulfillables.create();
      final Fulfillable<X> x = Fulfillables.create();
      final Fulfillable<Object> y = Fulfillables.create();
      @SuppressWarnings("unchecked") // can't create generic array, so have to cast from raw type
      final Fulfillable<Object> fullfillables[] = new Fulfillable[futures.length];
      for (int i = 0; i < futures.length; i++) {
         fullfillables[i] = Fulfillables.create();
      }
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
                  Object args[] = new Object[fullfillables.length];
                  for (int i = 0; i < fullfillables.length; i++) {
                     args[i] = fullfillables[i].get();
                  }
                  return NTuple.create(t.get(), u.get(), v.get(), w.get(), x.get(), y.get(), args);
               }
            };
      addCallback(futureT, new CombiningVisitor<T>(t, futureNTuple));
      addCallback(futureU, new CombiningVisitor<U>(u, futureNTuple));
      addCallback(futureV, new CombiningVisitor<V>(v, futureNTuple));
      addCallback(futureW, new CombiningVisitor<W>(w, futureNTuple));
      addCallback(futureX, new CombiningVisitor<X>(x, futureNTuple));
      addCallback(futureY, new CombiningVisitor<Object>(y, futureNTuple));
      for (int i = 0; i < futures.length; i++) {
         addCallback(futures[i], new CombiningVisitor<Object>(fullfillables[i], futureNTuple));
      }
      return futureNTuple;
   }

   public static ListenableFuture<Tuple> join(ListenableFuture<?>... futures) {
      return join(Arrays.asList(futures));
   }
   
   public static ListenableFuture<Tuple> join(Iterable<ListenableFuture<?>> futures) {
      return ListenableFutures.transform(ListenableFutures.join(futures),
            new Function<List<Object>, Tuple>() {
               @Override public Tuple apply(List<Object> list){
                  return Tuples.fromCollection(list);
               }
            });
   }
}
