package com.apriori.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;

/**
 * An experimental class to provide generator functionality in Java. Since the Java language does
 * not support a "yield" statement, generators cannot be done the normal way. So, instead of
 * the generator and consumer running in the same thread, and the generator's state/stack being
 * saved every time a value is produced and control returned to the consumer, this implementation
 * has the generator run on a separate thread. This approach has many downsides and is not useful
 * in practical situations for the following reasons:
 * <ol>
 * <li>A new thread is required for each iteration through the generated sequence. This makes it
 * easy to have a huge number of outstanding threads. It is possible to cause
 * {@link OutOfMemoryError}s this way since that is what happens when the JVM process cannot
 * allocate any more native threads. Using a thread pool to limit the number of threads can cause
 * the generators to appear frozen. This can happen if garbage collection is not happening
 * frequently, so unused generator threads continue to tie up threads in the pool. It also limits
 * the number of generated sequences that can be examined concurrently.
 * </ol>Transfer of control from one thread to another is significantly more expensive than just
 * popping data from the stack into the heap and then returning control to a caller. So this
 * approach leads to slow iteration. A value must be transferred from one thread to another and
 * threads must be parked and unparked for each transfer of control, from consumer to generator and
 * then back from generator to consumer.
 * </li>
 * Despite its impracticality, it was a fun experiment to implement. The trickier bits of the
 * implementation are to avoid thread leaks. We must be able to detect if a consumer never reaches
 * the end of the stream and the generator thread never finishes its work. When that happens we
 * must interrupt the thread so that it exits. It may be possible to do this using phantom
 * references, but checking the reference frequently enough from the generator thread can be tricky.
 * It would likely require polling the reference while the thread waits for the consumer to return
 * control to the generator. Instead, a finalizer is used. When the consumer no longer has a
 * reference to the sequence, the sequence can be collected. When its finalizer is executed, it will
 * interrupt the generator thread. The generator has a weak reference to the sequence so it can
 * distinguish between this interruption and other interruptions that it should ignore (in order to
 * best simulate a single-threaded generator pattern).
 *
 * @param <T> the type of value produced by the generator
 * @param <X> the type of exception that may be thrown while generating a value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see UncheckedGenerator
 */
//TODO: test
public abstract class Generator<T, X extends Throwable> {

