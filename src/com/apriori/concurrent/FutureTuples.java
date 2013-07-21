package com.apriori.concurrent;

import static com.apriori.concurrent.ListenableFutures.addCallback;

import com.apriori.tuples.NTuple;
import com.apriori.tuples.Pair;
import com.apriori.tuples.Quartet;
import com.apriori.tuples.Quintet;
import com.apriori.tuples.Trio;
import com.apriori.tuples.Tuple;
import com.apriori.tuples.Tuples;
import com.apriori.tuples.Unit;
import com.apriori.util.Fulfillable;
import com.apriori.util.Fulfillables;
import com.apriori.util.Function;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
   
   private static abstract class FutureTuple<T> extends SimpleListenableFuture<T> {
      private final Collection<ListenableFuture<?>> components;
      private final AtomicInteger remaining;
      
      FutureTuple(Collection<ListenableFuture<?>> components) {
         this.components = components;
         remaining = new AtomicInteger(components.size());
      }
      
      abstract T computeValue();
      
      void mark() {
         if (remaining.decrementAndGet() == 0) {
            try {
               setValue(computeValue());
            } catch (Throwable t) {
               setFailure(t);
            }
         }
      }
      
      @Override public boolean cancel(boolean mayInterrupt) {
         if (super.cancel(mayInterrupt)) {
            for (ListenableFuture<?> future : components) {
               future.cancel(mayInterrupt);
            }
            return true;
         }
         return false;
      }
   }

   private static class FullfillingCallback<T> implements FutureCallback<T> {
      private final Fulfillable<T> component;
      private final FutureTuple<?> result;
      
      FullfillingCallback(Fulfillable<T> component, FutureTuple<?> result) {
         this.component = component;
         this.result = result;
      }
      
      @Override
      public void onSuccess(T t) {
         if (component.fulfill(t)) {
            result.mark();
         }
      }

      @Override
      public void onFailure(Throwable t) {
         result.setFailure(t);
      }

      @Override
      public void onCancel() {
         result.setCancelled();
      }
   }
   
   public static <T, U> ListenableFuture<Pair<T, U>> asPair(ListenableFuture<? extends T> futureT,
         ListenableFuture<? extends U> futureU) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-arg is safe
      FutureTuple<Pair<T, U>> futurePair =
            new FutureTuple<Pair<T,U>>(Arrays.asList(futureT, futureU)) {
               @Override Pair<T, U> computeValue() {
                  return Pair.create(t.get(), u.get());
               }
            };
      addCallback(futureT, new FullfillingCallback<T>(t, futurePair));
      addCallback(futureU, new FullfillingCallback<U>(u, futurePair));
      return futurePair;
   }

   public static <T, U, V> ListenableFuture<Trio<T, U, V>> asTrio(
         ListenableFuture<? extends T> futureT, ListenableFuture<? extends U> futureU,
         ListenableFuture<? extends V> futureV) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-arg is safe
      FutureTuple<Trio<T, U, V>> futureTrio =
            new FutureTuple<Trio<T,U, V>>(Arrays.asList(futureT, futureU, futureV)) {
               @Override Trio<T, U, V> computeValue() {
                  return Trio.create(t.get(), u.get(), v.get());
               }
            };
      addCallback(futureT, new FullfillingCallback<T>(t, futureTrio));
      addCallback(futureU, new FullfillingCallback<U>(u, futureTrio));
      addCallback(futureV, new FullfillingCallback<V>(v, futureTrio));
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
      FutureTuple<Quartet<T, U, V, W>> futureQuartet =
            new FutureTuple<Quartet<T,U, V, W>>(Arrays.asList(futureT, futureU, futureV, futureW)) {
               @Override Quartet<T, U, V, W> computeValue() {
                  return Quartet.create(t.get(), u.get(), v.get(), w.get());
               }
            };
      addCallback(futureT, new FullfillingCallback<T>(t, futureQuartet));
      addCallback(futureU, new FullfillingCallback<U>(u, futureQuartet));
      addCallback(futureV, new FullfillingCallback<V>(v, futureQuartet));
      addCallback(futureW, new FullfillingCallback<W>(w, futureQuartet));
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
      FutureTuple<Quintet<T, U, V, W, X>> futureQuintet =
            new FutureTuple<Quintet<T,U, V, W, X>>(Arrays.asList(futureT, futureU, futureV, futureW,
                  futureX)) {
               @Override Quintet<T, U, V, W, X> computeValue() {
                  return Quintet.create(t.get(), u.get(), v.get(), w.get(), x.get());
               }
            };
      addCallback(futureT, new FullfillingCallback<T>(t, futureQuintet));
      addCallback(futureU, new FullfillingCallback<U>(u, futureQuintet));
      addCallback(futureV, new FullfillingCallback<V>(v, futureQuintet));
      addCallback(futureW, new FullfillingCallback<W>(w, futureQuintet));
      addCallback(futureX, new FullfillingCallback<X>(x, futureQuintet));
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
      FutureTuple<NTuple<T, U, V, W, X>> futureNTuple =
            new FutureTuple<NTuple<T,U, V, W, X>>(Arrays.asList(all)) {
               @Override NTuple<T, U, V, W, X> computeValue() {
                  Object args[] = new Object[fullfillables.length];
                  for (int i = 0; i < fullfillables.length; i++) {
                     args[i] = fullfillables[i].get();
                  }
                  return NTuple.create(t.get(), u.get(), v.get(), w.get(), x.get(), y.get(), args);
               }
            };
      addCallback(futureT, new FullfillingCallback<T>(t, futureNTuple));
      addCallback(futureU, new FullfillingCallback<U>(u, futureNTuple));
      addCallback(futureV, new FullfillingCallback<V>(v, futureNTuple));
      addCallback(futureW, new FullfillingCallback<W>(w, futureNTuple));
      addCallback(futureX, new FullfillingCallback<X>(x, futureNTuple));
      addCallback(futureY, new FullfillingCallback<Object>(y, futureNTuple));
      for (int i = 0; i < futures.length; i++) {
         addCallback(futures[i], new FullfillingCallback<Object>(fullfillables[i], futureNTuple));
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
