package com.bluegosling.graph;

import static com.bluegosling.concurrent.fluent.FluentFuture.completedFuture;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.graph.NodeOperations.Operation1;
import com.bluegosling.graph.NodeOperations.Operation10;
import com.bluegosling.graph.NodeOperations.Operation2;
import com.bluegosling.graph.NodeOperations.Operation3;
import com.bluegosling.graph.NodeOperations.Operation4;
import com.bluegosling.graph.NodeOperations.Operation5;
import com.bluegosling.graph.NodeOperations.Operation6;
import com.bluegosling.graph.NodeOperations.Operation7;
import com.bluegosling.graph.NodeOperations.Operation8;
import com.bluegosling.graph.NodeOperations.Operation9;
import com.bluegosling.reflect.TypeRef;
import com.bluegosling.result.Result;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * A node in a computation graph. Each node represents an operation that produces a value. The
 * operation may have inputs, which are edges that connect it to other nodes in the graph. The
 * output may be used as the input to another operation, which is also an edge that connects this
 * node to another. Up to ten inputs are allowed.
 *
 * @param <T> the type of value produced by this node's operation
 * 
 * @see #builder()
 * @see Graph
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: moar doc
public abstract class Node<T> {
   private final List<Input<?>> inputs;
   private final Set<Key<?>> inputKeys;
   private final Set<Node<?>> dependencies;
   
   Node(List<Input<?>> inputs) {
      this.inputs = Collections.unmodifiableList(inputs);
      this.inputKeys = Collections.unmodifiableSet(
            inputs.stream().map(Input::key).filter(k -> k != null).collect(toSet()));
      this.dependencies = Collections.unmodifiableSet(
            inputs.stream().map(Input::node).filter(n -> n != null).collect(toSet()));
   }

   /**
    * Returns the list of input edges to this node. An input can be another {@link Node} or a
    * {@link Key}
    *
    * @return the list of input edges
    */
   public List<Input<?>> inputs() {
      return inputs;
   }
   
   /**
    * The distinct set of inputs that are defined as {@link Key}s. These inputs are bound to a
    * value when the computation graph is {@linkplain Computation executed}.
    *
    * @return the set of {@link Key} inputs
    */
   public Set<Key<?>> inputKeys() {
      return inputKeys;
   }
   
   /**
    * The distinct set of other nodes that this node depends on. These are inputs that are linked
    * to other nodes in the graph.
    *
    * @return the set of {@link Node} inputs
    */
   public Set<Node<?>> dependencies() {
      return dependencies;
   }

   /**
    * Returns a new node that is the result of applying the given function to the values produced by
    * this node. This implicitly adds a new node to the graph whose sole input is this node.
    *
    * @param fn the function that produces values for the new node, given products from this node
    * @return a new node that is the result of applying the given function to the values produced
    *       by this node
    */
   public <U> Node<U> andThen(Function<T, U> fn) {
      return new Node1<>(new RequiredInput<>(this), fn::apply);
   }
   
   /**
    * Returns a graph consisting of this node and its transitive dependencies. Executing the graph
    * yields values that are produced by this node.
    *
    * @return a graph that yields values produced by this node
    */
   public Graph<T> toGraph() {
      return new Graph<>(this);
   }
   
   /**
    * Applies the given input arguments to the operation this node represents and returns the
    * result of the operation.
    *
    * @param args input values
    * @return the result of this node's operation with the given inputs
    */
   abstract FluentFuture<T> apply(Object args[]) throws Exception;

   /**
    * Returns a new node builder.
    *
    * @return a new node builder
    */
   public static NodeBuilder0 builder() {
      return new NodeBuilder0();
   }

   /**
    * An input to a node. An input is an incoming edge in the graph. The other side of the edge can
    * either be another node or a key. Keys represent inputs to the graph that must be supplied when
    * the graph is executed.
    * 
    * <p>An input can be optional. The node's operation is invoked even if computation of the
    * optional input failed. The node's operation accepts a {@link Result} for optional inputs,
    * which can represent a successful value for the input or a failure.
    * 
    * <p>An input can also be asynchronous, which allows the execution one node's operation to run
    * concurrently with the computation of one of its input. The node's operation accepts a future
    * for the input if it is asynchronous. (Asynchronous inputs are implicitly optional since the
    * future could complete unsuccessfully.)
    *
    * @param <T> the type of the input
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static abstract class Input<T> {
      final Key<?> key;
      final Node<?> node;
      
      Input(Key<?> key) {
         this.key = requireNonNull(key);
         this.node = null;
      }
      
      Input(Node<?> node) {
         this.key = null;
         this.node = requireNonNull(node);
      }

      /**
       * Returns the key associated with this input. If this input edge is connected to a node, not
       * a key, then {@code null} is returned.
       * 
       * <p>The actual type of the returned key depends on whether it is asynchronous or optional.
       * If it is asynchronous or optional, then this input's type will be
       * {@code FluentFuture<V>} where the key's type will be {@code V}. But if the input is
       * synchronous and required, the type of the key is the same as the type of the input.
       *
       * @return the key associated with this input or {@code null} if not associated with a key
       * @see #node()
       */
      public Key<?> key() {
         return key;
      }
      
      /**
       * Returns the node associated with this input. If this input edge is associated with a key,
       * not a node, then {@code null} is returned.
       * 
       * <p>The actual type of the returned node depends on whether it is asynchronous or optional.
       * If it is asynchronous or optional, then this input's type will be
       * {@code FluentFuture<V>} where the node's type will be {@code V}. But if the input is
       * synchronous and required, the type of the node is the same as the type of the input.
       *
       * @return the node associated with this input or {@code null} if not associated with a node
       * @see #key()
       */
      public Node<?> node() {
         return node;
      }
      
