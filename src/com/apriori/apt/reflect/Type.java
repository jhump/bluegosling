package com.apriori.apt.reflect;

import javax.lang.model.type.TypeMirror;


/**
 * A interface that represents a generic type. Using implementors of this interface,
 * information on generic type variables and even reifiable type parameters can be
 * programmatically determined for processed type elements.
 * 
 * <p>This mirrors the {@link java.lang.reflect.Type java.lang.reflect.Type} interface, but is for
 * representing types in source form for annotation processing vs. types in compiled
 * form at runtime.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see java.lang.reflect.Type
 * @see Types
 */
public interface Type {
   
   /**
    * A visitor for simulating multiple dispatch over the concrete implementations of {@link Type}.
    * This can be used to more elegantly handle polymorphism of types than using numerous
    * {@code if-else} type checks (either using {@code instanceof} or looking
    * at the value returned from {@link Type#getTypeKind()}) and then downcasts (or equivalent
    * calls to methods in {@link Types}, such as {@link Types#asClass(Type)}).
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <R> the type of value returned by the visitor
    * @param <P> an optional parameter that is passed to the visitor (if not needed, use {@link Void})
    * 
    * @see Type#accept(Visitor, Object)
    */
   interface Visitor<R, P> {
      /**
       * Called from {@link Type#accept(Visitor, Object)} to visit a {@link Class}.
       */
      R visitClass(Class clazz, P p);

      /**
       * Called from {@link Type#accept(Visitor, Object)} to visit a {@link GenericArrayType}.
       */
      R visitGenericArrayType(GenericArrayType arrayType, P p);
      
      /**
       * Called from {@link Type#accept(Visitor, Object)} to visit a {@link ParameterizedType}.
       */
      R visitParameterizedType(ParameterizedType parameterizedType, P p);

      /**
       * Called from {@link Type#accept(Visitor, Object)} to visit a {@link TypeVariable}.
       */
      R visitTypeVariable(TypeVariable<?> typeVariable, P p);
      
      /**
       * Called from {@link Type#accept(Visitor, Object)} to visit a {@link WildcardType}.
       */
      R visitWildcardType(WildcardType wildcardType, P p);
   }
   
   /**
    * An enumeration of the valid concrete kinds of {@link Type}s.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   enum Kind {
      /**
       * A class (or interface, enum, or annotation type) declaration or a raw/erased type. This
       * kind is also used to represent primitive types and array types that do not refer to
       * parameterized types or type variables.
       * 
       * @see Class
       */
      CLASS,

      /**
       * An array type whose component type refers to a parameterized type or type variable.
       * 
       * @see GenericArrayType
       */
      GENERIC_ARRAY_TYPE,
      
      /**
       * A parameterized type.
       * 
       * @see ParameterizedType
       */
      PARAMETERIZED_TYPE,
      
      /**
       * A type variable.
       * 
       * @see TypeVariable
       */
      TYPE_VARIABLE,
      
      /**
       * A wildcard type, which is any use of "?" in a type parameter.
       * 
       * @see WildcardType
       */
      WILDCARD_TYPE
   }
   
   /**
    * Uses the specified visitor to visit this type. This will dispatch one of the various methods
    * on the visitor based on the runtime type of this type.
    * 
    * @param visitor the visitor
    * @param p the optional parameter for the visitor
    * @return the value returned by the visitor
    */
   <R, P> R accept(Visitor<R, P> visitor, P p);

   /**
    * Gets the kind of this type.
    * 
    * @return the kind of this type
    */
   Kind getTypeKind();

   /**
    * Returns the underlying {@link TypeMirror} represented by this type.
    * 
    * @return the underlying type mirror
    */
   TypeMirror asTypeMirror();

   /**
    * Returns a string representation of this type, suitable for constructing a representation of
    * this type that could be used in Java source code. For most types, this is the same as
    * {@code toString()}, but one glaring exception to that is in {@link Class} where
    * {@link Class#toString() toString()} and {@link Class#toGenericString() toGenericString()}
    * return representations that resemble type declarations but {@link Class#toTypeString()
    * toTypeString()} returns a string that is a simple reference to the class's raw/erased type.
    * 
    * @return a string representation of this type
    */
   String toTypeString();
}
