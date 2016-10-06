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
import java.util.stream.StreamSupport;

import com.bluegosling.collections.MoreSpliterators;
import com.bluegosling.collections.MoreStreams;
import com.bluegosling.function.TriFunction;
import com.bluegosling.tuples.Pair;
import com.bluegosling.tuples.Trio;

/**
 * A fluent stream is a {@link Stream} that provides extra operations, including extensibility in
 * the ability to define {@linkplain #operator(StreamBridge) new} {@linkplain #operator(StreamNode)
 * intermediate} {@linkplain #operator(StreamOperator) operators}.
 *  
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element produced by the stream
 */
// TODO: more javadocs
public interface FluentStream<T> extends Stream<T> {
   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> sequential();

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> parallel();

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> unordered();

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> onClose(Runnable closeHandler);

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> filter(Predicate<? super T> predicate);

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   <R> FluentStream<R> map(Function<? super T, ? extends R> mapper);

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   <R> FluentStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> distinct();

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> sorted();
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> sorted(Comparator<? super T> comparator);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> peek(Consumer<? super T> action);

   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> limit(long maxSize);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overridden to co-variantly return a {@link FluentStream}.
    */
   @Override
   FluentStream<T> skip(long n);
   
   /**
    * Returns a stream that is the concatenation of this stream and then given stream.
    * 
    * @param other a stream
    * @return a stream whose data set is the concatenation of this stream and the given one
    * @see Stream#concat(Stream, Stream)
    */
   FluentStream<T> concat(Stream<? extends T> other);

   /**
    * Returns a stream that is the concatenation of this stream, followed by all of the given
    * streams.
    * 
    * @param others additional streams
    * @return a stream whose data set is the concatenation of this stream and the given ones
    */
   FluentStream<T> concat(Iterable<? extends Stream<? extends T>> others);

   /**
    * Returns a stream that combines the elements emitted by this stream and the given stream,
    * pairwise, in their natural encounter order, using the given combining function.
    * 
    * <p>The resulting stream is exhausted when the shorter of this stream or the given one is
    * exhausted, with remaining elements in the longer stream being discarded.
    * 
    * @param other a stream
    * @param combiner a function that combines an element from this stream and an element from the
    *       given stream
    * @return a stream that combines the elements of this stream and the given stream, pairwise
    * @see MoreStreams#zip(Iterable)
    */
   default <U, R> FluentStream<R> zip(Stream<? extends U> other,
         BiFunction<? super T, ? super U, ? extends R> combiner) {
      return zip(this, other, combiner);
   }

   /**
    * Returns a stream that combines the elements emitted by this stream the given two streams, in
    * their natural encounter order, using the given combining function.
    * 
    * @param other1 a stream
    * @param other2 another stream
    * @param combiner a function that combines elements from this stream and elements from the
    *       given two streams
    * @return a stream that combines the elements of this stream and the given two streams
    * @see MoreStreams#zip(Iterable)
    */
   default <U, V, R> FluentStream<R> zip(Stream<? extends U> other1, Stream<? extends V> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends R> combiner) {
      return zip(this, other1, other2, combiner);
   }
   
   /**
    * Returns a stream that batches the source elements in lists of the given size.
    * 
    * @param count the size of batches in the output stream
    * @return a stream that batches source elements
    */
   FluentStream<List<T>> batch(int count);

   /**
    * Returns a stream that groups elements in this stream by keys computed by the given function.
    * The given key extractor computes grouping keys. The given value extractor computes the values
    * that are grouped into corresponding collections. The resulting stream consists of entries of
    * keys and their corresponding value collections.
    * 
    * @param keyExtractor a function that extracts a grouping key from elements of this stream
    * @param valueExtractor a function that extracts grouped values from elements of this stream
    * @return a stream that groups elements in this stream
    */
   <K, V> FluentStream<Entry<K, Collection<V>>> groupBy(
         Function<? super T, ? extends K> keyExtractor,
         Function<? super T, ? extends V> valueExtractor);
   
