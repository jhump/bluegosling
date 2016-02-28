package com.bluegosling.apt.reflect;

import java.lang.reflect.GenericDeclaration;
import java.util.List;

/**
 * An element that may include the declaration of generic type variables. This currently includes
 * classes (and interfaces), methods, and constructors. This is analogous to
 * {@link GenericDeclaration}, except that it represents elements in Java source (during annotation
 * processing) vs. representing runtime types or elements (methods and constructors) of runtime
 * types.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see GenericDeclaration
 */
public interface ArGenericDeclaration {
   /**
    * Returns the list of type variables for the declaring method,
    * constructor, or class.
    * 
    * <p>Sadly, though Java allows co-variant return types in overridden
    * methods, it does not allow co-variance where the return types'
    * erasures are the same and are co-variant only based on generic
    * parameters. So methods with more specific return types (actual
    * type parameters instead of wildcards) must have different names.
    * 
    * @return the list of type variables
    * 
    * @see ArMethod#getMethodTypeVariables()
    * @see ArConstructor#getConstructorTypeVariables()
    * @see ArClass#getClassTypeVariables()
    */
   List<ArTypeVariable<?>> getTypeVariables();
   
   /**
    * Returns a string representation of the generic declaration that
    * includes references to type parameters. All type references will
    * also include generic type information, using string representations
    * built via {@link ArType#toTypeString()}.
    * 
    * @return a string representation of the generic declaration
    */
   String toGenericString();
}
