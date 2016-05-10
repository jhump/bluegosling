package com.bluegosling.collections;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * A simple stack interface. This interface is much more targeted for LIFO stacks and thus narrower
 * than {@link Deque} or {@link List}.
 *
 * @param <T> the type of element in the stack
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Stack<T> extends Collection<T> {

   /**
    * Pushes a value onto the stack.
    *
    * @param value the value to push
    * 
    * @see Deque#push(Object)
    */
   void push(T value);
   
   /**
    * Returns the element at the top of the stack. The element is not removed.
    *
    * @return the element at the top of the stack
    * @throws NoSuchElementException if the stack is empty
    * 
    * @see Deque#element()
    */
   T element();
   
   /**
    * Returns the element at the top of the stack or {@code null} if the stack is empty. The element
    * is not removed.
    *
    * @return the element at the top of the stack or {@code null}
    * 
    * @see Deque#peek()
    */
   default T peek() {
      try {
         return element();
      } catch (NoSuchElementException e) {
         return null;
      }
   }
   
   /**
    * Removes the element from the top of the stack and returns it.
    *
    * @return the element at the top of the stack
    * @throws NoSuchElementException if the stack is empty
    * 
    * @see Deque#pop()
    */
   T pop();
   
   /**
    * Removes the element from the top of the stack and returns it or returns {@code null} if the
    * stack is empty.
    *
    * @return the element at the top of the stack or {@code null}
    * 
    * @see Deque#poll()
    */
   default T poll() {
      try {
         return pop();
      } catch (NoSuchElementException e) {
         return null;
      }
   }

   /**
    * Adapts the {@link List} interface to this interface. Since lists usually have constant time
    * append and constant time removal from the end, the end of the list is considered the top of
    * the stack.
    *
    * @param list a list
    * @return a stack that is backed by the given list
    */
   static <T> Stack<T> fromList(List<T> list) {
      return new Stack<T>() {
         @Override
         public Iterator<T> iterator() {
            return CollectionUtils.reverseIterator(list.listIterator(list.size()));
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
         public void push(T value) {
            list.add(value);
         }

         @Override
         public T element() {
            if (list.isEmpty()) {
               throw new NoSuchElementException();
            }
            return list.get(list.size() - 1);
         }

         @Override
         public T peek() {
            if (list.isEmpty()) {
               return null;
            }
            return list.get(list.size() - 1);
         }

         @Override
         public T pop() {
            if (list.isEmpty()) {
               throw new NoSuchElementException();
            }
            return list.remove(list.size() - 1);
         }

         @Override
         public T poll() {
            if (list.isEmpty()) {
               return null;
            }
            return list.remove(list.size() - 1);
         }
         

         @Override
         public void clear() {
            list.clear();
         }

         @Override
         public boolean contains(Object o) {
            return list.contains(o);
         }

         @Override
         public Object[] toArray() {
            return list.toArray();
         }

         @Override
         public <U> U[] toArray(U[] a) {
            return list.toArray(a);
         }

         @Override
         public boolean add(T e) {
            return list.add(e);
         }

         @Override
         public boolean remove(Object o) {
            return CollectionUtils.removeObject(o, iterator(), true);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return list.containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends T> c) {
            return list.addAll(c);
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            return list.removeAll(c);
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            return list.retainAll(c);
         }
         
         @Override
         public boolean removeIf(Predicate<? super T> predicate) {
            return list.removeIf(predicate);
         }

         
         @Override
         public String toString() {
            return CollectionUtils.toString(list);
         }
      };
   }
   
   /**
    * Adapts the {@link Deque} interface to this interface. Most operations on the stack just call
    * through to the method of the same name on the given deque.
    *
    * @param deque a deque
    * @return a stack that is backed by the given deque
    */
   static <T> Stack<T> fromDeque(Deque<T> deque) {
      return new Stack<T>() {
         @Override
         public Iterator<T> iterator() {
            return deque.iterator();
         }

         @Override
         public int size() {
            return deque.size();
         }

         @Override
         public boolean isEmpty() {
            return deque.isEmpty();
         }

         @Override
         public void push(T value) {
            deque.push(value);
         }

         @Override
         public T element() {
            return deque.element();
         }

         @Override
         public T peek() {
            return deque.peek();
         }

         @Override
         public T pop() {
            return deque.pop();
         }

         @Override
         public T poll() {
            return deque.poll();
         }

         @Override
         public void clear() {
            deque.clear();
         }
         
         @Override
         public boolean contains(Object o) {
            return deque.contains(o);
         }

         @Override
         public Object[] toArray() {
            return deque.toArray();
         }

         @Override
         public <U> U[] toArray(U[] a) {
            return deque.toArray(a);
         }

         @Override
         public boolean add(T e) {
            deque.push(e);
            return true;
         }

         @Override
         public boolean remove(Object o) {
            return deque.remove(o);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return deque.containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends T> c) {
            for (T t : c) {
               deque.push(t);
            }
            return true;
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            return deque.removeAll(c);
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            return deque.retainAll(c);
         }
         
         @Override
         public boolean removeIf(Predicate<? super T> predicate) {
            return deque.removeIf(predicate);
         }
         
         @Override
         public String toString() {
            return CollectionUtils.toString(deque);
         }
      };
   }
}
