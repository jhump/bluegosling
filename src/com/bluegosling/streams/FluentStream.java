package com.bluegosling.streams;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.bluegosling.collections.MoreStreams;
import com.bluegosling.function.TriFunction;
import com.bluegosling.tuples.Pair;
import com.bluegosling.tuples.Trio;

public interface FluentStream<T> extends Stream<T> {
   @Override
   FluentStream<T> sequential();

   @Override
   FluentStream<T> parallel();

   @Override
   FluentStream<T> unordered();

   @Override
   FluentStream<T> onClose(Runnable closeHandler);

   @Override
   FluentStream<T> filter(Predicate<? super T> predicate);

   @Override
   <R> FluentStream<R> map(Function<? super T, ? extends R> mapper);

   @Override
   <R> FluentStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

   @Override
   FluentStream<T> distinct();

   @Override
   FluentStream<T> sorted();
   
   @Override
   FluentStream<T> sorted(Comparator<? super T> comparator);
   
   @Override
   FluentStream<T> peek(Consumer<? super T> action);

   @Override
   FluentStream<T> limit(long maxSize);
   
   @Override
   FluentStream<T> skip(long n);
   
   FluentStream<T> concat(Stream<? extends T> other);

   FluentStream<T> concat(Iterable<? extends Stream<? extends T>> others);

   default <U, R> FluentStream<R> zip(Stream<? extends U> other,
         BiFunction<? super T, ? super U, ? extends R> combiner) {
      return zip(this, other, combiner);
   }
   
   default <U, V, R> FluentStream<R> zip(Stream<? extends U> other1, Stream<? extends V> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends R> combiner) {
      return zip(this, other1, other2, combiner);
   }
   
   FluentStream<List<T>> batch(int count);

   <K, V> FluentStream<Entry<K, Collection<V>>> groupBy(
         Function<? super T, ? extends K> keyExtractor,
         Function<? super T, ? extends V> valueExtractor);
   
   default <K> FluentStream<Entry<K, Collection<T>>> groupBy(
         Function<? super T, ? extends K> keyExtractor) {
      return groupBy(keyExtractor, Function.identity());
   }
   
   Pair<FluentStream<T>, FluentStream<T>> fork();

   default Pair<FluentStream<T>, FluentStream<T>> partition(Predicate<? super T> criteria) {
      Pair<FluentStream<T>, FluentStream<T>> forked = fork();
      return Pair.create(forked.getFirst().filter(criteria),
            forked.getSecond().filter(criteria.negate()));
   }

   <K, U, V, W, X> FluentStream<X> join(Stream<? extends U> other,
         Function<? super T, ? extends K> keyExtractor1,
         Function<? super T, ? extends V> valueExtractor1,
         Function<? super U, ? extends K> keyExtractor2,
         Function<? super U, ? extends W> valueExtractor2,
         TriFunction<? super K, ? super Collection<V>, ? super Collection<W>, ? extends X> combiner);

   default <K, U, V> FluentStream<V> join(Stream<? extends U> other,
         Function<? super T, ? extends K> keyExtractor1,
         Function<? super U, ? extends K> keyExtractor2,
         TriFunction<? super K, ? super Collection<T>, ? super Collection<U>, ? extends V> combiner) {
      return join(other, keyExtractor1, Function.identity(), keyExtractor2, Function.identity(),
            combiner);
   }
   
   default <K, U, V, W> FluentStream<Trio<K, Collection<V>, Collection<W>>> join(
         Stream<? extends U> other,
         Function<? super T, ? extends K> keyExtractor1,
         Function<? super T, ? extends V> valueExtractor1,
         Function<? super U, ? extends K> keyExtractor2,
         Function<? super U, ? extends W> valueExtractor2) {
      return this.<K, U, V, W, Trio<K, Collection<V>, Collection<W>>>
            join(other, keyExtractor1, valueExtractor1, keyExtractor2, valueExtractor2,
                  Trio::create);
   }

   default <K, U> FluentStream<Trio<K, Collection<T>, Collection<U>>> join(
         Stream<? extends U> other, Function<? super T, ? extends K> keyExtractor1,
         Function<? super U, ? extends K> keyExtractor2) {
      return join(other, keyExtractor1, Function.identity(), keyExtractor2, Function.identity());
   }

   <K, V> FluentStream<Entry<K, Collection<V>>> merge(Stream<? extends T> other,
         Function<? super T, ? extends K> keyExtractor,
         Function<? super T, ? extends V> valueExtractor);

   default <K> FluentStream<Entry<K, Collection<T>>> merge(Stream<? extends T> other,
         Function<? super T, ? extends K> keyExtractor) {
      return merge(other, keyExtractor, Function.identity());
   }
   
   <U> FluentStream<U> operator(StreamOperator<U, T> operator);

   <U> FluentStream<U> operator(StreamNode<U, T> operator);
   
   <U> FluentStream<U> operator(StreamBridge<U, T> operator);

   static <T, U, R> FluentStream<R> zip(Stream<? extends T> stream1, Stream<? extends U> stream2,
         BiFunction<? super T, ? super U, ? extends R> combiner) {
      return upgrade(MoreStreams.zip(stream1, stream2, combiner));
   }

   static <T, U, V, R> FluentStream<R> zip(Stream<? extends T> stream1,
         Stream<? extends U> stream2, Stream<? extends V> stream3,
         TriFunction<? super T, ? super U, ? super V, ? extends R> combiner) {
      return upgrade(MoreStreams.zip(stream1, stream2, stream3, combiner));
   }

   static <T> FluentStream<List<T>> zip(Iterable<Stream<? extends T>> streams) {
      return upgrade(MoreStreams.zip(streams));
   }

   static <T> FluentStream<T> upgrade(Stream<T> stream) {
      return stream instanceof FluentStream
            ? (FluentStream<T>) stream
            : new StreamPipeline.Head<T>(stream);
   }
   
   static <T> FluentStream<T> fromSpliterator(Spliterator<? extends T> spliterator) {
      return new StreamPipeline.Head<T>(spliterator);
   }

   static <T> FluentStream<T> fromSpliterator(
         Supplier<? extends Spliterator<? extends T>> spliterator, int characteristics) {
      return new StreamPipeline.Head<T>(spliterator, characteristics);
   }

   static <T> FluentStream<T> fromSpliterator(
         Supplier<? extends Spliterator<? extends T>> spliterator, int characteristics,
               Comparator<? super T> comparator) {
      return new StreamPipeline.Head<T>(spliterator, characteristics, comparator);
   }
}
