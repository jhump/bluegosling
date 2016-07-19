package com.bluegosling.choice;

import com.bluegosling.possible.Possible;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * An object that represents a choice between one of a few options. This type represents a tagged
 * union (aka disjoint union or discriminating union). The types of values from which to choose can
 * be heterogeneous so, for type safety, each option has its own type variable.
 * 
 * <p>Implementing classes must ensure that one and only one option is available. It is not valid,
 * for example, for two of three options to be present in a choice out of three. This should be
 * intuitive for those with experience using union types in other languages.
 * 
 * <p>This base interface provides operations for a choice between <em>at least</em> two options.
 * Also included are sub-interfaces, named like <tt>Ops<em>N</em></tt>, that extend the operations
 * for choices of at least {@linkplain Ops3 three} {@linkplain Ops4 or} {@linkplain Ops5 more}
 * options. To prevent the onerousness of long-winded type declarations, five options is the limit.
 * 
 * <p>There are also more precise interfaces, named like <tt>Of<em>Num</em></tt>, for choices
 * between <em>exactly</em> {@linkplain OfTwo two}, {@linkplain OfThree three},
 * {@linkplain OfFour or} {@linkplain OfFive more} options.
 *
 * @param <A> the type of the first choice
 * @param <B> the type of the second choice
 * 
 * @see Choices
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: describe in doc why interfaces don't have contract* methods?
public interface Choice<A, B> {
   
   /**
    * Determines if the first option is present. For any given choice, exactly one of the
    * {@code has*} methods will return true.
    *
    * @return true if the first option is present, false otherwise
    */
   boolean hasFirst();
   
   /**
    * Determines if the second option is present. For any given choice, exactly one of the
    * {@code has*} methods will return true.
    *
    * @return true if the second option is present, false otherwise
    */
   boolean hasSecond();
   
   /**
    * Retrieves the value of whichever option is present.
    *
    * @return the value of whichever option is present
    */
   Object get();

   /**
    * Retrieves the value of the first option, if it is present.
    *
    * @return the value of the first option, if present
    * @throws NoSuchElementException if the first option is not present
    * 
    * @see #hasFirst()
    */
   A getFirst();

   /**
    * Retrieves the value of the second option, if it is present.
    *
    * @return the value of the second option, if present
    * @throws NoSuchElementException if the second option is not present
    * 
    * @see #hasSecond()
    */
   B getSecond();

   /**
    * Retrieves the possible value of the first option. The returned value will not be
    * {@linkplain Possible#isPresent() present} if the first option is not present.
    *
    * @return the possible value of the first option
    */
   Possible<A> tryFirst();

   /**
    * Retrieves the possible value of the second option. The returned value will not be
    * {@linkplain Possible#isPresent() present} if the second option is not present.
    *
    * @return the possible value of the second option
    */
   Possible<B> trySecond();
   
   /**
    * Transforms the value of the first option. If the first option is not present, this
    * effectively does nothing. If the first option is present, the given function is applied to
    * its value and a new choice is returned whose first option is the function's result.
    *
    * @param function the function to apply to the first option
    * @return a choice with the first option, it it was present, transformed by applying the given
    *       function to it
    */
   <T> Choice<T, B> mapFirst(Function<? super A, ? extends T> function);

   /**
    * Transforms the value of the second option. If the second option is not present, this
    * effectively does nothing. If the second option is present, the given function is applied to
    * its value and a new choice is returned whose second option is the function's result.
    *
    * @param function the function to apply to the second option
    * @return a choice with the second option, it it was present, transformed by applying the given
    *       function to it
    */
   <T> Choice<A, T> mapSecond(Function<? super B, ? extends T> function);
   
   /**
    * Extends the {@link Choice} interface with operations for choices with at least three options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * @param <C> the type of the third option
    * 
    * @see Choice.OfThree
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops3<A, B, C> extends Choice<A, B> {
      /**
       * Determines if the third option is present. For any given choice, exactly one of the
       * {@code has*} methods will return true.
       *
       * @return true if the third option is present, false otherwise
       */
      boolean hasThird();

      /**
       * Retrieves the value of the third option, if it is present.
       *
       * @return the value of the third option, if present
       * @throws NoSuchElementException if the third option is not present
       * 
       * @see #hasThird()
       */
      C getThird();

