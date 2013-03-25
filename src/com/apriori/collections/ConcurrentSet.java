// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of {@link Set} that supports concurrent access. The actual
 * underlying implementation/storage is defined at construction time by
 * providing a normal (non-concurrent) set implementation.
 * 
 * <p>This implementation reduces contention in operations on the set by using
 * multiple "shards", each shard being a set that stores some sub-set of the
 * items. Shards are accessed via read-write locks instead of via synchronizing
 * on the set itself to improve performance by allowing multiple readers
 * simultaneous access.
 * 
 * <p>To support iteration in the face of concurrent writes, this implementation
 * uses "copy-on-iteration" semantics. So each iterator essentially iterates
 * over snapshots of the underlying shards and thus will not reflect the effects
 * of mutative operations on the set that occur between the time the iterator is
 * created and the time iteration is complete. Note that the {@code Set}
 * implementation provided during construction must implement {@link Cloneable}
 * and its {@code clone()} method should return an object that also implements
 * {@link Set} and that is a shallow copy (a deep copy will incur a much greater
 * performance penalty during copy-on-iteration operations).
 * 
 * <p>A large number of concurrent iterations could result in a large number of
 * copies of the set on the heap at a given time, which would be inefficient. So
 * this implementation uses three strategies to reduce memory pressure:
 * <ol>
 * <li>The snapshots are taken "lazily". So if no write operation ever occurs
 * during an iteration, no copy is needed and the iterator effectively reads the
 * actual set contents instead of a copy.</li>
 * <li>Iterators will effectively share snapshots if iteration began when the
 * set was in the same state. They can even share partial snapshots if at least
 * one shard was in the same state when they were created.</li>
 * <li>The granularity of a snapshot is a single shard. So if mutative
 * operations only impact one shard, only one shard is copied on the heap for
 * the benefit iteration.</li>
 * </ol>
 * 
 * <p>Since the iterators use a snapshot, iteration over this set is strongly
 * consistent, unlike many other implementations of concurrent collections. It
 * is also worth noting that other operations on this set are also strongly
 * consistent and atomic. For example, {@link #clear()} is atomic, so no reader
 * thread should ever see the set in a partially-cleared state. Similarly, other
 * operations that require bulk access/updates to multiple shards -- like
 * {@link #size()}, {@link #addAll(Collection)}, {@link #removeAll(Collection)},
 * and {@link #retainAll(Collection)} -- are done so atomically. One potential
 * downside to this consistency is possibly degraded performance since multiple
 * locks must be held at once by a single thread to perform operations
 * atomically, which can increase lock contention.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> the type element contained in the set
 */
class ConcurrentSet<E> implements Serializable, Cloneable, Set<E> {

   private static final long serialVersionUID = -2010351788181385374L;
   
   private static final Method CLONE_METHOD;
   
   static {
      try {
         CLONE_METHOD = Object.class.getDeclaredMethod("clone");
         CLONE_METHOD.setAccessible(true);
      }
      catch (NoSuchMethodException e) {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * Iterates over a snapshot of the set.
    * 
    * <p>The shards are a snapshot and can be assumed to be immutable.
    * 
    * <p>Methods are synchronized so as not to corrupt the structure if
    * used from multiple threads. However, access from multiple threads is
    * discouraged since it could cause {@link #next()} to spuriously throw
    * exceptions, even if the code is first checking {@link #hasNext()}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class IteratorImpl implements Iterator<E> {
      private final Set<E> stableShards[];
      private Iterator<E> curIterator;
      private int curShard;
      private E lastElement;
      private boolean fetched;
      private boolean removed;
      private boolean done;
      
      IteratorImpl(Set<E> stableShards[]) {
         this.stableShards = stableShards;
         curIterator = stableShards[curShard].iterator();
      }

      /** {@inheritDoc} */
      @Override
      public synchronized boolean hasNext() {
         while (!done) {
            if (curIterator.hasNext()) {
               return true;
            } else {
               // clear our refs to no-longer need shards
               // so they can be gc'ed if they are copies
               stableShards[curShard++] = null;
               if (curShard == stableShards.length) {
                  done = true;
               } else {
                  curIterator = stableShards[curShard].iterator();
               }
            }
         }
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public synchronized E next() {
         if (hasNext()) {
            fetched = true;
            removed = false;
            lastElement = curIterator.next();
            return lastElement;
         } else {
            throw new NoSuchElementException();
         }
      }

      /** {@inheritDoc} */
      @Override
      public synchronized void remove() {
         if (removed) {
            throw new IllegalStateException("element already removed");
         } else if (!fetched) {
            throw new IllegalStateException("no element to remove");
         } else {
            removed = true;
            ConcurrentSet.this.remove(lastElement);
         }
      }
   }
   
   @SuppressWarnings("unchecked")
   static <E, S extends Set<E>> S makeClone(S set) {
      try {
         return (S) CLONE_METHOD.invoke(set);
      }
      catch (SecurityException e) {
         throw new RuntimeException(e);
      }
      catch (IllegalArgumentException e) {
         throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
      catch (InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }
   
   // ideally, these would be final -- but they aren't because we have
   // to set them during deserialization in addition to in constructor
   transient Set<E> shards[];
   private transient int shardModCounts[];
   private transient int shardLatestIteratorModCounts[];
   // all of the above fields must be accessed under these locks
   private transient ReentrantReadWriteLock shardLocks[];

   /**
    * Constructs a new set, based on the provided set implementation. The
    * specified set must implement {@link Cloneable} since that is how shards
    * are copied during copy-on-iteration operations. The new set will be
    * completely independent of the specified set, so changes made to the
    * specified set after this constructor returns will <em>not</em> be
    * reflected in the contents of the new {@code ConcurrentSet}.
    * 
    * <p>The expected level of concurrent writes is also the number of shards
    * that will be used internally in the new set.
    * 
    * <p>If fair read-write locks are used, read requests will get queued behind
    * pending write requests. Otherwise, read requests may proceed if one or
    * more readers already hold the lock, further delaying pending write
    * requests. Unfair locks will likely exhibit greater throughput and less
    * contention but could result in starvation of writer threads.
    *
    * @param <S> the type of the underlying set implementation
    * @param set the underlying set implementation
    * @param concurrency the number of expected concurrent writers
    * @param fair whether or not fair read-write locks are used
    */
   @SuppressWarnings("unchecked")
   <S extends Set<E> & Cloneable> ConcurrentSet(S set, int concurrency,
         boolean fair) {
      
      if (concurrency < 1) {
         throw new IllegalArgumentException("concurrency must be > 0");
      }
      S copy = makeClone(set);
      copy.clear(); // empty shard
      shards = new Set[concurrency];
      shards[0] = copy;
      for (int i = 1; i < concurrency; i++) {
         shards[i] = makeClone(copy);
      }
      shardLocks = new ReentrantReadWriteLock[concurrency];
      shardModCounts = new int[concurrency];
      shardLatestIteratorModCounts = new int[concurrency];
      for (int i = 0; i < concurrency; i++) {
         shardLocks[i] = new ReentrantReadWriteLock(fair);
         shardLatestIteratorModCounts[i] = -1;
      }
      addAll(set);
   }
   
   private void acquireWriteLock(int shard) {
      shardLocks[shard].writeLock().lock();
      if (shardLatestIteratorModCounts[shard] == shardModCounts[shard]) {
         shards[shard] = makeClone(shards[shard]);
      }
      shardModCounts[shard]++;
   }
   
   private void releaseWriteLock(int shard) {
      shardLocks[shard].writeLock().unlock();
   }

   void acquireWriteLocks() {
      for (int i = 0, len = shards.length; i < len; i++) {
         acquireWriteLock(i);
      }
   }
   
   void releaseWriteLocks() {
      for (int i = 0, len = shards.length; i < len; i++) {
         releaseWriteLock(i);
      }
   }
   
   void acquireWriteLocks(boolean effectedShards[]) {
      for (int i = 0, len = shards.length; i < len; i++) {
         if (effectedShards[i]) {
            acquireWriteLock(i);
         }
      }
   }
   
   void releaseWriteLocks(boolean effectedShards[]) {
      for (int i = 0, len = shards.length; i < len; i++) {
         if (effectedShards[i]) {
            releaseWriteLock(i);
         }
      }
   }
   
   private void acquireReadLock(int shard) {
      shardLocks[shard].readLock().lock();
   }
   
   private void releaseReadLock(int shard) {
      shardLocks[shard].readLock().unlock();
   }
   
   void acquireReadLocks() {
      for (int i = 0, len = shards.length; i < len; i++) {
         acquireReadLock(i);
      }
   }
   
   void releaseReadLocks() {
      for (int i = 0, len = shards.length; i < len; i++) {
         releaseReadLock(i);
      }
   }
   
   private void acquireReadLocks(boolean effectedShards[]) {
      for (int i = 0, len = shards.length; i < len; i++) {
         if (effectedShards[i]) {
            acquireReadLock(i);
         }
      }
   }
   
   private void releaseReadLocks(boolean effectedShards[]) {
      for (int i = 0, len = shards.length; i < len; i++) {
         if (effectedShards[i]) {
            releaseReadLock(i);
         }
      }
   }

   private int shardNumFor(Object o) {
      return ((o == null ? 0 : o.hashCode()) & 0x7fffffff) % shards.length;
   }
   
   <T> Collection<T>[] collectionToShards(Collection<T> coll) {
      int len = shards.length;
      int initialShardSize = (coll.size() + len - 1) / len;
      @SuppressWarnings("unchecked")
      Collection<T> ret[] = new Collection[len];
      // initialize shards
      for (int i = 0; i < len; i++) {
         ret[i] = new ArrayList<T>(initialShardSize);
      }
      // and scatter the collection to the shards
      for (T t : coll) {
         ret[shardNumFor(t)].add(t);
      }
      return ret;
   }
   
   boolean[] getEffectedShards(Collection<?> colls[]) {
      int len = colls.length;
      boolean ret[] = new boolean[len];
      for (int i = 0; i < len; i++) {
         if (!colls[i].isEmpty()) {
            ret[i] = true;
         }
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public boolean add(E e) {
      int shardNum = shardNumFor(e);
      acquireWriteLock(shardNum);
      try {
         return shards[shardNum].add(e);
      } finally {
         releaseWriteLock(shardNum);
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean addAll(Collection<? extends E> coll) {
      Collection<? extends E> colls[] = collectionToShards(coll);
      boolean effectedShards[] = getEffectedShards(colls);
      boolean ret = false;
      acquireWriteLocks(effectedShards);
      try {
         for (int i = 0, len = effectedShards.length; i < len; i++) {
            if (effectedShards[i]) {
               if (shards[i].addAll(colls[i])) {
                  ret = true;
               }
            }
         }
      } finally {
         releaseWriteLocks(effectedShards);
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public void clear() {
      acquireWriteLocks();
      try {
         for (int i = 0, len = shards.length; i < len; i++) {
            shards[i].clear();
         }
      } finally {
         releaseWriteLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean contains(Object o) {
      int shardNum = shardNumFor(o);
      acquireReadLock(shardNum);
      try {
         return shards[shardNum].contains(o);
      } finally {
         releaseReadLock(shardNum);
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean containsAll(Collection<?> coll) {
      Collection<?> colls[] = collectionToShards(coll);
      boolean effectedShards[] = getEffectedShards(colls);
      acquireReadLocks(effectedShards);
      try {
         for (int i = 0, len = effectedShards.length; i < len; i++) {
            if (effectedShards[i]) {
               if (!shards[i].containsAll(colls[i])) {
                  return false;
               }
            }
         }
         return true;
      } finally {
         releaseReadLocks(effectedShards);
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean isEmpty() {
      acquireReadLocks();
      try {
         for (Set<E> s : shards) {
            if (!s.isEmpty()) {
               return false;
            }
         }
         return true;
      } finally {
         releaseReadLocks();
      }
   }
   
   Set<E>[] getStableShards() {
      acquireReadLocks();
      try {
         int len = shards.length;
         @SuppressWarnings("unchecked")
         Set<E> stableShards[] = new Set[len];
         // get stable snapshot of references (individual shards may be cloned
         // on mutations after this point)
         System.arraycopy(shards, 0, stableShards, 0, len);
         // extra book-keeping for our lazy copy-on-iteration semantics
         System.arraycopy(shardModCounts, 0, shardLatestIteratorModCounts, 0, len);
         return stableShards;
      } finally {
         releaseReadLocks();
      }
   }
   
   Iterator<E> makeIterator(Set<E> stableShards[]) {
      return new IteratorImpl(stableShards);
   }

   /** {@inheritDoc} */
   @Override
   public Iterator<E> iterator() {
      return makeIterator(getStableShards());
   }

   /** {@inheritDoc} */
   @Override
   public boolean remove(Object o) {
      int shardNum = shardNumFor(o);
      acquireWriteLock(shardNum);
      try {
         return shards[shardNum].remove(o);
      } finally {
         releaseWriteLock(shardNum);
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean removeAll(Collection<?> coll) {
      Collection<?> colls[] = collectionToShards(coll);
      boolean effectedShards[] = getEffectedShards(colls);
      boolean ret = false;
      acquireWriteLocks(effectedShards);
      try {
         for (int i = 0, len = effectedShards.length; i < len; i++) {
            if (effectedShards[i]) {
               if (shards[i].removeAll(colls[i])) {
                  ret = true;
               }
            }
         }
      } finally {
         releaseWriteLocks(effectedShards);
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public boolean retainAll(Collection<?> coll) {
      Collection<?> colls[] = collectionToShards(coll);
      boolean effectedShards[] = getEffectedShards(colls);
      boolean ret = false;
      acquireWriteLocks(); // we need 'em all
      try {
         for (int i = 0, len = effectedShards.length; i < len; i++) {
            if (effectedShards[i]) {
               if (shards[i].retainAll(colls[i])) {
                  ret = true;
               }
            } else {
               // effectedShards[i] being false means that the
               // collection we sharded had no items for shard i
               // but that means we need to clear it because
               // we're retaining none of them
               if (!shards[i].isEmpty()) {
                  shards[i].clear();
                  ret = true;
               }
            }
         }
      } finally {
         releaseWriteLocks();
      }
      return ret;
   }
   
   private int sizeNoLocks() {
      // it's up to the caller to acquire necessary locks on all shards
      int sz = 0;
      for (int i = 0, len = shards.length; i < len; i++) {
         sz += shards[i].size();
      }
      return sz;
   }

   /** {@inheritDoc} */
   @Override
   public int size() {
      acquireReadLocks();
      try {
         return sizeNoLocks();
      } finally {
         releaseReadLocks();
      }
   }
   
   static void copyToArray(Set<?> shards[], Object a[]) {
      // caller is expect to have already acquired read locks for all shards
      int i = 0;
      for (Set<?> shard : shards) {
         for (Object o : shard) {
            a[i++] = o;
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public Object[] toArray() {
      acquireReadLocks();
      try {
         Object ret[] = new Object[sizeNoLocks()];
         copyToArray(shards, ret);
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public <T> T[] toArray(T[] a) {
      acquireReadLocks();
      try {
         int sz = sizeNoLocks();
         a = ArrayUtils.ensureCapacity(a, sz);
         copyToArray(shards, a);
         if (a.length > sz) {
            a[sz] = null;
         }
         return a;
      } finally {
         releaseReadLocks();
      }
   }
   
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      acquireReadLocks();
      try {
         out.writeInt(shards.length); // concurrency level
         // use an empty copy of the internal set implementation
         // as a "template" for creating shards during deserialization
         Set<E> emptyImpl = makeClone(shards[0]);
         emptyImpl.clear();
         out.writeObject(emptyImpl);
         out.writeBoolean(shardLocks[0].isFair()); // lock fairness
         out.writeInt(sizeNoLocks()); // number of elements
         // and now each element
         for (Iterator<E> iter = makeIterator(shards.clone()); iter.hasNext(); ) {
            out.writeObject(iter.next());
         }
      } finally {
         releaseReadLocks();
      }
   }
   
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      int concurrency = in.readInt();
      Set<E> template = (Set<E>) in.readObject();
      boolean fair = in.readBoolean();
      int size = in.readInt();
      // build instance fields
      template = makeClone(template); // defensive copy
      template.clear();
      shards = new Set[concurrency];
      shards[0] = template;
      for (int i = 1; i < concurrency; i++) {
         shards[i] = makeClone(template);
      }
      shardLocks = new ReentrantReadWriteLock[concurrency];
      shardModCounts = new int[concurrency];
      shardLatestIteratorModCounts = new int[concurrency];
      for (int i = 0; i < concurrency; i++) {
         shardLocks[i] = new ReentrantReadWriteLock(fair);
         shardLatestIteratorModCounts[i] = -1;
      }
      // and now populate
      while (size > 0) {
         E e = (E) in.readObject();
         shards[shardNumFor(e)].add(e);
         size--;
      }
   }
   
   @Override
   public boolean equals(Object o) {
      acquireReadLocks();
      try {
         return CollectionUtils.equals(this, o);
      } finally {
         releaseReadLocks();
      }
   }
   
   @Override
   public int hashCode() {
      acquireReadLocks();
      try {
         return CollectionUtils.hashCode(this);
      } finally {
         releaseReadLocks();
      }
   }
   
   @Override
   public String toString() {
      acquireReadLocks();
      try {
         return CollectionUtils.toString(this);
      } finally {
         releaseReadLocks();
      }
   }
   
   @Override
   public ConcurrentSet<E> clone() {
      try {
         @SuppressWarnings("unchecked")
         ConcurrentSet<E> copy = (ConcurrentSet<E>) super.clone();
         // deep copy
         copy.shardModCounts = new int[shardModCounts.length];
         copy.shardLatestIteratorModCounts = new int[shardLatestIteratorModCounts.length];
         copy.shards = shards.clone();
         copy.shardLocks = shardLocks.clone();
         for (int i = 0, len = shards.length; i < len; i++) {
            copy.shards[i] = makeClone(shards[i]);
            copy.shardLocks[i] = new ReentrantReadWriteLock(shardLocks[i].isFair());
         }
         return copy;
      } catch (CloneNotSupportedException e) {
         // should never happen since this class implements Cloneable and so must the shards
         throw new ClassCastException(Cloneable.class.getName());
      }
   }
}