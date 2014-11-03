package com.apriori.collections;

import static com.apriori.collections.Immutables.cast;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

// TODO: javadoc
// TODO: tests
// TODO: serialization
// TODO: extend AbstractLinkedImmutableList
public class LinkedPersistentList<E> implements PersistentList<E>, Serializable {
   /*
    * Since Java does not do tail-call optimization for recursive functions, these methods use
    * iteration (to ensure we don't overflow the stack) even though recursion would be more elegant
    * for nearly all of them. C'est la vie :(
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
      Iterator<? extends E> iterator = Iterables.reversed(iterable);
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
   
   protected void rangeCheck(int index) {
      if (index < 0 || index >= size) {
         throw new IndexOutOfBoundsException("" + index);
      }
   }
   
   protected void rangeCheckWide(int index) {
      if (index < 0 || index > size) {
         throw new IndexOutOfBoundsException("" + index);
      }
   }
      
   @Override
   public E get(int i) {
      rangeCheck(i);
      for (LinkedPersistentList<E> current = this; current != null; current = current.next, i--) {
         if (i == 0) {
            return value;
         }
      }
      // since we range-checked the index, we should never get here
      throw new AssertionError("not possible");
   }

   @Override
   public int indexOf(Object o) {
      int i = 0;
      for (LinkedPersistentList<E> current = this; current != null; current = current.next, i++) {
         if (current.value == null ? o == null : current.value.equals(o)) {
            return i;
         }
      }
      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      // don't want to blow out the call stack, so push into a stack so we can do this iteratively
      ArrayDeque<E> elements = new ArrayDeque<E>(size);
      for (LinkedPersistentList<E> current = this; current != null; current = current.next) {
         elements.push(current.value);
      }
      for (int i = size - 1; !elements.isEmpty(); i--) {
         E e = elements.pop();
         if (e == null ? o == null : e.equals(o)) {
            return i;
         }
      }
      return -1;
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
   public Object[] toArray() {
      Object a[] = new Object[size];
      int i = 0;
      for (LinkedPersistentList<E> current = this; current != null; current = current.next) {
         a[i++] = current.value;
      }
      return a;
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T[] toArray(T[] array) {
      Object a[] = ArrayUtils.newArrayIfTooSmall(array, size);
      int i = 0;
      for (LinkedPersistentList<E> current = this; current != null; current = current.next) {
         a[i++] = current.value;
      }
      if (a.length > size) {
         a[size] = null;
      }
      return (T[]) a;
   }

   @Override
   public boolean contains(Object o) {
      for (LinkedPersistentList<E> current = this; current != null; current = current.next) {
         if (current.value == null ? o == null : current.value.equals(o)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean containsAll(Iterable<?> items) {
      for (Object o : items) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      for (Object o : items) {
         if (contains(o)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public Iterator<E> iterator() {
      return new Iterator<E>() {
         private LinkedPersistentList<E> node = LinkedPersistentList.this;
               
         @Override public boolean hasNext() {
            return node != null;
         }

         @SuppressWarnings("synthetic-access")
         @Override
         public E next() {
            if (node == null) {
               throw new NoSuchElementException();
            }
            E ret = node.value;
            node = node.next;
            return ret;
         }
      };
   }

   @Override
   public PersistentList<E> subList(int from, int to) {
      rangeCheckWide(from);
      rangeCheckWide(to);
      if (from > to) {
         throw new IndexOutOfBoundsException(from + " > " + to);
      }
      if (from == size) {
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
         ArrayDeque<E> nodes = new ArrayDeque<E>(to - from);
         for (LinkedPersistentList<E> current = this; to > 0;
               current = current.next, to--, from--) {
            if (from >= 0) {
               nodes.push(current.value);
            }
         }
         LinkedPersistentList<E> ret = null;
         while (!nodes.isEmpty()) {
            ret = new LinkedPersistentList<E>(nodes.pop(), ret);
         }
         return ret;
      }
   }

   @Override
   public PersistentList<E> add(int i, E e) {
      rangeCheckWide(i);
      ArrayDeque<E> nodes = new ArrayDeque<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         nodes.push(current.value);
         current = current.next;
      }
      LinkedPersistentList<E> ret = new LinkedPersistentList<E>(e, current);
      while (!nodes.isEmpty()) {
         ret = new LinkedPersistentList<E>(nodes.pop(), ret);
      }
      return ret;
   }

   @Override
   public PersistentList<E> addAll(int i, Iterable<? extends E> items) {
      if (i == 0) {
         return addAll(items);
      }
      rangeCheckWide(i);
      Iterator<? extends E> iterator = Iterables.reversed(items);
      if (!iterator.hasNext()) {
         return this;
      }
      ArrayDeque<E> prefix = new ArrayDeque<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         prefix.push(current.value);
         current = current.next;
      }
      while (iterator.hasNext()) {
         current = new LinkedPersistentList<E>(iterator.next(), current);
      }
      iterator = prefix.iterator();
      while (iterator.hasNext()) {
         current = new LinkedPersistentList<E>(iterator.next(), current);
      }
      return current;
   }

   @Override
   public PersistentList<E> addFirst(E e) {
      return new LinkedPersistentList<E>(e, this);
   }

   @Override
   public PersistentList<E> addLast(E e) {
      return add(size, e);
   }

   @Override
   public PersistentList<E> set(int i, E e) {
      rangeCheck(i);
      ArrayDeque<E> nodes = new ArrayDeque<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         nodes.push(current.value);
         current = current.next;
      }
      // replace value of current node with specified value
      current = new LinkedPersistentList<E>(e, current.next);
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.pop(), current);
      }
      return current;
   }

   @Override
   public PersistentList<E> remove(int i) {
      rangeCheck(i);
      if (size == 1) {
         return EmptyList.instance();
      }
      ArrayDeque<E> nodes = new ArrayDeque<E>(i);
      LinkedPersistentList<E> current = this;
      while (i-- > 0) {
         nodes.push(current.value);
         current = current.next;
      }
      // skip over removed element
      current = current.next;
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.pop(), current);
      }
      return current;
   }

   @Override
   public PersistentList<E> rest() {
      return next == null ? EmptyList.<E>instance() : next;
   }

   @Override
   public PersistentList<E> add(E e) {
      return addFirst(e);
   }

   @Override
   public PersistentList<E> remove(Object o) {
      ArrayDeque<E> nodes = new ArrayDeque<E>();
      LinkedPersistentList<E> current = this;
      while (current != null) {
         if (o == null ? current.value == null : o.equals(current.value)) {
            break;
         }
         nodes.push(current.value);
         current = current.next;
      }
      if (current == null) {
         // didn't find it
         return this;
      }
      // skip over removed element
      current = current.next;
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.pop(), current);
      }
      return current == null ? EmptyList.<E>instance() : current;
   }

   @Override
   public PersistentList<E> removeAll(Object o) {
      ArrayDeque<E> nodes = new ArrayDeque<E>(size);
      LinkedPersistentList<E> current = this;
      boolean found = false;
      while (current != null) {
         if (!(o == null ? current.value == null : o.equals(current.value))) {
            // only add if value doesn't match object to remove
            found = true;
            nodes.push(current.value);
         }
         current = current.next;
      }
      if (!found) {
         // didn't find it
         return this;
      }
      while (!nodes.isEmpty()) {
         current = new LinkedPersistentList<E>(nodes.pop(), current);
      }
      return current == null ? EmptyList.<E>instance() : current;
   }

   private ArrayDeque<E> toDeque() {
      ArrayDeque<E> elements = new ArrayDeque<E>(size);
      for (LinkedPersistentList<E> current = this; current != null; current = current.next) {
         elements.push(current.value);
      }
      return elements;
   }
   
   private PersistentList<E> filter(Iterable<?> items, boolean remove) {
      // this stinks, but it's the best way to get adequate performance by using
      // the collection's contains method (which, worst case, will be the same as
      // the fallback using just Iterable, but best case [like if the collection is
      // a set with sub-linear contains()] much faster)
      if (items instanceof Collection) {
         Collection<?> coll = (Collection<?>) items;
         if (coll.isEmpty()) {
            return this;
         }
         ArrayDeque<E> nodes = toDeque();
         LinkedPersistentList<E> ret = null;
         while (!nodes.isEmpty()) {
            E e = nodes.pop();
            if (coll.contains(e) != remove) {
               ret = new LinkedPersistentList<E>(e, ret);
            }
         }
         return ret == null ? EmptyList.<E>instance() : ret;
         
      } else if (items instanceof ImmutableCollection) {
         ImmutableCollection<?> coll = (ImmutableCollection<?>) items;
         if (coll.isEmpty()) {
            return this;
         }
         ArrayDeque<E> nodes = toDeque();
         LinkedPersistentList<E> ret = null;
         while (!nodes.isEmpty()) {
            E e = nodes.pop();
            if (coll.contains(e) != remove) {
               ret = new LinkedPersistentList<E>(e, ret);
            }
         }
         return ret == null ? EmptyList.<E>instance() : ret;
         
      } else {
         if (!items.iterator().hasNext()) {
            return this;
         }
         ArrayDeque<E> nodes = toDeque();
         LinkedPersistentList<E> ret = null;
         while (!nodes.isEmpty()) {
            E e = nodes.pop();
            if (CollectionUtils.contains(items.iterator(), e) != remove) {
               ret = new LinkedPersistentList<E>(e, ret);
            }
         }
         return ret == null ? EmptyList.<E>instance() : ret;
      }
   }
   
   @Override
   public PersistentList<E> removeAll(Iterable<?> items) {
      return filter(items, true);
   }

   @Override
   public PersistentList<E> retainAll(Iterable<?> items) {
      return filter(items, false);
   }

   @Override
   public PersistentList<E> addAll(Iterable<? extends E> items) {
      Iterator<? extends E> iterator = Iterables.reversed(items);
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
      public boolean containsAll(Iterable<?> items) {
         return false;
      }

      @Override
      public boolean containsAny(Iterable<?> items) {
         return false;
      }

      @Override
      public Iterator<E> iterator() {
         return Collections.emptyIterator();
      }

      @Override
      public PersistentList<E> subList(int from, int to) {
         if (from != 0) {
            throw new IndexOutOfBoundsException("" + from);
         }
         if (to != 0) {
            throw new IndexOutOfBoundsException("" + to);
         }
         return this;
      }

      @Override
      public PersistentList<E> add(int i, E e) {
         if (i != 0) {
            throw new IndexOutOfBoundsException("" + i);
         }
         return add(e);
      }

      @Override
      public PersistentList<E> addAll(int i, Iterable<? extends E> items) {
         if (i != 0) {
            throw new IndexOutOfBoundsException("" + i);
         }
         return create(items);
      }

      @Override
      public PersistentList<E> addFirst(E e) {
         return add(e);
      }

      @Override
      public PersistentList<E> addLast(E e) {
         return add(e);
      }

      @Override
      public PersistentList<E> set(int i, E e) {
         throw new IndexOutOfBoundsException("" + i);
      }

      @Override
      public PersistentList<E> remove(int i) {
         throw new IndexOutOfBoundsException("" + i);
      }

      @Override
      public PersistentList<E> rest() {
         return this;
      }

      @SuppressWarnings("synthetic-access")
      @Override
      public PersistentList<E> add(E e) {
         return new LinkedPersistentList<E>(e, null);
      }

      @Override
      public PersistentList<E> remove(Object o) {
         return this;
      }

      @Override
      public PersistentList<E> removeAll(Object o) {
         return this;
      }

      @Override
      public PersistentList<E> removeAll(Iterable<?> items) {
         return this;
      }

      @Override
      public PersistentList<E> retainAll(Iterable<?> items) {
         return this;
      }

      @Override
      public PersistentList<E> addAll(Iterable<? extends E> items) {
         return create(items);
      }
   }
}
