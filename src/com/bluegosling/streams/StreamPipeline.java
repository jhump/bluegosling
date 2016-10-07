package com.bluegosling.streams;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bluegosling.choice.Either;
import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.MoreSpliterators;
import com.bluegosling.function.TriFunction;
import com.bluegosling.streams.StreamNode.Upstream;
import com.bluegosling.vars.Variable;
import com.bluegosling.vars.VariableInt;
import com.bluegosling.vars.VariableLong;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * An implementation of {@link FluentStream}. The stream is a pipeline of operations. Each
 * intermediate stage is added to the pipeline, forming a linked list of stages. Terminal operations
 * run the data through all stages, possibly in parallel.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <S> the type emitted by the source (aka root or head stage of the pipeline)
 * @param <U> the type produced by the upstream (aka predecessor) stage
 * @param <T> the type produced by this stage
 */
abstract class StreamPipeline<S, U, T> implements FluentStream<T> {
   
   /* 
    * Implementation notes:
    * ---------------------
    * 
    * The stream pipeline is effectively a linked list of operations. Each operation is an instance
    * of StreamPipeline. A new operation is added to the end as intermediate operations are defined.
    * 
    * There are three kinds of operations in the stream: terminal operations and then two kinds of
    * intermediate operations. Terminal operations must be careful to always #start() the last
    * operation in the stream (which in turn must start all predecessor operations) and to always
    * #close() when complete. Intermediate operations are marked as "linked", which prevents them
    * from being re-used in other streams. The two kinds of intermediate operations are (1) those
    * that can run independently in parallel, accepting and then emitting elements in the same
    * encounter order as the stream source; and (2) those that may need to modify encounter order
    * and/or consume the entire source stream before emitting elements to downstream operations. The
    * latter kind includes sorting operations and transformations that require a non-trivial
    * reduction of the input stream.
    * 
    * The three kinds of operations map to three concrete types. Intermediate is used for the first
    * (and simpler) kind of intermediate operation. Head is used as both the source of a stream and
    * is also used for the second kind of intermediate operation (where output of upstream operation
    * is consumed and the new Head represents the resulting new source that is emitted to any
    * subsequent operations). And Tail is used to execute many kinds of terminal operations. Some
    * terminal operations have bespoke and simpler implementations that do not require an instance
    * of Tail. The Tail class will submit processing to the ForkJoinPool#commonPool if the stream is
    * parallel, using a simple divide-and-conquer recursive task that involves splitting the source
    * spliterator and having each leaf ForkJoinTask process a split.
    * 
    * Implementations of methods that represent intermediate operations should *NOT* examine whether
    * the stream is parallel or not. That attribute can change between the time the intermediate
    * operation is created and the time the stream is actually processed. Instead, consulting this
    * attribute should be deferred until the stream starts.
    * 
    * There are three different ways of getting a spliterator for this stream:
    * 1) #spliterator()
    *    This is a public method and a terminal operation. The stream is started when this method is
    *    called. The returned spliterator will automatically call #close() on the stream when the
    *    data is exhausted, to prevent leaks. The returned spliterator also has a finalizer so that
    *    #close() can be called if it is garbage collected and never exhausted.
    * 2) #autoStartSpliterator()
    *    This is used for the second (heavier-weight) kind of intermediate operation. The call to
    *    #start() on the stream is deferred until it is first used. The spliterator does not try
    *    to automatically close the stream. Instead #attach() is called so that the subsequent
    *    operation closes the stream when it is done.
    * 3) #basicSpliterator()
    *    This is the simplest kind of spliterator: no attempts to manage the stream lifecycle are
    *    made. This is ideal for many terminal operations which explicitly start and then close the
    *    stream.
    *
    * Intermediate operations may examine the stream's spliterator characteristics. It is worth
    * noting, however, that with some kinds of streams this may cause the source of data to be
    * initialized. If interference is expected to be an issue, the stream should be created with
    * a supplier of the spliterator and the characteristics, which allows characteristics to be
    * queried without initializing the source of data.
    */
   
   private static final Collector<Object, Object, Object> NO_OP = Collector.of(
         () -> null, (a, t) -> {}, (a1, a2) -> null);
   
   /**
    * A collector that does nothing with the emitted elements and always return a final result of
    * {@code null}.
    */
   @SuppressWarnings("unchecked")
   private static <T, A, R> Collector<T, A, R> noOp() {
      return (Collector<T, A, R>) NO_OP;
   }
         
   final Spliterator<? extends S> source;
   final StreamPipeline<S, ?, U> predecessor;
   boolean started;
   boolean linked;
   AtomicBoolean closed = new AtomicBoolean();
   List<Runnable> onClose;
   
   StreamPipeline(Spliterator<? extends S> source, StreamPipeline<S, ?, U> predecessor) {
      this.source = requireNonNull(source);
      if (predecessor != null) {
         if (predecessor.linked || predecessor.started || predecessor.closed.get()) {
            throw new IllegalStateException("Stream already used");
         }
         // mark the predecessor as linked so it can't mistakenly be re-used
         predecessor.linked = true;
      }
      this.predecessor = predecessor;
   }
   
   /**
    * Returns a view of the given stage, backed by the given source, as an {@link Upstream}. The
    * given source will either be the pipeline's source or a subset thereof (via
    * {@link Spliterator#trySplit()}).
    * 
    * <p>This verifies that the stream is not closed and then delegates to
    * {@link #doAsUpstream(Spliterator)}.
    * 
    * @param source the source of data to be used
    * @return a view of the given stage, backed by the given source, as an {@link Upstream}
    */
   final Upstream<T> asUpstream(Spliterator<? extends S> source) {
      assert source != null;
      if (closed.get()) {
         throw new IllegalStateException("Stream already used");
      }
      return doAsUpstream(source);
   }
   
   boolean needNewSpliteratorIfOrdered() {
      return predecessor != null && predecessor.needNewSpliteratorIfOrdered();
   }
   
   void checkState() {
      if (linked || started || closed.get()) {
         throw new IllegalStateException("Stream already used");
      }
   }
   
   /**
    * Marks the stage as started, which makes it unusable for subsequent operations. This occurs
    * when a terminal operation for the stream begins.
    */
   void start() {
      if (started) {
         throw new IllegalStateException("Stream already used");
      }
      started = true;
      if (predecessor != null) {
         predecessor.start();
      }
   }
   
   /**
    * Returns a view of this stage, backed by the given source, as an {@link Upstream}.
    * 
    * @param source the source of data to be used
    * @return a view of the given stage, backed by the given source, as an {@link Upstream}
    * 
    * @see #asUpstream(Spliterator)
    */
   abstract Upstream<T> doAsUpstream(Spliterator<? extends S> source);

   /**
    * Returns the characteristics of the stream's source spliterator.
    * 
    * @return the characteristics of the stream's source spliterator
    */
   abstract int spliteratorCharacteristics();