   /**
    * Returns a stream that groups elements in this stream by keys computed by the given function.
    * The given key extractor computes grouping keys. The grouped values are the actual elements
    * of this stream. The default implementation is as follows:<pre>
    * stream.groupBy(keyExtractor, Function.identity());
    * </pre>
    * 
    * @param keyExtractor a function that extracts a grouping key from elements of this stream
    * @return a stream that groups elements in this stream
    */
   default <K> FluentStream<Entry<K, Collection<T>>> groupBy(
         Function<? super T, ? extends K> keyExtractor) {
      return groupBy(keyExtractor, Function.identity());
   }

   /**
    * Forks this stream into the given number of result streams, each with the same data as this
    * stream.
    * 
    * @param numForks the number of resulting forks
    * @return a supplier from which each fork is retrieved
    * @see MoreSpliterators#fork(Spliterator, int)
    */
   Supplier<FluentStream<T>> fork(int numForks);

   /**
    * Forks this stream into two.
    * 
    * @return the pair of resulting streams, each with the same data as this stream
    */
   default Pair<FluentStream<T>, FluentStream<T>> fork() {
      Supplier<FluentStream<T>> forks = fork(2);
      return Pair.create(forks.get(), forks.get());
   }

   /**
    * Partitions this stream into two, with elements that match the given predicate in the first
    * partition and elements that do not match it in the second partition.
    * 
    * @param criteria the predicate used to partition the stream
    * @return the pair of resulting streams, each with a disparate subset of this stream
    */
   default Pair<FluentStream<T>, FluentStream<T>> partition(Predicate<? super T> criteria) {
      Pair<FluentStream<T>, FluentStream<T>> forked = fork();
      return Pair.create(forked.getFirst().filter(criteria),
            forked.getSecond().filter(criteria.negate()));
   }

   /**
    * Joins this stream with the given stream using the given functions to extract corresponding
    * subsets and combine them.
    * 
    * @param other a stream
    * @param keyExtractor1 a function that extracts a grouping key from elements of this stream
    * @param valueExtractor1 a function that extracts grouped values from elements of this stream
    * @param keyExtractor2 a function that extracts a grouping key from elements of the other
    *       stream
    * @param valueExtractor2 a function that extracts grouped values from elements of the other
    *       stream
    * @param combiner a function that combines corresponding values from this and the other stream
    * @return a stream that is the joined result of this stream and the given stream
    */
   <K, U, V, W, X> FluentStream<X> join(Stream<? extends U> other,
         Function<? super T, ? extends K> keyExtractor1,
         Function<? super T, ? extends V> valueExtractor1,
         Function<? super U, ? extends K> keyExtractor2,
         Function<? super U, ? extends W> valueExtractor2,
         TriFunction<? super K, ? super Collection<V>, ? super Collection<W>, ? extends X> combiner);

   /**
    * Joins this stream with the given stream using the given functions to extract corresponding
    * subsets and combine them. The grouped values are the elements of the stream, as opposed to
    * values extracted from the stream via functions. The default implementation is as follows:<pre>
    * stream.join(other,
    *     keyExtractor1, Function.identity(),
    *     keyExtractor2, Function.identity(),
    *     combiner);
    * </pre>
    * 
    * @param other a stream
    * @param keyExtractor1 a function that extracts a grouping key from elements of this stream
    * @param keyExtractor2 a function that extracts a grouping key from elements of the other
    *       stream
    * @param combiner a function that combines corresponding values from this and the other stream
    * @return a stream that is the joined result of this stream and the given stream
    */
   default <K, U, V> FluentStream<V> join(Stream<? extends U> other,
         Function<? super T, ? extends K> keyExtractor1,
         Function<? super U, ? extends K> keyExtractor2,
         TriFunction<? super K, ? super Collection<T>, ? super Collection<U>, ? extends V> combiner) {
      return join(other, keyExtractor1, Function.identity(), keyExtractor2, Function.identity(),
            combiner);
   }
   
