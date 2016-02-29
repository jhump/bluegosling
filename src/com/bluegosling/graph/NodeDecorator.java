package com.bluegosling.graph;

import com.bluegosling.concurrent.futures.fluent.FluentFuture;

import java.util.concurrent.Callable;

/**
 * An object used to decorate a node operation. This can be used to add cross-cutting concerns to
 * all nodes in a graph, like for logging, metrics, etc. The decorator is invoked in the same thread
 * that calls {@link Computation#compute()}, so it can be used to propagate thread-local state from
 * the caller to worker threads that execute the node operations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface NodeDecorator {
   /**
    * Decorates the given callable, which is the operation (possibly already decorated) for the
    * given graph and node. The returned callable is a wrapper that can perform additional logic
    * before passing control to the given operation. 
    *
    * @param graph the graph being computed
    * @param node the node to decorate
    * @param operation the operation for the given node, possibly already wrapped by another
    *       decorator
    * @return a new callable that decorates the given one
    */
   <T> Callable<FluentFuture<T>> decorate(Graph<?> graph, Node<T> node,
         Callable<FluentFuture<T>> operation);
}
