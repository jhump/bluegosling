package com.bluegosling.reflect.caster;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

// TODO -- javadoc
public class AmbiguousMethodException extends Exception {
   private static final long serialVersionUID = -5266540302753855254L;

   private final Method targetMethod;
   private final Collection<Method> ambiguousMethods;
   
   AmbiguousMethodException(Method targetMethod, Collection<Method> ambiguousMethods) {
      super(""); // TODO: build message
      this.targetMethod = targetMethod;
      this.ambiguousMethods = ambiguousMethods;
   }
   
   public Method getTargetMethod() {
      return targetMethod;
   }
   
   public Collection<Method> getAmbiguousDispatchMethods() {
      return Collections.unmodifiableCollection(ambiguousMethods);
   }
}
