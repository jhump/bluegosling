package com.apriori.testing;

/**
 * Thrown when a query for a method signature isn't specific enough. Several of
 * the query methods on {@link InterfaceVerifier} can return a single matching method.
 * However, if there are multiple method signatures that match the query criteria,
 * this exception is thrown.
 * 
 * @author jhumphries
 */
@SuppressWarnings("serial")
public class TooManyMatchesException extends RuntimeException {
   
   /**
    * Constructs a new exception. The trigger of the exception is a query for
    * the specified criteria, which resulted in the specified number of results
    * (which will be greater than one).
    * 
    * @param criteria   Search criteria used when exception was generated
    * @param results    The number of matching method signatures (> 1)
    */
   public TooManyMatchesException(String criteria, int results) {
      super("Found too many matches (" + results + ") for search: " + criteria);
   }
}