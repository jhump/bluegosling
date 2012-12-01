package com.apriori.apt.reflect;

import java.util.List;

/**
 * An element that may include the declaration of generic type variables. This currently includes
 * classes (and interfaces), methods, and constructors. This is analogous to
 * {@link java.lang.reflect.GenericDeclaration java.lang.reflect.GenericDeclaration}, except that it
 * represents elements in Java source (during annotation processing) vs. representing runtime types
 * or elements (methods and constructors) of runtime types.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.reflect.GenericDeclaration
 */
public interface GenericDeclaration {
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
    * @see Method#getMethodTypeVariables()
    * @see Constructor#getConstructorTypeVariables()
    * @see Class#getClassTypeVariables()
    */
   List<TypeVariable<?>> getTypeVariables();
   
   /**
    * Returns a string representation of the generic declaration that
    * includes references to type parameters. All type references will
    * also include generic type information, using string representations
    * built via {@link Type#toTypeString()}.
    * 
    * @return a string representation of the generic declaration
    */
   String toGenericString();
}