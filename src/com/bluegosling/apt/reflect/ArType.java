package com.bluegosling.apt.reflect;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

import javax.lang.model.type.TypeMirror;


/**
 * A interface that represents a generic type. Using implementors of this interface, information on
 * generic type variables and even reifiable type parameters can be programmatically determined for
 * processed type elements.
 * 
 * <p>This mirrors the {@link Type} interface, but is for representing types in source form for
 * annotation processing vs. types in compiled form at runtime.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Type
 * @see AnnotatedType
 * @see TypeMirror
 * @see ArTypes
 */
public abstract class ArType extends ArAbstractAnnotatedConstruct<TypeMirror> {
   
   ArType(TypeMirror mirror) {
      super(mirror);
   }

   /**
    * A visitor for simulating multiple dispatch over the concrete implementations of
    * {@link ArType}. This can be used to more elegantly handle polymorphism of types than using
    * numerous {@code if-else} type checks (either using {@code instanceof} or looking at the value
    * returned from {@link ArType#getTypeKind()}) and then downcasts (or equivalent calls to methods
    * in {@link ArTypes}, such as {@link ArTypes#asClass(ArType)}).
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <R> the type of value returned by the visitor
    * @param <P> an optional parameter that is passed to the visitor (if not needed, use
    *    {@link Void})
    * 
    * @see ArType#accept(Visitor, Object)
    */
   interface Visitor<R, P> {
      /**
       * Visits a declared type. This is called from {@link ArType#accept(Visitor, Object)} when the
       * type being visited is an {@link ArDeclaredType}.
       * 
       * @param declaredType the declared type being visited
       * @param p extra parameter supplied to {@link ArType#accept(Visitor, Object)}
       * @return the result of visiting this declared type
       */
      R visitDeclaredType(ArDeclaredType declaredType, P p);

      /**
       * Visits a primitive type. This is called from {@link ArType#accept(Visitor, Object)} when
       * the type being visited is an {@link ArAnnotatedPrimitiveType}.
       * 
       * @param primitiveType the primitive type being visited
       * @param p extra parameter supplied to {@link ArType#accept(Visitor, Object)}
       * @return the result of visiting this declared type
       */
      R visitPrimitiveType(ArPrimitiveType primitiveType, P p);

      /**
       * Visits a array type. This is called from {@link ArType#accept(Visitor, Object)} when the
       * type being visited is an {@link ArArrayType}.
       * 
       * @param arrayType the array type being visited
       * @param p extra parameter supplied to {@link ArType#accept(Visitor, Object)}
       * @return the result of visiting this array type
       */
      R visitArrayType(ArArrayType arrayType, P p);
      
      /**
       * Visits a type variable This is called from {@link ArType#accept(Visitor, Object)} when the
       * type being visited is an {@link ArTypeVariable}.
       * 
       * @param typeVariable the type variable being visited
       * @param p extra parameter supplied to {@link ArType#accept(Visitor, Object)}
       * @return the result of visiting this type variable
       */
      R visitTypeVariable(ArTypeVariable typeVariable, P p);
      
      /**
       * Visits a wildcard type. This is called from {@link ArType#accept(Visitor, Object)} when the
       * type being visited is an {@link ArWildcardType}.
       * 
       * @param wildcardType the wildcard type being visited
       * @param p extra parameter supplied to {@link ArType#accept(Visitor, Object)}
       * @return the result of visiting this wildcard type
       */
      R visitWildcardType(ArWildcardType wildcardType, P p);
   }
   
   /**
    * An enumeration of the valid concrete kinds of {@link ArType}s.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   enum Kind {
      /**
       * A declared type, which can be an interface, enum, or annotation type. It could be a
       * parameterized type or it could be a raw/non-generic type.
       * 
       * @see ArDeclaredType
       */
      DECLARED_TYPE,
      
      /**
       * A primitive type or {@code void}.
       * 
       * @see ArPrimitiveType
       */
      PRIMITIVE_TYPE,

      /**
       * An array type.
       * 
       * @see ArArrayType
       */
      ARRAY_TYPE,
      
      /**
       * A type variable.
       * 
       * @see ArTypeVariable
       */
      TYPE_VARIABLE,
      
      /**
       * A wildcard type, which is any use of "?" in a type parameter.
       * 
       * @see ArWildcardType
       */
      WILDCARD_TYPE
   }
   
   /**
    * Uses the specified visitor to visit this type. This will dispatch one of the various methods
    * on the visitor based on the runtime type of this type.
    * 
    * @param visitor the visitor
    * @param p the optional parameter for the visitor
    * @param <R> the type of the value returned by the visitor
    * @param <P> the type of the optional parameter for the visitor
    * @return the value returned by the visitor
    */
   public abstract <R, P> R accept(Visitor<R, P> visitor, P p);

   /**
    * Gets the kind of this type.
    * 
    * @return the kind of this type
    */
   public abstract Kind getTypeKind();

   /**
    * Returns the underlying {@link TypeMirror} represented by this type.
    * 
    * @return the underlying type mirror
    */
   public TypeMirror asTypeMirror() {
      return delegate();
   }

   /**
    * Returns a string representation of this type, suitable for constructing a representation of
    * this type that could be used in Java source code.
    * 
    * @return a string representation of this type
    */
   @Override
   public abstract String toString();
}