      /**
       * Returns true if this is an asynchronous input or not. An asynchronous input's type is
       * {@link FluentFuture}.
       *
       * @return true if this is an asynchronous input or not
       */
      public abstract boolean isAsync();

      /**
       * Returns true if this is an optional input or not. An optional input's type is
       * {@link Result}, so that the node can see if the associated operation was successful or
       * not. 
       *
       * @return true if this is an optional input or not
       */
      public abstract boolean isOptional();

      @Override
      public boolean equals(Object o) {
         if (o instanceof Input) {
            Input<?> other = (Input<?>) o;
            return Objects.equals(key, other.key) && Objects.equals(node, other.node)
                  && isAsync() == other.isAsync();
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         int code = (node != null ? node : key).hashCode();
         return isAsync() ? ~code : code;
      }
      
      @Override
      public String toString() {
         Object i = node != null ? node : key;
         if (isAsync()) {
            return "Async[" + i + "]";
         } else {
            return i.toString();
         }
      }
   }
   
   /**
    * A required input.
    *
    * @param <T> the type of the input
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class RequiredInput<T> extends Input<T> {

      RequiredInput(Key<T> key) {
         super(key);
      }

      RequiredInput(Node<T> node) {
         super(node);
      }

      @Override
      public boolean isAsync() {
         return false;
      }

      @Override
      public boolean isOptional() {
         return false;
      }
   }
   
   /**
    * An asynchronous input.
    *
    * @param <T> the type of the input
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncInput<T> extends Input<FluentFuture<T>> {

      AsyncInput(Key<T> key) {
         super(key);
      }

      AsyncInput(Node<T> node) {
         super(node);
      }

      @Override
      public boolean isAsync() {
         return true;
      }
      
      @Override
      public boolean isOptional() {
         return false;
      }
   }
   
   /**
    * An optional input.
    *
    * @param <T> the type of the input
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class OptionalInput<T> extends Input<Result<T, ?>> {

      OptionalInput(Key<T> key) {
         super(key);
      }

      OptionalInput(Node<T> node) {
         super(node);
      }

      @Override
      public boolean isAsync() {
         return false;
      }
      
      @Override
      public boolean isOptional() {
         return true;
      }
   }
   
   /**
    * A node with an immediate result that accepts no inputs.
    *
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node0<T> extends Node<T> {
      private final Callable<T> op;
      
      Node0(Callable<T> op) {
         super(Collections.emptyList());
         this.op = op;
      }

      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 0;
         return completedFuture(op.call());
      }
   }
   
   /**
    * A node with an async (future) result that accepts no inputs.
    *
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode0<T> extends Node<T> {
      private final Callable<FluentFuture<T>> op;
      
      AsyncNode0(Callable<FluentFuture<T>> op) {
         super(Collections.emptyList());
         this.op = op;
      }

      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 0;
         return op.call();
      }
   }

   /**
    * A node with an immediate result that accepts one input.
    *
    * @param <A> the input type
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node1<A, T> extends Node<T> {
      private final Operation1<A, T> op;
      
      Node1(Input<?> input, Operation1<A, T> op) {
         super(Collections.singletonList(input));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 1;
         return completedFuture(op.execute((A) args[0]));
      }
   }
   
   /**
    * A node with an async (future) result that accepts one input.
    *
    * @param <A> the input type
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode1<A, T> extends Node<T> {
      private final Operation1<A, FluentFuture<T>> op;
      
      AsyncNode1(Input<?> input, Operation1<A, FluentFuture<T>> op) {
         super(Collections.singletonList(input));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 1;
         return op.execute((A) args[0]);
      }
   }

   /**
    * A node with an immediate result that accepts two inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node2<A, B, T> extends Node<T> {
      private final Operation2<A, B, T> op;
      
      Node2(Input<?> input1, Input<?> input2, Operation2<A, B, T> op) {
         super(Arrays.asList(input1, input2));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 2;
         return completedFuture(op.execute((A) args[0], (B) args[1]));
      }
   }
   
   /**
    * A node with an async (future) result that accepts two inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode2<A, B, T> extends Node<T> {
      private final Operation2<A, B, FluentFuture<T>> op;
      
      AsyncNode2(Input<?> input1, Input<?> input2, Operation2<A, B, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 2;
         return op.execute((A) args[0], (B) args[1]);
      }
   }
   
   /**
    * A node with an immediate result that accepts three inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node3<A, B, C, T> extends Node<T> {
      private final Operation3<A, B, C, T> op;
      
      Node3(Input<?> input1, Input<?> input2, Input<?> input3, Operation3<A, B, C, T> op) {
         super(Arrays.asList(input1, input2, input3));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 3;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2]));
      }
   }
   
   /**
    * A node with an async (future) result that accepts three inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode3<A, B, C, T> extends Node<T> {
      private final Operation3<A, B, C, FluentFuture<T>> op;
      
      AsyncNode3(Input<?> input1, Input<?> input2, Input<?> input3,
            Operation3<A, B, C, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 3;
         return op.execute((A) args[0], (B) args[1], (C) args[2]);
      }
   }
   
   /**
    * A node with an immediate result that accepts four inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node4<A, B, C, D, T> extends Node<T> {
      private final Operation4<A, B, C, D, T> op;
      
      Node4(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Operation4<A, B, C, D, T> op) {
         super(Arrays.asList(input1, input2, input3, input4));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 4;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3]));
      }
   }
   
   /**
    * A node with an async (future) result that accepts four inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode4<A, B, C, D, T> extends Node<T> {
      private final Operation4<A, B, C, D, FluentFuture<T>> op;
      
      AsyncNode4(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Operation4<A, B, C, D, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 4;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3]);
      }
   }

   /**
    * A node with an immediate result that accepts five inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node5<A, B, C, D, E, T> extends Node<T> {
      private final Operation5<A, B, C, D, E, T> op;
      
      Node5(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4, Input<?> input5,
            Operation5<A, B, C, D, E, T> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 5;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3],
               (E) args[4]));
      }
   }
   
   /**
    * A node with an async (future) result that accepts five inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode5<A, B, C, D, E, T> extends Node<T> {
      private final Operation5<A, B, C, D, E, FluentFuture<T>> op;
      
      AsyncNode5(Input<?> input1, Input<?> input2, Input<?> input3,
            Input<?> input4, Input<?> input5, Operation5<A, B, C, D, E, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 5;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3], (E) args[4]);
      }
   }

   /**
    * A node with an immediate result that accepts six inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node6<A, B, C, D, E, F, T> extends Node<T> {
      private final Operation6<A, B, C, D, E, F, T> op;
      
      Node6(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4, Input<?> input5,
            Input<?> input6, Operation6<A, B, C, D, E, F, T> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 6;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3],
               (E) args[4], (F) args[5]));
      }
   }

   /**
    * A node with an async (future) result that accepts six inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode6<A, B, C, D, E, F, T> extends Node<T> {
      private final Operation6<A, B, C, D, E, F, FluentFuture<T>> op;
      
      AsyncNode6(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Input<?> input5, Input<?> input6,
            Operation6<A, B, C, D, E, F, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 6;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3], (E) args[4],
               (F) args[5]);
      }
   }
   
   /**
    * A node with an immediate result that accepts seven inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node7<A, B, C, D, E, F, G, T> extends Node<T> {
      private final Operation7<A, B, C, D, E, F, G, T> op;
      
      Node7(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4, Input<?> input5,
            Input<?> input6, Input<?> input7, Operation7<A, B, C, D, E, F, G, T> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 7;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3],
               (E) args[4], (F) args[5], (G) args[6]));
      }
   }

   /**
    * A node with an async (future) result that accepts seven inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode7<A, B, C, D, E, F, G, T> extends Node<T> {
      private final Operation7<A, B, C, D, E, F, G, FluentFuture<T>> op;
      
      AsyncNode7(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Input<?> input5, Input<?> input6, Input<?> input7,
            Operation7<A, B, C, D, E, F, G, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 7;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3], (E) args[4],
               (F) args[5], (G) args[6]);
      }
   }
   
   /**
    * A node with an immediate result that accepts eight inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node8<A, B, C, D, E, F, G, H, T> extends Node<T> {
      private final Operation8<A, B, C, D, E, F, G, H, T> op;
      
      Node8(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4, Input<?> input5,
            Input<?> input6, Input<?> input7, Input<?> input8,
            Operation8<A, B, C, D, E, F, G, H, T> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7,
               input8));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 8;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3],
               (E) args[4], (F) args[5], (G) args[6], (H) args[7]));
      }
   }

   /**
    * A node with an async (future) result that accepts eight inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode8<A, B, C, D, E, F, G, H, T> extends Node<T> {
      private final Operation8<A, B, C, D, E, F, G, H, FluentFuture<T>> op;
      
      AsyncNode8(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Input<?> input5, Input<?> input6, Input<?> input7, Input<?> input8,
            Operation8<A, B, C, D, E, F, G, H, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7, input8));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 8;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3], (E) args[4],
               (F) args[5], (G) args[6], (H) args[7]);
      }
   }
   
   /**
    * A node with an immediate result that accepts nine inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node9<A, B, C, D, E, F, G, H, I, T> extends Node<T> {
      private final Operation9<A, B, C, D, E, F, G, H, I, T> op;
      
      Node9(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4, Input<?> input5,
            Input<?> input6, Input<?> input7, Input<?> input8, Input<?> input9,
            Operation9<A, B, C, D, E, F, G, H, I, T> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7, input8,
               input9));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 9;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3],
               (E) args[4], (F) args[5], (G) args[6], (H) args[7], (I) args[8]));
      }
   }

   /**
    * A node with an async (future) result that accepts nine inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode9<A, B, C, D, E, F, G, H, I, T> extends Node<T> {
      private final Operation9<A, B, C, D, E, F, G, H, I, FluentFuture<T>> op;
      
      AsyncNode9(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Input<?> input5, Input<?> input6, Input<?> input7, Input<?> input8, Input<?> input9,
            Operation9<A, B, C, D, E, F, G, H, I, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7, input8,
               input9));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 9;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3], (E) args[4],
               (F) args[5], (G) args[6], (H) args[7], (I) args[8]);
      }
   }
   
   /**
    * A node with an immediate result that accepts ten inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <J> the type of the tenth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Node10<A, B, C, D, E, F, G, H, I, J, T> extends Node<T> {
      private final Operation10<A, B, C, D, E, F, G, H, I, J, T> op;
      
      Node10(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4, Input<?> input5,
            Input<?> input6, Input<?> input7, Input<?> input8, Input<?> input9, Input<?> input10,
            Operation10<A, B, C, D, E, F, G, H, I, J, T> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7, input8, input9,
               input10));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 10;
         return completedFuture(op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3],
               (E) args[4], (F) args[5], (G) args[6], (H) args[7], (I) args[8], (J) args[9]));
      }
   }
   
   /**
    * A node with an async (future) result that accepts ten inputs.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <J> the type of the tenth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class AsyncNode10<A, B, C, D, E, F, G, H, I, J, T> extends Node<T> {
      private final Operation10<A, B, C, D, E, F, G, H, I, J, FluentFuture<T>> op;
      
      AsyncNode10(Input<?> input1, Input<?> input2, Input<?> input3, Input<?> input4,
            Input<?> input5, Input<?> input6, Input<?> input7, Input<?> input8, Input<?> input9,
            Input<?> input10, Operation10<A, B, C, D, E, F, G, H, I, J, FluentFuture<T>> op) {
         super(Arrays.asList(input1, input2, input3, input4, input5, input6, input7, input8, input9,
               input10));
         this.op = op;
      }

      @SuppressWarnings("unchecked") // trust caller, in Graph, to pass correct args
      @Override
      FluentFuture<T> apply(Object args[]) throws Exception {
         assert args.length == 10;
         return op.execute((A) args[0], (B) args[1], (C) args[2], (D) args[3], (E) args[4],
               (F) args[5], (G) args[6], (H) args[7], (I) args[8], (J) args[9]);
      }
   }
   
   /**
    * A factory for nodes with zero or more inputs.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder0 {
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts no arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Callable<T> op) {
         return new Node0<>(op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts no arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Callable<FluentFuture<T>> op) {
         return new AsyncNode0<>(op);
      }

      /**
       * Adds the first input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <T> NodeBuilder1<T> withInput(Class<T> input) {
         return new NodeBuilder1<>(new RequiredInput<>(Key.of(input)));
      }
      
      /**
       * Adds the first input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <T> NodeBuilder1<T> withInput(TypeRef<T> input) {
         return new NodeBuilder1<>(new RequiredInput<>(Key.of(input)));
      }
      
      /**
       * Adds the first input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <T> NodeBuilder1<T> withInput(Key<T> input) {
         return new NodeBuilder1<>(new RequiredInput<>(input));
      }
      
      /**
       * Adds the first input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <T> NodeBuilder1<T> dependingOn(Node<T> input) {
         return new NodeBuilder1<>(new RequiredInput<>(input));
      }
      
      public <T> NodeBuilder1<FluentFuture<T>> withAsyncInput(Class<T> input) {
         return new NodeBuilder1<>(new AsyncInput<>(Key.of(input)));
      }
      
      public <T> NodeBuilder1<FluentFuture<T>> withAsyncInput(TypeRef<T> input) {
         return new NodeBuilder1<>(new AsyncInput<>(Key.of(input)));
      }
      
      public <T> NodeBuilder1<FluentFuture<T>> withAsyncInput(Key<T> input) {
         return new NodeBuilder1<>(new AsyncInput<>(input));
      }
      
      public <T> NodeBuilder1<FluentFuture<T>> dependingAsyncOn(Node<T> input) {
         return new NodeBuilder1<>(new AsyncInput<>(input));
      }

      public <T> NodeBuilder1<Result<T, ?>> withOptionalInput(Class<T> input) {
         return new NodeBuilder1<>(new OptionalInput<>(Key.of(input)));
      }
      
      public <T> NodeBuilder1<Result<T, ?>> withOptionalInput(TypeRef<T> input) {
         return new NodeBuilder1<>(new OptionalInput<>(Key.of(input)));
      }
      
      public <T> NodeBuilder1<Result<T, ?>> withOptionalInput(Key<T> input) {
         return new NodeBuilder1<>(new OptionalInput<>(input));
      }
      
      public <T> NodeBuilder1<Result<T, ?>> dependingOptionallyOn(Node<T> input) {
         return new NodeBuilder1<>(new OptionalInput<>(input));
      }
   }
   
   /**
    * A factory for nodes with one or more inputs.
    * 
    * @param <A> the type of the first input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder1<A> {
      private final Input<A> input;
      
      NodeBuilder1(Input<A> input) {
         this.input = input;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts one argument and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation1<A, T> op) {
         return new Node1<>(input, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts one argument and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation1<A, FluentFuture<T>> op) {
         return new AsyncNode1<>(input, op);
      }

      /**
       * Adds the second input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param in the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <B> NodeBuilder2<A, B> withInput(Class<B> in) {
         return new NodeBuilder2<>(input, new RequiredInput<>(Key.of(in)));
      }
      
      /**
       * Adds the second input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param in the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <B> NodeBuilder2<A, B> withInput(TypeRef<B> in) {
         return new NodeBuilder2<>(input, new RequiredInput<>(Key.of(in)));
      }
      
      /**
       * Adds the second input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param in the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <B> NodeBuilder2<A, B> withInput(Key<B> in) {
         return new NodeBuilder2<>(input, new RequiredInput<>(in));
      }
      
      /**
       * Adds the second input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param in the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <B> NodeBuilder2<A, B> dependingOn(Node<B> in) {
         return new NodeBuilder2<>(input, new RequiredInput<>(in));
      }
      
      public <B> NodeBuilder2<A, FluentFuture<B>> withAsyncInput(Class<B> in) {
         return new NodeBuilder2<>(input, new AsyncInput<>(Key.of(in)));
      }
      
      public <B> NodeBuilder2<A, FluentFuture<B>> withAsyncInput(TypeRef<B> in) {
         return new NodeBuilder2<>(input, new AsyncInput<>(Key.of(in)));
      }
      
      public <B> NodeBuilder2<A, FluentFuture<B>> withAsyncInput(Key<B> in) {
         return new NodeBuilder2<>(input, new AsyncInput<>(in));
      }
      
      public <B> NodeBuilder2<A, FluentFuture<B>> dependingAsyncOn(Node<B> in) {
         return new NodeBuilder2<>(input, new AsyncInput<>(in));
      }

      public <B> NodeBuilder2<A, Result<B, ?>> withOptionalInput(Class<B> in) {
         return new NodeBuilder2<>(input, new OptionalInput<>(Key.of(in)));
      }
      
      public <B> NodeBuilder2<A, Result<B, ?>> withOptionalInput(TypeRef<B> in) {
         return new NodeBuilder2<>(input, new OptionalInput<>(Key.of(in)));
      }
      
      public <B> NodeBuilder2<A, Result<B, ?>> withOptionalInput(Key<B> in) {
         return new NodeBuilder2<>(input, new OptionalInput<>(in));
      }
      
      public <B> NodeBuilder2<A, Result<B, ?>> dependingOptionallyOn(Node<B> in) {
         return new NodeBuilder2<>(input, new OptionalInput<>(in));
      }
   }
   
   /**
    * A factory for nodes with two or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder2<A, B> {
      private final Input<A> input1;
      private final Input<B> input2;
      
      NodeBuilder2(Input<A> input1, Input<B> input2) {
         this.input1 = input1;
         this.input2 = input2;
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts two arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation2<A, B, T> op) {
         return new Node2<>(input1, input2, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts two arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation2<A, B, FluentFuture<T>> op) {
         return new AsyncNode2<>(input1, input2, op);
      }

      /**
       * Adds the third input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <C> NodeBuilder3<A, B, C> withInput(Class<C> input) {
         return new NodeBuilder3<>(input1, input2, new RequiredInput<>(Key.of(input)));
      }
      
      /**
       * Adds the third input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <C> NodeBuilder3<A, B, C> withInput(TypeRef<C> input) {
         return new NodeBuilder3<>(input1, input2, new RequiredInput<>(Key.of(input)));
      }
      
      /**
       * Adds the third input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <C> NodeBuilder3<A, B, C> withInput(Key<C> input) {
         return new NodeBuilder3<>(input1, input2, new RequiredInput<>(input));
      }
      
      /**
       * Adds the third input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <C> NodeBuilder3<A, B, C> dependingOn(Node<C> input) {
         return new NodeBuilder3<>(input1, input2, new RequiredInput<>(input));
      }

      public <C> NodeBuilder3<A, B, FluentFuture<C>> withAsyncInput(Class<C> input) {
         return new NodeBuilder3<>(input1, input2, new AsyncInput<>(Key.of(input)));
      }
      
      public <C> NodeBuilder3<A, B, FluentFuture<C>> withAsyncInput(TypeRef<C> input) {
         return new NodeBuilder3<>(input1, input2, new AsyncInput<>(Key.of(input)));
      }
      
      public <C> NodeBuilder3<A, B, FluentFuture<C>> withAsyncInput(Key<C> input) {
         return new NodeBuilder3<>(input1, input2, new AsyncInput<>(input));
      }
      
      public <C> NodeBuilder3<A, B, FluentFuture<C>> dependingAsyncOn(Node<C> input) {
         return new NodeBuilder3<>(input1, input2, new AsyncInput<>(input));
      }

      public <C> NodeBuilder3<A, B, Result<C, ?>> withOptionalInput(Class<C> input) {
         return new NodeBuilder3<>(input1, input2, new OptionalInput<>(Key.of(input)));
      }
      
      public <C> NodeBuilder3<A, B, Result<C, ?>> withOptionalInput(TypeRef<C> input) {
         return new NodeBuilder3<>(input1, input2, new OptionalInput<>(Key.of(input)));
      }
      
      public <C> NodeBuilder3<A, B, Result<C, ?>> withOptionalInput(Key<C> input) {
         return new NodeBuilder3<>(input1, input2, new OptionalInput<>(input));
      }
      
      public <C> NodeBuilder3<A, B, Result<C, ?>> dependingOptionallyOn(Node<C> input) {
         return new NodeBuilder3<>(input1, input2, new OptionalInput<>(input));
      }   
   }
   
   /**
    * A factory for nodes with three or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder3<A, B, C> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      
      NodeBuilder3(Input<A> input1, Input<B> input2, Input<C> input3) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts three arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation3<A, B, C, T> op) {
         return new Node3<>(input1, input2, input3, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts three arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation3<A, B, C, FluentFuture<T>> op) {
         return new AsyncNode3<>(input1, input2, input3, op);
      }

      /**
       * Adds the fourth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <D> NodeBuilder4<A, B, C, D> withInput(Class<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the fourth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <D> NodeBuilder4<A, B, C, D> withInput(TypeRef<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the fourth input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <D> NodeBuilder4<A, B, C, D> withInput(Key<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new RequiredInput<>(input));
      }

      /**
       * Adds the fourth input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <D> NodeBuilder4<A, B, C, D> dependingOn(Node<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new RequiredInput<>(input));
      }

      public <D> NodeBuilder4<A, B, C, FluentFuture<D>> withAsyncInput(Class<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new AsyncInput<>(Key.of(input)));
      }

      public <D> NodeBuilder4<A, B, C, FluentFuture<D>> withAsyncInput(TypeRef<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new AsyncInput<>(Key.of(input)));
      }

      public <D> NodeBuilder4<A, B, C, FluentFuture<D>> withAsyncInput(Key<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new AsyncInput<>(input));
      }

      public <D> NodeBuilder4<A, B, C, FluentFuture<D>> dependingAsyncOn(Node<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new AsyncInput<>(input));
      }

      public <D> NodeBuilder4<A, B, C, Result<D, ?>> withOptionalInput(Class<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new OptionalInput<>(Key.of(input)));
      }

      public <D> NodeBuilder4<A, B, C, Result<D, ?>> withOptionalInput(TypeRef<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new OptionalInput<>(Key.of(input)));
      }

      public <D> NodeBuilder4<A, B, C, Result<D, ?>> withOptionalInput(Key<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new OptionalInput<>(input));
      }

      public <D> NodeBuilder4<A, B, C, Result<D, ?>> dependingOptionallyOn(Node<D> input) {
         return new NodeBuilder4<>(input1, input2, input3, new OptionalInput<>(input));
      }
   }

   /**
    * A factory for nodes with four or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder4<A, B, C, D> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      
      NodeBuilder4(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts four arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation4<A, B, C, D, T> op) {
         return new Node4<>(input1, input2, input3, input4, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts four arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation4<A, B, C, D, FluentFuture<T>> op) {
         return new AsyncNode4<>(input1, input2, input3, input4, op);
      }

      /**
       * Adds the fifth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <E> NodeBuilder5<A, B, C, D, E> withInput(Class<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the fifth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <E> NodeBuilder5<A, B, C, D, E> withInput(TypeRef<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the fifth input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <E> NodeBuilder5<A, B, C, D, E> withInput(Key<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new RequiredInput<>(input));
      }

      /**
       * Adds the fifth input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <E> NodeBuilder5<A, B, C, D, E> dependingOn(Node<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new RequiredInput<>(input));
      }

      public <E> NodeBuilder5<A, B, C, D, FluentFuture<E>> withAsyncInput(Class<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new AsyncInput<>(Key.of(input)));
      }

      public <E> NodeBuilder5<A, B, C, D, FluentFuture<E>> withAsyncInput(TypeRef<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new AsyncInput<>(Key.of(input)));
      }

      public <E> NodeBuilder5<A, B, C, D, FluentFuture<E>> withAsyncInput(Key<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new AsyncInput<>(input));
      }

      public <E> NodeBuilder5<A, B, C, D, FluentFuture<E>> dependingAsyncOn(Node<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new AsyncInput<>(input));
      } 

      public <E> NodeBuilder5<A, B, C, D, Result<E, ?>> withOptionalInput(Class<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new OptionalInput<>(Key.of(input)));
      }

      public <E> NodeBuilder5<A, B, C, D, Result<E, ?>> withOptionalInput(TypeRef<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new OptionalInput<>(Key.of(input)));
      }

      public <E> NodeBuilder5<A, B, C, D, Result<E, ?>> withOptionalInput(Key<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new OptionalInput<>(input));
      }

      public <E> NodeBuilder5<A, B, C, D, Result<E, ?>> dependingOptionallyOn(Node<E> input) {
         return new NodeBuilder5<>(input1, input2, input3, input4, new OptionalInput<>(input));
      } 
   }

   /**
    * A factory for nodes with five or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder5<A, B, C, D, E> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      private final Input<E> input5;
      
      NodeBuilder5(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4,
            Input<E> input5) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
         this.input5 = input5;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts five arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation5<A, B, C, D, E, T> op) {
         return new Node5<>(input1, input2, input3, input4, input5, op);
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts five arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation5<A, B, C, D, E, FluentFuture<T>> op) {
         return new AsyncNode5<>(input1, input2, input3, input4, input5, op);
      }

      /**
       * Adds the sixth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <F> NodeBuilder6<A, B, C, D, E, F> withInput(Class<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the sixth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <F> NodeBuilder6<A, B, C, D, E, F> withInput(TypeRef<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the sixth input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <F> NodeBuilder6<A, B, C, D, E, F> withInput(Key<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new RequiredInput<>(input));
      }

      /**
       * Adds the sixth input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <F> NodeBuilder6<A, B, C, D, E, F> dependingOn(Node<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new RequiredInput<>(input));
      }

      public <F> NodeBuilder6<A, B, C, D, E, FluentFuture<F>> withAsyncInput(Class<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new AsyncInput<>(Key.of(input)));
      }

      public <F> NodeBuilder6<A, B, C, D, E, FluentFuture<F>> withAsyncInput(TypeRef<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new AsyncInput<>(Key.of(input)));
      }

      public <F> NodeBuilder6<A, B, C, D, E, FluentFuture<F>> withAsyncInput(Key<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5, new AsyncInput<>(input));
      }

      public <F> NodeBuilder6<A, B, C, D, E, FluentFuture<F>> dependingAsyncOn(Node<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5, new AsyncInput<>(input));
      }

      public <F> NodeBuilder6<A, B, C, D, E, Result<F, ?>> withOptionalInput(Class<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new OptionalInput<>(Key.of(input)));
      }

      public <F> NodeBuilder6<A, B, C, D, E, Result<F, ?>> withOptionalInput(TypeRef<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new OptionalInput<>(Key.of(input)));
      }

      public <F> NodeBuilder6<A, B, C, D, E, Result<F, ?>> withOptionalInput(Key<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new OptionalInput<>(input));
      }

      public <F> NodeBuilder6<A, B, C, D, E, Result<F, ?>> dependingOptionallyOn(Node<F> input) {
         return new NodeBuilder6<>(input1, input2, input3, input4, input5,
               new OptionalInput<>(input));
      }
   }
   
   /**
    * A factory for nodes with six or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder6<A, B, C, D, E, F> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      private final Input<E> input5;
      private final Input<F> input6;
      
      NodeBuilder6(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4,
            Input<E> input5, Input<F> input6) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
         this.input5 = input5;
         this.input6 = input6;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts six arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation6<A, B, C, D, E, F, T> op) {
         return new Node6<>(input1, input2, input3, input4, input5, input6, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts six arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation6<A, B, C, D, E, F, FluentFuture<T>> op) {
         return new AsyncNode6<>(input1, input2, input3, input4, input5, input6, op);
      }

      /**
       * Adds the seventh input to this node. The input is a required, synchronous input of the
       * given type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <G> NodeBuilder7<A, B, C, D, E, F, G> withInput(Class<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the seventh input to this node. The input is a required, synchronous input of the
       * given type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <G> NodeBuilder7<A, B, C, D, E, F, G> withInput(TypeRef<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the seventh input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <G> NodeBuilder7<A, B, C, D, E, F, G> withInput(Key<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new RequiredInput<>(input));
      }

      /**
       * Adds the seventh input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <G> NodeBuilder7<A, B, C, D, E, F, G> dependingOn(Node<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new RequiredInput<>(input));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, FluentFuture<G>> withAsyncInput(
            Class<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new AsyncInput<>(Key.of(input)));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, FluentFuture<G>> withAsyncInput(
            TypeRef<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new AsyncInput<>(Key.of(input)));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, FluentFuture<G>> withAsyncInput(
            Key<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new AsyncInput<>(input));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, FluentFuture<G>> dependingAsyncOn(
            Node<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new AsyncInput<>(input));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, Result<G, ?>> withOptionalInput(Class<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new OptionalInput<>(Key.of(input)));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, Result<G, ?>> withOptionalInput(TypeRef<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new OptionalInput<>(Key.of(input)));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, Result<G, ?>> withOptionalInput(Key<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new OptionalInput<>(input));
      }

      public <G> NodeBuilder7<A, B, C, D, E, F, Result<G, ?>> dependingOptionallyOn(Node<G> input) {
         return new NodeBuilder7<>(input1, input2, input3, input4, input5, input6,
               new OptionalInput<>(input));
      }
   }

   /**
    * A factory for nodes with seven or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder7<A, B, C, D, E, F, G> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      private final Input<E> input5;
      private final Input<F> input6;
      private final Input<G> input7;
      
      NodeBuilder7(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4,
            Input<E> input5, Input<F> input6, Input<G> input7) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
         this.input5 = input5;
         this.input6 = input6;
         this.input7 = input7;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts seven arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation7<A, B, C, D, E, F, G, T> op) {
         return new Node7<>(input1, input2, input3, input4, input5, input6, input7, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts seven arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation7<A, B, C, D, E, F, G, FluentFuture<T>> op) {
         return new AsyncNode7<>(input1, input2, input3, input4, input5, input6, input7, op);
      }

      /**
       * Adds the eighth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <H> NodeBuilder8<A, B, C, D, E, F, G, H> withInput(Class<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the eighth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <H> NodeBuilder8<A, B, C, D, E, F, G, H> withInput(TypeRef<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the eighth input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <H> NodeBuilder8<A, B, C, D, E, F, G, H> withInput(Key<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new RequiredInput<>(input));
      }

      /**
       * Adds the eighth input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <H> NodeBuilder8<A, B, C, D, E, F, G, H> dependingOn(Node<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new RequiredInput<>(input));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, FluentFuture<H>> withAsyncInput(
            Class<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new AsyncInput<>(Key.of(input)));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, FluentFuture<H>> withAsyncInput(
            TypeRef<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new AsyncInput<>(Key.of(input)));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, FluentFuture<H>> withAsyncInput(
            Key<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new AsyncInput<>(input));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, FluentFuture<H>> dependingAsyncOn(
            Node<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new AsyncInput<>(input));
      }
   
      public <H> NodeBuilder8<A, B, C, D, E, F, G, Result<H, ?>> withOptionalInput(Class<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new OptionalInput<>(Key.of(input)));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, Result<H, ?>> withOptionalInput(
            TypeRef<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new OptionalInput<>(Key.of(input)));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, Result<H, ?>> withOptionalInput(Key<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new OptionalInput<>(input));
      }

      public <H> NodeBuilder8<A, B, C, D, E, F, G, Result<H, ?>> dependingOptionallyOn(
            Node<H> input) {
         return new NodeBuilder8<>(input1, input2, input3, input4, input5, input6, input7,
               new OptionalInput<>(input));
      }
   }

   /**
    * A factory for nodes with eight or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder8<A, B, C, D, E, F, G, H> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      private final Input<E> input5;
      private final Input<F> input6;
      private final Input<G> input7;
      private final Input<H> input8;
      
      NodeBuilder8(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4,
            Input<E> input5, Input<F> input6, Input<G> input7, Input<H> input8) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
         this.input5 = input5;
         this.input6 = input6;
         this.input7 = input7;
         this.input8 = input8;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts eight arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation8<A, B, C, D, E, F, G, H, T> op) {
         return new Node8<>(input1, input2, input3, input4, input5, input6, input7, input8, op);
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts eight arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(Operation8<A, B, C, D, E, F, G, H, FluentFuture<T>> op) {
         return new AsyncNode8<>(input1, input2, input3, input4, input5, input6, input7, input8,
               op);
      }

      /**
       * Adds the ninth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, I> withInput(Class<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the ninth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, I> withInput(TypeRef<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the ninth input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, I> withInput(Key<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new RequiredInput<>(input));
      }

      /**
       * Adds the ninth input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, I> dependingOn(Node<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new RequiredInput<>(input));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, FluentFuture<I>> withAsyncInput(
            Class<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new AsyncInput<>(Key.of(input)));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, FluentFuture<I>> withAsyncInput(
            TypeRef<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new AsyncInput<>(Key.of(input)));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, FluentFuture<I>> withAsyncInput(
            Key<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new AsyncInput<>(input));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, FluentFuture<I>> dependingAsyncOn(
            Node<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new AsyncInput<>(input));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, Result<I, ?>> withOptionalInput(
            Class<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new OptionalInput<>(Key.of(input)));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, Result<I, ?>> withOptionalInput(
            TypeRef<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new OptionalInput<>(Key.of(input)));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, Result<I, ?>> withOptionalInput(
            Key<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new OptionalInput<>(input));
      }

      public <I> NodeBuilder9<A, B, C, D, E, F, G, H, Result<I, ?>> dependingOptionallyOn(
            Node<I> input) {
         return new NodeBuilder9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               new OptionalInput<>(input));
      }
   }

   /**
    * A factory for nodes with nine or more inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder9<A, B, C, D, E, F, G, H, I> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      private final Input<E> input5;
      private final Input<F> input6;
      private final Input<G> input7;
      private final Input<H> input8;
      private final Input<I> input9;
      
      NodeBuilder9(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4,
            Input<E> input5, Input<F> input6, Input<G> input7, Input<H> input8, Input<I> input9) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
         this.input5 = input5;
         this.input6 = input6;
         this.input7 = input7;
         this.input8 = input8;
         this.input9 = input9;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts nine arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation9<A, B, C, D, E, F, G, H, I, T> op) {
         return new Node9<>(input1, input2, input3, input4, input5, input6, input7, input8, input9,
               op);
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts nine arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(
            Operation9<A, B, C, D, E, F, G, H, I, FluentFuture<T>> op) {
         return new AsyncNode9<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, op);
      }

      /**
       * Adds the tenth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, J> withInput(Class<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the tenth input to this node. The input is a required, synchronous input of the given
       * type and is bound to a value when the graph is executed.
       * 
       * <p>The input is associated with an unqualified input key. This is the same as the
       * following:<br>
       * {@code builder.withInput(Key.of(input));}
       *
       * @param input the type of the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, J> withInput(TypeRef<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new RequiredInput<>(Key.of(input)));
      }

      /**
       * Adds the tenth input to this node. The input is a required, synchronous input with the
       * given input key and is bound to a value when the graph is executed.
       *
       * @param input the key for the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, J> withInput(Key<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new RequiredInput<>(input));
      }

      /**
       * Adds the tenth input to this node. The input is required and synchronous. Its value is
       * bound to the result of the given node when the graph is executed.
       *
       * @param input the node that provides the input
       * @return a node builder that can be used to add additional inputs or define the node's
       *       operation
       */
      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, J> dependingOn(Node<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new RequiredInput<>(input));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, FluentFuture<J>> withAsyncInput(
            Class<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new AsyncInput<>(Key.of(input)));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, FluentFuture<J>> withAsyncInput(
            TypeRef<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new AsyncInput<>(Key.of(input)));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, FluentFuture<J>> withAsyncInput(
            Key<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new AsyncInput<>(input));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, FluentFuture<J>> dependingAsyncOn(
            Node<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new AsyncInput<>(input));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, Result<J, ?>> withOptionalInput(
            Class<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new OptionalInput<>(Key.of(input)));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, Result<J, ?>> withOptionalInput(
            TypeRef<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new OptionalInput<>(Key.of(input)));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, Result<J, ?>> withOptionalInput(
            Key<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new OptionalInput<>(input));
      }

      public <J> NodeBuilder10<A, B, C, D, E, F, G, H, I, Result<J, ?>> dependingOptionallyOn(
            Node<J> input) {
         return new NodeBuilder10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, new OptionalInput<>(input));
      }
   }

   /**
    * A factory for nodes with ten inputs.
    * 
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <J> the type of the tenth input
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class NodeBuilder10<A, B, C, D, E, F, G, H, I, J> {
      private final Input<A> input1;
      private final Input<B> input2;
      private final Input<C> input3;
      private final Input<D> input4;
      private final Input<E> input5;
      private final Input<F> input6;
      private final Input<G> input7;
      private final Input<H> input8;
      private final Input<I> input9;
      private final Input<J> input10;
      
      NodeBuilder10(Input<A> input1, Input<B> input2, Input<C> input3, Input<D> input4,
            Input<E> input5, Input<F> input6, Input<G> input7, Input<H> input8, Input<I> input9,
            Input<J> input10) {
         this.input1 = input1;
         this.input2 = input2;
         this.input3 = input3;
         this.input4 = input4;
         this.input5 = input5;
         this.input6 = input6;
         this.input7 = input7;
         this.input8 = input8;
         this.input9 = input9;
         this.input10 = input10;
      }
      
      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts ten arguments and returns the node's result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> define(Operation10<A, B, C, D, E, F, G, H, I, J, T> op) {
         return new Node10<>(input1, input2, input3, input4, input5, input6, input7, input8, input9,
               input10, op);
      }

      /**
       * Defines the node's operation and returns the newly constructed node. The given operation
       * accepts ten arguments and returns an asynchronous (e.g. future) result.
       *
       * @param op the node's operation
       * @return the newly constructed node
       */
      public <T> Node<T> defineAsync(
            Operation10<A, B, C, D, E, F, G, H, I, J, FluentFuture<T>> op) {
         return new AsyncNode10<>(input1, input2, input3, input4, input5, input6, input7, input8,
               input9, input10, op);
      }
   }
}

