package com.bluegosling.collections.immutable;

import static com.bluegosling.collections.immutable.Immutables.cast;

import com.bluegosling.collections.MoreIterables;
import com.bluegosling.collections.MoreIterators;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// TODO: javadoc
// TODO: tests
// TODO: serialization
public class LinkedPersistentList<E> extends AbstractLinkedImmutableList<E>
implements PersistentList<E>, Serializable {
   /*
    * Since Java does not do tail-call optimization for recursive functions, these methods use
    * iteration (to ensure we don't overflow the stack) even though recursion would be more elegant
    * for nearly all of them. C'est la vie :(
    * Some of the recursive functions wouldn't be tail-recursion anyways. For those cases we use
    * iteration and first put elements into a heap-allocated stack (vs. on "the stack" as happens
    * with recursion).
    */

   private static final long serialVersionUID = -4079904855716212892L;

   public static <E> LinkedPersistentList<E> create() {
      return EmptyList.instance();
   }
   
   public static <E> LinkedPersistentList<E> create(Iterable<? extends E> iterable) {
      // recursion would be more elegant, but a large list would blow the stack
      if (iterable instanceof LinkedPersistentList) {
         LinkedPersistentList<E> list = cast((LinkedPersistentList<? extends E>) iterable);
         return list;
      }
      Iterator<? extends E> iterator = MoreIterables.reversed(iterable);
      LinkedPersistentList<E> node = null;
      while (iterator.hasNext()) {
         node = new LinkedPersistentList<>(iterator.next(), node);
      }
      return node != null ? node : EmptyList.instance(); 
   }
   
   private final E value;
   private final LinkedPersistentList<E> next;
   private final int size;
   
   private LinkedPersistentList(E value, LinkedPersistentList<E> next) {
      this.value = value;
      this.next = next;
      this.size = next == null ? 1 : next.size + 1;
   }

   @Override
   public E first() {
      return value;
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public LinkedPersistentList<E> with(int i, E e) {
      rangeCheckWide(i);
      ArrayList<E> nodes = new ArrayList<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         nodes.add(current.value);
         current = current.next;
      }
      LinkedPersistentList<E> ret = new LinkedPersistentList<E>(e, current);
      while (!nodes.isEmpty()) {
         ret = new LinkedPersistentList<E>(nodes.remove(nodes.size() - 1), ret);
      }
      return ret;
   }

   @Override
   public LinkedPersistentList<E> withAll(int i, Iterable<? extends E> items) {
      if (i == 0) {
         return withAll(items);
      }
      rangeCheckWide(i);
      Iterator<? extends E> iterator = MoreIterables.reversed(items);
      if (!iterator.hasNext()) {
         return this;
      }
      ArrayList<E> prefix = new ArrayList<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         prefix.add(current.value);
         current = current.next;
      }
      while (iterator.hasNext()) {
         current = new LinkedPersistentList<E>(iterator.next(), current);
      }
      ListIterator<E> listIterator = prefix.listIterator(prefix.size());
      while (listIterator.hasPrevious()) {
         current = new LinkedPersistentList<E>(listIterator.previous(), current);
      }
      return current;
   }

   @Override
   public LinkedPersistentList<E> withHead(E e) {
      return new LinkedPersistentList<E>(e, this);
   }

   @Override
   public LinkedPersistentList<E> withTail(E e) {
      return with(size, e);
   }

   @Override
   public LinkedPersistentList<E> withReplacement(int i, E e) {
      rangeCheck(i);
      ArrayList<E> nodes = new ArrayList<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         nodes.add(current.value);
         current = current.next;
      }
      // replace value of current node with specified value
      current = new LinkedPersistentList<E>(e, current.next);
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.remove(nodes.size() - 1), current);
      }
      return current;
   }

   @Override
   public LinkedPersistentList<E> withReplacements(UnaryOperator<E> operator) {
      ArrayList<E> nodes = toArrayList();
      LinkedPersistentList<E> current = create();
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(
               operator.apply(nodes.remove(nodes.size() - 1)), current);
      }
      return current;
   }

   @Override
   public LinkedPersistentList<E> without(int i) {
      rangeCheck(i);
      if (size == 1) {
         return EmptyList.instance();
      }
      ArrayList<E> nodes = new ArrayList<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         nodes.add(current.value);
         current = current.next;
      }
      // skip over removed element
      current = current.next;
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.remove(nodes.size() - 1), current);
      }
      return current;
   }

   @Override
   public LinkedPersistentList<E> rest() {
      return next == null ? EmptyList.<E>instance() : next;
   }

   @Override
   public LinkedPersistentList<E> subList(int from, int to) {
      rangeCheckWide(from);
      rangeCheckWide(to);
      if (from > to) {
         throw new IndexOutOfBoundsException(from + " > " + to);
      }
      if (from == to) {
         return EmptyList.instance();
      }
      if (to == size) {
         // no need to copy -- just go find the from node
         LinkedPersistentList<E> ret = this;
         while (from > 0) {
            ret = ret.next;
            from--;
         }
         return ret;
      } else {
         // must copy the sub-list :(
         ArrayList<E> nodes = new ArrayList<E>(to - from);
         for (LinkedPersistentList<E> current = this; to > 0;
               current = current.next, to--, from--) {
            if (from <= 0) {
               nodes.add(current.value);
            }
         }
         LinkedPersistentList<E> ret = null;
         while (!nodes.isEmpty()) {
            ret = new LinkedPersistentList<E>(nodes.remove(nodes.size() - 1), ret);
         }
         return ret;
      }
   }
   
   @Override
   public LinkedPersistentList<E> with(E e) {
      return withHead(e);
   }

   @Override
   public LinkedPersistentList<E> without(Object o) {
      ArrayList<E> nodes = new ArrayList<E>();
      LinkedPersistentList<E> current = this;
      while (current != null) {
         if (o == null ? current.value == null : o.equals(current.value)) {
            break;
         }
         nodes.add(current.value);
         current = current.next;
      }
      if (current == null) {
         // didn't find it
         return this;
      }
      // skip over removed element
      current = current.next;
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.remove(nodes.size() - 1), current);
      }
      return current == null ? EmptyList.<E>instance() : current;
   }

   @Override
   public LinkedPersistentList<E> withoutAny(Object o) {
      ArrayList<E> nodes = new ArrayList<E>(size);
      LinkedPersistentList<E> current = this;
      boolean found = false;
      while (current != null) {
         if (!(o == null ? current.value == null : o.equals(current.value))) {
            // only add if value doesn't match object to remove
            found = true;
            nodes.add(current.value);
         }
         current = current.next;
      }
      if (!found) {
         // didn't find it
         return this;
      }
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.remove(nodes.size() - 1), current);
      }
      return current == null ? EmptyList.<E>instance() : current;
   }

   private ArrayList<E> toArrayList() {
      ArrayList<E> elements = new ArrayList<E>(size);
      for (LinkedPersistentList<E> current = this; current != null; current = current.next) {
         elements.add(current.value);
      }
      return elements;
   }
   
   private Predicate<? super E> filter(Iterable<?> items, boolean remove) {
      if (items instanceof Collection) {
         Collection<?> coll = (Collection<?>) items;
         if (coll.isEmpty()) {
            return null;
         }
         return e -> coll.contains(e) == remove;
      } else {
         if (!items.iterator().hasNext()) {
            return null;
         }
         return e -> Iterables.contains(items, e) == remove;
      }
   }
   
   @Override
   public LinkedPersistentList<E> withoutAny(Iterable<?> items) {
      return withoutAny(filter(items, true));
   }

   @Override
   public LinkedPersistentList<E> withoutAny(Predicate<? super E> predicate) {
      ArrayList<E> nodes = toArrayList();
      LinkedPersistentList<E> ret = null;
      while (!nodes.isEmpty()) {
         E e = nodes.remove(nodes.size() - 1);
         if (!predicate.test(e)) {
            ret = new LinkedPersistentList<E>(e, ret);
         }
      }
      if (ret == null) {
         return create();
      }
      // return this if operation effectively made no change
      return ret.size() == this.size() ? this : ret;
   }

   @Override
   public LinkedPersistentList<E> withOnly(Iterable<?> items) {
      return withoutAny(filter(items, false));
   }
   
   @Override
   public LinkedPersistentList<E> removeAll() {
      return create();
   }

   @Override
   public LinkedPersistentList<E> withAll(Iterable<? extends E> items) {
      Iterator<? extends E> iterator = MoreIterables.reversed(items);
      LinkedPersistentList<E> node = this;
      while (iterator.hasNext()) {
         node = new LinkedPersistentList<E>(iterator.next(), node);
      }
      return node;
   }

   private static class EmptyList<E> extends LinkedPersistentList<E> {

      private static final long serialVersionUID = 1487151881149064099L;
      
      private static final Object[] EMPTY_ARRAY = new Object[0];
      
      static final EmptyList<Object> INSTANCE = new EmptyList<Object>();
      
      @SuppressWarnings("unchecked") // immutability makes it safe
      static <E> EmptyList<E> instance() {
         return (EmptyList<E>) INSTANCE;
      }
      
      @SuppressWarnings("synthetic-access") // accesses enclosing class private ctor
      private EmptyList() {
         super(null, null);
      }

      @Override
      public E get(int i) {
         throw new IndexOutOfBoundsException("" + i);
      }

      @Override
      public int indexOf(Object o) {
         return -1;
      }

      @Override
      public int lastIndexOf(Object o) {
         return -1;
      }

      @Override
      public E first() {
         throw new NoSuchElementException();
      }

      @Override
      public int size() {
         return 0;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Object[] toArray() {
         return EMPTY_ARRAY;
      }

      @Override
      public <T> T[] toArray(T[] array) {
         if (array.length > 0) {
            array[0] = null;
         }
         return array;
      }

      @Override
      public boolean contains(Object o) {
         return false;
      }

      @Override
      public boolean containsAll(Collection<?> items) {
         return items.isEmpty();
      }

      @Override
      public Iterator<E> iterator() {
         return Iterators.unmodifiableIterator(Collections.emptyIterator());
      }

      @Override
      public ListIterator<E> listIterator() {
         return MoreIterators.unmodifiableListIterator(Collections.emptyListIterator());
      }

      @Override
      public ListIterator<E> listIterator(int pos) {
         if (pos != 0) {
            throw new IndexOutOfBoundsException("" + pos);
         }
         return MoreIterators.unmodifiableListIterator(Collections.emptyListIterator());
      }

      @Override
      public LinkedPersistentList<E> subList(int from, int to) {
         if (from != 0) {
            throw new IndexOutOfBoundsException("" + from);
         }
         if (to != 0) {
            throw new IndexOutOfBoundsException("" + to);
         }
         return this;
      }

      @Override
      public LinkedPersistentList<E> with(int i, E e) {
         if (i != 0) {
            throw new IndexOutOfBoundsException("" + i);
         }
         return with(e);
      }

      @Override
      public LinkedPersistentList<E> withAll(int i, Iterable<? extends E> items) {
         if (i != 0) {
            throw new IndexOutOfBoundsException("" + i);
         }
         return create(items);
      }

      @Override
      public LinkedPersistentList<E> withHead(E e) {
         return with(e);
      }

      @Override
      public LinkedPersistentList<E> withTail(E e) {
         return with(e);
      }

      @Override
      public LinkedPersistentList<E> withReplacement(int i, E e) {
         throw new IndexOutOfBoundsException("" + i);
      }

      @Override
      public LinkedPersistentList<E> withReplacements(UnaryOperator<E> operator) {
         return this;
      }


      @Override
      public LinkedPersistentList<E> without(int i) {
         throw new IndexOutOfBoundsException("" + i);
      }

      @Override
      public LinkedPersistentList<E> rest() {
         throw new NoSuchElementException();
      }

      @SuppressWarnings("synthetic-access")
      @Override
      public LinkedPersistentList<E> with(E e) {
         return new LinkedPersistentList<E>(e, null);
      }

      @Override
      public LinkedPersistentList<E> without(Object o) {
         return this;
      }

      @Override
      public LinkedPersistentList<E> withoutAny(Object o) {
         return this;
      }

      @Override
      public LinkedPersistentList<E> withoutAny(Iterable<?> items) {
         return this;
      }

      @Override
      public LinkedPersistentList<E> withoutAny(Predicate<? super E> predicate) {
         return this;
      }

      @Override
      public LinkedPersistentList<E> withOnly(Iterable<?> items) {
         return this;
      }

      @Override
      public LinkedPersistentList<E> withAll(Iterable<? extends E> items) {
         return create(items);
      }
   }
}
