package com.apriori.testing;

/**
 * Thrown when a query for a method signature isn't specific enough. Several of the query methods on
 * {@link InterfaceVerifier} can return a single matching method. However, if there are multiple
 * method signatures that match the query criteria, this exception is thrown.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TooManyMatchesException extends RuntimeException {

   private static final long serialVersionUID = 8364311247452248384L;

   /**
    * Constructs a new exception. The trigger of the exception is a query for the specified
    * criteria, which resulted in the specified number of results (which will be greater than one).
    * 
    * @param criteria Search criteria used when exception was generated
    * @param results The number of matching method signatures (&gt; 1)
    */
   public TooManyMatchesException(String criteria, int results) {
      super("Found too many matches (" + results + ") for search: " + criteria);
   }
}