   /**
    * Joins this stream with the given stream using the given functions to extract corresponding
    * subsets. The default implementation is as follows:<pre>
    * stream.join(other,
    *     keyExtractor1, valueExtractor1,
    *     keyExtractor2, valueExtractor2,
    *     Trio::create);
    * </pre>
    * 
    * @param other a stream
    * @param keyExtractor1 a function that extracts a grouping key from elements of this stream
    * @param valueExtractor1 a function that extracts grouped values from elements of this stream
    * @param keyExtractor2 a function that extracts a grouping key from elements of the other
    *       stream
    * @param valueExtractor2 a function that extracts grouped values from elements of the other
    *       stream
    * @return a stream that is the joined result of this stream and the given stream
    */
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

   /**
    * Joins this stream with the given stream using the given functions to extract corresponding
    * subsets. The default implementation is as follows:<pre>
    * stream.join(other,
    *     keyExtractor1, Function.identity(),
    *     keyExtractor2, Function.identity(),
    *     Trio::create);
    * </pre>
    * 
    * @param other a stream
    * @param keyExtractor1 a function that extracts a grouping key from elements of this stream
    * @param keyExtractor2 a function that extracts a grouping key from elements of the other
    *       stream
    * @return a stream that is the joined result of this stream and the given stream
    */
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
   
   <U> FluentStream<U> operatorBridge(StreamBridge<U, T> operator);

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

   /**
    * Upgrades the given stream into a fluent stream. If the given stream is already an instance of
    * {@link FluentStream}, returns the given stream unchanged.
    * 
    * @param stream a stream
    * @return a fluent stream that represents the same data as the given stream
    */
   static <T> FluentStream<T> upgrade(Stream<T> stream) {
      return stream instanceof FluentStream
            ? (FluentStream<T>) stream
            : new StreamPipeline.Head<T>(stream);
   }
   
   /**
    * Creates a fluent stream from the given spliterator. The returned stream will be sequential,
    * unless explicitly configured {@linkplain #parallel() otherwise}.
    * 
    * @param spliterator a source of data
    * @return a stream of the given data
    * @see StreamSupport#stream(Spliterator, boolean)
    */
   static <T> FluentStream<T> fromSpliterator(Spliterator<? extends T> spliterator) {
      return new StreamPipeline.Head<T>(spliterator);
   }

   /**
    * Creates a fluent stream from the given spliterator source, with the given characteristics. The
    * returned stream will be sequential, unless explicitly configured {@linkplain #parallel()
    * otherwise}.
    * 
    * @param spliterator a supplier of the source of data
    * @param characteristics the characteristics of the source of data
    * @return a stream of the given data
    * @see StreamSupport#stream(Supplier, int, boolean)
    */
   static <T> FluentStream<T> fromSpliterator(
         Supplier<? extends Spliterator<? extends T>> spliterator, int characteristics) {
      return new StreamPipeline.Head<T>(spliterator, characteristics);
   }

   /**
    * Creates a fluent stream from the given spliterator source, with the given characteristics and
    * the given comparator if the source of data is sorted. The returned stream will be sequential,
    * unless explicitly configured {@linkplain #parallel() otherwise}.
    * 
    * @param spliterator a supplier of the source of data
    * @param characteristics the characteristics of the source of data
    * @param comparator the comparator, if the given source of data is sorted
    * @return a stream of the given data
    * @see StreamSupport#stream(Supplier, int, boolean)
    */
   static <T> FluentStream<T> fromSpliterator(
         Supplier<? extends Spliterator<? extends T>> spliterator, int characteristics,
         Comparator<? super T> comparator) {
      return new StreamPipeline.Head<T>(spliterator, characteristics, comparator);
   }
}
