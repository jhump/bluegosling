/**
 * A set of classes for managing mutable thread-safe references. These are similar in many ways to
 * an {@link java.util.concurrent.atomic.AtomicReference}, but an {@link
 * com.apriori.concurrent.atoms.Atom Atom} includes some additional APIs for atomic operations and
 * allows for some asynchronous processing in the form of "watchers".
 * 
 * <p>Another key difference is that this package provides a variety of implementations regarding
 * how a reference value is mutated:
 * <ul>
 *    <li><strong>CAS</strong>: A {@link com.apriori.concurrent.atoms.SimpleAtom} uses a CAS
 *    (Compare And Set) operation/loop to atomically perform updates to a reference. This is the
 *    same approach used by {@link java.util.concurrent.atomic.AtomicReference}.</li> 
 *    <li><strong>TLS</strong>: A {@link com.apriori.concurrent.atoms.ThreadLocalAtom} uses
 *    thread-local storage to update a reference. This means that after the reference is seeded from
 *    a root value (which happens during initialization of the atom in a new thread), the value is
 *    changed independently of other threads. No thread can see the updates made by another.</li>
 *    <li><strong>Asynchronous</strong>: An {@link com.apriori.concurrent.atoms.AsynchronousAtomTest}
 *    uses a thread pool to asynchronously perform updates. All updates for a given atom are
 *    serialized and applied sequentially. But many atoms can be updated concurrently using multiple
 *    threads in the pool.</li> 
 *    <li><strong>STM</strong>: A {@link
 *    com.apriori.concurrent.atoms.TransactionalAtom} performs updates using STM (Software
 *    Transactional Memory). A {@link com.apriori.concurrent.atoms.Transaction} supports MVCC
 *    (Multi-Version Concurrency Control), much like an RDBMS, and updates to references are
 *    performed when a transaction is committed. A transaction can provide multiple {@linkplain
 *    com.apriori.concurrent.atoms.Transaction.IsolationLevel levels of isolation}.</li>
 * </ul>
 *
 * @see com.apriori.concurrent.atoms.Atom
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.concurrent.atoms;