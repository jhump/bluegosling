package com.apriori.choice;

import com.apriori.possible.Possible;
import com.apriori.util.Function;

/**
 * An object that represents a choice between one of a few options. This type represents a tagged
 * union (aka disjoint union or discriminating union). The types of values from which to choose can
 * be heterogeneous so, for type safety, each option has its own type variable.
 * 
 * <p>Implementing classes must ensure that one and only one option is available. It is not valid,
 * for example, for two of three options to be present in a choice out of three. This should be
 * intuitive for those with experience using union types in other languages.
 * 
 * <p>This base interface provides operations for a choice between at least two options. Also
 * included are sub-interfaces that extend the operations for choices of at least three or more
 * options. To prevent the onerousness of long-winded type declarations, five options is the limit.
 *
 * @param <A> the type of the first choice
 * @param <B> the type of the second choice
 * 
 * @see Choices
 * @see Choices.Choices2
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
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
    * @throws IllegalStateException if the first option is not present
    * 
    * @see #hasFirst()
    */
   A getFirst();

   /**
    * Retrieves the value of the second option, if it is present.
    *
    * @return the value of the second option, if present
    * @throws IllegalStateException if the second option is not present
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
   <T> Choice<T, B> transformFirst(Function<? super A, ? extends T> function);

   /**
    * Transforms the value of the second option. If the second option is not present, this
    * effectively does nothing. If the second option is present, the given function is applied to
    * its value and a new choice is returned whose second option is the function's result.
    *
    * @param function the function to apply to the second option
    * @return a choice with the second option, it it was present, transformed by applying the given
    *       function to it
    */
   <T> Choice<A, T> transformSecond(Function<? super B, ? extends T> function);
   
   /**
    * Extends the {@link Choice} interface with operations for choices with at least three options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * @param <C> the type of the third option
    * 
    * @see Choices.Choices3
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
       * @throws IllegalStateException if the third option is not present
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
      
      // co-variantly constraint return type to an Ops3
      @Override <T> Ops3<T, B, C> transformFirst(Function<? super A, ? extends T> function);

      // co-variantly constraint return type to an Ops3
      @Override <T> Ops3<A, T, C> transformSecond(Function<? super B, ? extends T> function);
      
      /**
       * Transforms the value of the third option. If the third option is not present, this
       * effectively does nothing. If the third option is present, the given function is applied to
       * its value and a new choice is returned whose third option is the function's result.
       *
       * @param function the function to apply to the third option
       * @return a choice with the third option, it it was present, transformed by applying the given
       *       function to it
       */
      <T> Ops3<A, B, T> transformThird(Function<? super C, ? extends T> function);
   }

   /**
    * Extends the {@link Choice} interface with operations for choices with at least four options.
    *
    * @param <A> the type of the first option
    * @param <B> the type of the second option
    * @param <C> the type of the third option
    * @param <D> the type of the fourth option
    * 
    * @see Choices.Choices4
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
       * @throws IllegalStateException if the fourth option is not present
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

      // co-variantly constraint return type to an Ops4
      @Override <T> Ops4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function);

      // co-variantly constraint return type to an Ops4
      @Override <T> Ops4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function);
      
      // co-variantly constraint return type to an Ops4
      @Override <T> Ops4<A, B, T, D> transformThird(Function<? super C, ? extends T> function);
      
      /**
       * Transforms the value of the fourth option. If the fourth option is not present, this
       * effectively does nothing. If the fourth option is present, the given function is applied to
       * its value and a new choice is returned whose fourth option is the function's result.
       *
       * @param function the function to apply to the fourth option
       * @return a choice with the fourth option, it it was present, transformed by applying the given
       *       function to it
       */
      <T> Ops4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function);
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
    * @see Choices.Choices5
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
       * @throws IllegalStateException if the fifth option is not present
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
      
      // co-variantly constraint return type to an Ops5
      @Override <T> Ops5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function);

      // co-variantly constraint return type to an Ops5
      @Override <T> Ops5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function);

      // co-variantly constraint return type to an Ops5
      @Override <T> Ops5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function);
      
      // co-variantly constraint return type to an Ops5
      @Override <T> Ops5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function);

      /**
       * Transforms the value of the fifth option. If the fifth option is not present, this
       * effectively does nothing. If the fifth option is present, the given function is applied to
       * its value and a new choice is returned whose fifth option is the function's result.
       *
       * @param function the function to apply to the fifth option
       * @return a choice with the fifth option, it it was present, transformed by applying the given
       *       function to it
       */
      <T> Ops5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function);
   }
}