      /**
       * Retrieves the possible value of the third option. The returned value will not be
       * {@linkplain Possible#isPresent() present} if the third option is not present.
       *
       * @return the possible value of the third option
       */
      Possible<C> tryThird();
      
      // co-variantly constrains return type to an Ops3
      @Override <T> Ops3<T, B, C> mapFirst(Function<? super A, ? extends T> function);

      // co-variantly constrains return type to an Ops3
      @Override <T> Ops3<A, T, C> mapSecond(Function<? super B, ? extends T> function);
      
      /**
       * Transforms the value of the third option. If the third option is not present, this
       * effectively does nothing. If the third option is present, the given function is applied to
       * its value and a new choice is returned whose third option is the function's result.
       *
       * @param function the function to apply to the third option
       * @return a choice with the third option, it it was present, transformed by applying the given
       *       function to it
       */
      <T> Ops3<A, B, T> mapThird(Function<? super C, ? extends T> function);
   }

   /**
    * Extends the {@link Choice} interface with operations for choices with at least four options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * @param <C> the type of the third option
    * @param <D> the type of the fourth option
    * 
    * @see Choice.OfFour
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops4<A, B, C, D> extends Ops3<A, B, C> {
      /**
       * Determines if the fourth option is present. For any given choice, exactly one of the
       * {@code has*} methods will return true.
       *
       * @return true if the fourth option is present, false otherwise
       */
      boolean hasFourth();

      /**
       * Retrieves the value of the fourth option, if it is present.
       *
       * @return the value of the fourth option, if present
       * @throws NoSuchElementException if the fourth option is not present
       * 
       * @see #hasFourth()
       */
      D getFourth();

      /**
       * Retrieves the possible value of the fourth option. The returned value will not be
       * {@linkplain Possible#isPresent() present} if the fourth option is not present.
       *
       * @return the possible value of the fourth option
       */
      Possible<D> tryFourth();

      // co-variantly constrains return type to an Ops4
      @Override <T> Ops4<T, B, C, D> mapFirst(Function<? super A, ? extends T> function);

      // co-variantly constrains return type to an Ops4
      @Override <T> Ops4<A, T, C, D> mapSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constrains return type to an Ops4
      @Override <T> Ops4<A, B, T, D> mapThird(Function<? super C, ? extends T> function);
      
      /**
       * Transforms the value of the fourth option. If the fourth option is not present, this
       * effectively does nothing. If the fourth option is present, the given function is applied to
       * its value and a new choice is returned whose fourth option is the function's result.
       *
       * @param function the function to apply to the fourth option
       * @return a choice with the fourth option, it it was present, transformed by applying the given
       *       function to it
       */
      <T> Ops4<A, B, C, T> mapFourth(Function<? super D, ? extends T> function);
   }

   /**
    * Extends the {@link Choice} interface with operations for choices with at least five options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * @param <C> the type of the third option
    * @param <D> the type of the fourth option
    * @param <E> the type of the fifth option
    * 
    * @see Choice.OfFive
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops5<A, B, C, D, E> extends Ops4<A, B, C, D> {
      /**
       * Determines if the fifth option is present. For any given choice, exactly one of the
       * {@code has*} methods will return true.
       *
       * @return true if the fifth option is present, false otherwise
       */
      boolean hasFifth();

      /**
       * Retrieves the value of the fifth option, if it is present.
       *
       * @return the value of the fifth option, if present
       * @throws NoSuchElementException if the fifth option is not present
       * 
       * @see #hasFifth()
       */
      E getFifth();

      /**
       * Retrieves the possible value of the fifth option. The returned value will not be
       * {@linkplain Possible#isPresent() present} if the fifth option is not present.
       *
       * @return the possible value of the fifth option
       */
      Possible<E> tryFifth();
      
      // co-variantly constrains return type to an Ops5
      @Override <T> Ops5<T, B, C, D, E> mapFirst(Function<? super A, ? extends T> function);

      // co-variantly constrains return type to an Ops5
      @Override <T> Ops5<A, T, C, D, E> mapSecond(Function<? super B, ? extends T> function);

      // co-variantly constrains return type to an Ops5
      @Override <T> Ops5<A, B, T, D, E> mapThird(Function<? super C, ? extends T> function);
      
      // co-variantly constrains return type to an Ops5
      @Override <T> Ops5<A, B, C, T, E> mapFourth(Function<? super D, ? extends T> function);

      /**
       * Transforms the value of the fifth option. If the fifth option is not present, this
       * effectively does nothing. If the fifth option is present, the given function is applied to
       * its value and a new choice is returned whose fifth option is the function's result.
       *
       * @param function the function to apply to the fifth option
       * @return a choice with the fifth option, it it was present, transformed by applying the given
       *       function to it
       */
      <T> Ops5<A, B, C, D, T> mapFifth(Function<? super E, ? extends T> function);
   }
   
   /**
    * A visitor for a choice with two options. When passed to {@link OfTwo#visit(VisitorOfTwo)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface VisitorOfTwo<A, B, R> {
      /**
       * Called when visiting a choice where the first option is present.
       *
       * @param a the value of the first option that was present
       * @return the result of visiting the option
       */
      R visitFirst(A a);

      /**
       * Called when visiting a choice where the second option is present.
       *
       * @param b the value of the second option that was present
       * @return the result of visiting the option
       */
      R visitSecond(B b);
   }
   
   /**
    * A visitor for a choice with three options. When passed to {@link OfThree#visit(VisitorOfThree)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <C> the type of the choice's third option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface VisitorOfThree<A, B, C, R> extends VisitorOfTwo<A, B, R> {
      /**
       * Called when visiting a choice where the third option is present.
       *
       * @param c the value of the third option that was present
       * @return the result of visiting the option
       */
      R visitThird(C c);
   }

   /**
    * A visitor for a choice with four options. When passed to {@link OfFour#visit(VisitorOfFour)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <C> the type of the choice's third option
    * @param <D> the type of the choice's fourth option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface VisitorOfFour<A, B, C, D, R> extends VisitorOfThree<A, B, C, R> {
      /**
       * Called when visiting a choice where the fourth option is present.
       *
       * @param d the value of the fourth option that was present
       * @return the result of visiting the option
       */
      R visitFourth(D d);
   }

   /**
    * A visitor for a choice with five options. When passed to {@link OfFive#visit(VisitorOfFive)},
    * one of the {@code visit*} methods will be invoked, depending on which option is present.
    *
    * @param <A> the type of the choice's first option
    * @param <B> the type of the choice's second option
    * @param <C> the type of the choice's third option
    * @param <D> the type of the choice's fourth option
    * @param <E> the type of the choice's fifth option
    * @param <R> the result type from visiting the choice
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface VisitorOfFive<A, B, C, D, E, R> extends VisitorOfFour<A, B, C, D, R> {
      /**
       * Called when visiting a choice where the fifth option is present.
       *
       * @param e the value of the fifth option that was present
       * @return the result of visiting the option
       */
      R visitFifth(E e);
   }
   
   /**
    * Operations for a {@link Choice} that has exactly two options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface OfTwo<A, B> extends Choice<A, B> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link VisitorOfTwo#visitFirst(Object)} will be invoked.
       *
       * @param <R> the type of result from visiting
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor);
      
      /**
       * Expands this choice of two options into a choice of three options whose first option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @param <C> the type of an absent first option in the expanded choice
       * @return a choice of three options with the same value as this and whose first option is
       *       never present
       */
      <C> OfThree<C, A, B> expandFirst();
      
      /**
       * Expands this choice of two options into a choice of three options whose second option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @param <C> the type of an absent second option in the expanded choice
       * @return a choice of three options with the same value as this and whose second option is
       *       never present
       */
      <C> OfThree<A, C, B> expandSecond();
      
      /**
       * Expands this choice of two options into a choice of three options whose third option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will also be present in the new choice.
       *
       * @param <C> the type of an absent third option in the expanded choice
       * @return a choice of three options with the same value as this and whose third option is
       *       never present
       */
      <C> OfThree<A, B, C> expandThird();

      // co-variantly constrains return type to Choice.OfTwo
      @Override <T> OfTwo<T, B> mapFirst(Function<? super A, ? extends T> function);

      // co-variantly constrains return type to Choice.OfTwo
      @Override <T> OfTwo<A, T> mapSecond(Function<? super B, ? extends T> function);
   }

   public interface OfThree<A, B, C> extends Choice.Ops3<A, B, C> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link VisitorOfThree#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor);
      
      /**
       * Expands this choice of three options into a choice of four options whose first option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of four options with the same value as this and whose first option is
       *       never present
       */
      <D> OfFour<D, A, B, C> expandFirst();
      
      /**
       * Expands this choice of three options into a choice of four options whose second option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of three options with the same value as this and whose second option is
       *       never present
       */
      <D> OfFour<A, D, B, C> expandSecond();
      
      /**
       * Expands this choice of three options into a choice of four options whose third option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of four options with the same value as this and whose third option is
       *       never present
       */
      <D> OfFour<A, B, D, C> expandThird();

      /**
       * Expands this choice of three options into a choice of four options whose fourth option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will also be present in the new choice.
       *
       * @return a choice of four options with the same value as this and whose fourth option is
       *       never present
       */
      <D> OfFour<A, B, C, D> expandFourth();
      
      // co-variantly constrains return type to Choice.OfThree
      @Override <T> OfThree<T, B, C> mapFirst(Function<? super A, ? extends T> function);

      // co-variantly constrains return type to Choice.OfThree
      @Override <T> OfThree<A, T, C> mapSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfThree
      @Override <T> OfThree<A, B, T> mapThird(Function<? super C, ? extends T> function);
   }

   public interface OfFour<A, B, C, D> extends Choice.Ops4<A, B, C, D> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link VisitorOfFour#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(VisitorOfFour<? super A, ? super B, ? super C, ? super D, R> visitor);

      /**
       * Expands this choice of four options into a choice of five options whose first option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of five options with the same value as this and whose first option is
       *       never present
       */
      <E> OfFive<E, A, B, C, D> expandFirst();
      
      /**
       * Expands this choice of four options into a choice of five options whose second option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of four options with the same value as this and whose second option is
       *       never present
       */
      <E> OfFive<A, E, B, C, D> expandSecond();
      
      /**
       * Expands this choice of four options into a choice of five options whose third option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of five options with the same value as this and whose third option is
       *       never present
       */
      <E> OfFive<A, B, E, C, D> expandThird();

      /**
       * Expands this choice of four options into a choice of five options whose fourth option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will have its position shifted to the corresponding option of the
       * new choice.
       *
       * @return a choice of five options with the same value as this and whose fourth option is
       *       never present
       */
      <E> OfFive<A, B, C, E, D> expandFourth();

      /**
       * Expands this choice of four options into a choice of five options whose fifth option is
       * never present. The new choice will have the same value as this choice. Whichever option is
       * present in this choice will also be present in the new choice.
       *
       * @return a choice of five options with the same value as this and whose fifth option is
       *       never present
       */
      <E> OfFive<A, B, C, D, E> expandFifth();

      // co-variantly constrains return type to Choice.OfFour
      @Override <T> OfFour<T, B, C, D> mapFirst(Function<? super A, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFour
      @Override <T> OfFour<A, T, C, D> mapSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFour
      @Override <T> OfFour<A, B, T, D> mapThird(Function<? super C, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFour
      @Override <T> OfFour<A, B, C, T> mapFourth(Function<? super D, ? extends T> function);
   }

   public interface OfFive<A, B, C, D, E> extends Choice.Ops5<A, B, C, D, E> {
      /**
       * Invokes the appropriate visit method on the given visitor. For example, if this choice has
       * the first option present, then {@link VisitorOfFive#visitFirst(Object)} will be invoked.
       *
       * @param visitor the visitor
       * @return the value returned by the given visitor
       */
      <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor);
      
      // co-variantly constrains return type to Choice.OfFive
      @Override <T> OfFive<T, B, C, D, E> mapFirst(Function<? super A, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFive
      @Override <T> OfFive<A, T, C, D, E> mapSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFive
      @Override <T> OfFive<A, B, T, D, E> mapThird(Function<? super C, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFive
      @Override <T> OfFive<A, B, C, T, E> mapFourth(Function<? super D, ? extends T> function);
      
      // co-variantly constrains return type to Choice.OfFive
      @Override <T> OfFive<A, B, C, D, T> mapFifth(Function<? super E, ? extends T> function);
   }
}