   /**
    * Computes characteristics this a source spliterator. The given values will be the
    * characteristics, as reported by this stream's source spliterator or a subset thereof.
    * Some stages modify the characteristics that subsequent stages see, so this is where that
    * modification occurs. For example, a {@link #filter(Predicate)} stage will remove the
    * {@linkplain Spliterator#SIZED sized} characteristic because, even if the number of source
    * elements may be known apriori, the number that will pass the filter is not.
    * 
    * @param characteristics a given set of spliterator characteristics
    * @return an adjusted set of characteristics
    */
   abstract int spliteratorCharacteristics(int characteristics);

   /**
    * Computes or estimates the size of the source data. This may override the source spliterator's
    * estimate, since some stages may modify the number of elements that will be emitted.
    * 
    * @return the size of the source data, or an estimate thereof
    */
   abstract long spliteratorSize();

   /**
    * Returns the comparator for the source spliterator, if the source spliterator has the
    * {@linkplain Spliterator#SORTED sorted} characteristic.
    * 
    * @return the comparator for the source spliterator
    */
   abstract Comparator<? super T> spliteratorComparator();

   @Override
   public IntStream mapToInt(ToIntFunction<? super T> mapper) {
      checkState();
      linked = true;
      return StreamSupport
            .intStream(
                  () -> MoreSpliterators.mapToInt(basicSpliterator(), mapper),
                  spliteratorCharacteristics(),
                  isParallel())
            .onClose(this::close);
   }

   @Override
   public LongStream mapToLong(ToLongFunction<? super T> mapper) {
      checkState();
      linked = true;
      return StreamSupport
            .longStream(
                  () -> MoreSpliterators.mapToLong(basicSpliterator(), mapper),
                  spliteratorCharacteristics(),
                  isParallel())
            .onClose(this::close);
   }

