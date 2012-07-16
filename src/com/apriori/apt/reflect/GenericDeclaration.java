package com.apriori.apt.reflect;

import java.util.List;

//TODO: javadoc!!
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
