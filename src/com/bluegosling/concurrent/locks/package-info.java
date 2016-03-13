/**
 * New types of locks and synchronizers. These include some new implementations of the standard
 * {@link java.util.concurrent.locks.Lock} interface as well as some alternative APIs.
 * 
 * <p>One new lock,
 * {@link com.bluegosling.concurrent.locks.HierarchicalLock}, can be used to implement
 * MVCC locking with a hierarchy of objects, like might be used in a database system. It is similar
 * to a {@link java.util.concurrent.locks.ReadWriteLock} except that is also provides a lock
 * hierarchy. Acquiring a lock (in any mode) requires acquiring the lock's parent in shared
 * (e.g. "read") mode. This lock also has deadlock-detection and provides the ability to promote
 * locks (e.g. shared → exclusive or child lock → parent lock) and to demote them (exclusive →
 * shared or parent → child).
 */
package com.bluegosling.concurrent.locks;