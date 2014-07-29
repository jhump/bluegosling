package com.apriori.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.function.BiFunction;

/**
 * A visitor for generic types. This enables dynamic-dispatch-like algorithms without the
 * {@code if-else} boiler-plate with {@code instanceof} checks and casts.
 *
 * @param <R> the type of value returned from visiting a type
 * @param <P> an optional context parameter that is supplied to the visitor when visiting a type
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Types#visit(java.lang.reflect.Type, TypeVisitor, Object)
 */
public interface TypeVisitor<R, P> {
   
   /**
    * Visits the given type. This is the entry point and generally need not be overridden by
    * implementing classes. This method will re-dispatch to one of the other visit methods based on
    * the kind of type specified.
    *
    * @param type the type
    * @param param the context parameter
    * @return the result of visiting the given type
    */
   default R visit(Type type, P param) {
      if (type instanceof Class) {
         return visitClass((Class<?>) type, param);
      } else if (type instanceof ParameterizedType) {
         return visitParameterizedType((ParameterizedType) type, param);
      } else if (type instanceof GenericArrayType) {
         return visitGenericArrayType((GenericArrayType) type, param);
      } else if (type instanceof WildcardType) {
         return visitWildcardType((WildcardType) type, param);
      } else if (type instanceof TypeVariable) {
         return visitTypeVariable((TypeVariable<?>) type, param);
      } else {
         return visitUnknownType(type, param);
      }
   }
   
   /**
    * The default action for visiting a type, called when visiting a type where the corresponding
    * {@code visit} method has not been overridden.
    * 
    * <p>The default implementation returns {@code null}.
    *
    * @param type a generic type
    * @param param the context parameter
    * @return the result of visiting the given type
    */
   @SuppressWarnings("unused") // arguments are for the benefit of sub-classes
   default R defaultAction(Type type, P param) {
      return null;
   }
   
   /**
    * Invoked when the visited type is a class token.
    *
    * @param clazz the class token
    * @param param the context parameter
    * @return the result of visiting the given class
    */
   default R visitClass(Class<?> clazz, P param) {
      return defaultAction(clazz, param);
   }

   /**
    * Invoked when the visited type is a parameterized type.
    *
    * @param parameterizedType the parameterized type
    * @param param the context parameter
    * @return the result of visiting the given parameterized type
    */
   default R visitParameterizedType(ParameterizedType parameterizedType, P param) {
      return defaultAction(parameterizedType, param);
   }

   /**
    * Invoked when the visited type is a generic array type.
    *
    * @param arrayType the generic array type
    * @param param the context parameter
    * @return the result of visiting the given generic array type
    */
   default R visitGenericArrayType(GenericArrayType arrayType, P param) {
      return defaultAction(arrayType, param);
   }

   /**
    * Invoked when the visited type is a wildcard type.
    *
    * @param wildcardType the wildcard type
    * @param param the context parameter
    * @return the result of visiting the given wildcard type
    */
   default R visitWildcardType(WildcardType wildcardType, P param) {
      return defaultAction(wildcardType, param);
   }

   /**
    * Invoked when the visited type is a type variable.
    *
    * @param typeVariable the type variable
    * @param param the context parameter
    * @return the result of visiting the given type variable
    */
   default R visitTypeVariable(TypeVariable<?> typeVariable, P param) {
      return defaultAction(typeVariable, param);
   }
   
