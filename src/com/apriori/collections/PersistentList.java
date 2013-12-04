package com.apriori.collections;

/**
 * A fully persistent list. This provides mutation operations that return new lists. Since changes
 * to a persistent data structure preserve their previous versions, persistent lists are also
 * immutable.
 * 
 * @param <E> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PersistentList<E> extends ImmutableList<E>, PersistentCollection<E> {
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of just
    * an {@link ImmutableList}.
    */
   @Override PersistentList<E> subList(int from, int to);
   
   /**
    * Adds an element at the specified index. The persistent list will copy as little as possible
    * to construct the new version of the list with the new node. Some implementations may benefit
    * from adding elements closer to the front; others may do better adding elements near the end.
    * 
    * @param i the index at which the element is added
    * @param e the new element
    * @return a new list with the specified element inserted at the specified position
    * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than
    *       this list's {@link #size()}
    */
   PersistentList<E> add(int i, E e);
   
   /**
    * Adds all of the specified elements at the specified index. The first element fetched from the
    * specified collection will be at the specified index. The second element fetched will be at the
    * specified index plus one, and so on.
    * 
    * @param i the index at which the elements are added
    * @param items the new elements
    * @return a new list with the specified elements inserted at the specified position
    * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than
    *       this list's {@link #size()}
    */
   PersistentList<E> addAll(int i, Iterable<? extends E> items);
   
   /**
    * Adds the specified element to the front of the list. For some implementations, this may be
    * a constant time operation. For others, it may be linear, <em>O(n)</em>.
    * 
    * @param e the new element
    * @return a new list with the specified element inserted at the front of the list
    */
   PersistentList<E> addFirst(E e);

   /**
    * Adds the specified element to the end of the list. For some implementations, this may be
    * a constant time operation. For others, it may be linear, <em>O(n)</em>.
    * 
    * @param e the new element
    * @return a new list with the specified element inserted at the end of the list
    */
   PersistentList<E> addLast(E e);
   
   PersistentList<E> set(int i, E e);
   PersistentList<E> remove(int i);
   @Override PersistentList<E> rest();
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method should run in constant time, or at worst sub-linear time. It makes no
    * guarantees as to what position the new element will occupy. It is typically either the front
    * of the list or the end of the list. 
    * 
    * <p>Use this when order of elements is less important than speed of insertion. If order does
    * matter, then use {@link #addFirst(Object)} or {@link #addLast(Object)} instead.
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> add(E e);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> remove(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> removeAll(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> removeAll(Iterable<?> items);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> retainAll(Iterable<?> items);

   /**
    * {@inheritDoc}
    * 
    * <p>This method will preserve the order of the specified collection as far as their order in
    * the returned new list. This method may insert the items at the front of the list or may
    * instead append them at the end of the list, depending on which is more efficient.
    * 
    * <p>Use this when order of elements is less important than speed of insertion. If order does
    * matter, then use {@link #addAll(int, Iterable)} instead.
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentList} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentList<E> addAll(Iterable<? extends E> items);
}
