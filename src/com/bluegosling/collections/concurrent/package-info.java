/**
 * New implementations of concurrent data structures.
 * 
 * <p>This package contains several blocking queue and deque implementations that are lock-free.
 * Under certain workloads, particularly where contention might arise (many consumers and/or many
 * producers), these implementations may provide superior performance to the standard
 * implementations included in the {@code java.util.concurrent} package (JSR 166).
 * 
 * <p>There is also a factory class for instantiating
 * {@linkplain com.bluegosling.collections.concurrent.ShardedConcurrentSets concurrent sets}. It
 * operates using normal not-thread-safe set implementations and makes them
 * thread-safe using sharding, for parallelism, and read-write locks, for thread-safety.
 * 
 * <p>Also present are concurrent structures that use persistent collections and compare-and-swap.
 * These include both a
 * {@linkplain com.bluegosling.collections.concurrent.PersistentListBackedConcurrentList list}
 * (which implements a new {@link com.bluegosling.collections.concurrent.ConcurrentList} interface)
 * and a {@linkplain com.bluegosling.collections.concurrent.PersistentMapBackedConcurrentMap map}.
 */
package com.bluegosling.collections.concurrent;