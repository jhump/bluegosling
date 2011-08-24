package com.apriori.testing;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method signature, composed of a method name and sequence of
 * parameter types.
 * 
 * <p>{@code MethodSignature} objects are used in place of {@code java.lang.reflect.Method}
 * objects when configuring the invocation handler. A single invocation handler could
 * implement multiple {@code Method}s with identical signatures if it is constructed for
 * multiple interfaces and an interface with the same signature is defined in more than
 * one of them.
 * 
 * @author jhumphries
 */
public class MethodSignature {
   
   private String name;
   private List<Class<?>> argTypes;

   /**
    * Constructs a new signature for the specified method.
    * 
    * @param m the method
    */
   public MethodSignature(Method m) {
      this(m.getName(), m.getParameterTypes());
   }
   
   /**
    * Constructs a new signature based on the given name and list of
    * arguments.
    * 
    * @param name the method name
    * @param argTypes the parameter types
    */
   public MethodSignature(String name, Class<?>... argTypes) {
      this.name = name;
      this.argTypes = Arrays.asList(argTypes);
   }

   /**
    * Returns the name of the method that this signature represents.
    * 
    * @return the method name
    */
   public String getName() {
      return name;
   }
   
   /**
    * Returns the parameter types for the method that this signature represents.
    * 
    * @return the list of parameter types
    */
   public List<Class<?>> getParameterTypes() {
      return Collections.unmodifiableList(argTypes);
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof MethodSignature) {
         MethodSignature ms = (MethodSignature) o;
         return this.name.equals(ms.name) && this.argTypes.equals(ms.argTypes);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      int ret = 17;
      ret = 31 * ret + name.hashCode();
      ret = 31 * ret + argTypes.hashCode();
      return ret;
   }
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name);
      sb.append("(");
      boolean first = true;
      for (Class<?> argType : argTypes) {
         if (first) {
            first = false;
         } else {
            sb.append(", ");
         }
         sb.append(argType.getName());
      }
      sb.append(")");
      return sb.toString();
   }
}