   /**
    * Invoked when the visited type is not a known sub-type of the {@link Type} interface. 
    *
    * @param type the type
    * @param param the context parameter
    * @return the result of visiting the given type
    */
   default R visitUnknownType(Type type, P param) {
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
      private BiFunction<? super Type, ? super P, ? extends R> defaultAction; 
      private BiFunction<? super Class<?>, ? super P, ? extends R> classAction; 
      private BiFunction<? super ParameterizedType, ? super P, ? extends R> paramTypeAction; 
      private BiFunction<? super GenericArrayType, ? super P, ? extends R> arrayAction; 
      private BiFunction<? super WildcardType, ? super P, ? extends R> wildcardAction; 
      private BiFunction<? super TypeVariable<?>, ? super P, ? extends R> typeVarAction;
      private BiFunction<? super Type, ? super P, ? extends R> unknownTypeAction;

      /**
       * Defines the default action, invoked for types where no other, more specific action has been
       * configured.
       *
       * @param action the default action
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> defaultAction(BiFunction<? super Type, ? super P, ? extends R> action) {
         this.defaultAction = action;
         return this;
      }
      
      /**
       * Defines the action taken when visiting a class token.
       *
       * @param action the action taken when visiting a class token
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onClass(BiFunction<? super Class<?>, ? super P, ? extends R> action) {
         this.classAction = action;
         return this;
      }

      /**
       * Defines the action taken when visiting a parameterized type.
       *
       * @param action the action taken when visiting a parameterized type
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onParameterizedType(
            BiFunction<? super ParameterizedType, ? super P, ? extends R> action) {
         this.paramTypeAction = action;
         return this;
      }

      /**
       * Defines the action taken when visiting a generic array type.
       *
       * @param action the action taken when visiting a generic array type
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onGenericArrayType(
            BiFunction<? super GenericArrayType, ? super P, ? extends R> action) {
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
            BiFunction<? super WildcardType, ? super P, ? extends R> action) {
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
            BiFunction<? super TypeVariable<?>, ? super P, ? extends R> action) {
         this.typeVarAction = action;
         return this;
      }

      /**
       * Defines the action taken when visiting an unrecognized type.
       *
       * @param action the action taken when visiting an unrecognized type
       * @return {@code this}, for method chaining
       */
      public Builder<R, P> onUnknownType(BiFunction<? super Type, ? super P, ? extends R> action) {
         this.unknownTypeAction = action;
         return this;
      }
      
      /**
       * Builds a visitor using the actions defined so far. Any actions not defined will simply use
       * the default implementation of {@link DefaultTypeVisitor}.
       *
       * @return a visitor that uses the configured actions when visiting a type
       */
      public TypeVisitor<R, P> build() {
         final BiFunction<? super Type, ? super P, ? extends R> def = defaultAction; 
         final BiFunction<? super Class<?>, ? super P, ? extends R> onClass = classAction; 
         final BiFunction<? super ParameterizedType, ? super P, ? extends R> onParamType =
               paramTypeAction; 
         final BiFunction<? super GenericArrayType, ? super P, ? extends R> onArray = arrayAction; 
         final BiFunction<? super WildcardType, ? super P, ? extends R> onWildcard = wildcardAction; 
         final BiFunction<? super TypeVariable<?>, ? super P, ? extends R> onTypeVar =
               typeVarAction;
         final BiFunction<? super Type, ? super P, ? extends R> onUnknownType = unknownTypeAction;
         
         return new TypeVisitor<R, P>() {
            @Override
            public R defaultAction(Type type, P param) {
               return def != null
                     ? def.apply(type, param)
                     : null;
            }
            
            @Override
            public R visitClass(Class<?> clazz, P param) {
               return onClass != null
                     ? onClass.apply(clazz, param)
                     : defaultAction(clazz, param);
            }

            @Override
            public R visitParameterizedType(ParameterizedType parameterizedType, P param) {
               return onParamType != null
                     ? onParamType.apply(parameterizedType, param)
                     : defaultAction(parameterizedType, param);
            }

            @Override
            public R visitGenericArrayType(GenericArrayType arrayType, P param) {
               return onArray != null
                     ? onArray.apply(arrayType, param)
                     : defaultAction(arrayType, param);
            }

            @Override
            public R visitWildcardType(WildcardType wildcardType, P param) {
               return onWildcard != null
                     ? onWildcard.apply(wildcardType, param)
                     : defaultAction(wildcardType, param);
            }

            @Override
            public R visitTypeVariable(TypeVariable<?> typeVariable, P param) {
               return onTypeVar != null
                     ? onTypeVar.apply(typeVariable, param)
                     : defaultAction(typeVariable, param);
            }
            
            @Override
            public R visitUnknownType(Type type, P param) {
               return onUnknownType != null
                     ? onUnknownType.apply(type, param)
                     : defaultAction(type, param);
            }
         };
      }
   }
}
