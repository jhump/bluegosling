package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a method signature, composed of a method name and sequence of parameter types.
 * 
f * <p>{@code MethodSignature} objects are used in place of {@code java.lang.reflect.Method} objects
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
      this.name = m.getName();
      this.argTypes = Arrays.asList(m.getParameterTypes());
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
      this(name, Arrays.asList(argTypes));
   }

   /**
    * Constructs a new signature based on the given name and list of arguments.
    * 
    * @param name The method name
    * @param argTypes The parameter types
    * @throws NullPointerException If the specified method name is {@code null} or any of the
    *            specified argument types is {@code null}
    */
   public MethodSignature(String name, List<Class<?>> argTypes) {
      this.name = requireNonNull(name);
      List<Class<?>> list = new ArrayList<>(argTypes);
      for (Class<?> arg : list) {
         requireNonNull(arg);
      }
      this.argTypes = list;
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
      return Objects.hash(name, argTypes);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name);
      sb.append('(');
      boolean first = true;
      for (Class<?> argType : argTypes) {
         if (first) {
            first = false;
         }
         else {
            sb.append(',');
         }
         sb.append(argType.getTypeName());
      }
      sb.append(')');
      return sb.toString();
   }
}
