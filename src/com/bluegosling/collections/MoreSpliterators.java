package com.bluegosling.collections;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.bluegosling.function.Suppliers;
import com.google.common.base.Function;

/**
 * Utility methods for working with and creating instances of {@link Spliterator}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public final class MoreSpliterators {
   private MoreSpliterators() {
   }
   
   /**
    * Casts the given spliterator to emitting a supertype. Since the spliterator only produces
    * values of its type parameter and does not consume them, this is safe.
    * 
    * @param spliterator a spliterator
    * @return the same spliterator, but emitting a supertype of the given spliterator's type
    */
   @SuppressWarnings("unchecked")
   public static <T> Spliterator<T> cast(Spliterator<? extends T> spliterator) {
      return (Spliterator<T>) spliterator;
   }
   
   /**
    * Returns a spliterator that has the same data as the given spliterator except that each value
    * is first transformed via the given function.
    * 
    * @param spliterator a spliterator
    * @param fn the function that is applied to values emitted by the given spliterator
    * @return a spliterator that emits the results of applying the given function to the values
    *       emitted by the given spliterator
    */
   public static <T, U> Spliterator<U> map(Spliterator<T> spliterator,
         Function<? super T, ? extends U> fn) {
      requireNonNull(spliterator);
      requireNonNull(fn);
      return new Spliterator<U>() {
         @Override
         public boolean tryAdvance(Consumer<? super U> action) {
            return spliterator.tryAdvance(t -> action.accept(fn.apply(t)));
         }
         
         @Override
         public Spliterator<U> trySplit() {
            Spliterator<T> child = spliterator.trySplit();
            return child == null ? null : map(child, fn);
         }

         @Override
         public long estimateSize() {
            return spliterator.estimateSize();
         }

         @Override
         public int characteristics() {
            // after mapping to new type, contents might not be distinct, non-null, or sorted 
            return spliterator.characteristics()
                  & ~(Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SORTED);
         }
         
         @Override
         public void forEachRemaining(Consumer<? super U> action) {
            spliterator.forEachRemaining(t -> action.accept(fn.apply(t)));
         }

         @Override
         public long getExactSizeIfKnown() {
            return spliterator.getExactSizeIfKnown();
         }
      };
   }
   
   /**
    * Returns a spliterator that applies the given function to values emitted by the given
    * spliterator and then flattens the results. This is equivalent to the following:<pre>
    * {@link #flatten(Spliterator) flatten}({@link #map(Spliterator, Function) map}(spliterator, fn))
    * </pre>
    * 
    * @param spliterator a spliterator
    * @param fn the function applied to values emitted by the given spliterator
    * @return a spliterator that flattens the results of applying the given function to the values
    *       emitted by the given spliterator
    */
   public static <T, U> Spliterator<U> flatMap(Spliterator<T> spliterator,
         Function<? super T, ? extends Spliterator<? extends U>> fn) {
      requireNonNull(spliterator);
      requireNonNull(fn);
      return flatten(map(spliterator, fn));
   }
   
   /**
    * Returns a spliterator that filters the data emitted by the given spliterator. Only data for
    * which the given predicate returns {@code true} will be emitted by the returned spliterator.
    * 
    * @param spliterator a spliterator
    * @param filter the predicate used to filter the elements
    * @return a spliterator that emits only the items from the given spliterator that match the
    *       given filter
    */
   public static <T> Spliterator<T> filter(Spliterator<T> spliterator,
         Predicate<? super T> filter) {
      requireNonNull(spliterator);
      requireNonNull(filter);
      return new Spliterator<T>() {

         @Override
         public boolean tryAdvance(Consumer<? super T> action) {
            return spliterator.tryAdvance(t -> {
               if (filter.test(t)) {
                  action.accept(t);                  
               }
            });
         }

         @Override
         public void forEachRemaining(Consumer<? super T> action) {
            spliterator.forEachRemaining(t -> {
               if (filter.test(t)) {
                  action.accept(t);                  
               }
            });
         }

         @Override
         public Spliterator<T> trySplit() {
            Spliterator<T> child = spliterator.trySplit();
            return child == null ? null : filter(child, filter);
         }

         @Override
         public long estimateSize() {
            return spliterator.estimateSize();
         }

         @Override
         public long getExactSizeIfKnown() {
            return -1;
         }

         @Override
         public int characteristics() {
            return spliterator.characteristics() & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
         }

         @Override
         public Comparator<? super T> getComparator() {
            return spliterator.getComparator();
         }
      };
   }
   
   /**
    * Returns a spliterator that emits the elements of one spliterator concatenated with the
    * elements of another.
    * 
    * @param spliterator1 one spliterator
    * @param spliterator2 another spliterator
    * @return a spliterator that emits the elements from the first spliterator followed by the
    *       elements from the second spliterator
    */
   public static <T> Spliterator<T> concat(Spliterator<? extends T> spliterator1,
         Spliterator<? extends T> spliterator2) {
      requireNonNull(spliterator1);
      requireNonNull(spliterator2);
      return new ConcatSpliterator<>(requireNonNull(spliterator1), requireNonNull(spliterator2));
   }
   
   /**
    * Returns a spliterator that emits the elements of the given spliterators, concatenated all
    * together. This is a convenience method that is equivalent to the following:<pre>
    * {@link #concat(Iterable) concat}(Arrays.asList(spliterators));
    * </pre>
    * 
    * @param spliterators the spliterators
    * @return a spliterator that emits the elements from the first given spliterator, and then those
    *       from the second given spliterator, and so on
    */
   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T> Spliterator<T> concat(
         Spliterator<? extends T>... spliterators) {
      return concat(Arrays.asList(spliterators));
   }

   /**
    * Returns a spliterator that emits the elements of the given spliterators, concatenated all
    * together.
    * 
    * @param spliterators the spliterators
    * @return a spliterator that emits the elements from the first given spliterator, and then those
    *       from the second given spliterator, and so on
    */
   public static <T> Spliterator<T> concat(
         Iterable<? extends Spliterator<? extends T>> spliterators) {
      if (spliterators instanceof Collection) {
         return flatten(
               ((Collection<? extends Spliterator<? extends T>>) spliterators)
                     .spliterator());
      }
      return flatten(spliterators.iterator());
   }

   /**
    * Flattens the given iterator so that all elements from its constituent spliterators are
    * concatenated together.
    * 
    * @param spliterators an iterator that yields spliterators
    * @return a spliterator that emits the concatenation of all elements emitted by spliterators
    *       that are emitted by the given iterator  
    */
   public static <T> Spliterator<T> flatten(
         Iterator<? extends Spliterator<? extends T>> spliterators) {
      return flatten(Spliterators.spliteratorUnknownSize(spliterators, 0));
   }

   /**
    * Flattens the given spliterator so that all elements from its constituent spliterators are
    * concatenated together.
    * 
    * @param spliterators a spliterator that yields spliterators
    * @return a spliterator that emits the concatenation of all elements emitted by spliterators
    *       that are emitted by the given spliterator  
    */
   public static <T> Spliterator<T> flatten(
         Spliterator<? extends Spliterator<? extends T>> nested) {
      return new FlatSpliterator<>(nested);
   }
   
   /**
    * Returns a spliterator that emits the elements from the given spliterator, after using the
    * given function to transform them into primitive {@code int} values.
    * 
    * @param spliterator the spliterator
    * @param fn a function that transforms objects in the given spliterator into primitives
    * @return a spliterator that emits {@code int} values computed by applying the given function
    *       to values emitted by the given iterator
    */
   public static <T> Spliterator.OfInt mapToInt(Spliterator<? extends T> spliterator,
         ToIntFunction<? super T> fn) {
      return new MappedToIntSpliterator<>(requireNonNull(spliterator), requireNonNull(fn));
   }

   /**
    * Returns a spliterator that emits the elements from the given spliterator, after using the
    * given function to transform them into primitive {@code long} values.
    * 
    * @param spliterator the spliterator
    * @param fn a function that transforms objects in the given spliterator into primitives
    * @return a spliterator that emits {@code long} values computed by applying the given function
    *       to values emitted by the given iterator
    */
   public static <T> Spliterator.OfLong mapToLong(Spliterator<? extends T> spliterator,
         ToLongFunction<? super T> fn) {
      return new MappedToLongSpliterator<>(requireNonNull(spliterator), requireNonNull(fn));
   }

   /**
    * Returns a spliterator that emits the elements from the given spliterator, after using the
    * given function to transform them into primitive {@code double} values.
    * 
    * @param spliterator the spliterator
    * @param fn a function that transforms objects in the given spliterator into primitives
    * @return a spliterator that emits {@code double} values computed by applying the given function
    *       to values emitted by the given iterator
    */
   public static <T> Spliterator.OfDouble mapToDouble(Spliterator<? extends T> spliterator,
         ToDoubleFunction<? super T> fn) {
      return new MappedToDoubleSpliterator<>(requireNonNull(spliterator), requireNonNull(fn));
   }

   /**
    * A spliterator that will delegate to a source provided by the given supplier. The returned
    * value is "lazy" in that it will wait as long as possible to invoke the supplier and retrieve
    * the source.
    * 
    * <p>Any method invocation on the returned spliterator will trigger invocation of the given
    * supplier. The supplier will only be invoked once.
    * 
    * @param source a supplier of a spliterator
    * @return a spliterator with the same data and characteristics as the one retrieved from the
    *       first invocation of the given supplier
    */
   public static <T> Spliterator<T> lazySpliterator(
         Supplier<? extends Spliterator<? extends T>> source) {
      requireNonNull(source);
      return new LazySpliterator<>(source);
   }

   /**
    * A spliterator that will delegate to a source provided by the given supplier. The returned
    * value is "lazy" in that it will wait as long as possible to invoke the supplier and retrieve
    * the source.
    * 
    * <p>Any method invocation on the returned spliterator, other than queries for the spliterator's
    * characteristics, will trigger invocation of the given supplier. The supplier will only be
    * invoked once.
    * 
    * @param source a supplier of a spliterator
    * @param characteristics spliterator characteristics
    * @return a spliterator with the given characteristics and the same data as the one retrieved
    *       from the first invocation of the given supplier
    */
   public static <T> Spliterator<T> lazySpliterator(
         Supplier<? extends Spliterator<? extends T>> source, int characteristics) {
      return new LazySpliterator<>(source, characteristics);
   }

   /**
    * A spliterator that will delegate to a source provided by the given supplier. The returned
    * value is "lazy" in that it will wait as long as possible to invoke the supplier and retrieve
    * the source.
    * 
    * <p>Any method invocation on the returned spliterator, other than queries for the spliterator's
    * characteristics or comparator, will trigger invocation of the given supplier. The supplier
    * will only be invoked once.
    * 
    * @param source a supplier of a spliterator
    * @param characteristics spliterator characteristics
    * @param comparator the spliterator's comparator; ignored if the given characteristics do not
    *       include {@link Spliterator#SORTED}
    * @return a spliterator with the given characteristics and comparator and the same data as the
    *       one retrieved from the first invocation of the given supplier
    */
   public static <T> Spliterator<T> lazySpliterator(
         Supplier<? extends Spliterator<? extends T>> source, int characteristics,
         Comparator<? super T> comparator) {
      return new LazySpliterator<>(source, characteristics, comparator);
   }
   
   /**
    * Forks the given source of data into multiple sources. The returned supplier will return a
    * different spliterator, as many as the given number of forks. Each such spliterator will be
    * independent and have the same data as the given spliterator. This allows a single source of
    * data to drive multiple consumers.
    * 
    * <p>For extremely large sources of data, caution must be used to avoid excessive heap usage.
    * Whenever data is requested from a fork, it is retrieved from the given source spliterator (via
    * {@link #tryAdvance(Consumer)}) and then buffered in queues (one per fork) so that all of the
    * other forks can also retrieve the same item. So if the forks are used sequentially, the
    * entirety of the data will end up queued in buffers after the first fork is used, and then
    * flushed as the other forks are used. For limited exposure to this buffering, all consumers of
    * the data should be scheduled to run concurrently. That way queued data is quickly consumed by
    * concurrent consumers.
    * 
    * <p>Attempts to split a fork will result in the underlying spliterator being split if possible
    * (if some other fork has not already split it). But some spliterators cannot split after
    * traversal begins. So it is possible to see interference from concurrent forks caused by one
    * fork starting traversal, inhibiting the ability for a different fork to split. This could
    * result in reduced parallelism.
    * 
    * <p>Typical usage will involve all forks being used similarly (same or similar number of splits
    * in a recursive task, before any traversal starts), which should result in similar enough
    * access patterns across forks that interference won't be problematic.
    * 
    * <p>If an attempt is made to retrieve more spliterators from the returned supplier than the
    * requested number of forks, the supplier will throw {@link IllegalStateException}.
    * 
    * @param source the spliterator that is the source of data for all forks
    * @param numberOfForks the number of forks that can be retrieved from the returned supplier
    * @return a supplier of the forks; each invocation of the supplier retrieves one of the
    *       requested forks
    */
   public static <T> Supplier<Spliterator<T>> fork(Spliterator<? extends T> source,
         int numberOfForks) {
      if (numberOfForks < 2) {
         throw new IllegalArgumentException("number of forks must be >= 1");
      }
      return new ForkingSpliterator<>(requireNonNull(source), numberOfForks);
   }

   /**
    * A spliterator that yields the result of concatenating two given spliterators.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of values emitted by the spliterator
    */
   private static class ConcatSpliterator<T> implements Spliterator<T> {
      private final Spliterator<? extends T> s1;
      private Spliterator<? extends T> s2;
      private boolean traversingSecond;
      
      ConcatSpliterator(Spliterator<? extends T> s1, Spliterator<? extends T> s2) {
         this.s1 = s1;
         this.s2 = s2;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         if (traversingSecond) {
            // consuming the second spliterator
            return s2.tryAdvance(action);
         } else {
            // still on the first one
            if (s1.tryAdvance(action)) {
               return true;
            }
            // first one exhausted, move on to second one
            if (s2 == null) {
               return false;
            }
            traversingSecond = true;
            return s2.tryAdvance(action);
         }
      }

      @Override
      public Spliterator<T> trySplit() {
         if (!traversingSecond) {
            traversingSecond = true;
            return cast(s1);
         }
         return cast(s2.trySplit());
      }

      @Override
      public long estimateSize() {
         long sz = s2.estimateSize();
         if (!traversingSecond) {
            sz += s1.estimateSize();
         }
         return sz;
      }

      @Override
      public int characteristics() {
         int ch = s2.characteristics();
         if (!traversingSecond) {
            ch &= s1.characteristics();
         }
         return ch & ~(Spliterator.SORTED | Spliterator.DISTINCT);
      }
   }
   
   /**
    * A spliterator that takes a spliterator of spliterators and "flattens" the result, yielding
    * the elements emitted by those spliterators.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element emitted by the spliterator
    */
   private static class FlatSpliterator<T>
   implements Spliterator<T>, Consumer<Spliterator<? extends T>> {
      private final Spliterator<? extends Spliterator<? extends T>> splits;
      private Spliterator<? extends T> current;
      
      FlatSpliterator(Spliterator<? extends Spliterator<? extends T>> splits) {
         this.splits = splits;
      }

      @Override
      public void accept(Spliterator<? extends T> t) {
         current = t;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         while (true) {
            if (current == null) {
               if (!splits.tryAdvance(this)) {
                  return false;
               }
            }
            if (current.tryAdvance(action)) {
               return true;
            }
            current = null;
         }
      }

      @Override
      public Spliterator<T> trySplit() {
         Spliterator<? extends Spliterator<? extends T>> other = splits.trySplit();
         if (other != null) {
            FlatSpliterator<T> ret = new FlatSpliterator<>(other);
            ret.current = current;
            current = null;
            return ret;
         }
         if (current == null) {
            if (!splits.tryAdvance(this)) {
               return null;
            }
         }
         return cast(current.trySplit());
      }

      @Override
      public long estimateSize() {
         long sz = splits.estimateSize();
         if (current != null) {
            sz += current.estimateSize() - 1;
         }
         return sz;
      }

      @Override
      public int characteristics() {
         return 0;
      }
   }
   
   /**
    * A spliterator that maps elements emitted by one spliterator into primitive {@code int}
    * values.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of elements being mapped to {@code int}
    */
   private static class MappedToIntSpliterator<T> implements Spliterator.OfInt {
      private final Spliterator<T> source;
      private final ToIntFunction<? super T> fn;
      
      public MappedToIntSpliterator(Spliterator<T> source, ToIntFunction<? super T> fn) {
         this.source = source;
         this.fn = fn;
      }

      @Override
      public boolean tryAdvance(Consumer<? super Integer> action) {
         return source.tryAdvance(t -> action.accept(fn.applyAsInt(t)));
      }

      @Override
      public long estimateSize() {
         return source.estimateSize();
      }

      @Override
      public int characteristics() {
         return source.characteristics();
      }

      @Override
      public Spliterator.OfInt trySplit() {
         Spliterator<T> split = source.trySplit();
         return split == null ? null : new MappedToIntSpliterator<>(split, fn);
      }

      @Override
      public boolean tryAdvance(IntConsumer action) {
         return source.tryAdvance(t -> action.accept(fn.applyAsInt(t)));
      }
   }
   
   /**
    * A spliterator that maps elements emitted by one spliterator into primitive {@code long}
    * values.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of elements being mapped to {@code long}
    */
   private static class MappedToLongSpliterator<T> implements Spliterator.OfLong {
      private final Spliterator<T> source;
      private final ToLongFunction<? super T> fn;
      
      public MappedToLongSpliterator(Spliterator<T> source, ToLongFunction<? super T> fn) {
         this.source = source;
         this.fn = fn;
      }

      @Override
      public boolean tryAdvance(Consumer<? super Long> action) {
         return source.tryAdvance(t -> action.accept(fn.applyAsLong(t)));
      }

      @Override
      public long estimateSize() {
         return source.estimateSize();
      }

      @Override
      public int characteristics() {
         return source.characteristics();
      }

      @Override
      public Spliterator.OfLong trySplit() {
         Spliterator<T> split = source.trySplit();
         return split == null ? null : new MappedToLongSpliterator<>(split, fn);
      }

      @Override
      public boolean tryAdvance(LongConsumer action) {
         return source.tryAdvance(t -> action.accept(fn.applyAsLong(t)));
      }
   }
   
   /**
    * A spliterator that maps elements emitted by one spliterator into primitive {@code double}
    * values.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of elements being mapped to {@code double}
    */
   private static class MappedToDoubleSpliterator<T> implements Spliterator.OfDouble {
      private final Spliterator<T> source;
      private final ToDoubleFunction<? super T> fn;
      
      public MappedToDoubleSpliterator(Spliterator<T> source, ToDoubleFunction<? super T> fn) {
         this.source = source;
         this.fn = fn;
      }

      @Override
      public boolean tryAdvance(Consumer<? super Double> action) {
         return source.tryAdvance(t -> action.accept(fn.applyAsDouble(t)));
      }

      @Override
      public long estimateSize() {
         return source.estimateSize();
      }

      @Override
      public int characteristics() {
         return source.characteristics();
      }

      @Override
      public Spliterator.OfDouble trySplit() {
         Spliterator<T> split = source.trySplit();
         return split == null ? null : new MappedToDoubleSpliterator<>(split, fn);
      }

      @Override
      public boolean tryAdvance(DoubleConsumer action) {
         return source.tryAdvance(t -> action.accept(fn.applyAsDouble(t)));
      }
   }
   
   /**
    * A spliterator that defers retrieval of an underlying source as long as possible. Only when
    * the underlying source is necessary to service a method invocation will the source be
    * retrieved from a given supplier.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of data emitted by the spliterator
    */
   private static class LazySpliterator<T> implements Spliterator<T> {
      private final Supplier<? extends Spliterator<? extends T>> delegate;
      private final boolean hasCharacteristics;
      private final int characteristics;
      private final boolean hasComparator;
      private final Comparator<? super T> comparator;
      
      LazySpliterator(Supplier<? extends Spliterator<? extends T>> delegate) {
         this.delegate = Suppliers.memoize(delegate);
         hasCharacteristics = false;
         characteristics = 0;
         hasComparator = false;
         comparator = null;
      }

      LazySpliterator(Supplier<? extends Spliterator<? extends T>> delegate, int characteristics) {
         this.delegate = Suppliers.memoize(delegate);
         hasCharacteristics = true;
         this.characteristics = characteristics;
         hasComparator = false;
         comparator = null;
      }
      
      LazySpliterator(Supplier<? extends Spliterator<? extends T>> delegate, int characteristics,
            Comparator<? super T> comparator) {
         this.delegate = Suppliers.memoize(delegate);
         hasCharacteristics = true;
         this.characteristics = characteristics;
         hasComparator = true;
         this.comparator = comparator;
      }

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
         return delegate.get().tryAdvance(action);
      }

      @Override
      public Spliterator<T> trySplit() {
         return cast(delegate.get().trySplit());
      }

      @Override
      public Comparator<? super T> getComparator() {
         if (!hasCharacteristics(Spliterator.SORTED)) {
            throw new IllegalStateException();
         }
         if (hasComparator) {
            return comparator == CollectionUtils.naturalOrder() ? null : comparator;
         }
         @SuppressWarnings("unchecked")
         Comparator<? super T> c = (Comparator<? super T>) delegate.get().getComparator();
         return c;
      }
      
      @Override
      public long estimateSize() {
         return delegate.get().estimateSize();
      }

      @Override
      public int characteristics() {
         return hasCharacteristics
               ? characteristics
               : delegate.get().characteristics();
      }
   }
   
   /**
    * A spliterator that forks its output to multiple destinations. The number of destinations must
    * be defined up front. This class wraps a spliterator and is a factory for the forks, which are
    * themselves spliterators.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of data provided by the spliterator
    * 
    * @see MoreSpliterators#fork(Spliterator, int)
    */
   static class ForkingSpliterator<T> implements Supplier<Spliterator<T>> {
      
      private static final Object TERMINATE = new Object();
      private static final Object NULL = new Object();

      private final ReentrantLock lock = new ReentrantLock();
      private final Spliterator<? extends T> source;
      private final Queue<Object>[] buffers;
      
      /* non-final fields below are only accessed while lock is held */
      
      private int forkCount;
      private boolean traversalStarted;
      
      // tree of splits
      SplitNode<T> parent;
      
      // doubly-linked list of leaves, to navigate backwards to head of sub-tree when we commence
      // traversal and to then move forward to tail of sub-tree as traversal proceeds
      ForkingSpliterator<T> previous;
      ForkingSpliterator<T> next;

      ForkingSpliterator(Spliterator<? extends T> source, int numberOfForks) {
         this(source, numberOfForks, null);
      }

      @SuppressWarnings("unchecked") // generic array creation
      ForkingSpliterator(Spliterator<? extends T> source, int numberOfForks, SplitNode<T> parent) {
         this.source = source;
         this.buffers = (Queue<Object>[]) new Queue<?>[numberOfForks];
         for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new ConcurrentLinkedQueue<>();
         }
         this.forkCount = numberOfForks;
         this.parent = parent;
      }
      
      ForkingSpliterator(Spliterator<? extends T> source, int numberOfForks, SplitNode<T> parent,
            ForkingSpliterator<T> previous, ForkingSpliterator<T> next) {
         this(source, numberOfForks, parent);
         this.previous = previous;
         this.next = next;
      }
      
      @Override
      public Spliterator<T> get() {
         lock.lock();
         try {
            if (forkCount == 0) {
               throw new IllegalStateException("already supplied all forks");
            }
            forkCount--;
            return createFork(forkCount, null);
         } finally {
            lock.unlock();
         }
      }
      
      Fork<T> createFork(int index, SplitNode<T> parent) {
         return new Fork<>(index, this, parent);
      }
      
      Spliterator<T> trySplit(Fork<T> fork) {
         lock.lock();
         try {
            SplitNode<T> node = findSplit(fork.parent);
            if (node != null) {
               Fork<T> ret = node.split.createFork(fork.bufferIndex, node);
               fork.parent = node;
               return ret;
            }
            if (traversalStarted) {
               return null;
            }
            Spliterator<? extends T> split = source.trySplit();
            if (split == null) {
               return null;
            }
            // move this node down a level in the tree
            ForkingSpliterator<T> f =
                  new ForkingSpliterator<>(split, buffers.length, null, previous, this);
            fork.parent = parent = new SplitNode<>(parent, f);
            f.parent = parent;
            Fork<T> ret = f.createFork(fork.bufferIndex, parent);
            if (previous != null) {
               previous.lock.lock();
               try {
                  previous.next = f;
               } finally {
                  previous.lock.unlock();
               }
            }
            previous = f;
            return ret;
         } finally {
            lock.unlock();
         }
      }
      
      SplitNode<T> findSplit(SplitNode<T> expectedParent) {
         assert lock.isHeldByCurrentThread();
         if (parent == expectedParent) {
            return null;
         }
         // another fork already split, so we need to find it in the tree
         for (SplitNode<T> n = parent; ; n = n.parent) {
            if (n.split == this) {
               return null;
            }
            if (n.parent == expectedParent) {
               return n;
            }
         }
      }
      
      ForkingSpliterator<T> findHead(Fork<T> fork) {
         ReentrantLock l = lock;
         l.lock();
         try {
            if (fork.parent == parent || previous == null) {
               // already have the right split
               traversalStarted = true;
               return this;
            }
            ForkingSpliterator<T>[] boundaries = boundaries(fork.parent);
            if (contains(boundaries, previous)) {
               // at the limit, nowhere else to go
               traversalStarted = true;
               return this;
            }
            ForkingSpliterator<T> head = null;
            for (ForkingSpliterator<T> curr = previous; curr != null; curr = curr.previous) {
               // hand-over-hand locking as we walk backwards through list of siblings
               ReentrantLock prev = l;
               curr.lock.lock();
               l = curr.lock;
               prev.unlock();
               
               head = curr;
               if (contains(boundaries, curr.previous)) {
                  // reached the limit for our sub-tree
                  break;
               }
            }
            assert l == head.lock;
            head.traversalStarted = true;
            return head;
         } finally {
            l.unlock();
         }
      }
      
      ForkingSpliterator<T>[] boundaries(SplitNode<T> p) {
         if (p == null) {
            return null;
         }
         // TODO(jh): dynamically sub in a Set instead of array if depth is high
         @SuppressWarnings("unchecked") // generic array creation
         ForkingSpliterator<T>[] ret =
               (ForkingSpliterator<T>[]) new ForkingSpliterator<?>[parent.depth];
         int i = 0;
         while (p != null) {
            ret[i++] = p.split;
            p = p.parent;
         }
         return ret;
      }
      
      boolean contains(ForkingSpliterator<T>[] array, ForkingSpliterator<T> item) {
         if (array == null) {
            return false;
         }
         for (ForkingSpliterator<T> a : array) {
            if (a == item) {
               return true;
            }
         }
         return false;
      }
      
      boolean tryAdvance(Fork<T> fork, Consumer<? super T> action) {
         Object o = buffers[fork.bufferIndex].poll();
         if (o == null) {
            lock.lock();
            try {
               assert traversalStarted;
               // double-check now that we have lock
               o = buffers[fork.bufferIndex].poll();
               if (o == null) {
                  if (!source.tryAdvance(t -> publish(t == null ? NULL : t))) {
                     publish(TERMINATE);
                  }
                  o = buffers[fork.bufferIndex].poll();
                  assert o != null;
               }
            } finally {
               lock.unlock();
            }
         }
         if (o == TERMINATE) {
            // try to find sibling split for this fork
            ForkingSpliterator<T> n;
            lock.lock();
            try {
               if (fork.parent == parent || next == null) {
                  return false;
               }
               SplitNode<T> p = fork.parent;
               // see if we're already at the end of the sub-tree
               while (p != null) {
                  if (p.split == this) {
                     // yep, we're done
                     return false;
                  }
                  p = p.parent;
               }
               n = next;
            } finally {
               lock.unlock();
            }

            // Haven't reached limit of sub-tree; safe to proceed. We need to mark the next
            // node such that traversal has started (to prevent concurrent split that could
            // cause our traversal to miss items). But we can't acquire lock without first
            // releasing the one we hold because we could deadlock with a call to findHead
            // (which does hand-over-hand locking backwards: always following previous links).
            // Note that a concurrent split could happen between the time we release this lock
            // and the time we acquire the next. So before assuming we have the right node, we
            // must check to see if we need to go back to a better starting point (using same
            // hand-over-hand locking), just in case.
            fork.current = n.findStart(this);
            return false;
         }
         @SuppressWarnings("unchecked")
         T t = o == NULL ? null : (T) o;
         action.accept(t);
         return true;
      }
      
      ForkingSpliterator<T> findStart(ForkingSpliterator<T> boundary) {
         ReentrantLock l = lock;
         l.lock();
         try {
            if (previous == boundary) {
               traversalStarted = true;
               return this;
            }
            for (ForkingSpliterator<T> curr = previous; curr != null; curr = curr.previous) {
               // hand-over-hand locking as we walk backwards through list of siblings
               ReentrantLock prev = l;
               curr.lock.lock();
               l = curr.lock;
               prev.unlock();
               
               if (curr.previous == boundary) {
                  curr.traversalStarted = true;
                  return curr;
               }
            }
            throw new AssertionError(
                  "Failed to find successor to " + boundary + ", starting at " + this);
         } finally {
            l.unlock();
         }
      }
      
      void publish(Object item) {
         assert lock.isHeldByCurrentThread();
         for (Queue<Object> q : buffers) {
            q.add(item);
         }
      }

      /**
       * A node in a tree of splits. The tree is inverted, with nodes having references up (parent)
       * and to the right (sibling) vs. the traditional references downward (children). The leaves
       * of the tree are instances of {@link ForkingSpliterator}, not other {@link SplitNode}s.
       * 
       * <p>When a spliterator splits, that leaf moves down a level in the tree, which is realized
       * by adding a new parent node:
       * <p><strong>Before</strong>
       * <pre>
       *  null
       *   ^
       *   |
       * split0
       * </pre>
       * <p><strong>After</strong>
       * <pre>
       *       null
       *        ^
       *        |
       *      node0
       *     /    \
       *    /      \
       * split0    split1
       * </pre>
       * <p>If we were to recursively split each of these once more, to end up splitting the data
       * into fourths, we'd have the following tree:
       * <pre>
       *                  null
       *                   ^
       *                   |
       *              -> node0 <-
       *             /            \
       *            /              \
       *       node1                node2
       *       /   \                /   \ 
       *      /     \              /     \
       * split0     split2    split1     split3
       * </pre>
       * <p>Individual forks track their current spliterator and its expected parent. If another
       * fork has already split the spliterator, then the spliterator's parent node will differ from
       * what the fork expects. In this way, the fork knows its expected and actual location in the
       * tree. It navigates upwards to find splits that another fork has already concurrently
       * performed. If a concurrent fork performed more splits than we want, we can "unsplit" by
       * merging adjacent siblings. (Merging involves traversing a linked list of leaf spliterators,
       * examining up the tree to ensure traversal doesn't continue beyond the scope of the fork's
       * expected split.)
       * 
       * <p>When {@link Spliterator#trySplit()} is called and the spliterator's characteristics
       * include {@linkplain Spliterator#ORDERED ordered}, the returned spliterator is a prefix of
       * the data, and the existing spliterator then represents only the subsequent elements. This
       * means that when traversal of an ordered data set commences, we must first navigate
       * <em>backwards</em> through the linked list of leaves to find head of the list that
       * represents the fork's current sub-tree.
       * 
       * <p>For example, using the above diagram, for the sub-tree rooted at {@code null}, the expected
       * iteration order is {@code split3}, {@code split1}, {@code split2}, and finally
       * {@code split0}. For the sub-tree rooted at {@code node0}, the iteration order is
       * {@code split2} and then {@code split0}.
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       *
       * @param <T> the type of data emitted by the spliterators
       */
      static class SplitNode<T> {
         final SplitNode<T> parent;
         final ForkingSpliterator<T> split;
         final int depth;
         
         SplitNode(SplitNode<T> parent, ForkingSpliterator<T> split) {
            this.parent = parent;
            this.split = split;
            this.depth = parent == null ? 1 : parent.depth + 1;
         }
      }
      
      /**
       * A single fork of a forked spliterator.
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       *
       * @param <T> the type of data emitted by the spliterator
       */
      static class Fork<T> implements Spliterator<T> {
         final int bufferIndex;
         ForkingSpliterator<T> current;
         SplitNode<T> parent;
         boolean traversalStarted;
         
         Fork(int bufferIndex, ForkingSpliterator<T> current, SplitNode<T> parent) {
            this.bufferIndex = bufferIndex;
            this.current = current;
            this.parent = parent;
         }

         @Override
         public boolean tryAdvance(Consumer<? super T> action) {
            if (!traversalStarted) {
               traversalStarted = true;
               // navigate to head of current sub-tree
               current = current.findHead(this);
            }
            while (true) {
               ForkingSpliterator<T> f = current;
               if (current.tryAdvance(this, action)) {
                  return true;
               }
               if (f == current) {
                  // current split didn't change; we're done
                  return false;
               }
               // we've advanced to the next split in the sub-tree, so try again with that one
            }
         }

         @Override
         public Spliterator<T> trySplit() {
            return current.trySplit(this);
         }

         @Override
         public long estimateSize() {
            return current.source.estimateSize();
         }

         @Override
         public int characteristics() {
            return current.source.characteristics();
         }
         
         @Override
         @SuppressWarnings("unchecked")
         public Comparator<? super T> getComparator() {
            return (Comparator<? super T>) current.source.getComparator();
         }
      }
   }
}
