package com.bluegosling.apt.reflect;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;

import javax.lang.model.element.Element;

/**
 * An element that can have annotations. This includes representations of things in the Java
 * programming language which can be annotated, like packages, types, fields, constructors, methods,
 * and parameters.
 * 
 * <p>The Java programming language also allows local variables (including exception variables
 * defined in catch blocks) to be annotated, but support for this during annotation processing is
 * incomplete, so there currently are no such implementations.
 *
 * <p>This interface is analogous to {@link AnnotatedElement}, except that it represents a
 * source-level type (during annotation processing) instead of an actual runtime type.  Unlike
 * core reflection, this interface can be used to inspect annotations whose retention
 * policy is {@link RetentionPolicy#CLASS} or {@link RetentionPolicy#SOURCE}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotatedElement
 * @see Element
 */
public interface ArAnnotatedElement extends ArAnnotatedConstruct {
   /**
    * Returns the underlying {@link Element} for this object.
    * 
    * @return the underlying element
    */
   Element asElement();
}
