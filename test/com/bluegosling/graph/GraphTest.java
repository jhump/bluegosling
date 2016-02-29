package com.bluegosling.graph;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bluegosling.concurrent.futures.fluent.FluentFuture;

import org.junit.Test;

//TODO: MOAR TESTS
public class GraphTest {

   @Test public void oneNode_zeroInputs() throws Exception {
      Node<String> node = Node.builder().define(() -> "abc");
      
      Graph<String> graph = node.toGraph();
      assertTrue(graph.inputKeys().isEmpty());
      assertSame(node, graph.resultNode());
      
      Computation<String> comp = graph.newComputation();
      // no args, so can't bind an input
      assertThrows(IllegalArgumentException.class, () -> comp.bindInput(String.class, "x"));

      checkComputation(comp, "abc");
   }

   @Test public void multiNode_zeroInputs() throws Exception {
      Node<String> node1 = Node.builder().define(() -> "1");
      Node<String> node2 = Node.builder().define(() -> "2");
      Node<String> node3 = Node.builder().dependingOn(node1).dependingOn(node2)
            .define((s1, s2) -> s1 + "-" + s2);
      
      Graph<String> graph = node3.toGraph();
      assertTrue(graph.inputKeys().isEmpty());
      assertSame(node3, graph.resultNode());
      
      Computation<String> comp = graph.newComputation();
      // no args, so can't bind an input
      assertThrows(IllegalArgumentException.class, () -> comp.bindInput(String.class, "x"));

      checkComputation(comp, "1-2");
   }

   @Test public void multiNode_oneInputs() throws Exception {
      Node<String> node1 = Node.builder().withInput(Integer.class).define(String::valueOf);
      Node<String> node2 = Node.builder().define(() -> "2");
      Node<String> node3 = Node.builder().dependingOn(node1).dependingOn(node2)
            .define((s1, s2) -> s1 + "-" + s2);
      
      Graph<String> graph = node3.toGraph();
      assertEquals(node1.inputKeys(), graph.inputKeys());
      assertSame(node3, graph.resultNode());

      Computation<String> comp = graph.newComputation();
      // no string args, so can't bind this input
      assertThrows(IllegalArgumentException.class, () -> comp.bindInput(String.class, "x"));
      // no qualified int, so can't bind this either
      assertThrows(IllegalArgumentException.class,
            () -> comp.bindInput(Key.of(Integer.class, 'i'), 123));
      // can't actually compute because we haven't yet bound the input
      assertThrows(IllegalStateException.class, () -> comp.compute());
      
      comp.bindInput(Integer.class, 123);
      checkComputation(comp, "123-2");
   }
   
   private <T> void checkComputation(Computation<T> computation, T expectedResult)
         throws Exception {
      FluentFuture<T> value = computation.compute();
      assertEquals(expectedResult, value.get());
      
      // result is memoized and not recomputed
      assertSame(value, computation.compute());
      
      // can't even try to bind after execution
      assertThrows(IllegalStateException.class, () -> computation.bindInput(String.class, "x"));
   }
}
