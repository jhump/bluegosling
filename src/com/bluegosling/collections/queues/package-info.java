/**
 * New interfaces and implementations related to the standard Java {@link java.util.Queue}
 * interface.
 * 
 * <p>This package also includes a new related-but-different interface named
 * {@link com.bluegosling.collections.queues.PriorityQueue}. This interface is quite different from
 * the JRE's class of the same name in that it exposes additional operations of a classical
 * priority-queue ADT, mainly {@code reduce-key}. This decouples an element's priority from its
 * intrinsic value, making it more useful for certain types of graph algorithms.  
 */
package com.bluegosling.collections.queues;