   @Override
   public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
      checkState();
      linked = true;
      return StreamSupport
            .doubleStream(
                  () -> MoreSpliterators.mapToDouble(spliterator(), mapper),
                  spliteratorCharacteristics(),
                  isParallel())
            .onClose(this::close);
   }

   @Override
   public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
      checkState();
      linked = true;
      return StreamSupport
            .stream(this::spliterator, spliteratorCharacteristics(), isParallel())
            .flatMapToInt(mapper)
            .onClose(this::close);
   }
   
   @Override
   public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
      checkState();
      linked = true;
      return StreamSupport
            .stream(this::spliterator, spliteratorCharacteristics(), isParallel())
            .flatMapToLong(mapper)
            .onClose(this::close);
   }

   @Override
   public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
      checkState();
      linked = true;
      return StreamSupport
            .stream(this::spliterator, spliteratorCharacteristics(), isParallel())
            .flatMapToDouble(mapper)
            .onClose(this::close);
   }

   @Override
   public void forEach(Consumer<? super T> action) {
      checkState();
      new Tail<>(this, Collector.of(() -> null, (a, t) -> action.accept(t), (a1, a2) -> null))
            .execute();
   }

   @Override
   public void forEachOrdered(Consumer<? super T> action) {
      if ((spliteratorCharacteristics() & Spliterator.ORDERED) == 0) {
         forEach(action);
      }
      checkState();
      collect(Collectors.toList()).forEach(action);
   }

   @Override
   public Object[] toArray() {
      return collect(Collector.of(ArrayList::new, ArrayList::add,
            (l1, l2) -> { l1.addAll(l2); return l1; }, ArrayList::toArray));
   }

   @Override
   public <A> A[] toArray(IntFunction<A[]> generator) {
      return collect(Collector.of(ArrayList::new, ArrayList::add,
            (l1, l2) -> { l1.addAll(l2); return l1; }, l -> l.toArray(generator.apply(0))));
   }

   @Override
   public T reduce(T identity, BinaryOperator<T> accumulator) {
      return collect(Collector.of(() -> new Variable<T>(identity),
            (v, t) -> v.set(accumulator.apply(v.get(), t)),
            (v1, v2) -> { v1.set(accumulator.apply(v1.get(), v2.get())); return v1; },
            Variable::get));
   }

   @Override
   public Optional<T> reduce(BinaryOperator<T> accumulator) {
      return collect(Collector.of(() -> new Variable<T>(),
            (v, t) -> v.set(v.get() == null ? t : accumulator.apply(v.get(), t)),
            (v1, v2) -> { 
               if (v1.get() == null) {
                  return v2;
               }
               if (v2.get() == null) {
                  return v1;
               }
               v1.set(accumulator.apply(v1.get(), v2.get()));
               return v1;
            },
            v -> Optional.ofNullable(v.get())));
   }

   @Override
   public <V> V reduce(V identity, BiFunction<V, ? super T, V> accumulator,
         BinaryOperator<V> combiner) {
      return collect(Collector.of(() -> new Variable<V>(identity),
            (v, t) -> v.set(accumulator.apply(v.get(), t)),
            (v1, v2) -> { v1.set(combiner.apply(v1.get(), v2.get())); return v1; },
            Variable::get));
   }

   @Override
   public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator,
         BiConsumer<R, R> combiner) {
      return collect(Collector.of(supplier, accumulator,
            (r1, r2) -> { combiner.accept(r1, r2); return r1; }));
   }

   @Override
   public <R, A> R collect(Collector<? super T, A, R> collector) {
      checkState();
      return new Tail<>(this, collector).execute();
   }

   @Override
   public Optional<T> min(Comparator<? super T> comparator) {
      return reduce((t1, t2) -> comparator.compare(t1, t2) <= 0 ? t1 : t2);
   }

   @Override
   public Optional<T> max(Comparator<? super T> comparator) {
      return reduce((t1, t2) -> comparator.compare(t1, t2) >= 0 ? t1 : t2);
   }

   @Override
   public long count() {
      checkState();
      if ((spliteratorCharacteristics() & Spliterator.SIZED) != 0) {
         start();
         try {
            return spliterator().estimateSize();
         } finally {
            close();
         }
      }
      if (isParallel()) {
         return collect(Collector.of(() -> new AtomicLong(),
               (s, t) -> s.incrementAndGet(),
               (s1, s2) -> { s1.addAndGet(s2.get()); return s1; },
               AtomicLong::get,
               Characteristics.CONCURRENT));
      } else {
         return collect(Collector.of(() -> new VariableLong(),
               (s, t) -> s.incrementAndGet(),
               (s1, s2) -> { s1.addAndGet(s2.get()); return s1; },
               VariableLong::get));
      }
   }

   @Override
   public boolean anyMatch(Predicate<? super T> predicate) {
      checkState();
      if (isParallel()) {
         AtomicBoolean anyMatched = new AtomicBoolean();
         StreamPipeline<S, T, Void> matcher = new Intermediate<>(this,
               () -> new AbstractStreamNode<Void, T>() {
                  @Override
                  public boolean getNext(Upstream<T> upstream, Consumer<? super Void> action) {
                     while (!anyMatched.get() && nextUpstream(upstream)) {
                        if (predicate.test(latestUpstream())) {
                           if (anyMatched.compareAndSet(false, true)) {
                              action.accept(null);
                              return true;
                           }
                           return false;
                        }
                     }
                     return false;
                  }
               },
               ch -> ch & ~(Spliterator.SIZED | Spliterator.SUBSIZED),
               sz -> 1);
        new Tail<>(matcher, noOp()).execute();
        return anyMatched.get();
      } else {
         start();
         try {
            for (Iterator<T> iter = basicIterator(); iter.hasNext(); ) {
               if (predicate.test(iter.next())) {
                  return true;
               }
            }
            return false;
         } finally {
            close();
         }
      }
   }

   @Override
   public boolean allMatch(Predicate<? super T> predicate) {
      checkState();
      if (isParallel()) {
         AtomicBoolean allMatched = new AtomicBoolean(true);
         StreamPipeline<S, T, Void> matcher = new Intermediate<>(this,
               () -> new AbstractStreamNode<Void, T>() {
                  @Override
                  public boolean getNext(Upstream<T> upstream, Consumer<? super Void> action) {
                     while (allMatched.get() && nextUpstream(upstream)) {
                        if (!predicate.test(latestUpstream())) {
                           if (allMatched.compareAndSet(true, false)) {
                              action.accept(null);
                              return true;
                           }
                           return false;
                        }
                     }
                     return false;
                  }
               },
               ch -> ch & ~(Spliterator.SIZED | Spliterator.SUBSIZED),
               sz -> 1);
        new Tail<>(matcher, noOp()).execute();
        return allMatched.get();
     } else {
        start();
        try {
           for (Iterator<T> iter = basicIterator(); iter.hasNext(); ) {
              if (!predicate.test(iter.next())) {
                 return false;
              }
           }
           return true;
        } finally {
           close();
        }
      }
   }

   @Override
   public boolean noneMatch(Predicate<? super T> predicate) {
      checkState();
      if (isParallel()) {
         AtomicBoolean noneMatched = new AtomicBoolean(true);
         StreamPipeline<S, T, Void> matcher = new Intermediate<>(this,
               () -> new AbstractStreamNode<Void, T>() {
                  @Override
                  public boolean getNext(Upstream<T> upstream, Consumer<? super Void> action) {
                     while (noneMatched.get() && nextUpstream(upstream)) {
                        if (predicate.test(latestUpstream())) {
                           if (noneMatched.compareAndSet(true, false)) {
                              action.accept(null);
                              return true;
                           }
                           return false;
                        }
                     }
                     return false;
                  }
               },
               ch -> ch & ~(Spliterator.SIZED | Spliterator.SUBSIZED),
               sz -> 1);
        new Tail<>(matcher, noOp()).execute();
        return noneMatched.get();
     } else {
        start();
        try {
           for (Iterator<T> iter = basicIterator(); iter.hasNext(); ) {
              if (predicate.test(iter.next())) {
                 return false;
              }
           }
           return true;
        } finally {
           close();
        }
      }
   }

   @Override
   public Optional<T> findFirst() {
      checkState();
      if ((spliteratorCharacteristics() & Spliterator.ORDERED) == 0) {
         return findAny();
      }
      return getFirst();
   }
   
   @Override
   public Optional<T> findAny() {
      checkState();
      if (isParallel()) {
         AtomicMarkableReference<T> encountered = new AtomicMarkableReference<>(null, false);
         StreamPipeline<S, T, Void> matcher = new Intermediate<>(this,
               () -> new AbstractStreamNode<Void, T>() {
                  @Override
                  public boolean getNext(Upstream<T> upstream, Consumer<? super Void> action) {
                     while (!encountered.isMarked() && nextUpstream(upstream)) {
                        if (encountered.compareAndSet(null, latestUpstream(), false, true)) {
                           action.accept(null);
                           return true;
                        }
                     }
                     return false;
                  }
               },
               ch -> ch & ~(Spliterator.SUBSIZED),
               sz -> 1);
        new Tail<>(matcher, noOp()).execute();
        return encountered.isMarked() ? Optional.of(encountered.getReference()) : Optional.empty();
      } else {
         return getFirst();
      }
   }

   private Optional<T> getFirst() {
      checkState();
      start();
      try {
         Iterator<T> iter = basicIterator();
         return iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();
      } finally {
         close();
      }
   }

   @Override
   public Iterator<T> iterator() {
      checkState();
      start();
      // The stream will be closed either when the iterator is exhausted or (if never exhausted)
      // when it is garbage collected. The latter is achieved via the use of a finalizer.
      return new Iterator<T>() {
         final Upstream<T> u = asUpstream(source);
         boolean needNext = true;
         Variable<T> next = new Variable<>();
         Consumer<T> upstreamReceiver = next::set;

         @Override
         protected void finalize() {
            close();
         }

         private void maybeFetchNext() {
            if (!needNext) {
               return;
            }
            if (!u.getUpstream(upstreamReceiver)) {
               next = null;
               close();
            }
            needNext = false;
         }
         
         @Override
         public boolean hasNext() {
            maybeFetchNext();
            return next != null;
         }

         @Override
         public T next() {
            maybeFetchNext();
            if (next == null) {
               throw new NoSuchElementException();
            }
            T ret = next.getAndSet(null);
            needNext = true;
            return ret;
         }
      };
   }
   
   /**
    * Returns an iterator over the stream of elements. Unlike {@link #iterator()}, this does not
    * mark the stage as started nor does it try to automatically close the stream once exhausted.
    * Because of that, it may have slightly lower overhead, but is only suitable for use in terminal
    * operations that properly handle the stream lifecycle (e.g. start and close).
    * 
    * @return an iterator over the stream of elements that does not mark the stage as started and
    *       does not try to automatically close the stream upon completion
    */
   private Iterator<T> basicIterator() {
      return new Iterator<T>() {
         final Upstream<T> u = asUpstream(source);
         boolean needNext = true;
         Variable<T> next = new Variable<>();
         Consumer<T> upstreamReceiver = next::set;

         private void maybeFetchNext() {
            if (!needNext) {
               return;
            }
            if (!u.getUpstream(upstreamReceiver)) {
               next = null;
            }
            needNext = false;
         }
         
         @Override
         public boolean hasNext() {
            maybeFetchNext();
            return next != null;
         }

         @Override
         public T next() {
            maybeFetchNext();
            if (next == null) {
               throw new NoSuchElementException();
            }
            T ret = next.getAndSet(null);
            needNext = true;
            return ret;
         }
      };
   }

   @Override
   public Spliterator<T> spliterator() {
      checkState();
      start();
      return new ClosingPipelineSpliterator();
   }
   
   private Spliterator<T> basicSpliterator() {
      return new PipelineSpliterator();
   }
   
   private Spliterator<T> autoStartSpliterator() {
      return new AutoStartPipelineSpliterator();
   }

   @Override
   public boolean isParallel() {
      return predecessor.isParallel();
   }

   @Override
   public void close() {
      if (closed.compareAndSet(false, true)) {
         Throwable th = null;
         for (Runnable r : onClose) {
            try {
               r.run();
            } catch (Throwable t) {
               if (th == null) {
                  th = t;
               } else if (th != t) {
                  th.addSuppressed(t);
               }
            }
         }
         if (predecessor != null) {
            try {
               predecessor.close();
            } catch (Throwable t) {
               if (th == null) {
                  th = t;
               } else if (th != t) {
                  th.addSuppressed(t);
               }
            }
         }
         if (th != null) {
            throw Throwables.propagate(th);
         }
      }
   }

   @Override
   public FluentStream<T> sequential() {
      predecessor.sequential();
      return this;
   }

   @Override
   public FluentStream<T> parallel() {
      predecessor.parallel();
      return this;
   }

   @Override
   public FluentStream<T> unordered() {
      if ((spliteratorCharacteristics() & Spliterator.ORDERED) == 0) {
         return this;
      }
      return new StreamPipeline<S, T, T>(source, this) {
         @Override
         Upstream<T> doAsUpstream(Spliterator<? extends S> source) {
            return predecessor.doAsUpstream(source);
         }

         @Override
         int spliteratorCharacteristics() {
            return spliteratorCharacteristics(predecessor.spliteratorCharacteristics());
         }

         @Override
         int spliteratorCharacteristics(int characteristics) {
            return characteristics & ~(Spliterator.ORDERED);
         }

         @Override
         long spliteratorSize() {
            return predecessor.spliteratorSize();
         }
         
         @Override
         Comparator<? super T> spliteratorComparator() {
            return predecessor.spliteratorComparator();
         }
      };
   }

   @Override
   public FluentStream<T> onClose(Runnable closeHandler) {
      if (onClose == null) {
         onClose = new ArrayList<>();
      }
      onClose.add(closeHandler);
      return this;
   }

   @Override
   public FluentStream<T> filter(Predicate<? super T> predicate) {
      checkState();
      return new Intermediate<>(this,
            () -> new AbstractStreamNode<T, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super T> action) {
                  while (nextUpstream(upstream)) {
                     T t = latestUpstream();
                     if (predicate.test(t)) {
                        action.accept(t);
                        return true;
                     }
                  }
                  return false;
               }
            },
            ch -> ch & ~(Spliterator.SIZED | Spliterator.SUBSIZED),
            LongUnaryOperator.identity());
   }

   @Override
   public <R> FluentStream<R> map(Function<? super T, ? extends R> mapper) {
      checkState();
      return new Intermediate<>(this,
            () -> new AbstractStreamNode<R, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super R> action) {
                  if (nextUpstream(upstream)) {
                     action.accept(mapper.apply(latestUpstream()));
                     return true;
                  }
                  return false;
               }
            },
            ch -> ch & ~(Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.NONNULL),
            LongUnaryOperator.identity());
   }

   @Override
   public <R> FluentStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
      checkState();
      return new Intermediate<>(this,
            () -> new AbstractStreamNode<R, T>() {
               Spliterator<? extends R> current;
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super R> action) {
                  while (true) {
                     if (current == null) {
                        if (nextUpstream(upstream)) {
                           current = mapper.apply(latestUpstream()).spliterator();
                        } else {
                           return false;
                        }
                     }
                     if (current.tryAdvance(action)) {
                        return true;
                     } else {
                        current = null;
                     }
                  }
               }
            },
            ch -> ch & ~(Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.NONNULL
                          | Spliterator.SIZED | Spliterator.SUBSIZED),
            LongUnaryOperator.identity());
   }

   @Override
   public FluentStream<T> distinct() {
      checkState();
      if ((spliteratorCharacteristics() & Spliterator.DISTINCT) != 0) {
         return this; // already distinct
      }
      Variable<Set<T>> set = new Variable<>();
      return new Intermediate<S, T, T>(this,
            () -> new AbstractStreamNode<T, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super T> action) {
                  while (nextUpstream(upstream)) {
                     T t = latestUpstream();
                     if (set.get().add(t)) {
                        action.accept(t);
                        return true;
                     }
                  }
                  return false;
               }
            },
            ch -> ch & ~(Spliterator.SIZED | Spliterator.SUBSIZED) | Spliterator.DISTINCT,
            LongUnaryOperator.identity()) {
         @Override void start() {
            super.start();
            set.set(isParallel() ? new LinkedHashSet<>() : ConcurrentHashMap.newKeySet());
         }
      };
   }

   @Override
   public FluentStream<T> sorted() {
      return sorted(CollectionUtils.naturalOrder());
   }

   @Override
   public FluentStream<T> sorted(Comparator<? super T> comparator) {
      checkState();
      if ((spliteratorCharacteristics() & Spliterator.SORTED) != 0) {
         if (Objects.equal(comparator == CollectionUtils.naturalOrder() ? null : comparator,
               spliteratorComparator())) {
            return this; // already properly sorted
         }
      }
      linked = true;
      return attach(new Head<T>(
            () -> {
               // rely on the base implementation of sort
               Stream<T> sorted = StreamSupport.stream(
                     this::autoStartSpliterator, spliteratorCharacteristics(), isParallel());
               if (comparator == CollectionUtils.naturalOrder()) {
                  sorted = sorted.sorted();
               } else {
                  sorted = sorted.sorted(comparator);
               }
               return sorted.spliterator(); 
            },
            spliteratorCharacteristics() & ~(Spliterator.CONCURRENT)
                  | Spliterator.ORDERED | Spliterator.SORTED,
            comparator));
   }

   @Override
   public FluentStream<T> peek(Consumer<? super T> peeker) {
      checkState();
      return new Intermediate<>(this,
            () -> new AbstractStreamNode<T, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super T> action) {
                  if (nextUpstream(upstream)) {
                     T t = latestUpstream();
                     peeker.accept(t);
                     action.accept(t);
                     return true;
                  }
                  return false;
               }
            },
            IntUnaryOperator.identity(),
            LongUnaryOperator.identity());
   }
   
   @FunctionalInterface
   interface Incrementer {
      long incrementAndGet();
      
      static Incrementer simple() {
         @SuppressWarnings("serial")
         class SimpleIncrementer extends VariableLong implements Incrementer {
         }
         return new SimpleIncrementer();
      }

      static Incrementer threadSafe() {
         @SuppressWarnings("serial")
         class AtomicIncrementer extends AtomicLong implements Incrementer {
         }
         return new AtomicIncrementer();
      }
   }

   @Override
   public FluentStream<T> limit(long maxSize) {
      checkState();
      Variable<Incrementer> counter = new Variable<>();
      return new LimitSkipIntermediate<>(this,
            () -> new AbstractStreamNode<T, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super T> action) {
                  if (counter.get().incrementAndGet() > maxSize) {
                     return false;
                  }
                  if (upstream.getUpstream(action)) {
                     return true;
                  }
                  return false;
               }
            },
            () -> {
               if (isParallel()) {
                  counter.set(Incrementer.threadSafe());
               } else {
                  counter.set(Incrementer.simple());
               }
            },
            ch -> ch & ~(Spliterator.SUBSIZED),
            sz -> Math.max(sz, maxSize));
   }

   @Override
   public FluentStream<T> skip(long n) {
      checkState();
      Variable<Incrementer> counter = new Variable<>();
      return new LimitSkipIntermediate<>(this,
            () -> new AbstractStreamNode<T, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super T> action) {
                  if (counter.get().incrementAndGet() < n) {
                     // skip the item
                     upstream.getUpstream(a -> {});
                     return false;
                  }
                  if (upstream.getUpstream(action)) {
                     return true;
                  }
                  return false;
               }
            },
            () -> {
               if (isParallel()) {
                  counter.set(Incrementer.threadSafe());
               } else {
                  counter.set(Incrementer.simple());
               }
            },
            ch -> ch & ~(Spliterator.SUBSIZED),
            sz -> Math.max(0, sz - n));
   }

   @Override
   public FluentStream<List<T>> batch(int count) {
      if (count <= 0) {
         throw new IllegalArgumentException();
      }
      checkState();
      return new Intermediate<>(this,
            () -> new AbstractStreamNode<List<T>, T>() {
               @Override
               public boolean getNext(Upstream<T> upstream, Consumer<? super List<T>> action) {
                  ArrayList<T> list = new ArrayList<>();
                  while (list.size() < count && upstream.getUpstream(list::add));
                  if (list.isEmpty()) {
                     return false;
                  }
                  action.accept(list);
                  return true;
               }
            },
            ch -> ch & ~(Spliterator.SUBSIZED | Spliterator.SORTED) | Spliterator.NONNULL,
            sz -> {
               long numBatches = sz / count;
               return numBatches * count < sz ? numBatches + 1 : numBatches;
            });
   }
   
   @Override
   public FluentStream<T> concat(Stream<? extends T> other) {
      return new Head<>(Stream.concat(this, other));
   }

   @Override
   public FluentStream<T> concat(Iterable<? extends Stream<? extends T>> others) {
      FluentStream<T> stream = new Head<T>(
            () -> MoreSpliterators.concat(
                  Iterables.transform(cons(this, others), Stream::spliterator)),
            0);
      stream.onClose(() -> {
         close();
         for (Stream<?> s : others) {
            s.close();
         }
      });
      return stream;
   }
   
   private static <T> Iterable<T> cons(T car, Iterable<? extends T> cdr) {
      return () -> new Iterator<T>() {
         Iterator<? extends T> rest = cdr.iterator();
         boolean consumedFirst;
         
         @Override
         public boolean hasNext() {
            return !consumedFirst || rest.hasNext();
         }

         @Override
         public T next() {
            if (!consumedFirst) {
               consumedFirst = true;
               return car;
            }
            return rest.next();
         }
      };
   }

   public <V> FluentStream<V> operator(StreamNode<V, T> operator) {
      checkState();
      return new Intermediate<S, T, V>(this, () -> operator, IntUnaryOperator.identity(),
            LongUnaryOperator.identity());
   }

   public <V> FluentStream<V> operator(StreamOperator<V, T> operator) {
      checkState();
      return new Intermediate<S, T, V>(this, operator::createNode,
            operator::spliteratorCharacteristics, operator::spliteratorEstimatedSize) {
         {
            onClose(() -> {
               try {
                  operator.close();
               } catch (RuntimeException | Error e) {
                  throw e;
               } catch (Exception e) {
                  throw new RuntimeException(e);
               }
            });
         }

         @Override
         void start() {
            super.start();
            operator.startStream();
         }
      };
   }
   
   @Override
   public <V> FluentStream<V> operatorBridge(StreamBridge<V, T> operator) {
      checkState();
      Spliterator<T> split = autoStartSpliterator();
      Either<Stream<V>, Spliterator<V>> result = operator.bridgeFrom(split, isParallel());
      if (result.hasFirst()) {
         Stream<V> stream = result.getFirst();
         if (stream == StreamPipeline.this) {
            if (started) {
               close();
               throw new IllegalStateException("Operator incorrectly started stream");
            }
            @SuppressWarnings("unchecked") // if result is us, we know V and T are same type
            FluentStream<V> ret = (FluentStream<V>) StreamPipeline.this;
            return ret;
         }
         linked = true;
         return attach(new Head<>(stream));
      }
      Spliterator<V> newSplit = result.getSecond();
      if (newSplit == split) {
         if (started) {
            close();
            throw new IllegalStateException("Operator incorrectly started stream");
         }
         @SuppressWarnings("unchecked") // if same spliterator, we know V and T are same type
         FluentStream<V> ret = (FluentStream<V>) StreamPipeline.this;
         return ret;
      }
      linked = true;
      return attach(new Head<>(newSplit));
   }

   @Override
   public Supplier<FluentStream<T>> fork(int numForks) {
      checkState();
      linked = true;
      Supplier<Spliterator<T>> forks = MoreSpliterators.fork(basicSpliterator(), 2);
      int characteristics = spliteratorCharacteristics();
      AtomicBoolean atomicStarted = new AtomicBoolean();
      class ForkedHead extends Head<T> {
         ForkedHead(Supplier<Spliterator<T>> source, int spliteratorCharacteristics) {
            super(source, spliteratorCharacteristics);
            onClose(StreamPipeline.this::close);
         }
         @Override void start() {
            super.start();
            if (atomicStarted.compareAndSet(false, true)) {
               // only start upstream once
               StreamPipeline.this.start();
            }
         }
      };
      return () -> {
         Spliterator<T> forkedSpliterator = forks.get();
         return new ForkedHead(() -> forkedSpliterator, characteristics);
      };
   }

   @Override
   public <K, V> FluentStream<Entry<K, Collection<V>>> groupBy(
         Function<? super T, ? extends K> keyExtractor,
         Function<? super T, ? extends V> valueExtractor) {
      checkState();
      linked = true;
      return new Head<>(
            () -> {
               Map<K, Collection<V>> map = toMap(this, keyExtractor, valueExtractor);
               return map.entrySet().spliterator();
            },
            Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.DISTINCT);
   }

   @Override
   public <K, V> FluentStream<Entry<K, Collection<V>>> merge(Stream<? extends T> other,
         Function<? super T, ? extends K> keyExtractor,
         Function<? super T, ? extends V> valueExtractor) {
      checkState();
      linked = true;
      return new Head<>(
            () -> {
               Map<K, Collection<V>> map =
                     toMap(Stream.concat(this, other), keyExtractor, valueExtractor);
               return map.entrySet().spliterator();
            },
            Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.DISTINCT);
   }

   @Override
   public <K, O, V, W, X> FluentStream<X> join(
         Stream<? extends O> other,
         Function<? super T, ? extends K> keyExtractor1,
         Function<? super T, ? extends V> valueExtractor1,
         Function<? super O, ? extends K> keyExtractor2,
         Function<? super O, ? extends W> valueExtractor2,
         TriFunction<? super K, ? super Collection<V>, ? super Collection<W>, ? extends X> combiner) {
      checkState();
      linked = true;
      return new Head<>(
            () -> {
               Map<K, Collection<V>> map1;
               Map<K, Collection<W>> map2;
               if (isParallel()) {
                  CompletableFuture<Map<K, Collection<W>>> future = CompletableFuture.supplyAsync(
                        () -> toMap(other, keyExtractor2, valueExtractor2));
                  map1 = toMap(this, keyExtractor1, valueExtractor1);
                  map2 = future.join();
               } else {
                  map1 = toMap(this, keyExtractor1, valueExtractor1);
                  map2 = toMap(other, keyExtractor2, valueExtractor2);
               }
               Set<K> keys = Sets.union(map1.keySet(), map2.keySet());
               Stream<X> s = keys.stream()
                     .map(k -> {
                        Collection<V> c1 = map1.get(k);
                        Collection<W> c2 = map2.get(k);
                        return combiner.apply(k,
                              c1 == null ? Collections.emptyList() : c1,
                              c2 == null ? Collections.emptyList() : c2);
                     });
               if (isParallel()) {
                  s = s.parallel();
               }
               return s.spliterator();
            },
            Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.DISTINCT);
   }
   
   private static <T, K, V> Map<K, Collection<V>> toMap(Stream<? extends T> stream,
         Function<? super T, ? extends K> keyExtractor,
         Function<? super T, ? extends V> valueExtractor) {
      Map<K, Collection<V>> map = stream.isParallel()
            ? new ConcurrentHashMap<>()
            : new HashMap<>();
      Function<K, Collection<V>> listFactory = stream.isParallel()
            ? k -> new ConcurrentLinkedQueue<>()
            : k -> new ArrayList<>();
      Characteristics[] ch = stream.isParallel()
            ? new Characteristics[] { Characteristics.CONCURRENT }
            : new Characteristics[0]; 
      stream.collect(Collector.of(() -> map,
            (m, t) -> m.computeIfAbsent(keyExtractor.apply(t), listFactory)
                  .add(valueExtractor.apply(t)),
            (m1, m2) -> { m1.putAll(m2); return m1; },
            ch));
      return map;
   }
   
   /**
    * Attaches the given stream to this one by having the given stream close this one when it is
    * closed. This is used to ensure that resources are freed by this stream after a dependent
    * stream is closed.
    *  
    * @param newStream the new stream
    * @return the given stream
    */
   private <V> FluentStream<V> attach(Head<V> newStream) {
      newStream.onClose(this::close);
      return newStream;
   }

   /**
    * A spliterator for the stream pipeline. The items emitted are the results of the source of
    * data flowing through all configured stages (the associated pipeline stage and all of its
    * predecessors).
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class PipelineSpliterator implements Spliterator<T> {
      final Spliterator<? extends S> source;
      final Upstream<T> u;
      final int characteristics;
      final long size;

      PipelineSpliterator() {
         this(StreamPipeline.this.source, spliteratorCharacteristics(), spliteratorSize());
      }

      PipelineSpliterator(Spliterator<? extends S> source, int characteristics, long size) {
         this.source = source;
         this.u = asUpstream(source);
         this.characteristics = characteristics;
         this.size = size;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         return u.getUpstream(action);
      }

      @Override
      public Spliterator<T> trySplit() {
         Spliterator<? extends S> split = source.trySplit();
         return split != null
               ? wrap(split, spliteratorCharacteristics(split.characteristics()))
               : null;
      }

      PipelineSpliterator wrap(Spliterator<? extends S> spliterator, int characteristics) {
         return new PipelineSpliterator(spliterator, characteristics, spliterator.estimateSize());
      }

      @Override
      public Comparator<? super T> getComparator() {
         if (!hasCharacteristics(Spliterator.SORTED)) {
            throw new IllegalStateException();
         }
         return spliteratorComparator();
      }
      
      @Override
      public long estimateSize() {
         return size;
      }

      @Override
      public int characteristics() {
         return characteristics;
      }
   }
   
   /**
    * A spliterator for the stream pipeline that starts the pipeline on first use. This relies on
    * downstream usages properly {@linkplain StreamPipeline#close() closing} after use in order to
    * clean-up (e.g. no finalizers to ensure close is called). "First use" means the first time
    * either {@link #tryAdvance(Consumer)} or {@link #trySplit()} is called. Other methods can be
    * freely invoked without causing the pipeline to be started.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class AutoStartPipelineSpliterator extends PipelineSpliterator {
      AutoStartPipelineSpliterator() {
         super();
      }
      
      AutoStartPipelineSpliterator(Spliterator<? extends S> spliterator, int characteristics,
            long size) {
         super(spliterator, characteristics, size);
      }
      
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         if (!StreamPipeline.this.started) {
            start();
         }
         return super.tryAdvance(action);
      }

      @Override
      public Spliterator<T> trySplit() {
         if (!StreamPipeline.this.started) {
            start();
         }
         return super.trySplit();
      }
   }

   /**
    * A spliterator for the pipeline that automatically closes the stream upon completion. The
    * stream completes when the spliterator, and any child spliterators, are exhausted. If this
    * never happens, the stream is closed when the spliterator is garbage collected. The latter is
    * achieved via the use of a finalizer.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class ClosingPipelineSpliterator extends PipelineSpliterator {
      final Runnable onClose;
      final AtomicInteger count;

      ClosingPipelineSpliterator() {
         super();
         this.onClose = StreamPipeline.this::close;
         this.count = new AtomicInteger(1);
      }

      ClosingPipelineSpliterator(Spliterator<? extends S> source, int characteristics, long size,
            Runnable onClose, AtomicInteger count) {
         super(source, characteristics, size);
         this.onClose = onClose;
         this.count = count;
      }

      @Override
      protected void finalize() {
         close();
      }
      
      private void close() {
         if (count.decrementAndGet() == 0) {
            onClose.run();
         }
      }
      
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         if (!super.tryAdvance(action)) {
            close();
            return false;
         }
         return true;
      }


      @Override
      public Spliterator<T> trySplit() {
         Spliterator<T> split = super.trySplit();
         if (split != null) {
            count.incrementAndGet();
         }
         return split;
      }
      
      @Override
      PipelineSpliterator wrap(Spliterator<? extends S> spliterator, int characteristics) {
         return new ClosingPipelineSpliterator(spliterator, characteristics, size, onClose, count);
      }
   }

   /**
    * The head of a stream pipeline, backed by a source of data. The source of data is a
    * spliterator. This kind of pipeline stage has no predecessor. It may have a "logical"
    * predecessor, however, if the spliterator is actually sourced by another pipeline stage. (Some
    * kinds of intermediate stages require that sort of pipeline configuration.)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <S> the type of the source data
    */
   static class Head<S> extends StreamPipeline<S, Void, S> {
      boolean parallel;

      Head(Stream<? extends S> source) {
         this(source::spliterator);
         onClose(source::close);
         parallel = source.isParallel();
      }
      
      Head(Supplier<? extends Spliterator<? extends S>> source) {
         this(MoreSpliterators.lazySpliterator(source));
      }
      
      Head(Supplier<? extends Spliterator<? extends S>> source, int characteristics) {
         this(MoreSpliterators.lazySpliterator(source, characteristics));
      }

      Head(Supplier<? extends Spliterator<? extends S>> source, int characteristics,
            Comparator<? super S> comparator) {
         this(MoreSpliterators.lazySpliterator(source, characteristics, comparator));
      }

      Head(Spliterator<? extends S> source) {
         super(source, null);
      }

      @Override Upstream<S> doAsUpstream(Spliterator<? extends S> source) {
         return source::tryAdvance;
      }

      @Override
      public boolean isParallel() {
         return parallel;
      }

      @Override
      public FluentStream<S> sequential() {
         parallel = false;
         return this;
      }

      @Override
      public FluentStream<S> parallel() {
         parallel = true;
         return this;
      }

      @Override
      int spliteratorCharacteristics() {
         return source.characteristics();
      }

      @Override
      int spliteratorCharacteristics(int characteristics) {
         return characteristics;
      }

      @Override
      long spliteratorSize() {
         return source.estimateSize();
      }

      @Override
      @SuppressWarnings("unchecked")
      Comparator<? super S> spliteratorComparator() {
         return (Comparator<? super S>) source.getComparator();
      }
   }
   
   /**
    * An intermediate stage in the pipeline. This kind of pipeline stage is backed by a predecessor
    * stage, vs. directly backed by a source of data.
    * 
    * When this kind of stage is executed, one or more {@link AbstractStreamNode}s is created (one
    * for each concurrently executing unit or thread, for parallel streams). These nodes then act to
    * apply the intermediate stage logic and deliver resulting data to any successor stages.
    *
    * @param <S> the type of the source of data
    * @param <U> the type of data emitted by the upstream (aka predecessor) stage
    * @param <T> the type of data emitted by this stage
    */
   static class Intermediate<S, U, T> extends StreamPipeline<S, U, T> {
      private final Supplier<StreamNode<T, U>> nodeFactory;
      private final IntUnaryOperator spliteratorCharacteristics;
      private final LongUnaryOperator spliteratorSize;
      
      Intermediate(StreamPipeline<S, ?, U> upstream,
            Supplier<StreamNode<T, U>> nodeFactory,
            IntUnaryOperator spliteratorCharacteristics, LongUnaryOperator spliteratorSize) {
         super(upstream.source, upstream);
         this.nodeFactory = nodeFactory;
         this.spliteratorCharacteristics = spliteratorCharacteristics;
         this.spliteratorSize = spliteratorSize;
      }
      
      @Override
      int spliteratorCharacteristics() {
         return spliteratorCharacteristics(predecessor.spliteratorCharacteristics());
      }

      @Override
      int spliteratorCharacteristics(int characteristics) {
         return spliteratorCharacteristics.applyAsInt(characteristics);
      }

      @Override
      long spliteratorSize() {
         return spliteratorSize.applyAsLong(predecessor.spliteratorSize());
      }
      
      @Override
      @SuppressWarnings("unchecked")
      Comparator<? super T> spliteratorComparator() {
         return (Comparator<? super T>) predecessor.spliteratorComparator();
      }

      @Override
      Upstream<T> doAsUpstream(Spliterator<? extends S> source) {
         Upstream<U> upstream = predecessor.asUpstream(source);
         StreamNode<T, U> node = nodeFactory.get();
         return next -> node.getNext(upstream, next);
      }
   }
   
   /**
    * A special kind of intermediate stage used for {@link #limit(long)} and {@link #skip(long)}
    * operations. Both of these operations need to initialize state that is shared across all
    * stream nodes, for counting the number of elements emitted or skipped. Also, if the data is
    * ordered, then extra steps are taken in a parallel invocation to ensure that the first
    * elements of the stream are skipped or emitted.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <S> the type of the source of data
    * @param <U> the type of data emitted by the upstream (aka predecessor) stage
    * @param <T> the type of data emitted by this stage
    * 
    * @see Intermediate
    */
   static class LimitSkipIntermediate<S, U, T> extends Intermediate<S, U, T> {
      private final Runnable onStart;
      
      LimitSkipIntermediate(StreamPipeline<S, ?, U> upstream,
            Supplier<StreamNode<T, U>> nodeFactory, Runnable onStart,
            IntUnaryOperator spliteratorCharacteristics, LongUnaryOperator spliteratorSize) {
         super(upstream, nodeFactory, spliteratorCharacteristics, spliteratorSize);
         this.onStart = onStart;
      }
      
      @Override
      void start() {
         super.start();
         onStart.run();
      }
      
      @Override
      boolean needNewSpliteratorIfOrdered() {
         return true;
      }
   }

   /**
    * The tail of a stream pipeline. This is the last stage in a pipeline and corresponds to a
    * terminal operation that is a reduction of the stream.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <U> the type of data emitted by the upstream (aka predecessor) stage
    * @param <A> the type of the accumulator, which collects elements as they are emitted 
    * @param <R> the type of the reduction result
    */
   static class Tail<U, A, R> {
      private final StreamPipeline<?, ?, U> predecessor;
      private final Collector<? super U, A, ? extends R> collector;
      
      Tail(StreamPipeline<?, ?, U> predecessor, Collector<? super U, A, ? extends R> collector) {
         this.predecessor = predecessor;
         this.collector = collector;
      }
      
      /**
       * Executes the stream and returns the reduced result.
       * 
       * @return the result of reducing the stream
       */
      R execute() {
         predecessor.start();
         try {
            A accumulator = predecessor.isParallel() && ForkJoinPool.getCommonPoolParallelism() > 1
                  ? invokeParallel(predecessor.basicSpliterator())
                  : invokeSequential(predecessor.basicSpliterator(), null);
            return collector.finisher().apply(accumulator);
         } finally {
            predecessor.close();
         }
      }
         
      /**
       * Invokes the stream pipeline for the given items (the source of data or a subset thereof)
       * sequentially, on the calling thread. If the given result object is not null, the results
       * are accumulated into it. If null, a new accumulator is created.
       * 
       * @param items the data to be processed
       * @param results if non-null, the accumulator to use
       * @return the accumulator into which processed items were emitted; will either be the
       *       given results object or, if the given results object is null, a newly created
       *       accumulator object
       */
      A invokeSequential(Spliterator<U> items, A results) {
         if (results == null) {
            results = collector.supplier().get();
         }
         Variable<U> incoming = new Variable<>();
         Consumer<U> incomingReceiver = incoming::set;
         BiConsumer<A, ? super U> accumulator = collector.accumulator();
         while (items.tryAdvance(incomingReceiver)) {
            accumulator.accept(results, incoming.get());
         }
         return results;
      }
      
      /**
       * Invokes the stream pipeline for the given items (the source of data) in parallel. This
       * submits a {@link RecursiveTask} to the {@linkplain ForkJoinPool#commonPool() common pool}
       * to recursively split the given source and process the data.
       * 
       * @param source the source of the data
       * @return the accumulator into which processed items were emitted
       */
      A invokeParallel(Spliterator<U> source) {
         int characteristics = source.characteristics();
         if ((characteristics & Spliterator.ORDERED) != 0
               && predecessor.needNewSpliteratorIfOrdered()) {
            // we create a new spliterator that will first extract elements in order from existing
            // one to make sure we preserve encounter order on the underlying spliterator even when
            // it will be accessed/processed concurrently
            source = Spliterators.spliterator(
                  Spliterators.iterator(source),
                  source.estimateSize(),
                  characteristics);
         }
         A results = collector.characteristics().contains(Characteristics.CONCURRENT)
               ? collector.supplier().get()
               : null;
         return ForkJoinPool.commonPool()
               .submit(new ParallelInvocation<>(this, source, results))
               .join();
      }
   }
   
   /**
    * A recursive task that recursively splits a source of data, processes it, and combines the
    * accumulated results into a single object.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <S> the type of the source data, emitted by the last stage of a stream pipeline
    * @param <R> the type of the result object
    */
   static class ParallelInvocation<S, R> extends RecursiveTask<R> {
      private static final long serialVersionUID = -349549636795679899L;
      
      private static final int THRESHOLD = 100;
      private static final int MAX_DEPTH = computeDepth(ForkJoinPool.getCommonPoolParallelism() * 2);
      
      private final Tail<S, R, ?> pipeline;
      private final Spliterator<S> source;
      private final R results;
      private int depth;

      ParallelInvocation(Tail<S, R, ?> pipeline, Spliterator<S> source, R results) {
         this(pipeline, source, results, MAX_DEPTH);
      }

      private ParallelInvocation(Tail<S, R, ?> pipeline, Spliterator<S> source,
            R results, int depth) {
         this.pipeline = pipeline;
         this.source = source;
         this.results = results;
         this.depth = depth;
      }

      @Override
      public R compute() {
         Spliterator<S> split;
         if ((source.hasCharacteristics(Spliterator.SIZED) && source.estimateSize() <= THRESHOLD)
               || depth == 0 || (split = source.trySplit()) == null) {
            return pipeline.invokeSequential(source, results);
         }
         depth--;
         ParallelInvocation<S, R> other = new ParallelInvocation<>(pipeline, split, results, depth);
         // fork to compute half of the split
         other.fork();
         // we've split our source and decremented depth, so recursing will compute other half
         R r1 = compute();

         // join the results
         R r2 = other.join();
         if (results != null) {
            assert r1 == results;
            assert r2 == results;
            return results;
         }
         return pipeline.collector.combiner().apply(r1, r2);
      }
      
      private static int computeDepth(int parallelism) {
         if (parallelism < 0) {
            return 0;
         }
         int powerOfTwo = Integer.highestOneBit(parallelism);
         int depth = Integer.numberOfTrailingZeros(powerOfTwo);
         return parallelism > powerOfTwo ? depth + 1 : depth;
      }
   }
   

   
   
   public static void main(String[] args) {
      showCharacteristics(new ArrayList<>());
      showCharacteristics(new HashSet<>());
      showCharacteristics(new LinkedHashSet<>());
      showCharacteristics(new ConcurrentLinkedQueue<>());
      showCharacteristics(ConcurrentHashMap.newKeySet());
      showCharacteristics(new TreeSet<>());
      
      Spliterator<Integer> sp = new Random().ints()
            .mapToObj(i -> i)
            .limit(100)
            .parallel()
            .sorted()
            .spliterator();
      printCharacteristics("100 random ints, sorted", sp);
      VariableInt maxDepth = new VariableInt();
      split(sp, 0, maxDepth);
      System.out.println("max depth = " + maxDepth.get());
   }
   
   private static void split(Spliterator<?> sp, int depth, VariableInt maxDepth) {
      if (depth > maxDepth.get()) {
         maxDepth.set(depth);
      }
      Spliterator<?> other = sp.trySplit();
      if (other == null) {
         while (sp.tryAdvance(System.out::println));
      } else {
         split(sp, depth + 1, maxDepth);
         split(other, depth + 1, maxDepth);
      }
   }
   
   private static void showCharacteristics(Collection<?> coll) {
      Spliterator<?> spliter = coll.spliterator();
      printCharacteristics(coll.getClass().getSimpleName(), spliter);
      printCharacteristics("    (s)sorted", StreamSupport.stream(spliter, false).sorted().spliterator());
      printCharacteristics("    (p)sorted", StreamSupport.stream(spliter, true).sorted().spliterator());
   }
   
   private static Map<String, Integer> characteristics = ImmutableMap.<String, Integer>builder()
         .put("concurrent", Spliterator.CONCURRENT)
         .put("distinct", Spliterator.DISTINCT)
         .put("immutable", Spliterator.IMMUTABLE)
         .put("non-null", Spliterator.NONNULL)
         .put("ordered", Spliterator.ORDERED)
         .put("sized", Spliterator.SIZED)
         .put("sorted", Spliterator.SORTED)
         .put("sub-sized", Spliterator.SUBSIZED)
         .build();
   
   private static void printCharacteristics(String heading, Spliterator<?> spliter) {
      System.out.print(heading);
      System.out.print(":");
      for (Entry<String, Integer> ch : characteristics.entrySet()) {
         if ((spliter.characteristics() & ch.getValue()) != 0) {
            System.out.print(" ");
            System.out.print(ch.getKey());
         }
      }
      System.out.println();
   }
}