package com.apriori.reflect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method signature, composed of a method name and sequence of parameter types.
 * 
 * <p>
 * {@code MethodSignature} objects are used in place of {@code java.lang.reflect.Method} objects
 * when configuring an {@link com.apriori.testing.InterfaceVerifier InterfaceVerifier}. A proxy
 * could implement multiple {@code Method}s with identical signatures if it is constructed for
 * multiple interfaces. A {@link MethodSignature} represents all such methods.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MethodSignature {

   private final String name;
   private final List<Class<?>> argTypes;

   /**
    * Constructs a new signature for the specified method.
    * 
    * @param m the method
    * @throws NullPointerException If the specified method is {@code null}
    */
   public MethodSignature(Method m) {
      this(m.getName(), m.getParameterTypes());
   }

   /**
    * Constructs a new signature based on the given name and list of arguments.
    * 
    * @param name The method name
    * @param argTypes The parameter types
    * @throws NullPointerException If the specified method name is {@code null} or any of the
    *            specified argument types is {@code null}
    */
   public MethodSignature(String name, Class<?>... argTypes) {
      if (name == null) {
         throw new NullPointerException();
      }
      for (Class<?> arg : argTypes) {
         if (arg == null) {
            throw new NullPointerException();
         }
      }
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
         return name.equals(ms.name) && argTypes.equals(ms.argTypes);
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
         }
         else {
            sb.append(", ");
         }
         sb.append(argType.getTypeName());
      }
      sb.append(")");
      return sb.toString();
   }
}
