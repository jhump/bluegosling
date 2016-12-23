package com.bluegosling.apt.reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a generic method signature, composed of a method name and sequence of generic
 * parameter types.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ArGenericMethodSignature {

   private final String name;
   private final List<ArType> argTypes;

   /**
    * Constructs a new signature for the specified method.
    * 
    * @param m the method
    * @throws NullPointerException If the specified method is {@code null}
    */
   public ArGenericMethodSignature(ArMethod m) {
      this(m.getName(), m.getGenericParameterTypes());
   }

   /**
    * Constructs a new signature based on the given name and list of arguments.
    * 
    * @param name The method name
    * @param argTypes The parameter types
    * @throws NullPointerException If the specified method name is {@code null} or any of the
    *            specified argument types is {@code null}
    */
   public ArGenericMethodSignature(String name, ArType... argTypes) {
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
   public ArGenericMethodSignature(String name, List<ArType> argTypes) {
      if (name == null) {
         throw new NullPointerException();
      }
      for (ArType arg : argTypes) {
         if (arg == null) {
            throw new NullPointerException();
         }
      }
      this.name = name;
      this.argTypes = new ArrayList<>(argTypes);
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
   public List<ArType> getParameterTypes() {
      return Collections.unmodifiableList(argTypes);
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ArGenericMethodSignature) {
         ArGenericMethodSignature ms = (ArGenericMethodSignature) o;
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
      for (ArType argType : argTypes) {
         if (first) {
            first = false;
         }
         else {
            sb.append(",");
         }
         sb.append(argType.toString());
      }
      sb.append(")");
      return sb.toString();
   }
}
