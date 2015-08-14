package com.apriori.apt.reflect;

import com.apriori.apt.ProcessingEnvironments;
import com.apriori.apt.reflect.ArType.Kind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Container of helper methods for working with {@link ArType}s.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class ArTypes {
   private ArTypes() {
   }
   
   /**
    * Returns a type that corresponds to the specified type mirror.
    * 
    * @param mirror a type mirror
    * @return a corresponding type
    * @throws NullPointerException if the specified type mirror is null
    */
   public static ArType forTypeMirror(TypeMirror mirror) {
      return ReflectionVisitors.TYPE_MIRROR_VISITOR.visit(mirror);
   }

   // TODO: doc...

   public static ArType forType(Type type) {
      return forType(type, ProcessingEnvironments.elements(), ProcessingEnvironments.types());
   }

   public static ArType forType(Type type, Elements elementUtils, Types typeUtils) {
      return forTypeMirror(asMirror(type, elementUtils, typeUtils));
   }

   private static TypeMirror asMirror(Type type, Elements elementUtils, Types typeUtils) {
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>) type;
         TypeElement element = elementUtils.getTypeElement(clazz.getCanonicalName());
         return typeUtils.getDeclaredType(element);
      } else if (type instanceof ParameterizedType) {
         ParameterizedType pType = (ParameterizedType) type;
         Type typeArgs[] = pType.getActualTypeArguments();
         TypeMirror typeMirrorArgs[] = new TypeMirror[typeArgs.length];
         for (int i = 0; i < typeArgs.length; i++) {
            typeMirrorArgs[i] = asMirror(typeArgs[i], elementUtils, typeUtils);
         }
         TypeElement element =
               elementUtils.getTypeElement(((Class<?>) pType.getRawType()).getCanonicalName());
         Type owner = pType.getOwnerType();
         if (owner == null) {
            return typeUtils.getDeclaredType(element, typeMirrorArgs);
         } else {
            return typeUtils.getDeclaredType(
                  (DeclaredType) asMirror(owner, elementUtils, typeUtils), element, typeMirrorArgs);
         }
      } else if (type instanceof GenericArrayType) {
         GenericArrayType aType = (GenericArrayType) type;
         return typeUtils.getArrayType(
               asMirror(aType.getGenericComponentType(), elementUtils, typeUtils));
      } else if (type instanceof WildcardType) {
         WildcardType wcType = (WildcardType) type;
         TypeMirror extendsBound, superBound;
         Type[] bounds = wcType.getLowerBounds();
         if (bounds.length > 0) {
            assert bounds.length == 1;
            assert wcType.getUpperBounds().length == 0
                  || (wcType.getUpperBounds().length == 1
                        && wcType.getUpperBounds()[0] == Object.class);
            superBound = asMirror(bounds[0], elementUtils, typeUtils);
            extendsBound = null;
         } else {
            bounds = wcType.getUpperBounds();
            if (bounds.length == 0) {
               superBound = null;
               extendsBound = null;
            } else {
               assert bounds.length == 1;
               superBound = null;
               extendsBound = asMirror(bounds[0], elementUtils, typeUtils);
            }
         }
         return typeUtils.getWildcardType(extendsBound, superBound);
      } else if (type instanceof TypeVariable) {
         TypeVariable<?> typeVar = (TypeVariable<?>) type;
         GenericDeclaration gd = typeVar.getGenericDeclaration();
         Parameterizable element;
         if (gd instanceof Class) {
            Class<?> clazz = (Class<?>) gd;
            element = elementUtils.getTypeElement(clazz.getCanonicalName());
         } else if (gd instanceof Constructor || gd instanceof Method) {
            element = findExecutable((Executable) gd, elementUtils, typeUtils);
         } else {
            throw new IllegalArgumentException("Unprocessable generic declaration: " + gd);
         }
         if (element == null) {
            throw new IllegalArgumentException(
                  "Could not resolve element for generic declaration: " + gd);
         }
         return findTypeParameter(element, typeVar.getName()).asType();
      } else {
         throw new IllegalArgumentException("Unprocessable type: " + type.getTypeName());
      }
   }
   
   private static ExecutableElement findExecutable(Executable ex, Elements elementUtils,
         Types typeUtils) {
      TypeElement el = elementUtils.getTypeElement(ex.getDeclaringClass().getCanonicalName());
      if (el == null) {
         return null;
      }
      for (Element en : el.getEnclosedElements()) {
         ExecutableElement exel = null;
         if (ex instanceof Constructor && en.getKind() == ElementKind.CONSTRUCTOR) {
            exel = (ExecutableElement) en;
         } else if (ex instanceof Method && en.getKind() == ElementKind.METHOD) {
            // for a method, need to check name and return type
            if (ex.getName().equals(en.getSimpleName().toString())) {
               TypeElement retType =
                     elementUtils.getTypeElement(((Method) ex).getReturnType().getCanonicalName());
               if (retType != null) {
                  exel = (ExecutableElement) en;
                  if (!typeUtils.isSameType(typeUtils.erasure(exel.getReturnType()),
                        retType.asType())) {
                     exel = null; // never mind, wrong return type
                  }
               }
            }
         }
         if (exel != null) {
            // check parameter types
            Class<?>[] pTypes = ex.getParameterTypes();
            List<? extends VariableElement> pElems = exel.getParameters();
            int count = pElems.size(); 
            if (count == pTypes.length) {
               boolean match = true;
               for (int idx = 0; idx < count; idx++) {
                  TypeElement paramType =
                        elementUtils.getTypeElement(pTypes[idx].getCanonicalName());
                  if (paramType == null
                        || !typeUtils.isSameType(typeUtils.erasure(pElems.get(idx).asType()),
                              paramType.asType())) {
                     match = false;
                     break;
                  }
               }
               if (match) {
                  return exel;
               }
            }
         }
      }
      return null;
   }
   
   private static TypeParameterElement findTypeParameter(Parameterizable element, String name) {
      for (TypeParameterElement param : element.getTypeParameters()) {
         if (param.getSimpleName().toString().equals(name)) {
            return param;
         }
      }
      throw new IllegalArgumentException(
            "Given declaration has no type parameter named " + name + ": " + element);
   }
   
   /**
    * Downcasts the specified type as a {@link ArClass}.
    * 
    * @param type a type
    * @return a class
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not {@link ArType.Kind#CLASS}
    */
   public static ArClass asClass(ArType type) {
      if (type.getTypeKind() != Kind.CLASS) {
         throw new IllegalArgumentException("Specified type is not a Class");
      }
      return (ArClass) type;
   }

   /**
    * Downcasts the specified type as a {@link ArGenericArrayType}.
    * 
    * @param type a type
    * @return a generic array type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#GENERIC_ARRAY_TYPE}
    */
   public static ArGenericArrayType asGenericArrayType(ArType type) {
      if (type.getTypeKind() != Kind.GENERIC_ARRAY_TYPE) {
         throw new IllegalArgumentException("Specified type is not a GenericArrayType");
      }
      return (ArGenericArrayType) type;
   }

   /**
    * Downcasts the specified type as a {@link ArParameterizedType}.
    * 
    * @param type a type
    * @return a parameterized type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#PARAMETERIZED_TYPE}
    */
   public static ArParameterizedType asParameterizedType(ArType type) {
      if (type.getTypeKind() != Kind.PARAMETERIZED_TYPE) {
         throw new IllegalArgumentException("Specified type is not a ParameterizedType");
      }
      return (ArParameterizedType) type;
   }

   /**
    * Downcasts the specified type as a {@link ArTypeVariable}.
    * 
    * @param type a type
    * @return a type variable
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#TYPE_VARIABLE}
    */
   public static ArTypeVariable<?> asTypeVariable(ArType type) {
      if (type.getTypeKind() != Kind.TYPE_VARIABLE) {
         throw new IllegalArgumentException("Specified type is not a TypeVariable");
      }
      return (ArTypeVariable<?>) type;
   }
   
   /**
    * Downcasts the specified type as a {@link ArWildcardType}.
    * 
    * @param type a type
    * @return a wildcard type
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type's kind is not
    *       {@link ArType.Kind#WILDCARD_TYPE}
    */
   public static ArWildcardType asWildcardType(ArType type) {
      if (type.getTypeKind() != Kind.WILDCARD_TYPE) {
         throw new IllegalArgumentException("Specified type is not a WildcardType");
      }
      return (ArWildcardType) type;
   }
}