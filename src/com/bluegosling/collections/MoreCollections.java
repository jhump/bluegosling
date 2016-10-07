package com.bluegosling.collections;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import com.bluegosling.collections.views.TransformingIterator;
import com.bluegosling.function.TriFunction;

public final class MoreCollections {
   private MoreCollections() {}

   public static <T, U, V> Collection<V> zip(Collection<? extends T> c1, Collection<? extends U> c2,
         BiFunction<? super T, ? super U, ? extends V> fn) {
      return new AbstractCollection<V>() {
         @Override
         public Iterator<V> iterator() {
            return MoreIterators.zip(c1.iterator(), c2.iterator(), fn);
         }

         @Override
         public int size() {
            return Math.min(c1.size(), c2.size());
         }
      };
   }

   public static <T, U, V> Collection<V> zipExact(Collection<? extends T> c1,
         Collection<? extends U> c2, BiFunction<? super T, ? super U, ? extends V> fn) {
      checkArgument(c1.size() == c2.size(), "Collections to be zipped must have the same size");
      return zip(c1, c2, fn);
   }

   public static <T, U, V, W> Collection<W> zip(Collection<? extends T> c1,
         Collection<? extends U> c2, Collection<? extends V> c3,
         TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      return new AbstractCollection<W>() {
         @Override
         public Iterator<W> iterator() {
            return MoreIterators.zip(c1.iterator(), c2.iterator(), c3.iterator(), fn);
         }

         @Override
         public int size() {
            return Math.min(Math.min(c1.size(), c2.size()), c3.size());
         }
      };
   }

   public static <T, U, V, W> Collection<W> zipExact(Collection<? extends T> c1,
         Collection<? extends U> c2, Collection<? extends V> c3,
         TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      int sz = c1.size();
      checkArgument(c2.size() == sz && c3.size() == sz,
            "Collections to be zipped must have the same size");
      return zip(c1, c2, c3, fn);
   }
   
   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T> Collection<List<T>> zip(Collection<? extends T>... colls) {
      return zip(Arrays.asList(colls));
   }

   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T> Collection<List<T>> zipExact(Collection<? extends T>... colls) {
      if (colls.length == 0) {
         return Collections.emptyList();
      }
      int sz = colls[0].size();
      for (int i = 1; i < colls.length; i++) {
         checkArgument(colls[i].size() == sz, "Collections to be zipped must have the same size");
      }
      return zip(colls);
   }
   
   public static <T> Collection<List<T>> zip(Iterable<? extends Collection<? extends T>> colls) {
      return new AbstractCollection<List<T>>() {
         @Override
         public Iterator<List<T>> iterator() {
            Iterator<? extends Collection<? extends T>> i = colls.iterator();
            return MoreIterators.zip(new TransformingIterator<>(i,
                  // javac gives raw type warning if using method reference:
                  c -> c.iterator()),
                  MoreIterables.trySize(colls).orElse(8));
         }

         @Override
         public int size() {
            Iterator<? extends Collection<? extends T>> iter = colls.iterator();
            if (!iter.hasNext()) {
               return 0;
            }
            int min = iter.next().size();
            while (iter.hasNext()) {
               int sz = iter.next().size();
               if (sz < min) {
                  min = sz;
               }
            }
            return min;
         }
      };
   }

   public static <T> Collection<List<T>> zipExact(
         Iterable<? extends Collection<? extends T>> colls) {
      Iterator<? extends Collection<? extends T>> iter = colls.iterator();
      if (!iter.hasNext()) {
         return Collections.emptyList();
      }
      int sz = iter.next().size();
      while (iter.hasNext()) {
         checkArgument(iter.next().size() == sz,
               "Collections to be zipped must have the same size");
      }
      return zip(colls);
   }
}
