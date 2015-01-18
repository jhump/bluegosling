package com.apriori.generator;

import com.apriori.concurrent.DeadlockException;
import com.apriori.vars.Variable;
import com.apriori.vars.VariableBoolean;

import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Provides generator functionality in Java. Since the Java language does not support co-routines,
 * generators, or "green" threads, generators cannot be done the normal way. So, instead of the
 * generator and consumer running in the same thread, and the generator's state/stack being saved
 * every time a value is produced and control returned to the consumer, this implementation
 * has the generator run on a separate thread. This approach has some down sides that may limit its
 * utility/practicality:
 * <ol>
 * <li>A separate thread is required for each iteration through the generated sequence. When used a
 * lot and across many consumer threads, this makes it easy to have a huge number of outstanding
 * generator threads. It is possible to cause {@link OutOfMemoryError}s this way since that is what
 * happens when the JVM process cannot allocate any more native threads. Using a thread pool to
 * limit the number of threads is one way to mitigate this issue, but caution must be exercised as
 * this approach can severely limit throughput and even cause deadlock.</li>
 * <li>Transfer of control from one thread to another is significantly more expensive than just
 * popping data from the stack into the heap and then returning control to a caller. So this
 * approach leads to slower iteration. A value must be transferred from one thread to another and
 * threads must be parked and unparked for each transfer of control, from consumer to generator and
 * then back from generator to consumer.</li>
 * </ol>
 * <p>The trickier bits of using separate threads to run generators involve avoiding thread leaks
 * when a sequence is "abandoned". This happens if a consumer never reaches the end of the stream
 * and the generator thread never finishes its work. When that happens we must interrupt the thread
 * so that it exits. This is achieved using finalizers. This has the inherent down side that it is
 * dependent on garbage collection and running finalizers, which means that an unfinished generator
 * thread can needlessly tie up system resources for a non-deterministic amount of time.
 * 
 * <p>{@link Sequence}s produced by this generator are not intended to be used simultaneously from
 * multiple consumer threads. If incorrect usage is detected, {@link Sequence#next(Object)} will
 * throw a {@link ConcurrentModificationException}.
 * 
 * <p>To create a generator, either use a flavor of {@link #create} and provide a lambda that will
 * yield all generated values OR create a sub-class and override the {@link #run(Object, Output)}
 * method with logic that yields the generated values.
 *
 * @param <T> the type of value produced by the generator
 * @param <U> the type of value passed to the generator
 * @param <X> the type of exception that may be thrown while generating a value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see UncheckedGenerator
 */
public abstract class Generator<T, U, X extends Throwable> {

   /**
    * Represents the output of a generated sequence. A generator sends data to its consumer by
    * providing data through this interface. For consistency with generator functionality in other
    * languages, the name of the method is "yield".
    *
    * @param <T> the type of value produced
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Output<T, U> {
      /**
       * Yields a value from the generator. This sends the specified value to the consumer and
       * also suspends the generator until the consumer requests another value. Any value supplied
       * by the consumer in its call to {@link Sequence#next(Object)} is returned from this method.
       *
       * @param t the value to send to the consumer
       * @return the subsequent value provided by the consumer
       * @throws SequenceAbandonedException if the consumer has abandoned the sequence, in which
       *       case the generator should exit
       */
      U yield(T t);
   }
   
   /**
    * Sequence number, for giving worker threads unique names.
    */
   static final AtomicInteger threadSeq = new AtomicInteger();
   
   /**
    * A shared executor used for running generator workers when no other executor is given.
    */
   private static final Executor SHARED_EXECUTOR = Executors.newCachedThreadPool(
         new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
               Thread th = new Thread(r);
               th.setName("Generator-" + threadSeq.incrementAndGet());
               th.setDaemon(true);
               return th;
            }
         });

   /**
    * Creates a new generator whose generation logic is performed by a {@link BiConsumer} that
    * accepts the initial value provided to the generator as well as the generator's output. The new
    * generator uses a default, shared thread pool.
    *
    * @param consumer the consumer that accepts the generator's output and uses it to yield
    *       generated values
    * @return a new generator that uses a default, shared thread pool to run generation logic
    */
   public static <T, U> UncheckedGenerator<T, U> create(BiConsumer<U, Output<T, U>> consumer) {
      if (consumer == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T, U>() {
         @Override protected void run(U initialValue, Output<T, U> out) {
            consumer.accept(initialValue, out);
         }
      };
   }

   /**
    * Creates a new generator whose generation logic is performed by a {@link Consumer} that accepts
    * the generator's output. The new generator uses a default, shared thread pool.
    *
    * @param consumer the consumer that accepts the generator's output and uses it to yield
    *       generated values
    * @return a new generator that uses a default, shared thread pool to run generation logic
    */
   public static <T> UncheckedGenerator<T, Void> create(Consumer<Output<T, Void>> consumer) {
      if (consumer == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T, Void>() {
         @Override protected void run(Void initialValue, Output<T, Void> out) {
            consumer.accept(out);
         }
      };
   }
   
   /**
    * Creates a new generator whose generation logic is performed by a {@link BiConsumer} that
    * accepts the initial value provided to the generator as well as the generator's output. The new
    * generator uses the specified executor to run logic for each execution.
    *
    * <p>Care must be exercised when using a custom executor. If the parallelism allowed by the
    * executor is insufficient, throughput can be dramatically limited and deadlock may occur.
    * 
    * @param consumer the consumer that accepts the generator's output and uses it to yield
    *       generated values
    * @param executor an executor
    * @return a new generator that uses the given executor to run generation logic
    */
   public static <T, U> UncheckedGenerator<T, U> create(BiConsumer<U, Output<T, U>> consumer,
         Executor executor) {
      if (consumer == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T, U>(executor) {
         @Override protected void run(U initialValue, Output<T, U> out) {
            consumer.accept(initialValue, out);
         }
      };
   }
   
   /**
    * Creates a new generator whose generation logic is performed by a {@link Consumer} that accepts
    * the generator's output. The new generator uses the specified executor to run logic for each
    * execution.
    * 
    * <p>Care must be exercised when using a custom executor. If the parallelism allowed by the
    * executor is insufficient, throughput can be dramatically limited and deadlock may occur.
    *
    * @param consumer the consumer that accepts the generator's output and uses it to yield
    *       generated values
    * @param executor an executor
    * @return a new generator that uses the given executor to run generation logic
    */
   public static <T> UncheckedGenerator<T, Void> create(Consumer<Output<T, Void>> consumer,
         Executor executor) {
      if (consumer == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T, Void>(executor) {
         @Override protected void run(Void initialValue, Output<T, Void> out) {
            consumer.accept(out);
         }
      };
   }
   
   final Executor executor;

   /**
    * Constructs a new generator that uses a default, shared thread pool to run generation logic.
    */
   protected Generator() {
      this(SHARED_EXECUTOR);
   }

   /**
    * Constructs a new generator that uses the given executor to run generation logic.
    *
    * <p>Care must be exercised when using a custom executor. If the parallelism allowed by the
    * executor is insufficient, throughput can be dramatically limited and deadlock may occur.
    * 
    * @param executor an executor
    */
   protected Generator(Executor executor) {
      this.executor = executor;
   }

   /**
    * Performs generation logic. To send values to the sequence consumer, invoke
    * {@link Output#yield(Object)} on the given output object.
    *
    * @param initialValue the initial value supplied to the generator, in the first call by the
    *    consumer to {@link Sequence#next(Object)}
    * @param out the output to which generated values are sent
    * @throws X if an error occurs while generating values
    */
   protected abstract void run(U initialValue, Output<T, U> out) throws X;
   
   /**
    * Returns a view of this generator as an {@link Iterable}. Exceptions thrown during generation
    * will result in runtime exceptions being thrown from the iterator's {@code next()} and/or
    * {@code hasNext()} methods.
    * 
    * <p>Each call to the returned object's {@link Iterable#iterator() iterator} method starts a new
    * sequence.
    * 
    * @return a view of this generator as an {@link Iterable}.
    * 
    * @see Sequence#asIterator()
    */
   public Iterable<T> asIterable() {
      return () -> start().asIterator();
   }
   
   /**
    * Starts generation of a sequence of values. Each call to this method will asynchronously invoke
    * {@link #run(Object, Output)}. If the generator is started in the same thread that calls this
    * method, it will abort (since that would otherwise result in deadlock). In that case, the very
    * first call to {@link Sequence#next(Object)} will throw a {@link DeadlockException}.
    *
    * @return the sequence of generated values
    */
   public Sequence<T, U, X> start() {
      return new SequenceImpl().start();
   }

   /**
    * A sentinel value indicating a null element being returned from the sequence. We don't use an
    * actual {@code null} reference since that indicates that the value hasn't yet been computed.
    */
   static final Object NULL = new Object();
   
   /**
    * A sentinel value indicating that the sequence is finished. This is a terminal value: no
    * further values will be emitted by the sequence.
    */
   static final Object FINISHED = new Object();
   
   /**
    * A value indicating that the sequence failed. This is a terminal value: no further values will
    * be emitted by the sequence.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Failure {
      Throwable cause;
      
      Failure(Throwable cause) {
         this.cause = cause;
      }
   }
   
   /**
    * Returns true if the given value is a terminal value (e.g. the very last value emitted by a
    * sequence).
    *
    * @param o a value
    * @return true if the given value is a terminal value
    */
   static final boolean isTerminal(Object o) {
      return o == FINISHED || o instanceof Failure;
   }
   
   /**
    * The synchronization mechanism which allows handing control over from consumer to producer
    * thread and vice versa.
    *
    * @param <T> the type of values emitted by the producer
    * @param <U> the type of values passed to the producer
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync<T, U> {
      @SuppressWarnings("rawtypes")
      private static final AtomicReferenceFieldUpdater<Sync, Thread> CONSUMER =
            AtomicReferenceFieldUpdater.newUpdater(Sync.class, Thread.class, "consumerThread");
      
      volatile Thread producerThread;
      volatile Object producerValue;
      volatile Thread consumerThread;
      volatile Object consumerValue;
      
      Sync() {
      }
      
      /**
       * Queries the producer for the next value. This should only be called by a consumer thread.
       * This parks the current thread and unparks the producer thread. This thread is unparked and
       * returns when the producer next yields a value or if the producer terminates.
       * 
       * <p>Once the producer emits a terminal value, any subsequent calls to this method will
       * return that final value.
       * 
       * <p>The returned object will be one of the following:
       * <ul>
       * <li>A value, an instance of type {@code T}</li>
       * <li>A {@link Failure}, if the generator terminated due to an exception</li>
       * <li>Or {@link Generator#FINISHED FINISHED}, if the generator terminated normally</li>
       * </ul>
       *
       * @return the object emitted by the producer
       * @throws ConcurrentModificationException if another consumer thread is waiting (a generator
       *       should only be used by one consumer thread at a time)
       */
      Object queryProducer(U u) {
         Thread th = producerThread;
         if (th == null) {
            assert isTerminal(producerValue);
            return producerValue;
         }
         if (!CONSUMER.compareAndSet(this, null, Thread.currentThread())) {
            throw new ConcurrentModificationException(
                  "Sequence cannot be used simultaneously from two threads");
         }
         consumerValue = u != null ? u : NULL;
         boolean interrupted = false;
         // let producer compute the next value
         LockSupport.unpark(th);
         try {
            while (true) {
               Object ret = producerValue;
               if (ret != null) {
                  if (isTerminal(ret)) {
                     producerThread = null;
                  } else {
                     producerValue = null;
                  }
                  assert consumerValue == null;
                  assert consumerThread == null;
                  return ret != NULL ? ret : null;
               }
               LockSupport.park(this);
               if (Thread.interrupted()) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) {
               Thread.currentThread().interrupt();
            }
         }
      }
      
      /**
       * Sends a value to the consumer thread. This should only be called by the producer thread.
       *
       * @param t the value to send to the consumer
       */
      void sendValueToConsumer(T t) {
         boolean sent = sendToConsumer(t != null ? t : NULL);
         assert sent;
      }

      /**
       * Sends a failure to the consumer thread. This should only be called by the producer thread.
       * This is a terminal value.
       *
       * @param t the failure to send to the consumer thread
       */
      void sendFailureToConsumer(Throwable t) {
         assert t != null;
         sendToConsumer(new Failure(t));
      }

      /**
       * Signals the end of values to the consumer thread. This should only be called by the
       * producer thread. This is a terminal value.
       */
      void sendEndToConsumer() {
         sendToConsumer(FINISHED);
      }
      
      /**
       * Waits for the next query from a consumer thread. This should only be called by the producer
       * thread.
       *
       * @param onInterrupt a block that is executed if this thread gets interrupted while waiting
       *       on the consumer to query the producer
       */
      U waitForNextQuery(Runnable onInterrupt) {
         Object ret;
         while ((ret = consumerValue) == null) {
            LockSupport.park(this);
            if (Thread.interrupted()) {
               onInterrupt.run();
            }
         }
         consumerValue = null;
         assert consumerThread != null;
         @SuppressWarnings("unchecked")
         U u = (U) (ret == NULL ? null : ret);
         return u;
      }

      /**
       * Sends the given value to the consumer (only called from producer thread). Returns true if
       * the consumer was unparked and allowed to consume the value. Returns false if no consumer
       * was waiting for it (should not usually happen, but can in cases of abnormal termination of
       * the generator).
       *
       * @param o the value to send to the consumer
       * @return true if a consumer was waiting for the value
       */
      private boolean sendToConsumer(Object o) {
         Thread th = CONSUMER.getAndSet(this, null);
         assert o != null;
         while (producerValue != null) {
            // WTF? Consumer still hasn't consumed the last value? That means we were invoked from
            // a second consumer thread. In the interest of not throwing an exception here in the
            // producer thread, we'll let it slide. We'd rather detect this in consumer thread and
            // throw there. And that's exactly what we try to do when we CAS the consumerThread
            // field in #queryProducer(Object).
            Thread.yield();
         }
         producerValue = o;
         if (th == null) {
            return false;
         } else {
            LockSupport.unpark(th);
            return true;
         }
      }

      /*
       * The following two methods are synchronized so that setting the thread and interrupting it
       * are mutually exclusive. This avoids a race where the producer is shutting down and sets the
       * field to null while a concurrent thread is trying to interrupt it. In such a scenario, the
       * interrupter could see the field as non-null, but then the producer terminate, and then the
       * interrupt delivered. This could spuriously interrupt a subsequent task on the same thread.
       * So synchronized lets us ensure that the thread is interrupted if-and-only-if the producer
       * is still running.
       */
      
      /**
       * Sets the producer thread. This is used from the producer thread to initialize the value and
       * to clear it when the producer terminates.
       *
       * @param producerThread the producer thread or {@code null}
       */
      synchronized void setProducer(Thread producerThread) {
         this.producerThread = producerThread;
      }
      
      /**
       * Interrupts the producer thread if there is one. This is used to clean-up in the event of
       * abandoned sequences. In such a case, the producer thread may be blocked indefinitely,
       * waiting on the consumer to query it again. When the sequence is garbage collected, it will
       * interrupt the producer thread and allow it to exit and release resources (including the
       * thread).
       */
      synchronized void interruptProducer() {
         Thread th = producerThread;
         if (th != null) {
            th.interrupt();
         }
      }
   }
   
   /**
    * An implementation of {@link Sequence}, for providing generated values to consumers.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SequenceImpl implements Sequence<T, U, X> {
      private final Sync<T, U> sync = new Sync<>();
      
      SequenceImpl() {
      }
      
      /**
       * Interrupts the generator thread when the sequence is garbage collected.
       */
      @Override protected void finalize() throws Throwable {
         sync.interruptProducer();
         super.finalize();
      }
      
      /**
       * Starts the generation logic.
       */
      SequenceImpl start() {
         executor.execute(createSequenceRunner(sync, Generator.this, this));
         // wait for producer thread to be initialized (could have used a CountDownLatch or a
         // future, but why use another synchronizer object when we already have sync?)
         boolean interrupted = false;
         try {
            // have to look at both thread and value fields because producer could set thread, get
            // interrupted, set value to terminal state, and finally clear thread all while we were
            // parked
            while (sync.producerThread == null && sync.producerValue == null) {
               LockSupport.park();
               if (Thread.interrupted()) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) {
               Thread.currentThread().interrupt();
            }
         }

         return this;
      }
      
      @Override
      public T next(U u) throws X {
         Object o = sync.queryProducer(u);
         if (o == FINISHED) {
            throw new SequenceFinishedException();
         } else if (o instanceof Failure) {
            // The producer thread can only throw unchecked exceptions or instances of X, so this
            // should be safe (see Generator#run(Object, Output))
            @SuppressWarnings("unchecked")
            X x = (X) ((Failure) o).cause;
            throw x;
         } else {
            @SuppressWarnings("unchecked")
            T t = (T) o;
            return t;
         }
      }
   }

   /**
    * Constructs the block of code that is executed to generate items in the sequence. The resulting
    * object cannot have a strong reference to the sequence, so the sequence can be garbage
    * collected when consumers are no longer referencing it. The object does maintain a weak
    * reference to the sequence. When the weak reference is cleared, that indicates that there are
    * no more consumers and that the code should abort.
    *
    * @param sync the object used to transfer control back and forth between main consumer thread
    *       and producer (co-routine) thread
    * @param generator the generator object
    * @param sequence the sequence object
    * @return a task that will generate the values in the sequence
    */
   static <T, U> Runnable createSequenceRunner(Sync<T, U> sync, Generator<T, U, ?> generator,
         Sequence<T, U, ?> sequence) {
      WeakReference<Sequence<T, U, ?>> sequenceRef = new WeakReference<>(sequence);
      Variable<Thread> caller = new Variable<>(Thread.currentThread());
      // The returned task must not have a reference to the sequence other than sequenceRef. That
      // way, the sequence can be gc'ed if it is abandoned (e.g. consumer does not exhaust the
      // sequence). The task will notice this by observing sequenceRef as cleared and will try to
      // terminate.
      return () -> {
         // initialize producer field and notify caller it's been set
         sync.setProducer(Thread.currentThread());
         
         try {
            if (caller.get() == Thread.currentThread()) {
               // not allowed!
               throw new DeadlockException("Generator cannot run in same thread as caller");
            }
            LockSupport.unpark(caller.getAndSet(null)); // set to null so caller thread can be GC'ed
            VariableBoolean interrupted = new VariableBoolean();
            Runnable onInterrupt = () -> {
               interrupted.set(true);
               if (sequenceRef.get() == null) {
                  // sequence has been gc'ed; terminate
                  throw new SequenceAbandonedException();
               }
            };
            
            // wait for consumer to invoke next() for the first time
            U initialValue = sync.waitForNextQuery(onInterrupt);

            // run the generator
            Output<T, U> output = t -> {
               sync.sendValueToConsumer(t);
               U u = sync.waitForNextQuery(onInterrupt);
               if (interrupted.getAndSet(false)) {
                  // swallowing interrupts is gross: restore interrupt status
                  Thread.currentThread().interrupt();
               }
               return u;
            };
            generator.run(initialValue, output);
            
            // clean finish
            sync.sendEndToConsumer();
            
         } catch (SequenceAbandonedException e) {
            // ignore
         } catch (Throwable t) {
            // un-clean finish
            sync.sendFailureToConsumer(t);
         } finally {
            sync.setProducer(null);
         }
      };
   }
}