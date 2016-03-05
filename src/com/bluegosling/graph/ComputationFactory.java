package com.bluegosling.graph;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.concurrent.Executor;

// TODO: doc
// TODO: tests
public class ComputationFactory {
   private final Executor executor;
   private final Iterable<NodeDecorator> decorators;
   
   ComputationFactory(Executor executor, Iterable<NodeDecorator> decorators) {
      this.executor = executor;
      this.decorators = decorators;
   }
   
   public <T> Computation<T> newComputation(Graph<T> graph) {
      return new Computation<>(graph, executor, decorators);
   }
   
   public static Builder newBuilder() {
      return new Builder();
   }
   
   public static class Builder {
      private Executor executor;
      private final ArrayList<NodeDecorator> decorators = new ArrayList<>();
      
      Builder() {
      }
      
      public Builder setExecutor(Executor executor) {
         this.executor = executor;
         return this;
      }
      
      public Builder addDecorator(NodeDecorator decorator) {
         this.decorators.add(decorator);
         return this;
      }
      
      public Builder addDecorators(Iterable<NodeDecorator> decs) {
         Iterables.addAll(this.decorators, decs);
         return this;
      }
      
      public ComputationFactory build() {
         return new ComputationFactory(executor, decorators);
      }
   }
}