   /**
    * Represents the output of a generated sequence. A generator sends data to its consumer by
    * providing data through this interface. For symmetry with generator functionality in other
    * languages, the output method name of "yield" was chosen.
    *
    * @param <T> the type of value produced
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface Output<T> {
      /**
       * Yields a value from the generator. This sends the specified value to the consumer and
       * also suspends the generator until the consumer requests another value.
       *
       * @param t the value to send to the consumer
       */
      void yield(T t);
   }
   
   /**
    * Runs a generator (in a different thread).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private interface Runner {
      /**
       * Executes the specified block and returns a future.
       *
       * @param runnable the block of code to execute
       * @return a future that will complete when the block finishes executing
       */
      Future<?> run(Runnable runnable);
   }
   
   /**
    * A runner that always creates a new thread to execute code.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static enum ThreadRunner implements Runner {
      /**
       * The singleton instance of this class. No other instances are needed since this class is
       * stateless.
       */
      INSTANCE;
      
      @Override public Future<?> run(Runnable runnable) {
         FutureTask<?> task = new FutureTask<Void>(runnable, null);
         new Thread(task).start();
         return task;
      }
   }

   /**
    * A runner that always creates a new thread using a given {@link ThreadFactory}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ThreadFactoryRunner implements Runner {
      private final ThreadFactory factory;
      
      ThreadFactoryRunner(ThreadFactory factory) {
         this.factory = factory;
      }
      
      @Override public Future<?> run(Runnable runnable) {
         FutureTask<?> task = new FutureTask<Void>(runnable, null);
         factory.newThread(task).start();
         return task;
      }
   }
   
   /**
    * A runner that submits code to an {@link Executor}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ExecutorRunner implements Runner {
      private final Executor executor;
      
      ExecutorRunner(Executor executor) {
         this.executor = executor;
      }
      
      @Override public Future<?> run(Runnable runnable) {
         FutureTask<?> task = new FutureTask<Void>(runnable, null);
         executor.execute(task);
         return task;
      }
   }

   /**
    * Creates a new generator whose generation logic is performed by a {@link Sink} that accepts
    * the generator's output. This will prove useful API in Java 8 since {@link Sink} is a
    * functional interface. The new generated creates new threads for each iteration.
    *
    * @param sink the sink that accepts the generator's output and uses it to yield generated values
    * @return a new generator that creates a new thread for each iteration
    */
   public static <T> UncheckedGenerator<T> create(final Sink<Output<T>> sink) {
      if (sink == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T>() {
         @Override protected void run(Output<T> out) {
            sink.accept(out);
         }
      };
   }

   /**
    * Creates a new generator whose generation logic is performed by a {@link Sink} that accepts
    * the generator's output. This will prove useful API in Java 8 since {@link Sink} is a
    * functional interface. The new generated uses the specified factory to create new threads for
    * each iteration.
    *
    * @param sink the sink that accepts the generator's output and uses it to yield generated values
    * @param factory a thread factory
    * @return a new generator that uses the given factory to create a new thread for each iteration
    */
   public static <T> UncheckedGenerator<T> create(final Sink<Output<T>> sink,
         ThreadFactory factory) {
      if (sink == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T>(factory) {
         @Override protected void run(Output<T> out) {
            sink.accept(out);
         }
      };
   }

   /**
    * Creates a new generator whose generation logic is performed by a {@link Sink} that accepts
    * the generator's output. This will prove useful API in Java 8 since {@link Sink} is a
    * functional interface. The new generator uses the specified executor to run logic for each
    * execution.
    *
    * @param sink the sink that accepts the generator's output and uses it to yield generated values
    * @param executor an executor
    * @return a new generator that uses the given executor to run generation logic
    */
   public static <T> UncheckedGenerator<T> create(final Sink<Output<T>> sink,
         Executor executor) {
      if (sink == null) {
         throw new NullPointerException();
      }
      return new UncheckedGenerator<T>(executor) {
         @Override protected void run(Output<T> out) {
            sink.accept(out);
         }
      };
   }
   
   final Runner runner;

   /**
    * Constructs a new generator that creates a new thread for each iteration.
    */
   protected Generator() {
      this(ThreadRunner.INSTANCE);
   }

   /**
    * Constructs a new generator that uses the given thread factory to create a new thread for each
    * iteration.
    *
    * @param factory a thread factory
    */
   protected Generator(ThreadFactory factory) {
      this(new ThreadFactoryRunner(factory));
   }

   /**
    * Constructs a new generator that uses the given executor to run generation logic.
    *
    * @param executor an executor
    */
   protected Generator(Executor executor) {
      this(new ExecutorRunner(executor));
   }

   private Generator(Runner runner) {
      this.runner = runner;
   }

   /**
    * Performs generation logic. To send values to the sequence consumer, invoke
    * {@link Output#yield(Object)} on the given output object.
    *
    * @param out the output to which generated values are sent
    * @throws X if an error occurs while generating values
    */
   protected abstract void run(Output<T> out) throws X;
   
   /**
    * Returns a view of this generator as an {@link Iterable}. Exceptions thrown during generation
    * will result in runtime exceptions being thrown from the iterator's {@code next()} and/or
    * {@code hasNext()} methods.
    *
    * @return a view of this generator as an {@link Iterable}.
    */
   public Iterable<T> asIterable() {
      return new Iterable<T>() {
         @Override
         public Iterator<T> iterator() {
            return start().asIterator();
         }
      };
   }
   
   /**
    * Starts generation of a sequence of values. Each call to this method will asynchronously invoke
    * {@link #run(Output)}.
    *
    * @return the sequence of generated values
    */
   public Sequence<T, X> start() {
      SequenceImpl sequence = new SequenceImpl();
      sequence.start();
      return sequence;
   }
   
   /**
    * Puts a value in the specified queue, ignoring interruptions.
    *
    * @param queue the queue
    * @param element the element to put into the queue
    */
   static <E> void put(SynchronousQueue<E> queue, E element) {
      put(queue, element, null);
   }
   
   /**
    * Puts a value in the specified queue, running the given block on interruption.
    *
    * @param queue the queue
    * @param element the element to put into the queue
    * @param onInterrupt code to run when the thread is interrupted while waiting to put an element
    *       in the queue, or {@code null} to indicate that the interrupt should be ignored
    */
   static <E> void put(SynchronousQueue<E> queue, E element, Runnable onInterrupt) {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               queue.put(element);
               break;
            } catch (InterruptedException e) {
               interrupted = true;
               if (onInterrupt != null) {
                  onInterrupt.run();
               }
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }

   /**
    * Takes a value from the specified queue, ignoring interruptions.
    *
    * @param queue the queue
    * @return the element to taken from the queue
    */
   static <E> E take(SynchronousQueue<E> queue) {
      return take(queue, null);
   }
   
   /**
    * Takes a value from the specified queue, running the given block on interruption.
    *
    * @param queue the queue
    * @param onInterrupt code to run when the thread is interrupted while waiting to take an element
    *       from the queue, or {@code null} to indicate that the interrupt should be ignored
    * @return the element to taken from the queue
    */
   static <E> E take(SynchronousQueue<E> queue, Runnable onInterrupt) {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               return queue.take();
            } catch (InterruptedException e) {
               interrupted = true;
               if (onInterrupt != null) {
                  onInterrupt.run();
               }
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }

   /**
    * Singleton message object, used to hand control from the consumer to the generator, to compute
    * and return the next item in the sequence. 
    */
   static final Object NEXT = new Object();
   
   /**
    * An exception thrown in the consuming thread when the generator fails. The cause of this
    * exception will be what caused the generator thread to fail.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @SuppressWarnings("serial")
   private static class SequenceException extends RuntimeException {
      SequenceException(Throwable cause) {
         super(cause);
      }
   }

   /**
    * An exception thrown in the generator thread to indicate that the corresponding sequence is
    * no longer live. This means that the generator has no more consumers and the generator thread
    * should exit.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @SuppressWarnings("serial")
   private static class SequenceIsDeadException extends RuntimeException {
      SequenceIsDeadException() {
      }
   }

   /**
    * An implementation of {@link Sequence}, for providing generated values to consumers.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SequenceImpl implements Sequence<T, X> {
      final SynchronousQueue<Source<T>> fromProducer = new SynchronousQueue<Source<T>>();
      final SynchronousQueue<Object> fromConsumer = new SynchronousQueue<Object>();
      Future<?> future;
      boolean finished;
      
      SequenceImpl() {
      }
      
      /**
       * Interrupts the generator thread when the sequence is garbage collected. This is done by
       * canceling the future that represents the generation logic.
       */
      @Override protected void finalize() throws Throwable {
         if (future != null) {
            future.cancel(true);
         }
         super.finalize();
      }
      
      /**
       * Starts the generation logic.
       */
      void start() {
         future = runner.run(createSequenceRunner(fromProducer, fromConsumer, Generator.this, this));
      }
      
      /**
       * Returns the next item in the sequence. This blocks until the generation logic provides the
       * next value or sends a message indicating the sequence is finished or generation failed.
       * 
       * @return the next item in the sequence
       * @throws X if the generation logic failed (the exception thrown is the same instance that
       *       was thrown in the generation logic)
       */
      @Override
      public T next() throws X {
         if (finished) {
            throw new SequenceFinishedException();
         }
         put(fromConsumer,  NEXT);
         try {
            return take(fromProducer).get();
         } catch (SequenceFinishedException e) {
            finished = true;
            throw e;
         } catch (SequenceException e) {
            finished = true;
            @SuppressWarnings("unchecked")
            X x = (X) e.getCause();
            throw x;
         }
      }
      
      @Override
      public Iterator<T> asIterator() {
         return new Iterator<T>() {
            private T next;
            private boolean retrievedNext;
            private boolean hasNext;
            
            private void retrieveNext() {
               if (retrievedNext) {
                  return;
               }
               try {
                  next = SequenceImpl.this.next();
                  hasNext = true;
               } catch (SequenceFinishedException e) {
                  hasNext = false;
               } catch (Throwable t) {
                  hasNext = false;
                  if (t instanceof Error) {
                     throw (Error) t;
                  } else if (t instanceof RuntimeException) {
                     throw (RuntimeException) t;
                  } else {
                     throw new RuntimeException(t);
                  }
               } finally {
                  retrievedNext = true;
               }
            }
            
            @Override
            public boolean hasNext() {
               retrieveNext();
               return hasNext;
            }

            @Override
            public T next() {
               retrieveNext();
               if (!hasNext) {
                  throw new NoSuchElementException();
               }
               retrievedNext = false;
               T ret = next;
               next = null; // let it be collected
               return ret;
            }

            @Override
            public void remove() {
               throw new UnsupportedOperationException();
            }
         };
      }
   }
   
   /**
    * Constructs the block of code that is executed to generate items in the sequence. The resulting
    * object cannot have a strong reference to the sequence, so the sequence can be garbage
    * collected when consumers are no longer referencing it. The object does maintain a weak
    * reference to the sequence. When the weak reference is cleared, that indicates that there are
    * no more consumers and that the code should abort.
    *
    * @param fromProducer the queue used to transfer data from the generator thread to the consumer
    * @param fromConsumer the queue used to transfer messages from the consumer to the generator
    * @param generator the generator object
    * @param sequence the sequence object
    * @return a task that will generate the values in the sequence
    */
   static <T> Runnable createSequenceRunner(final SynchronousQueue<Source<T>> fromProducer,
         final SynchronousQueue<Object> fromConsumer, final Generator<T, ?> generator,
         Sequence<T, ?> sequence) {
      final WeakReference<Sequence<T, ?>> sequenceRef = new WeakReference<Sequence<T,?>>(sequence);
      final Runnable onInterrupt = new Runnable() {
         @Override
         public void run() {
            if (sequenceRef.get() == null) {
               throw new SequenceIsDeadException();
            }
         }
      };
      return new Runnable() {
         @Override
         public void run() {
            Throwable failure = null;
            try {
               // wait for consumer to invoke next() for the first time
               take(fromConsumer);
               
               Output<T> output = new Output<T>() {
                  @Override
                  public void yield(T t) {
                     // send value to consumer
                     put(fromProducer, Sources.of(t), onInterrupt);
                     // and wait for it invoke next() again
                     take(fromConsumer, onInterrupt);
                  }
               };
      
               generator.run(output);
               
            } catch (Throwable t) {
               failure = t;
               
            } finally {
               if (sequenceRef.get() != null) {
                  try {
                     if (failure == null) {
                        put(fromProducer, new Source<T>() {
                           @Override
                           public T get() {
                              throw new SequenceFinishedException();
                           }
                        }, onInterrupt);
                     } else {
                        final Throwable t = failure;
                        put(fromProducer, new Source<T>() {
                           @Override
                           public T get() {
                              throw new SequenceException(t);
                           }
                        }, onInterrupt);
                     }
                  } catch (SequenceIsDeadException ignored) {
                  }
               }
            }
         }
      };
   }
}