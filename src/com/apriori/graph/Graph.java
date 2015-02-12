package com.apriori.graph;

import static com.apriori.concurrent.ThreadFactories.newGroupingDaemonThreadFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
public class Graph<T> {
   
   private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(
         newGroupingDaemonThreadFactory(Graph.class.getName()));
   private static final ComputationFactory DEFAULT_FACTORY = ComputationFactory.newBuilder()
         .setExecutor(DEFAULT_EXECUTOR).build();
   
   private final Node<T> resultNode;
   private final Set<Key<?>> inputs;
   
   Graph(Node<T> resultNode) {
      this.resultNode = resultNode;
      this.inputs = findAllInputs(resultNode);
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
    * Creates a new computation for this graph using a default {@linkplain ComputationFactory
    * factory}. The default factory uses no node decorators and executes the nodes using the
    * {@link #defaultExecutor()}.
    *
    * @return a new computation for this graph
    */
   public Computation<T> newComputation() {
      return DEFAULT_FACTORY.newComputation(this);
   }
   
   /**
    * The default thread pool executor, used by computations created with {@link #newComputation()}.
    * The default is a {@linkplain Executors#newCachedThreadPool() cached thread pool} with no
    * upper limit on threads so as to prevent any deadlock from the use of asynchronous inputs.
    *
    * @return the default thread pool executor
    */
   public static Executor defaultExecutor() {
      return DEFAULT_EXECUTOR;
   }
}
