package com.bluegosling.apt.reflect;

import java.lang.reflect.GenericDeclaration;
import java.util.List;

import javax.lang.model.element.Parameterizable;

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
 * @see Parameterizable
 */
public interface ArGenericDeclaration {
   /**
    * Returns the list of type parameters for the declaring method, constructor, or class.
    * 
    * @return the list of type variables
    */
   List<? extends ArTypeParameter<?>> getTypeParameters();
}
