package com.apriori.graph;

import static com.apriori.concurrent.ListenableFuture.dereference;
import static com.apriori.concurrent.ListenableFuture.join;

import static java.util.Objects.requireNonNull;

import com.apriori.concurrent.FutureVisitor;
import com.apriori.concurrent.ListenableFuture;
import com.apriori.concurrent.SettableRunnableFuture;
import com.apriori.reflect.TypeRef;
import com.apriori.util.Immediate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A computation graph. Each node in a computation graph represents an operation. The way nodes are
 * connected implicitly constrains parallelism. Computing the result of a graph will run the node
 * operations in parallel where it can, queuing up operations as their inputs are computed.
 * 
 * <p>Computation graphs are static. Once the graph is created, the nodes are fixed. So calculations
 * cannot change edges or create/remove nodes conditionally during execution. To execute graphs
 * where steps are conditional, you could check the condition in a node and just immediately return
 * a sentinel value if the node is not actually needed. Dependent node operations can look for the
 * sentinel value to decide if that value is available, or they can just re-check the condition to
 * see if they even want to examine the input.
 * 
 * <p>Defining the graph is done by defining the {@linkplain Node nodes} and edges.
 * 
 * @param <T> the type of value produced by the computation graph
 * 
 * @see Node#toGraph()
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class Graph<T> {
   
   private static final AtomicInteger THREAD_ID_SEQUENCE = new AtomicInteger();
   private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(r);
      t.setName(Graph.class.getName() + "-" + THREAD_ID_SEQUENCE.incrementAndGet());
      t.setDaemon(true);
      return t;
   });
   
   private final Node<T> resultNode;
   private final Executor executor;
   private final Set<Key<?>> inputs;
   
   Graph(Node<T> resultNode) {
      this(requireNonNull(resultNode), DEFAULT_EXECUTOR, findAllInputs(resultNode));
   }
   
   private Graph(Node<T> resultNode, Executor executor, Set<Key<?>> inputs) {
      this.resultNode = resultNode;
      this.executor = executor;
      this.inputs = inputs;
   }
   
   private static Set<Key<?>> findAllInputs(Node<?> node) {
      Set<Key<?>> inputs = new HashSet<>();
      findAllInputs(node, inputs);
      return Collections.unmodifiableSet(inputs);
   }
   
   private static void findAllInputs(Node<?> node, Set<Key<?>> soFar) {
      soFar.addAll(node.inputKeys());
      for (Node<?> dep : node.dependencies()) {
         findAllInputs(dep, soFar);
      }
   }
   
   /**
    * The input keys that must be bound to the graph in order to compute it. This set is computed
    * by looking at all nodes in the graph and finding all of the distinct input keys.
    *
    * @return the set of input keys that must be bound in order to compute the graph
    */
   public Set<Key<?>> inputKeys() {
      return inputs;
   }
   
   /**
    * The node in the graph that supplies the final product of the graph.
    *
    * @return the graph node that supplies the graph's result
    */
   public Node<T> resultNode() {
      return resultNode;
   }
   
   /**
    * Creates a new computation for this graph. The graph represents the computation's structure. An
    * individual computation is an object that can be invoked and produce a value.
    *
    * @return a new computation for this graph
    */
   public Computation<T> newComputation() {
      return newComputation(executor);
   }
   
   /**
    * Creates a new computation for this graph that uses the given executor to execute node
    * operations.
    * 
    * <p><strong>Note</strong>: Using constrained parallelism with nodes that have async inputs can
    * cause the computation to deadlock.
    *
    * @param e an executor
    * @return a new computation for this graph
    * 
    * @see #withDefaultExecutor(Executor)
    */
   public Computation<T> newComputation(Executor e) {
      return new Computation<>(this, requireNonNull(e));
   }
   
   /**
    * Creates a copy of this graph that uses a different default executor for new computations. This
    * is for computations that are created without specifying an executor to use.
    * 
    * <p><strong>Note</strong>: Using constrained parallelism with nodes that have async inputs can
    * cause the computation to deadlock.
    * 
    * <p>This can happen because nodes with async inputs are allowed to start before all of their
    * dependencies are complete. If such nodes get queued <em>before</em> their async dependencies,
    * they could block all threads of execution, waiting on dependencies to complete. If this
    * happens, the computation has deadlocked and won't finish.
    * 
    * <p>For this reason, the default executor (when not overridden with this method) does not
    * constrain parallelism.
    *
    * @param e the executor to use as the default executor for new computations
    * @return a new graph that uses the given executor as its default
    */
   public Graph<T> withDefaultExecutor(Executor e) {
      return new Graph<>(resultNode, requireNonNull(e), inputs);
   }

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
   public static class Computation<T> {
      private final Graph<T> graph;
      private final Executor executor;
      private final Map<Key<?>, ListenableFuture<?>> inputs;
      private ListenableFuture<T> result;
      
      Computation(Graph<T> graph, Executor executor) {
         this.graph = graph;
         this.executor = executor;
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
         bind(key, ListenableFuture.completedFuture(input));
         return this;
      }
      
      /**
       * Binds an async input to the computation. All inputs must be bound before the computation
       * can execute. If the input key has a {@linkplain Key#qualifier() qualifier}, you must use
       * {@link #bindAsyncInput(Key, ListenableFuture)} instead.
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
      public <U> Computation<T> bindAsyncInput(Class<U> type, ListenableFuture<U> input) {
         return bindAsyncInput(Key.of(type), input);
      }
      
      /**
       * Binds an async input to the computation. All inputs must be bound before the computation
       * can execute. If the input key has a {@linkplain Key#qualifier() qualifier}, you must use
       * {@link #bindAsyncInput(Key, ListenableFuture)} instead.
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
      public <U> Computation<T> bindAsyncInput(TypeRef<U> type, ListenableFuture<U> input) {
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
      public <U> Computation<T> bindAsyncInput(Key<U> type, ListenableFuture<U> input) {
         bind(type, input);
         return this;
      }
      
      private <U> void bind(Key<U> key, ListenableFuture<U> future) {
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
      public ListenableFuture<T> compute() {
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
         
         Map<Node<?>, ListenableFuture<?>> resolved = new HashMap<>();
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
      private <U> ListenableFuture<U> resolveFuture(Node<U> node,
            Map<Node<?>, ListenableFuture<?>> resolved) {
         @SuppressWarnings("unchecked") // we put the correct type into map, so trust what comes out
         ListenableFuture<U> alreadyResolved = (ListenableFuture<U>) resolved.get(node);
         if (alreadyResolved != null) {
            return alreadyResolved;
         }
         List<Node.Input<?>> nodeInputs = node.inputs();
         int len = nodeInputs.size();
         ListenableFuture<?> deps[] = new ListenableFuture<?>[len];
         List<ListenableFuture<?>> syncDeps = new ArrayList<>(len);
         for (int i = 0; i < len; i++) {
            Node.Input<?> in = nodeInputs.get(i);
            ListenableFuture<?> future;
            if (in.node() != null) {
               future = resolveFuture(in.node(), resolved);
            } else {
               future = inputs.get(in.key());
            }
            deps[i] = future;
            if (!in.isAsync()) {
               syncDeps.add(future);
            }
         }
         ListenableFuture<U> ret = dereference(chain(join(syncDeps), () -> {
            Object args[] = new Object[len];
            for (int i = 0; i < len; i++) {
               Node.Input<?> in = nodeInputs.get(i);
               if (in.isAsync()) {
                  args[i] = deps[i];
               } else if (in.isOptional()) {
                  args[i] = Immediate.fromCompletedFuture(deps[i]);
               } else {
                  if (deps[i].isFailed()) {
                     // we want to propagate the cause of failure, regardless
                     // if it's checked or not...
                     sneakyThrow(deps[i].getFailure());
                  }
                  args[i] = deps[i].getResult();
               }
            }
            return node.apply(args);
         }, executor));
         resolved.put(node, ret);
         return ret;
      }
   }
   
   @SuppressWarnings("unchecked") // we rely on unchecked exception to be sneaky
   static <X extends Throwable> void sneakyThrow(Throwable th) throws X {
      throw (X) th;
   }
   
   // similar to ListenableFuture.chainTo(...) except that it keeps cancellation status in sync
   // (e.g. canceling returned future also cancels input future) and the chained task can throw
   // a checked exception
   static <T, U> ListenableFuture<U> chain(ListenableFuture<T> src, Callable<U> op,
         Executor executor) {
      SettableRunnableFuture<U> result = new SettableRunnableFuture<U>(op) {
         // keep cancellation status in sync
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               src.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      src.visitWhenDone(new FutureVisitor<T>() {
         @Override
         public void successful(T t) {
            executor.execute(result);
         }

         @Override
         public void failed(Throwable failure) {
            result.setFailure(failure);
         }

         @Override
         public void cancelled() {
            result.cancel(false);
         }
      });
      return result;
   }
}
