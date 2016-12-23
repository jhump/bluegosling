package com.bluegosling.apt.reflect;

import java.lang.reflect.Executable;
import java.util.List;

import javax.lang.model.element.ExecutableElement;

/**
 * An interface that represents a member that has executable code -- a constructor or a method. This
 * interface factors out many common methods between the two regarding parameters and exception
 * declarations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Executable
 * @see ExecutableElement
 */
public interface ArExecutableMember extends ArMember {

   /**
    * Returns the list of parameters declared for the method or constructor.
    * 
    * @return the list of parameters
    * 
    * @see Executable#getParameters()
    * @see ExecutableElement#getParameters()
    */
   List<? extends ArParameter<?>> getParameters();
   
   /**
    * Returns the list of parameter types. The returned types are raw types with any generic
    * type information erased. For full type information, use {@link #getGenericParameterTypes()}.
    * 
    * @return the list of parameter types
    * 
    * @see Executable#getParameterTypes()
    */
   List<ArClass> getParameterTypes();

   /**
    * Returns the list of parameter types. The returned types include all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the list of parameter types
    * 
    * @see Executable#getGenericParameterTypes()
    */
   List<ArType> getGenericParameterTypes();
   
   /**
    * Determines whether this executable member can be invoked with variable arity (aka var-args).
    * If so, then the last parameter type must be an array into which the var-args are assembled
    * prior to dispatch.
    * 
    * @return true if this member has variable arity; false otherwise
    * 
    * @see Executable#isVarArgs()
    */
   boolean isVarArgs();
   
   /**
    * Returns lists of parameter annotations, one list for each parameter. If there are no
    * parameters then an empty list is returned. Otherwise, the annotations are returned as a list
    * of lists, with one list corresponding to each parameter. If a given parameter has no
    * annotations then the list corresponding to that parameter will be empty.
    * 
    * @return the list of lists of annotations
    * 
    * @see Executable#getParameterAnnotations()
    */
   List<List<ArAnnotation>> getParameterAnnotations();
   
   /**
    * Returns the list of declared exception types that can be thrown by this executable member. The
    * returned types are raw types with any generic type information erased. For full type
    * information, use {@link #getGenericExceptionTypes()}.
    * 
    * @return the list of parameter types
    * 
    * @see Executable#getExceptionTypes()
    */
   List<ArClass> getExceptionTypes();

   /**
    * Returns the list of declared exception types that can be thrown by this executable memebr. The
    * returned types include all generic type information that was specified in the source
    * declaration of the type.
    * 
    * @return the list of exception types
    * 
    * @see Executable#getGenericExceptionTypes()
    * @see ExecutableElement#getThrownTypes()
    */
   List<ArType> getGenericExceptionTypes();
}
