package com.bluegosling.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A simple stack interface. This interface is much more targeted for LIFO stacks and thus narrower
 * than {@link Deque} or {@link List}.
 *
 * @param <T> the type of element in the stack
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Stack<T> extends SizedIterable<T> {

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
    * Drains the stack by popping all elements from the stack and adding them to the given
    * collection. The top of the stack will be the first item added to the collection. 
    *
    * @param coll the collection into which items are drained
    */
   default void drainTo(Collection<? super T> coll) {
      while (!isEmpty()) {
         try {
            coll.add(pop());
         } catch (NoSuchElementException e) {
            // last item was concurrently removed
         }
      }
   }
   
   /**
    * Drains the stack and returns its contents in a new instance. This can be interpreted as a
    * "clone" then "clear" operation, in one.
    *
    * @return a new stack that has all of the contents that were held by this stack, in the same
    *       order
    */
   Stack<T> removeAll();

   /**
    * Removes all items from the stack.
    */
   default void clear() {
      removeAll();
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
         public Stack<T> removeAll() {
            Stack<T> ret = fromList(new ArrayList<>(list));
            list.clear();
            return ret;
         }
         
         @Override
         public void clear() {
            list.clear();
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
         public Stack<T> removeAll() {
            Stack<T> ret = fromDeque(new ArrayDeque<>(deque));
            deque.clear();
            return ret;
         }
         
         @Override
         public void clear() {
            deque.clear();
         }
      };
   }
}
