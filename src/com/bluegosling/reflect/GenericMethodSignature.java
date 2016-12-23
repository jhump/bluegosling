package com.bluegosling.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a generic method signature, composed of a method name and sequence of parameter types.
 * 
 * <p>{@code MethodSignature} objects are used in place of {@code java.lang.reflect.Method} objects
 * when configuring an {@link com.bluegosling.testing.InterfaceVerifier InterfaceVerifier}. A proxy
 * could implement multiple {@code Method}s with identical signatures if it is constructed for
 * multiple interfaces. A {@link MethodSignature} represents all such methods.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class GenericMethodSignature {
   private final String name;
   private final List<Type> argTypes;

   /**
    * Constructs a new signature for the specified method.
    * 
    * @param m the method
    * @throws NullPointerException If the specified method is {@code null}
    */
   public GenericMethodSignature(Method m) {
      this.name = m.getName();
      this.argTypes = Arrays.asList(m.getGenericParameterTypes());
   }

   /**
    * Constructs a new signature based on the given name and list of arguments.
    * 
    * @param name The method name
    * @param argTypes The parameter types
    * @throws NullPointerException If the specified method name is {@code null} or any of the
    *            specified argument types is {@code null}
    */
   public GenericMethodSignature(String name, Type... argTypes) {
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
   public GenericMethodSignature(String name, List<Type> argTypes) {
      this.name = requireNonNull(name);
      List<Type> list = new ArrayList<>(argTypes);
      for (Type arg : list) {
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
   public List<Type> getParameterTypes() {
      return Collections.unmodifiableList(argTypes);
   }
   
   /**
    * Returns the erased method signature. The erased signature has the same name as this signature,
    * but each parameter in the erased signature will be the {@linkplain Types#getErasure(Type)
    * erasure} of the corresponding parameter in this signature.
    * 
    * @return the erased method signature
    */
   public MethodSignature erasure() {
      List<Class<?>> erasedTypes = new ArrayList<>(argTypes.size());
      for (Type t : argTypes) {
         erasedTypes.add(Types.getErasure(t));
      }
      return new MethodSignature(name, erasedTypes);
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof GenericMethodSignature) {
         GenericMethodSignature gms = (GenericMethodSignature) o;
         if (!name.equals(gms.name)) {
            return false;
         }
         if (argTypes.size() != gms.argTypes.size()) {
            return false;
         }
         for (int i = 0; i < argTypes.size(); i++) {
            if (!Types.equals(argTypes.get(i), gms.argTypes.get(i))) {
               return false;
            }
         }
         return true;
      }
      return false;
   }

   @Override
   public int hashCode() {
      int hash = name.hashCode();
      for (Type arg : argTypes) {
         hash = Types.hashCode(arg) + 31 * hash;
      }
      return hash;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name);
      sb.append('(');
      boolean first = true;
      for (Type argType : argTypes) {
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
