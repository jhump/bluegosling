package com.bluegosling.graph;

import static com.bluegosling.concurrent.fluent.FluentFuture.dereference;
import static com.bluegosling.concurrent.fluent.FluentFuture.join;

import com.bluegosling.concurrent.FutureListener;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.SettableRunnableFluentFuture;
import com.bluegosling.reflect.TypeRef;
import com.bluegosling.util.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * A single instance of the computation graph. This represents a single invocation of the graph.
 * Executing the computation involves binding its required inputs and then calling the
 * {@link #compute()} method.
 * 
 * <p>This class is not thread safe. Multiple threads can safely share references to the graph
 * and then each create their own computations. Since the result of the computation is a future,
 * that too can be shared across threads. But the computation itself is not intended for use from
 * multiple threads.
 *
 * @param <T> the type of value produced by the computation
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class Computation<T> {
   private final Graph<T> graph;
   private final Executor executor;
   private final Iterable<NodeDecorator> decorators;
   private final Map<Key<?>, FluentFuture<?>> inputs;
   private FluentFuture<T> result;
   
   Computation(Graph<T> graph, Executor executor, Iterable<NodeDecorator> decorators) {
      this.graph = graph;
      this.executor = executor;
      this.decorators = decorators;
      this.inputs = new HashMap<>(graph.inputKeys().size() * 4 / 3);
   }
   
   /**
    * Returns the graph that defines the structure of this computation.
    *
    * @return the graph that defines the structure of this computation
    */
   public Graph<T> graph() {
      return graph;
   }
   
   /**
    * Binds an input value to the computation. All inputs must be bound before the computation
    * can execute. If the input key has a {@linkplain Key#qualifier() qualifier}, you must use
    * {@link #bindInput(Key, Object)} instead.
    * 
    * <p>This is shorthand for the following:<br>
    * {@code computation.bindInput(Key.of(type), input);}
    *
    * @param type the type of the input key
    * @param input the value to bind to this input
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if this computation has already been executed (computations
    *       can only be used once)
    * @throws IllegalArgumentException if the given type does not correspond to an input of this
    *       computation
    *       
    * @see Graph#inputKeys()
    */
   public <U> Computation<T> bindInput(Class<U> type, U input) {
      return bindInput(Key.of(type), input);
   }
   
   /**
    * Binds an input value to the computation. All inputs must be bound before the computation
    * can execute. If the input key has a {@linkplain Key#qualifier() qualifier}, you must use
    * {@link #bindInput(Key, Object)} instead.
    * 
    * <p>This is shorthand for the following:<br>
    * {@code computation.bindInput(Key.of(type), input);}
    *
    * @param type the type of the input key
    * @param input the value to bind to this input
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if this computation has already been executed (computations
    *       can only be used once)
    * @throws IllegalArgumentException if the given type does not correspond to an input of this
    *       computation
    *       
    * @see Graph#inputKeys()
    */
   public <U> Computation<T> bindInput(TypeRef<U> type, U input) {
      return bindInput(Key.of(type), input);
   }
   
   /**
    * Binds an input value to the computation. All inputs must be bound before the computation
    * can execute.
    *
    * @param key the input key
    * @param input the value to bind to this input
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if this computation has already been executed (computations
    *       can only be used once)
    * @throws IllegalArgumentException if the given key does not correspond to an input of this
    *       computation
    *       
    * @see Graph#inputKeys()
    */
   public <U> Computation<T> bindInput(Key<U> key, U input) {
      bind(key, FluentFuture.completedFuture(input));
      return this;
   }
   
   /**
    * Binds an async input to the computation. All inputs must be bound before the computation
    * can execute. If the input key has a {@linkplain Key#qualifier() qualifier}, you must use
    * {@link #bindAsyncInput(Key, FluentFuture)} instead.
    * 
    * <p>This is shorthand for the following:<br>
    * {@code computation.bindAsyncInput(Key.of(type), input);}
    *
    * @param type the type of the input key
    * @param input the future whose result will be bound to this input
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if this computation has already been executed (computations
    *       can only be used once)
    * @throws IllegalArgumentException if the given type does not correspond to an input of this
    *       computation
    *       
    * @see Graph#inputKeys()
    */
   public <U> Computation<T> bindAsyncInput(Class<U> type, FluentFuture<U> input) {
      return bindAsyncInput(Key.of(type), input);
   }
   
   /**
    * Binds an async input to the computation. All inputs must be bound before the computation
    * can execute. If the input key has a {@linkplain Key#qualifier() qualifier}, you must use
    * {@link #bindAsyncInput(Key, FluentFuture)} instead.
    * 
    * <p>This is shorthand for the following:<br>
    * {@code computation.bindAsyncInput(Key.of(type), input);}
    *
    * @param type the type of the input key
    * @param input the future whose result will be bound to this input
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if this computation has already been executed (computations
    *       can only be used once)
    * @throws IllegalArgumentException if the given type does not correspond to an input of this
    *       computation
    *       
    * @see Graph#inputKeys()
    */
   public <U> Computation<T> bindAsyncInput(TypeRef<U> type, FluentFuture<U> input) {
      return bindAsyncInput(Key.of(type), input);
   }
   
   /**
    * Binds an async input to the computation. All inputs must be bound before the computation
    * can execute.
    *
    * @param type the type of the input key
    * @param input the future whose result will be bound to this input
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if this computation has already been executed (computations
    *       can only be used once)
    * @throws IllegalArgumentException if the given key does not correspond to an input of this
    *       computation
    *       
    * @see Graph#inputKeys()
    */
   public <U> Computation<T> bindAsyncInput(Key<U> type, FluentFuture<U> input) {
      bind(type, input);
      return this;
   }
   
   private <U> void bind(Key<U> key, FluentFuture<U> future) {
      if (result != null) {
         throw new IllegalStateException("This computation has already been started. "
               + "Use a new computation to define different inputs and run it again.");
      }
      if (!graph.inputKeys().contains(key)) {
         throw new IllegalArgumentException("This computation has no input of type " + key);
      }
      inputs.put(key, future);
   }
   
   /**
    * Executes the computation. The returned future will complete when the computation is done.
    * The future will then yield the result of the computation.
    * 
    * <p>Computations are intended to be invoked once. If this method is called a subsequent
    * time, the same future is returned as from the initial call.
    *
    * @return a future that produces the result of the computation
    */
   public FluentFuture<T> compute() {
      if (result != null) {
         return result;
      }
      
      if (inputs.size() != graph.inputKeys().size()) {
         String message = graph.inputKeys().stream()
               .filter(k -> !inputs.containsKey(k))
               .map(Key::toString)
               .collect(Collectors.joining(", ",
                     "Missing input(s) required for computation: ", ""));
         throw new IllegalStateException(message);
      }
      
      Map<Node<?>, FluentFuture<?>> resolved = new HashMap<>();
      return result = resolveFuture(graph.resultNode(), resolved);
   }
   
   /**
    * Resolves the given node into a future value. The value may be an immediate (e.g. already
    * completed) future. If the node has already been visited and resolved, the previously
    * resolved value is returned.
    *
    * @param node the node to resolve
    * @param resolved cache of resolved nodes
    * @return the resolved future value of the given node
    */
   private <U> FluentFuture<U> resolveFuture(Node<U> node,
         Map<Node<?>, FluentFuture<?>> resolved) {
      @SuppressWarnings("unchecked") // we put the correct type into map, so trust what comes out
      FluentFuture<U> alreadyResolved = (FluentFuture<U>) resolved.get(node);
      if (alreadyResolved != null) {
         return alreadyResolved;
      }
      List<Node.Input<?>> nodeInputs = node.inputs();
      int len = nodeInputs.size();
      FluentFuture<?> deps[] = new FluentFuture<?>[len];
      List<FluentFuture<?>> synchronousDeps = new ArrayList<>(len);
      for (int i = 0; i < len; i++) {
         Node.Input<?> in = nodeInputs.get(i);
         FluentFuture<?> future;
         if (in.node() != null) {
            future = resolveFuture(in.node(), resolved);
         } else {
            future = inputs.get(in.key());
         }
         deps[i] = future;
         if (!in.isAsync()) {
            synchronousDeps.add(future);
         }
      }
      FluentFuture<U> ret = dereference(chain(join(synchronousDeps),
            decorate(node, decorators.iterator(), () -> node.apply(buildArgs(node, deps))),
            executor));
      resolved.put(node, ret);
      return ret;
   }
   
   private <U> Callable<FluentFuture<U>> decorate(Node<U> node, Iterator<NodeDecorator> iter,
         Callable<FluentFuture<U>> operation) {
      return iter.hasNext()
            ? iter.next().decorate(graph, node, decorate(node, iter, operation))
            : operation;
   }
   
   private Object[] buildArgs(Node<?> node, FluentFuture<?> deps[]) {
      int len = deps.length;
      List<Node.Input<?>> nodeInputs = node.inputs();
      Object args[] = new Object[len];
      for (int i = 0; i < len; i++) {
         Node.Input<?> in = nodeInputs.get(i);
         if (in.isAsync()) {
            args[i] = deps[i];
         } else if (in.isOptional()) {
            args[i] = Result.fromCompletedFuture(deps[i]);
         } else {
            if (deps[i].isFailed()) {
               // we want to propagate the cause of failure, regardless
               // if it's checked or not...
               sneakyThrow(deps[i].getFailure());
            }
            args[i] = deps[i].getResult();
         }
      }
      return args;
   }
   
   @SuppressWarnings("unchecked") // we rely on unchecked exception to be sneaky
   static <X extends Throwable> void sneakyThrow(Throwable th) throws X {
      throw (X) th;
   }

   /**
    * Chains the given operation to run once the source future completes. This is kind of like
    * {@link FluentFuture#chainTo(Callable, Executor)} with a couple of exceptions:
    * <ul>
    * <li>The cancellation status of the returned future is kept in sync with the source. So
    * cancelling the returned future will cancel the source and vice versa.</li>
    * <li>An exception in the source future isn't automatically propagated to the returned future.
    * Instead, even on failure, the given task is run once the source future completes.
    * </ul>
    *
    * @param src the source, or input, future
    * @param op the operation that is executed when the source completes
    * @param executor the executed used to run the operation
    * @return a future that represents the future result of the given operation
    */
   static <T, U> FluentFuture<U> chain(FluentFuture<T> src, Callable<U> op,
         Executor executor) {
      SettableRunnableFluentFuture<U> result = new SettableRunnableFluentFuture<U>(op) {
         // keep cancellation status in sync
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.cancel(mayInterrupt)) {
               src.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      src.addListener(FutureListener.forRunnable(result), executor);
      return result;
   }
}
