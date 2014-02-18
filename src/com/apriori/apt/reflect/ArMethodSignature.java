package com.apriori.apt.reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method signature, composed of a method name and sequence of parameter types.
 * 
 * This class is identical to {@link com.apriori.reflect.MethodSignature} except that it uses
 * {@link com.apriori.apt.reflect.ArClass} instead of {@link java.lang.Class} to model parameter
 * types, so it is suitable for use from an annotation processor.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see com.apriori.reflect.MethodSignature
 */
public class ArMethodSignature {

   private final String name;
   private final List<ArClass> argTypes;

   /**
    * Constructs a new signature for the specified method.
    * 
    * @param m the method
    * @throws NullPointerException If the specified method is {@code null}
    */
   public ArMethodSignature(ArMethod m) {
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
   public ArMethodSignature(String name, ArClass... argTypes) {
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
   public ArMethodSignature(String name, List<ArClass> argTypes) {
      if (name == null) {
         throw new NullPointerException();
      }
      for (ArClass arg : argTypes) {
         if (arg == null) {
            throw new NullPointerException();
         }
      }
      this.name = name;
      this.argTypes = new ArrayList<ArClass>(argTypes);
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
   public List<ArClass> getParameterTypes() {
      return Collections.unmodifiableList(argTypes);
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ArMethodSignature) {
         ArMethodSignature ms = (ArMethodSignature) o;
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
      for (ArClass argType : argTypes) {
         if (first) {
            first = false;
         }
         else {
            sb.append(",");
         }
         sb.append(argType.getName());
      }
      sb.append(")");
      return sb.toString();
   }
}
