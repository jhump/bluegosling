package com.bluegosling.reflect;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.util.function.BiFunction;

/**
 * A visitor for annotated types. This enables dynamic-dispatch-like algorithms without the
 * {@code if-else} boiler-plate with {@code instanceof} checks and casts. To visit a type, use
 * the main entry-point method: {@link #visit(AnnotatedType, Object)}.
 *
 * @param <R> the type of value returned from visiting a type
 * @param <P> an optional context parameter that is supplied to the visitor when visiting a type
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface AnnotatedTypeVisitor<R, P> {
   
   /**
    * Visits the given type. This is the entry point and generally need not be overridden by
    * implementing classes. This method will re-dispatch to one of the other visit methods based on
    * the kind of given type.
    * 
    * <p>If the given type is not a known sub-interface of {@link AnnotatedType} then the
    * {@linkplain #defaultAction(AnnotatedType, Object) default action} is invoked. This is the
    * expected case when the annotated type is a non-generic declared type, a raw type (e.g. an
    * instance of a generic declared type with no type parameters), or a primitive type. It could
    * also happen for unknown kinds of annotated types.
    *
    * @param type the type
    * @param param the context parameter
    * @return the result of visiting the given type
    */
   default R visit(AnnotatedType type, P param) {
      if (type instanceof AnnotatedParameterizedType) {
         return visitParameterizedType((AnnotatedParameterizedType) type, param);
      } else if (type instanceof AnnotatedArrayType) {
         return visitArrayType((AnnotatedArrayType) type, param);
      } else if (type instanceof AnnotatedWildcardType) {
         return visitWildcardType((AnnotatedWildcardType) type, param);
      } else if (type instanceof AnnotatedTypeVariable) {
         return visitTypeVariable((AnnotatedTypeVariable) type, param);
      } else {
         return defaultAction(type, param);
      }
   }
   
   /**
    * The default action for visiting a type, called when visiting a type where the corresponding
    * {@code visit*} method has not been overridden.
    * 
    * <p>The default implementation returns {@code null}.
    *
    * @param type an annotated type
    * @param param the context parameter
    * @return the result of visiting the given type
    */
   default R defaultAction(AnnotatedType type, P param) {
      return null;
   }
   
   /**
    * Invoked when the visited type is a parameterized type.
    *
    * @param parameterizedType the parameterized type
    * @param param the context parameter
    * @return the result of visiting the given parameterized type
    */
   default R visitParameterizedType(AnnotatedParameterizedType parameterizedType, P param) {
      return defaultAction(parameterizedType, param);
   }

   /**
    * Invoked when the visited type is a generic array type.
    *
    * @param arrayType the generic array type
    * @param param the context parameter
    * @return the result of visiting the given generic array type
    */
   default R visitArrayType(AnnotatedArrayType arrayType, P param) {
      return defaultAction(arrayType, param);
   }

   /**
    * Invoked when the visited type is a wildcard type.
    *
    * @param wildcardType the wildcard type
    * @param param the context parameter
    * @return the result of visiting the given wildcard type
    */
   default R visitWildcardType(AnnotatedWildcardType wildcardType, P param) {
      return defaultAction(wildcardType, param);
   }

   /**
    * Invoked when the visited type is a type variable.
    *
    * @param typeVariable the type variable
    * @param param the context parameter
    * @return the result of visiting the given type variable
    */
   default R visitTypeVariable(AnnotatedTypeVariable typeVariable, P param) {
      return defaultAction(typeVariable, param);
   }
   
   /**
    * Invoked when the visited type is not suitable for one of the more specific {@code visit*}
    * methods. This happens when the underlying type is a non-generic declared type, a raw type
    * (a generic type with no type arguments), or a primitive type.
    * 
    * <p>This could also happen for unknown or unexpected kinds of {@link AnnotatedType}.
    * 
    * @param type an annotated type
    * @param param the context parameter
    * @return the result of visiting the given type
    */
   default R visitOtherType(AnnotatedType type, P param) {
      return defaultAction(type, param);
   }
   
   /**
    * A builder of visitors, using functional interfaces as the building blocks.
    *
    * @param <R> the type of value returned by visitors built with this builder
    * @param <P> an optional context parameter type for visitors built with this buildert
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Builder<R, P> {
      private BiFunction<? super AnnotatedType, ? super P, ? extends R> defaultAction; 
      private BiFunction<? super AnnotatedParameterizedType, ? super P, ? extends R> paramTypeAction; 
      private BiFunction<? super AnnotatedArrayType, ? super P, ? extends R> arrayAction; 
      private BiFunction<? super AnnotatedWildcardType, ? super P, ? extends R> wildcardAction; 
      private BiFunction<? super AnnotatedTypeVariable, ? super P, ? extends R> typeVarAction;
      private BiFunction<? super AnnotatedType, ? super P, ? extends R> otherTypeAction;
      
      /**
       * Defines the default action, invoked for types where no other, more specific action has been
       * configured.
       *
       * @param action the default action
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> defaultAction(
            BiFunction<? super AnnotatedType, ? super P, ? extends R> action) {
         this.defaultAction = action;
         return this;
      }
      
      /**
       * Defines the action taken when visiting a parameterized type.
       *
       * @param action the action taken when visiting a parameterized type
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onParameterizedType(
            BiFunction<? super AnnotatedParameterizedType, ? super P, ? extends R> action) {
         this.paramTypeAction = action;
         return this;
      }

      /**
       * Defines the action taken when visiting a generic array type.
       *
       * @param action the action taken when visiting a generic array type
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onArrayType(
            BiFunction<? super AnnotatedArrayType, ? super P, ? extends R> action) {
         this.arrayAction = action;
         return this;
      }

      /**
       * Defines the action taken when visiting a wildcard type.
       *
       * @param action the action taken when visiting a wildcard type
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onWildcardType(
            BiFunction<? super AnnotatedWildcardType, ? super P, ? extends R> action) {
         this.wildcardAction = action;
         return this;
      }

      /**
       * Defines the action taken when visiting a type variable.
       *
       * @param action the action taken when visiting a type variable
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onTypeVariable(
            BiFunction<? super AnnotatedTypeVariable, ? super P, ? extends R> action) {
         this.typeVarAction = action;
         return this;
      }
      
      /**
       * Defines the action taken when visiting a type that is not any of the known sub-interfaces
       * of {@link AnnotatedType}.
       *
       * @param action the action taken when visiting the type
       * @return {@code this}, for method chaining
       * 
       * @see AnnotatedTypeVisitor#visitOtherType(AnnotatedType, Object)
       */
      public Builder<R, P> onOtherType(
            BiFunction<? super AnnotatedType, ? super P, ? extends R> action) {
         this.otherTypeAction = action;
         return this;
      }

      /**
       * Builds a visitor using the actions defined so far. Any actions not defined will behave the
       * same as their default implementations.
       *
       * @return a visitor that uses the configured actions when visiting a type
       */
      public AnnotatedTypeVisitor<R, P> build() {
         final BiFunction<? super AnnotatedType, ? super P, ? extends R> def = defaultAction; 
         final BiFunction<? super AnnotatedParameterizedType, ? super P, ? extends R> onParamType =
               paramTypeAction; 
         final BiFunction<? super AnnotatedArrayType, ? super P, ? extends R> onArray = arrayAction; 
         final BiFunction<? super AnnotatedWildcardType, ? super P, ? extends R> onWildcard =
               wildcardAction; 
         final BiFunction<? super AnnotatedTypeVariable, ? super P, ? extends R> onTypeVar =
               typeVarAction;
         final BiFunction<? super AnnotatedType, ? super P, ? extends R> onOtherType =
               otherTypeAction; 
         
         return new AnnotatedTypeVisitor<R, P>() {
            @Override
            public R defaultAction(AnnotatedType type, P param) {
               return def != null
                     ? def.apply(type, param)
                     : null;
            }
            
            @Override
            public R visitParameterizedType(AnnotatedParameterizedType parameterizedType, P param) {
               return onParamType != null
                     ? onParamType.apply(parameterizedType, param)
                     : defaultAction(parameterizedType, param);
            }

            @Override
            public R visitArrayType(AnnotatedArrayType arrayType, P param) {
               return onArray != null
                     ? onArray.apply(arrayType, param)
                     : defaultAction(arrayType, param);
            }

            @Override
            public R visitWildcardType(AnnotatedWildcardType wildcardType, P param) {
               return onWildcard != null
                     ? onWildcard.apply(wildcardType, param)
                     : defaultAction(wildcardType, param);
            }

            @Override
            public R visitTypeVariable(AnnotatedTypeVariable typeVariable, P param) {
               return onTypeVar != null
                     ? onTypeVar.apply(typeVariable, param)
                     : defaultAction(typeVariable, param);
            }
            
            @Override
            public R visitOtherType(AnnotatedType type, P param) {
               return onOtherType != null
                     ? onOtherType.apply(type, param)
                     : defaultAction(type, param);
            }
         };
      }
   }
}
