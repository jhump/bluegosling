package com.apriori.collections;

import java.util.List;

/**
 * Provides methods for interacting with linked list nodes, just like
 * we're all used to from CS 101 class...
 * 
 * <p>A simple linked list implementation could store its structural
 * internally using objects that implement the {@code Node} interface.
 * 
 * @author jhumphries
 *
 * @param <E>  The type of element contained in the list
 */
public interface LinkedList<E> extends List<E> {

	/**
	 * Represents (or is) a node in the linked list structure. For
	 * simplicity, this doesn't include list mutator methods. Use the
	 * methods of {@code ListIterator} for that instead.
	 *
	 * @author jhumphries
	 *
	 * @param <E> The type of value stored in the node
	 */
	interface Node<E> {
		/**
		 * Gets the current index into the list for this node. Zero
		 * represents the first item in the list and {@size() - 1}
		 * represents the last item in the list.
		 * 
		 * @return   The index for this node in the list
		 */
		int currentIndex();
		
		/**
		 * Gets the value of this node. If this node represents the
		 * first item in the list, for example, then this would be
		 * the same as {@code list.get(0)}.
		 * 
		 * @return   The value of this node
		 */
		E value();

		/**
		 * Gets the next node in the list.
		 * 
		 * @return   The next node in the list or {@code null} if this
		 * 			 is the last node in the list
		 */
		Node<E> next();

		/**
		 * Creates a {@code ListIterator} from this position in the
		 * list. Calling {@code iterator.next()} would return the
		 * same element as {@code node.next().value()}. If this node
		 * represents the first item in the list, for example, then
		 * this would be the same as {@code list.listIterator(1)}.
		 * 
		 * @return   An iterator from this node's position in the list
		 */
		ListIterator<E> iteratorFrom();
	}

	/**
	 * Extends {@code java.util.ListIterator} to provide access to
	 * {@code Node} instances in this list.
	 * 
	 * @author jhumphries
	 *
	 * @param <E> The type of element contained in the list
	 */
	interface ListIterator<E> extends java.util.ListIterator<E> {
		/**
		 * Returns the {@code Node} for the next element in the list.
		 * Calling {@code iterator.nodeNode().value()} would be
		 * similar to calling {@code iterator.next()}.
		 * 
		 * @return   The node that represents the next item in the
		 *           iteration or null if the iteration has no
		 *           more items
		 */
		Node<E> nextNode();
	}
	
	/**
	 * Narrows the return type of {@code java.util.List.listIterator()}
	 * to return an instance of {@code LinkedList.ListIterator}.
	 */
	@Override
	public ListIterator<E> listIterator();

	/**
	 * Narrows the return type of {@code java.util.List.listIterator(int)}
	 * to return an instance of {@code LinkedList.ListIterator}.
	 */
	@Override
	public ListIterator<E> listIterator(int from);

	/**
	 * Narrows the return type of {@code java.util.List.subList()}
	 * to return a {@code LinkedList}.
	 */
	@Override
	public LinkedList<E> subList(int from, int to);

	/**
	 * Gets the list's head node, which corresponds to the first
	 * element in the list.
	 * 
	 * @return The node for the first item in the list or {@code null}
	 *         if the list is empty
	 */
	public Node<E> getHead();

	/**
	 * Returns the first item in the list, like the Lisp "car" function.
	 * 
	 * @return The first item in the list or {@code null} if the list
	 *         is empty
	 */
	public E car();
	
	/**
	 * Returns the subset of this list that excludes the first item, like
	 * the Lisp "cdr" function. If the list is empty, this will return
	 * {@code null}. If the list has only one element (returned by the
	 * {@code car()} method) then this will return an empty list.
	 * 
	 * @return The rest of the list, after the first item
	 */
	public LinkedList<E> cdr();
}
