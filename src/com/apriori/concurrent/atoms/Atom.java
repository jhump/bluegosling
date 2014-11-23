package com.apriori.concurrent.atoms;

/**
 * A thread-safe reference type. Common to all atoms are the ability to retrieve its current value
 * and to add "watchers", which are called back when the atom's value changes. Atoms also support
 * mutations, to change the atom's current value. However, mutation methods are defined on sub-types
 * since there are basically two mutually exclusive types of mutation:
 * <ul>
 *   <li><strong>Synchronous</strong>: Mutation events are made immediately, in the current thread.
 *   When a synchronous mutation method has returned, the change has been made and watchers have
 *   been notified. One subtle exception is the {@link TransactionalAtom}, in which case the
 *   mutation is visible in the current transaction but not visible to other threads until the
 *   transaction commits, and watchers are not notified until the change is committed.</li>
 *   <li><strong>Asynchronous</strong>: Mutation events are queued immediately but may execute at
 *   some point in the future, in a different thread. Asynchronous mutations return futures that
 *   are completed when the changes have been made and watchers have been notified.</li>
 * </ul>
 *
 * @param <T> the type of the atom's value
 * 
 * @see SynchronousAtom
 * @see AsynchronousAtom
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Atom<T> {
   /**
    * Watches an atom for changes to its value. Implementations of this interface must be
    * thread-safe since they may be called concurrently from multiple threads.
    *
    * @param <T> the type of the watched atom's value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   interface Watcher<T> {
      /**
       * Handles changes to the specified atom.
       *
       * @param atom the atom whose value has changed
       * @param oldValue the atom's value prior to the change
       * @param newValue the atom's new value after the change
       */
      void changed(Atom<? extends T> atom, T oldValue, T newValue);
   }
   
   /**
    * Retrieves the current value for this atom.
    *
    * @return the current value for the atom
    */
   T get();
   
   /**
    * Registers a watcher, which will receive notifications when the atom's value changes.
    *
    * @param watcher the watcher
    * @return true if the watcher was registered; false if no change occurred because the watcher
    *       was already registered
    */
   boolean addWatcher(Watcher<? super T> watcher);

   /**
    * Unregisters a watcher.
    *
    * @param watcher the watcher
    * @return true if the watcher was unregistered; false if no change occurred because the watcher
    *       was never registered to begin with
    */
   boolean removeWatcher(Watcher<? super T> watcher);
}
