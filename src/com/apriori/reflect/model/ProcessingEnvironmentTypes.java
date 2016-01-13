package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * An implementation of {@link Types} backed by an annotation processing environment. This delegates
 * most methods to a {@link javax.lang.model.util.Types} instance.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class ProcessingEnvironmentTypes implements Types {
   private final javax.lang.model.util.Types base;
   private final Elements elementUtils;
   
   ProcessingEnvironmentTypes(ProcessingEnvironment env) {
      this.base = env.getTypeUtils();
      this.elementUtils = new ProcessingEnvironmentElements(env.getElementUtils(), this);
   }
   
   ProcessingEnvironmentTypes(javax.lang.model.util.Types base, Elements elementUtils) {
      this.base = base;
      this.elementUtils = elementUtils;
   }
   
   Elements getElementUtils() {
      return elementUtils;
   }

   @Override
   public Element asElement(TypeMirror t) {
      return base.asElement(t);
   }

   @Override
   public boolean isSameType(TypeMirror t1, TypeMirror t2) {
      return base.isSameType(t1, t2);
   }

   @Override
   public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
      return base.isSubtype(t1, t2);
   }

   @Override
   public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
      return base.isAssignable(t1, t2);
   }

   @Override
   public boolean contains(TypeMirror t1, TypeMirror t2) {
      return base.contains(t1, t2);
   }

   @Override
   public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
      return base.isSubsignature(m1, m2);
   }

   @Override
   public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
      return base.directSupertypes(t);
   }

   @Override
   public TypeMirror erasure(TypeMirror t) {
      return base.erasure(t);
   }

   @Override
   public TypeElement boxedClass(PrimitiveType p) {
      return base.boxedClass(p);
   }

   @Override
   public PrimitiveType unboxedType(TypeMirror t) {
      return base.unboxedType(t);
   }

   @Override
   public TypeMirror capture(TypeMirror t) {
      return base.capture(t);
   }

   @Override
   public PrimitiveType getPrimitiveType(TypeKind kind) {
      return base.getPrimitiveType(kind);
   }

   @Override
   public NullType getNullType() {
      return base.getNullType();
   }

   @Override
   public NoType getNoType(TypeKind kind) {
      return base.getNoType(kind);
   }

   @Override
   public ArrayType getArrayType(TypeMirror componentType) {
      return base.getArrayType(componentType);
   }

   @Override
   public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
      return base.getWildcardType(extendsBound, superBound);
   }

   @Override
   public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
      return base.getDeclaredType(typeElem, typeArgs);
   }

   @Override
   public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
         TypeMirror... typeArgs) {
      return base.getDeclaredType(containing, typeElem, typeArgs);
   }

   @Override
   public TypeMirror asMemberOf(DeclaredType containing, Element element) {
      return base.asMemberOf(containing, element);
   }

   @Override
   public TypeMirror getTypeMirror(AnnotatedType type) {
      return getTypeMirror(type.getType());
   }

   @Override
   public TypeMirror getTypeMirror(Type type) {
      if (type instanceof GenericArrayType) {
         return getArrayTypeMirror((GenericArrayType) type);
      } else if (type instanceof ParameterizedType) {
         return getParameterizedTypeMirror((ParameterizedType) type);
      } else if (type instanceof java.lang.reflect.WildcardType) {
         return getWildcardTypeMirror((java.lang.reflect.WildcardType) type);
      } else if (type instanceof java.lang.reflect.TypeVariable) {
         return getTypeVariableMirror((java.lang.reflect.TypeVariable<?>) type);
      } else {
         Class<?> clazz = (Class<?>) type;
         if (clazz.isArray()) {
            return getArrayTypeMirror(clazz);
         }
         if (clazz.isPrimitive()) {
            return clazz == void.class
                  ? base.getNoType(TypeKind.VOID)
                  : getPrimitiveTypeMirror(clazz);
         }
         Class<?> owner = Modifier.isStatic(clazz.getModifiers())
               ? null : clazz.getEnclosingClass();
         return owner != null
               ? getDeclaredType((DeclaredType) getTypeMirror(owner),
                     elementUtils.getTypeElement(clazz))
               : getDeclaredType(elementUtils.getTypeElement(clazz));
      }
   }

   @Override
   public ReferenceType getReferenceTypeMirror(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is a primitive: " + clazz.getName());
      }
      return (ReferenceType) getTypeMirror(clazz);
   }

   @Override
   public DeclaredType getDeclaredTypeMirror(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is a primitive: " + clazz.getName());
      } else if (clazz.isArray()) {
         throw new IllegalArgumentException("Given class is an array: " + clazz.getName());
      }
      return (DeclaredType) getTypeMirror(clazz);
   }

   @Override
   public PrimitiveType getPrimitiveTypeMirror(Class<?> clazz) {
      if (clazz == boolean.class) {
         return base.getPrimitiveType(TypeKind.BOOLEAN);
      } else if (clazz == byte.class) {
         return base.getPrimitiveType(TypeKind.BYTE);
      } else if (clazz == short.class) {
         return base.getPrimitiveType(TypeKind.SHORT);
      } else if (clazz == char.class) {
         return base.getPrimitiveType(TypeKind.CHAR);
      } else if (clazz == int.class) {
         return base.getPrimitiveType(TypeKind.INT);
      } else if (clazz == long.class) {
         return base.getPrimitiveType(TypeKind.LONG);
      } else if (clazz == float.class) {
         return base.getPrimitiveType(TypeKind.FLOAT);
      } else if (clazz == double.class) {
         return base.getPrimitiveType(TypeKind.DOUBLE);
      } else {
         throw new AssertionError("Unsupported primitive type " + clazz);
      }
   }
   
   @Override
   public ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType) {
      Type t = arrayType.getType();
      if (t instanceof Class) {
         return getArrayTypeMirror((Class<?>) t);
      } else {
         return getArrayTypeMirror((GenericArrayType) t);
      }
   }

   @Override
   public ArrayType getArrayTypeMirror(Class<?> clazz) {
      if (!clazz.isArray()) {
         throw new IllegalArgumentException("Given class is not an array: " + clazz.getName());
      }
      TypeMirror componentType = getTypeMirror(clazz.getComponentType());
      return base.getArrayType(componentType);
   }

   @Override
   public ArrayType getArrayTypeMirror(GenericArrayType arrayType) {
      TypeMirror componentType = getTypeMirror(arrayType.getGenericComponentType());
      return base.getArrayType(componentType);
   }

   @Override
   public DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType) {
      return getParameterizedTypeMirror((ParameterizedType) parameterizedType.getType());
   }
   
   @Override
   public DeclaredType getParameterizedTypeMirror(ParameterizedType parameterizedType) {
      Type owner = parameterizedType.getOwnerType();
      Type[] args = parameterizedType.getActualTypeArguments();
      TypeMirror[] argMirrors = new TypeMirror[args.length];
      for (int i = 0; i < args.length; i++) {
         argMirrors[i] = getTypeMirror(args[i]);
      }
      TypeElement rawType = elementUtils.getTypeElement((Class<?>) parameterizedType.getRawType());
      return owner == null
            ? base.getDeclaredType(rawType, argMirrors)
            : base.getDeclaredType((DeclaredType) getTypeMirror(owner), rawType, argMirrors);
   }

   @Override
   public WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType) {
      return getWildcardTypeMirror((java.lang.reflect.WildcardType) wildcardType.getType());
   }

   @Override
   public WildcardType getWildcardTypeMirror(java.lang.reflect.WildcardType wildcardType) {
      Type[] lowerBounds = wildcardType.getLowerBounds();
      TypeMirror superBound;
      if (lowerBounds == null || lowerBounds.length == 0) {
         superBound = null;
      } else {
         assert lowerBounds.length == 1;
         superBound = getTypeMirror(lowerBounds[0]);
      }
      TypeMirror extendsBound;
      if (superBound != null) {
         Type[] upperBounds = wildcardType.getUpperBounds();
         if (upperBounds == null || upperBounds.length == 0) {
            extendsBound = null;
         } else {
            assert upperBounds.length == 1;
            extendsBound = getTypeMirror(upperBounds[0]);
         }
      } else {
         extendsBound = null;
      }
      return base.getWildcardType(extendsBound, superBound);
   }

   @Override
   public TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar) {
      return getTypeVariableMirror((java.lang.reflect.TypeVariable<?>) typeVar.getType());
   }

   @Override
   public TypeVariable getTypeVariableMirror(java.lang.reflect.TypeVariable<?> typeVar) {
      TypeParameterElement element = elementUtils.getTypeParameterElement(typeVar); 
      return (TypeVariable) element.asType();
   }
}
