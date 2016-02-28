/**
 * APIs for representing complex processes as a dependency graph. The graph can be "computed" to
 * produce results, and the nodes' operations will be executed concurrently where possible to
 * maximize parallelism.
 * 
 * <p>A graph representation is useful because it decouples the structure of the process from its
 * actual execution. Each unit of work in the process, a single node in the graph, can be
 * implemented with simple synchronous code. In this way, the whole process can be described as a
 * sequence of simple steps. But, when the graph is actually computed, any parallelism afforded by
 * the structure and dependencies in the graph will automatically be exploited. This abstracts away
 * details like threads and futures, making a complex process easier to reason about.
 * 
 * <p>A few definitions may be useful:
 * <dl>
 * <dt>{@linkplain com.bluegosling.graph.Graph Graph}</dt>
 * <dd>The static structure of a computation. Graphs are immutable and represent a set of one or
 * more nodes. One of the nodes is the "result" node and produces the result of the whole graph. A
 * graph can require explicit inputs, that must be bound when the graph is computed.</dd>
 * <dt>{@linkplain com.bluegosling.graph.Node Node}</dt>
 * <dd>A single node in a graph. A node represents a single operation that acts on inputs (if any,
 * up to ten) and yields a value. That value may be passed as an input to another node in the graph.
 * Node inputs can come either from other nodes or from external inputs that must be supplied when
 * the graph is computed.</dd>
 * <dt>{@linkplain com.bluegosling.graph.Key Input Key}</dt>
 * <dd>A key that describes an input to the graph. Values are bound to these inputs when the graph
 * is computed. A key consists of a type (the type of the input) and an optional qualifier (for
 * distinguishing multiple inputs with the same type).</dd>
 * <dt>{@linkplain com.bluegosling.graph.Computation Computation}</dt>
 * <dd>A single execution of the graph. A computation represents a single invocation of the logic in
 * the graph and of its nodes' operations. Inputs are defined for a computation, and then it is
 * executed to produce the graph's result.</dd>
 * </dl>
 */
package com.bluegosling.graph;
