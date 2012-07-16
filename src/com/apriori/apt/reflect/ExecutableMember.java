package com.apriori.apt.reflect;

import java.util.List;

// TODO: javadoc
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
   
   List<Class> getParameterTypes();

   List<Type> getGenericParameterTypes();
   
   boolean isVarArgs();
   
   List<List<Annotation>> getParameterAnnotations();
   
   List<Class> getExceptionTypes();

   List<Type> getGenericExceptionTypes();
}
