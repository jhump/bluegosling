package com.apriori.collections;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

// TODO: javadoc
// TODO: tests
public class LinkedPersistentList<E> implements PersistentList<E> {
   /*
    * Since Java does not do tail-call optimization for recursive functions, these methods use
    * iteration even though recursion would be more elegant for nearly all of them.
    * C'est la vie :(
    */

   public static <E> LinkedPersistentList<E> create() {
      return EmptyList.instance();
   }
   
   public static <E> LinkedPersistentList<E> create(Iterable<? extends E> iterable) {
      // recursion would be more elegant, but a large list would blow the stack
      
      if (iterable instanceof LinkedPersistentList) {
         @SuppressWarnings({ "unchecked", "rawtypes" }) // since it's immutable and we know
                                                      // type bound, this cast will be safe
         LinkedPersistentList<E> list = (LinkedPersistentList) iterable;
         return list;
      }
      
      if (iterable instanceof List) {
         List<? extends E> list = (List<? extends E>) iterable;
         ListIterator<? extends E> iterator = list.listIterator(list.size());
         LinkedPersistentList<E> node = null;
         while (iterator.hasPrevious()) {
            node = new LinkedPersistentList<E>(iterator.previous(), node);
         }
         if (node != null) {
            return node;
         }
      } else {
         Iterator<? extends E> iterator = iterable.iterator();
         if (iterator.hasNext()) {
            ArrayDeque<E> stack; 
            if (iterable instanceof Collection) {
               stack = new ArrayDeque<E>(((Collection<? extends E>) iterable).size());
            } else if (iterable instanceof ImmutableCollection) {
               stack = new ArrayDeque<E>(((ImmutableCollection<? extends E>) iterable).size());
            } else {
               stack = new ArrayDeque<E>();
            }
            while (iterator.hasNext()) {
               stack.push(iterator.next());
            }
            LinkedPersistentList<E> node = null;
            while (!stack.isEmpty()) {
               node = new LinkedPersistentList<E>(stack.pop(), node);
            }
            if (node != null) {
               return node;
            }
         }
      }

      return EmptyList.instance();
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
   public E get(int i) {
      if (i < 0) {
         throw new IndexOutOfBoundsException("" + i);
      }
      for (LinkedPersistentList<E> current = this; current != null; current = current.next, i--) {
         if (i == 0) {
            return value;
         }
      }
      throw new IndexOutOfBoundsException("" + i);
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
      // don't want to blow out the call stack, so allocate one so we can do this iteratively
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
      Object a[] = ArrayUtils.ensureCapacity(array, size);
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
      return Immutables.asIfMutable(immutableIterator());
   }
   
   @Override
   public ImmutableIterator<E> immutableIterator() {
      return new ImmutableIterator<E>() {
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
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> add(int i, E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addAll(int i, Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addFirst(E e) {
      return new LinkedPersistentList<E>(e, this);
   }

   @Override
   public PersistentList<E> addLast(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> set(int i, E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> remove(int i) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> rest() {
      return next;
   }

   @Override
   public PersistentList<E> add(E e) {
      return addFirst(e);
   }

   @Override
   public PersistentList<E> remove(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> removeAll(Object o) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> removeAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> retainAll(Iterable<?> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public PersistentList<E> addAll(Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }

   private static class EmptyList<E> extends LinkedPersistentList<E> {
      private static final Object[] EMPTY_ARRAY = new Object[0];
      private static final ImmutableIterator<Object> EMPTY_ITERATOR =
            new ImmutableIterator<Object>() {
               @Override public boolean hasNext() {
                  return false;
               }

               @Override public Object next() {
                  throw new NoSuchElementException();
               }
            };
      
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
         return Immutables.asIfMutable(immutableIterator());
      }
      
      @SuppressWarnings("unchecked") // immutability makes this safe
      @Override
      public ImmutableIterator<E> immutableIterator() {
         return (ImmutableIterator<E>) EMPTY_ITERATOR;
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
         return null;
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
