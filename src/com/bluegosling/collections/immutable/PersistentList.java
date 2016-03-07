package com.bluegosling.collections.immutable;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A fully persistent list. This provides mutation operations that return new lists. Since changes
 * to a persistent data structure preserve their previous versions, persistent lists are also
 * immutable.
 * 
 * @param <E> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PersistentList<E> extends List<E>, PersistentCollection<E> {
   
   @Deprecated
   @Override
   default E set(int i, E e) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default void add(int i, E e) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean addAll(int i, Collection<? extends E> coll) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default E remove(int i) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean add(E e) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean addAll(Collection<? extends E> coll) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean remove(Object o) {
      throw new UnsupportedOperationException();
   }   
   
   @Deprecated
   @Override
   default boolean removeAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   default boolean retainAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   default void replaceAll(UnaryOperator<E> op) {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   default void clear() {
      throw new UnsupportedOperationException();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of just
    * a {@link List}.
    */
   @Override PersistentList<E> subList(int from, int to);
   
   /**
    * Adds an element at the specified index. The persistent list will copy as little as possible
    * to construct the new version of the list with the new node. Some implementations may benefit
    * from adding elements closer to the front; others may do better adding elements near the end.
    * 
    * <p>This is analogous to the {@link List#add(int, Object)} operation in mutable lists.
    * 
    * @param i the index at which the element is added
    * @param e the new element
    * @return a new list with the specified element inserted at the specified position
    * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than
    *       this list's {@link #size()}
    */
   PersistentList<E> with(int i, E e);
   
   /**
    * Adds all of the specified elements at the specified index. The first element fetched from the
    * specified collection will be at the specified index. The second element fetched will be at the
    * specified index plus one, and so on.
    * 
    * <p>This is analogous to the {@link List#addAll(int, Collection)} operation in mutable lists.
    * 
    * @param i the index at which the elements are added
    * @param items the new elements
    * @return a new list with the specified elements inserted at the specified position
    * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than
    *       this list's {@link #size()}
    */
   PersistentList<E> withAll(int i, Iterable<? extends E> items);
   
   /**
    * Adds the specified element to the front of the list. For some implementations, this may be
    * a constant time operation. For others, it may be linear, <em>O(n)</em>.
    * 
    * @param e the new element
    * @return a new list with the specified element inserted at the front of the list
    */
   PersistentList<E> withHead(E e);

   /**
    * Adds the specified element to the end of the list. For some implementations, this may be
    * a constant time operation. For others, it may be linear, <em>O(n)</em>.
    * 
    * <p>This is analogous to the {@link List#add(Object)} operation in mutable lists.
    * 
    * @param e the new element
    * @return a new list with the specified element inserted at the end of the list
    */
   PersistentList<E> withTail(E e);
   
   /**
    * Sets the element at the given index to the specified new value. The persistent list will copy
    * as little as possible to construct the new version of the list with the updated value. Some
    * implementations may benefit from setting elements closer to the front; others may do better
    * setting elements near the end.
    * 
    * <p>This is analogous to the {@link List#set} operation in mutable lists.
    *
    * @param i the index to the element to update
    * @param e the new element's value
    * @return a new list with the element at the specified position updated to the specified value
    * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than
    *       or equal to this list's {@link #size()}
    */
   PersistentList<E> withReplacement(int i, E e);
   
   /**
    * Updates the element at every index using the given operator to compute their replacements.
    * This is analogous to the {@link List#replaceAll} operation in mutable lists.
    *
    * @param operator an operator that computes the replacement value for each given list element
    * @return a new list with every element replaced with values computed by the given operator
    * 
    * @see List#replaceAll(UnaryOperator)
    */
   PersistentList<E> withReplacements(UnaryOperator<E> operator);
   
   /**
    * Removes the element at the given index. The persistent list will copy as little as possible to
    * construct the new version of the list with the element removed. Some implementations may
    * benefit from removing elements closer to the front; others may do better removing elements
    * near the end.
    *
    * @param i the index of the element to remove
    * @return a new list with the element at the specified position removed
    * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than
    *       or equal to this list's {@link #size()}
    */
   PersistentList<E> without(int i);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method should run in constant time, or at worst sub-linear time. It makes no
    * guarantees as to what position the new element will occupy. It is typically either the front
    * of the list or the end of the list. 
    * 
    * <p>Use this when order of elements is less important than speed of insertion, like when using
    * the list as an efficient "bag" (unordered collection that allows duplicates). If order does
    * matter, then use {@link #withHead(Object)} or {@link #withTail(Object)} instead.
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> with(E e);
   
   /**
    * {@inheritDoc}
    * 
    * <p>If the list contains more than one occurrence of the given element, this method makes no
    * guarantees as to which one would be removed, though it is typically either the first
    * occurrence or the last.
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> without(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> withoutAny(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> withoutAny(Iterable<?> items);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> withOnly(Iterable<?> items);

   /**
    * {@inheritDoc}
    * 
    * <p>This method will preserve the order of the specified collection as far as their order in
    * the returned new list. This method may insert the items at the front of the list or may
    * instead append them at the end of the list, depending on which is more efficient.
    * 
    * <p>Use this when order of elements is less important than speed of insertion. If order does
    * matter, then use {@link #withAll(int, Iterable)} instead.
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> withAll(Iterable<? extends E> items);
   
   /**
    * Returns an empty list.
    *
    * @return an empty persistent list
    */
   @Override PersistentList<E> removeAll();
}
