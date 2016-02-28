package com.bluegosling.graph;

import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Functional interfaces that represent operations executed by nodes in a computation graph.
 *
 * <p>These interfaces are very similar to {@link Function} and its variants with a few exceptions:
 * <ul>
 * <li>Functions only accept {@linkplain Function one} or {@linkplain BiFunction two} arguments.
 * Node operations can accept up to ten. Many type arguments quickly become unwieldy, so operations
 * with many arguments should usually be defined with lambdas, where the type arguments can be
 * inferred and boiler-plate in type definitions can be greatly reduced.</li>
 * <li>Functions only throw unchecked exceptions. Node operations, on the other hand, are allowed to
 * throw checked exceptions.</li>
 * </ul>
 * There is no operation interface that accepts zero arguments because the JRE already provides a
 * suitable interface: {@link Callable}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class NodeOperations {
   private NodeOperations() {
   }

   /**
    * An operation that accepts a single input and produces a result.
    *
    * @param <A> the input type
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation1<A, T> {
      /**
       * Executes the operation.
       *
       * @param in the input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in) throws Exception;
   }

   /**
    * An operation that accepts two inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation2<A, B, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2) throws Exception;
   }
   
   /**
    * An operation that accepts three inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation3<A, B, C, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3) throws Exception;
   }
   
   /**
    * An operation that accepts four inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation4<A, B, C, D, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4) throws Exception;
   }
   
   /**
    * An operation that accepts five inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation5<A, B, C, D, E, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @param in5 the fifth input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4, E in5) throws Exception;
   }
   
   /**
    * An operation that accepts six inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation6<A, B, C, D, E, F, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @param in5 the fifth input
       * @param in6 the sixth input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4, E in5, F in6) throws Exception;
   }

   /**
    * An operation that accepts seven inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation7<A, B, C, D, E, F, G, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @param in5 the fifth input
       * @param in6 the sixth input
       * @param in7 the seventh input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4, E in5, F in6, G in7) throws Exception;
   }

   /**
    * An operation that accepts eight inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation8<A, B, C, D, E, F, G, H, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @param in5 the fifth input
       * @param in6 the sixth input
       * @param in7 the seventh input
       * @param in8 the eighth input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4, E in5, F in6, G in7, H in8) throws Exception;
   }

   /**
    * An operation that accepts nine inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation9<A, B, C, D, E, F, G, H, I, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @param in5 the fifth input
       * @param in6 the sixth input
       * @param in7 the seventh input
       * @param in8 the eighth input
       * @param in9 the ninth input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4, E in5, F in6, G in7, H in8, I in9) throws Exception;
   }

   /**
    * An operation that accepts ten inputs and produces a result.
    *
    * @param <A> the type of the first input
    * @param <B> the type of the second input
    * @param <C> the type of the third input
    * @param <D> the type of the fourth input
    * @param <E> the type of the fifth input
    * @param <F> the type of the sixth input
    * @param <G> the type of the seventh input
    * @param <H> the type of the eighth input
    * @param <I> the type of the ninth input
    * @param <J> the type of the tenth input
    * @param <T> the result type
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public interface Operation10<A, B, C, D, E, F, G, H, I, J, T> {
      /**
       * Executes the operation.
       *
       * @param in1 the first input
       * @param in2 the second input
       * @param in3 the third input
       * @param in4 the fourth input
       * @param in5 the fifth input
       * @param in6 the sixth input
       * @param in7 the seventh input
       * @param in8 the eighth input
       * @param in9 the ninth input
       * @param in10 the tenth input
       * @return the result
       * @throws Exception if anything goes awry
       */
      T execute(A in1, B in2, C in3, D in4, E in5, F in6, G in7, H in8, I in9, J in10)
            throws Exception;
   }
}
