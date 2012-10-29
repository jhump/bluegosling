package com.apriori.apt.reflect;

import java.util.List;

/**
 * An interface that represents a member that has executable code -- a constructor or a method. This
 * interface factors out many common methods between the two regarding parameters and exception
 * declarations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ExecutableMember extends Member {

   /**
    * Returns the list of parameters declared for the method or constructor.
    * 
    * <p>Sadly, though Java allows co-variant return types in overridden
    * methods, it does not allow co-variance where the return types'
    * erasures are the same and are co-variant only based on generic
    * parameters. So methods with more specific return types (actual
    * type parameters instead of wildcards) must have different names.
    * 
    * @return the list of parameters
    * 
    * @see Method#getMethodParameters()
    * @see Constructor#getConstructorParameters()
    */
   List<Parameter<?>> getParameters();
   
   /**
    * Returns the list of parameter types. The returned types are raw types with any generic
    * type information erased. For full type information, use {@link #getGenericParameterTypes()}.
    * 
    * @return the list of parameter types
    * 
    * @see java.lang.reflect.Constructor#getParameterTypes()
    * @see java.lang.reflect.Method#getParameterTypes()
    */
   List<Class> getParameterTypes();

   /**
    * Returns the list of parameter types. The returned types include all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the list of parameter types
    * 
    * @see java.lang.reflect.Constructor#getGenericParameterTypes()
    * @see java.lang.reflect.Method#getGenericParameterTypes()
    */
   List<Type> getGenericParameterTypes();
   
   /**
    * Determines whether this executable member can be invoked with variable arity (aka var-args).
    * If so, then the last parameter type must be an array into which the var-args are assembled
    * prior to dispatch.
    * 
    * @return true if this member has variable arity; false otherwise
    * 
    * @see java.lang.reflect.Constructor#isVarArgs()
    * @see java.lang.reflect.Method#isVarArgs()
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
    * @see java.lang.reflect.Constructor#getParameterAnnotations()
    * @see java.lang.reflect.Method#getParameterAnnotations()
    */
   List<List<Annotation>> getParameterAnnotations();
   
   /**
    * Returns the list of declared exception types that can be thrown by this executable member. The
    * returned types are raw types with any generic type information erased. For full type
    * information, use {@link #getGenericExceptionTypes()}.
    * 
    * @return the list of parameter types
    * 
    * @see java.lang.reflect.Constructor#getExceptionTypes()
    * @see java.lang.reflect.Method#getExceptionTypes()
    */
   List<Class> getExceptionTypes();

   /**
    * Returns the list of declared exception types that can be thrown by this executable memebr. The
    * returned types include all generic type information that was specified in the source
    * declaration of the type.
    * 
    * @return the list of exception types
    * 
    * @see java.lang.reflect.Constructor#getGenericExceptionTypes()
    * @see java.lang.reflect.Method#getGenericExceptionTypes()
    */
   List<Type> getGenericExceptionTypes();
}
