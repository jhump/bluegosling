package com.bluegosling.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bluegosling.collections.views.TransformingIterator;
import com.bluegosling.function.TriFunction;

/**
 * Utility methods for working with and creating instances of {@link Stream}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class MoreStreams {
   private MoreStreams() {}

   /**
    * Combines the given two streams into one by "zipping" them together. The first item from each
    * stream is combined using the given function to produce the first item of the combined stream.
    * The same goes for the second item and so on. When either stream is exhausted, the combined
    * stream is also exhausted. So if one stream has more items than the other, the extra items in
    * the longer stream are ignored.
    * 
    * @param s1 the first stream
    * @param s2 the second stream
    * @param fn the function used to combine the items from each stream into one
    * @return a new stream that emits the result of "zipping" together items from the given streams
    */
   public static <T, U, V> Stream<V> zip(Stream<? extends T> s1, Stream<? extends U> s2,
         BiFunction<? super T, ? super U, ? extends V> fn) {
      Spliterator<? extends T> sp1 = s1.spliterator();
      Spliterator<? extends U> sp2 = s2.spliterator();
      int characteristics = (sp1.characteristics() & sp2.characteristics()) | Spliterator.NONNULL;
      Iterator<V> iter =
            MoreIterators.zip(Spliterators.iterator(sp1), Spliterators.iterator(sp2), fn);
      Spliterator<V> spliter;
      if ((characteristics & Spliterator.SIZED) != 0) {
         spliter = Spliterators.spliterator(iter, Math.min(sp1.estimateSize(), sp2.estimateSize()),
               characteristics);
      } else {
         spliter = Spliterators.spliteratorUnknownSize(iter, characteristics);
      }
      return StreamSupport.stream(spliter, false);
   }
   
   /**
    * Combines the given three streams into one by "zipping" them together. The first item from each
    * stream is combined using the given function to produce the first item of the combined stream.
    * The same goes for the second item and so on. When any stream is exhausted, the combined
    * stream is also exhausted. So if any stream has more items than the shortest, the extra items
    * in the longer stream(s) are ignored.
    * 
    * @param s1 the first stream
    * @param s2 the second stream
    * @param s3 the third stream
    * @param fn the function used to combine the items from each stream into one
    * @return a new stream that emits the result of "zipping" together items from the given streams
    */
   public static <T, U, V, W> Stream<W> zip(Stream<? extends T> s1, Stream<? extends U> s2,
         Stream<? extends V> s3, TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      Spliterator<? extends T> sp1 = s1.spliterator();
      Spliterator<? extends U> sp2 = s2.spliterator();
      Spliterator<? extends V> sp3 = s3.spliterator();
      int characteristics = (sp1.characteristics() & sp2.characteristics() & sp3.characteristics())
            | Spliterator.NONNULL;
      Iterator<W> iter = MoreIterators.zip(Spliterators.iterator(sp1), Spliterators.iterator(sp2),
            Spliterators.iterator(sp3), fn);
      Spliterator<W> spliter;
      if ((characteristics & Spliterator.SIZED) != 0) {
         spliter = Spliterators.spliterator(iter,
               Math.min(Math.min(sp1.estimateSize(), sp2.estimateSize()), sp3.estimateSize()),
               characteristics);
      } else {
         spliter = Spliterators.spliteratorUnknownSize(iter, characteristics);
      }
      return StreamSupport.stream(spliter, false);
   }
   
   /**
    * Combines the given array of streams into one by "zipping" them together. The first item is
    * taken from each stream (in the given order) and put into a list. The first item of the
    * combined stream is that list. The same is done for the second item and so on. When any stream
    * is exhausted, the combined stream is also exhausted. So if any stream has more items than the
    * shortest, the extra items in the longer stream(s) are ignored.
    * 
    * <p>If the given array has no streams (e.g. length is zero), the returned stream is empty.
    * 
    * @param streams the streams to be combined
    * @return a new stream that emits the result of "zipping" together items from the given streams
    *       into lists
    */
   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T> Stream<List<T>> zip(Stream<? extends T>... streams) {
      return zip(Arrays.asList(streams));
   }

   /**
    * Combines the given streams into one by "zipping" them together. The first item is taken from 
    * each stream (in the iteration order of the given iterable) and put into a list. The first item
    * of the combined stream is that list. The same is done for the second item and so on. When any
    * stream is exhausted, the combined stream is also exhausted. So if any stream has more items
    * than the shortest, the extra items in the longer stream(s) are ignored.
    * 
    * <p>If the given iterable is empty, the returned stream is empty.
    * 
    * @param streams the streams to be combined
    * @return a new stream that emits the result of "zipping" together items from the given streams
    *       into lists
    */
   public static <T> Stream<List<T>> zip(Iterable<? extends Stream<? extends T>> streams) {
      int countHint = MoreIterables.trySize(streams).orElse(8);
      List<Spliterator<? extends T>> spliters = new ArrayList<>(countHint);
      for (Stream<? extends T> st : streams) {
         spliters.add(st.spliterator());
      }
      if (spliters.isEmpty()) {
         return Stream.empty();
      }
      int characteristics = spliters.get(0).characteristics();
      for (int i = 1; i < spliters.size(); i++) {
         characteristics &= spliters.get(i).characteristics();
      }
      characteristics |= Spliterator.NONNULL;
      Iterator<List<T>> iter = MoreIterators.zip(new TransformingIterator<>(spliters.iterator(),
            Spliterators::iterator), spliters.size());
      Spliterator<List<T>> spliter;
      if ((characteristics & Spliterator.SIZED) != 0) {
         long min = spliters.get(0).estimateSize();
         for (int i = 1; i < spliters.size(); i++) {
            long sz = spliters.get(i).estimateSize();
            if (sz < min) {
               min = sz;
            }
         }
         spliter = Spliterators.spliterator(iter, min, characteristics);
      } else {
         spliter = Spliterators.spliteratorUnknownSize(iter, characteristics);
      }
      return StreamSupport.stream(spliter, false);
   }
}
