package com.apriori.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

// TODO: javadoc
public interface Stack<T> extends SizedIterable<T> {

   void push(T value);
   
   T element();
   
   default T peek() {
      try {
         return element();
      } catch (NoSuchElementException e) {
         return null;
      }
   }
   
   T pop();
   
   default T poll() {
      try {
         return pop();
      } catch (NoSuchElementException e) {
         return null;
      }
   }

   default void drainTo(Collection<? super T> coll) {
      while (!isEmpty()) {
         try {
            coll.add(pop());
         } catch (NoSuchElementException e) {
            // last item was concurrently removed
         }
      }
   }
   
   Stack<T> removeAll();
   
   default void clear() {
      removeAll();
   }
   
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
