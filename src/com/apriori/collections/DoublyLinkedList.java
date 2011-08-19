package com.apriori.collections;

/**
 * Extends {@code LinkedList} to provide more methods -- for interacting
 * with nodes in <em>double</em> linked lists.
 * 
 * @author jhumphries
 *
 * @param <E> the type of element contained in the list
 */
public interface DoublyLinkedList<E> extends LinkedList<E> {

	/**
	 * Represents (or is) a node in the linked list structure. This
	 * extends {@code LinkedList.Node} to provide methods for the
	 * "back" links -- which different doubly linked lists from
	 * their singly linked counterparts.
	 *
	 * @author jhumphries
	 *
	 * @param <E> the type of value stored in the node
	 */
	interface Node<E> extends LinkedList.Node<E> {
		/**
		 * Narrows the return type from {@code LinkedList.Node}
		 * to {@code DoublyLinkedList.Node}.
		 */
		Node<E> next();
		
		/**
		 * Gets the previous node in the list.
		 * 
		 * @return the previous node in the list or {@code null} if this
		 * 			is the first node in the list
		 */
		Node<E> previous();

		/**
		 * Narrows the return type from {@code LinkedList.ListIterator}
		 * to {@code DoublyLinkedList.ListIterator}.
		 */
		ListIterator<E> iteratorFrom();
	}

	/**
	 * Extends {@code LinkedList.ListIterator} to provide additional
	 * methods for the "back" links in the list.
	 * 
	 * @author jhumphries
	 *
	 * @param <E> the type of element contained in the list
	 */
	interface ListIterator<E> extends LinkedList.ListIterator<E> {
		/**
		 * Narrows the return type from {@code LinkedList.Node}
		 * to {@code DoublyLinkedList.Node}.
		 */
	   
		Node<E> nextNode();
		
		/**
		 * Returns the {@code Node} for the previous element in the list.
		 * Calling {@code iterator.previousNode().value()} would be
		 * similar to calling {@code iterator.previous()}.
		 * 
		 * @return the node that represents the previous item in the
		 * 			iteration or null if the iteration has no
		 * 			prior items
		 */
		Node<E> previousNode();
	}
	
	/**
	 * Further narrows the return type from {@code LinkedList.ListIterator}
	 * to {@code DoublyLinkedList.ListIterator}.
	 */
	@Override
	public ListIterator<E> listIterator();

	/**
	 * Further narrows the return type from {@code LinkedList.ListIterator}
	 * to {@code DoublyLinkedList.ListIterator}.
	 */
	@Override
	public ListIterator<E> listIterator(int from);

	/**
	 * Further narrows the return type from {@code LinkedList} to
	 * {@code DoublyLinkedList}.
	 */
	@Override
	public DoublyLinkedList<E> subList(int from, int to);

	/**
	 * Gets the list's head node, which corresponds to the first
	 * element in the list. This extends {@code LinkedList.getHead()}
	 * in order to narrow the return type from {@code LinkedList.Node}
	 * to {@code DoublyLinkedList.Node}.
	 * 
	 * @return the node for the first item in the list or {@code null}
	 * 			if the list is empty
	 */
	@Override
	public Node<E> getHead();

	/**
	 * Gets the list's tail node, which corresponds to the last
	 * element in the list.
	 * 
	 * @return the node for the last item in the list or {@code null}
	 * 			if the list is empty
	 */
	public Node<E> getTail();
	
	/**
	 * Narrows the return type of {@code LinkedList.cdr()}
	 * to return a {@code DoublyLinkedList}.
	 */
	@Override
	public DoublyLinkedList<E> cdr();
}
