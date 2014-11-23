package com.apriori.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;

/**
 * A simple growable array that is backed by an {@link ArrayList}. This implementation basically
 * doubles the underlying array capacity when it gets full. This implementation does not
 * automatically shrink the underlying array, instead requiring callers to use {@link #trimToSize()}
 * to reduce the object's memory footprint.
 * 
 * <p>Because of the array-doubling technique and the fact that memory must be explicitly reclaimed
 * after shrinking, this implementation can waste O(n) memory. Also, the array-doubling requires
 * allocation of and then copying into a new (potentially very large) array. So, although amortized
 * runtime complexity for growing the array by one is constant, the worst case is linear when it
 * must perform a doubling.
 * 
 * <p>Despite these disadvantages, constant factors for random access operations are very low, and
 * the use of a single underlying array is very cache-friendly. Also, the average case order of
 * wasted memory is often much lower than the worst case. So this makes it a very high-performing
 * implementation under many conditions.
 *
 * @param <T> the type of element in the array
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: consider replacing this with ArrayDoublingGrowableArray and manage the array directly
// instead of using an ArrayList to manage the underlying array
// TODO: tests
public class ArrayListGrowableArray<T> implements GrowableArray<T>, Serializable, Cloneable {

   private static final long serialVersionUID = 7834555062989870534L;
   
   /**
    * An {@link ArrayList} that lets the enclosing class directly invoke
    * {@link #removeRange(int, int)}.
    *
    * @param <T> the type of element in the list
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class InternalArrayList<T> extends ArrayList<T> {

      private static final long serialVersionUID = 6264804794546117868L;
      
      InternalArrayList() {
      }

      InternalArrayList(Collection<? extends T> coll) {
         super(coll);
      }

      // overridden just to make this method visible to the enclosing class
      @Override
      protected void removeRange(int fromIndex, int toIndex) {
         super.removeRange(fromIndex, toIndex);
      }
   }
   
   private InternalArrayList<T> list;
   
   /**
    * Constructs a new, empty growable array.
    */
   public ArrayListGrowableArray() {
      list = new InternalArrayList<>();
   }

   /**
    * Constructs a new growable array with the given size. All elements will initially be
    * {@code null}.
    *
    * @param initialSize the size of the new array
    */
   public ArrayListGrowableArray(int initialSize) {
      list = new InternalArrayList<>(Collections.nCopies(initialSize, null));
   }
   
   /**
    * Constructs a new growable array with the given contents. The new array will have the same
    * size as the given values. The order of values in the array is the same as the iteration
    * order of the given values.
    *
    * @param values a collection or sequence of values
    */
   public ArrayListGrowableArray(Iterable<? extends T> values) {
      if (values instanceof Collection) {
         list = new InternalArrayList<>((Collection<? extends T>) values);
      } else {
         list = new InternalArrayList<>();
         for (T t : values) {
            list.add(t);
         }
      }
   }
   
   @SuppressWarnings("unchecked")
   @Override
   public ArrayListGrowableArray<T> clone() {
      if (getClass().equals(ArrayListGrowableArray.class)) {
         return new ArrayListGrowableArray<T>(list);
      } else {
         try {
            ArrayListGrowableArray<T> clone = (ArrayListGrowableArray<T>) super.clone();
            clone.list = (InternalArrayList<T>) this.list.clone();
            return clone;
         } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
         }
      }
   }
   
   @Override
   public Iterator<T> iterator() {
      // we don't directly return the list's iterator because growable array iterators
      // aren't supposed to support remove()
      final Iterator<T> iter = list.iterator();
      return new Iterator<T>() {
         @Override
         public boolean hasNext() {
            return iter.hasNext();
         }

         @Override
         public T next() {
            return iter.next();
         }
      };
   }

   @Override
   public Spliterator<T> spliterator() {
      return list.spliterator();
   }

   @Override
   public int size() {
      return list.size();
   }
   
   @Override
   public boolean isEmpty() {
      return list.isEmpty();
   }
   
   @Override
   public void clear() {
      list.clear();
   }

   @Override
   public T get(int index) {
      return list.get(index);
   }

   @Override
   public void set(int index, T value) {
      list.set(index, value);
   }

   @Override
   public void growBy(int numNewElements) {
      list.addAll(Collections.nCopies(numNewElements, null));
   }

   @Override
   public void shrinkBy(int numRemovedElements) {
      int sz = list.size();
      if (numRemovedElements < 0 || numRemovedElements > sz) {
         throw new IllegalArgumentException();
      }
      list.removeRange(sz - numRemovedElements, sz);
   }
   
   @Override
   public void push(T t) {
      list.add(t);
   }
   
   @Override
   public void pushAll(Iterable<? extends T> values) {
      if (values instanceof Collection) {
         list.addAll((Collection<? extends T>) values);
      } else {
         for (T t : values) {
            list.add(t);
         }
      }
   }
   
   @Override
   public T pop() {
      return list.remove(list.size() - 1);
   }
   
   /**
    * Trims the underlying array to be the same length as the size of this growable array. This
    * reduces wasted memory at the expense of (potentially) having to create a new, smaller array
    * first (into which the contents are copied).
    */
   public void trimToSize() {
      list.trimToSize();
   }
}
