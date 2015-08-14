package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;


enum CoreReflectionTypes implements Types {
   INSTANCE;

   @Override
   public Element asElement(TypeMirror t) {
      if (t instanceof DeclaredType) {
         return ((DeclaredType) t).asElement();
      } else if (t instanceof TypeVariable) {
         return ((TypeVariable) t).asElement();
      } else {
         return null;
      }
   }

   @Override
   public boolean isSameType(TypeMirror t1, TypeMirror t2) {
      TypeKind k = t1.getKind();
      if (t2.getKind() != k) {
         return false;
      } else if (k == TypeKind.ERROR || k == TypeKind.OTHER) {
         throw new IllegalArgumentException("Invalid type kind: " + k);
      } else if (k == TypeKind.EXECUTABLE || k == TypeKind.PACKAGE) {
         return t1.equals(t2);
      } else if (k == TypeKind.INTERSECTION) {
         // TODO: implement me
         return false;
      } else if (k == TypeKind.UNION) {         
         // TODO: implement me
         return false;
      } else if (k == TypeKind.ARRAY || k == TypeKind.DECLARED || k == TypeKind.TYPEVAR
            || k == TypeKind.WILDCARD) {
         // TODO: implement me
         return false;
      } else {
         // the rest are primitive types and special "none" types, in which case, simply
         // having the same kind means they are the same type
         return true;
      }
   }

   @Override
   public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean contains(TypeMirror t1, TypeMirror t2) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
      // TODO: implement me
      return false;
   }

   @Override
   public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeMirror erasure(TypeMirror t) {
      if (t.getKind() == TypeKind.NULL ||
            t.getKind() == TypeKind.NONE) {
         return t;
      } else if (t.getKind() == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid kind has no erasure: " + t.getKind());
      } else if (t.getKind() == TypeKind.EXECUTABLE) {
         return new CoreReflectionErasedExecutableType(((CoreReflectionExecutableType) t).base());
      } else if (t.getKind() == TypeKind.INTERSECTION) {
         return erasure(((CoreReflectionIntersectionType) t).getBounds().get(0));
      } else if (t.getKind() == TypeKind.UNION) {
         // TODO
         return null;
      } else {
         AnnotatedType at = ((CoreReflectionBaseTypeMirror) t).base();
         Class<?> erased = com.apriori.reflect.Types.getRawType(at.getType());
         return CoreReflectionTypes.INSTANCE.getTypeMirror(
               AnnotatedTypes.newAnnotatedType(erased, at.getAnnotations()));
      }
   }

   @Override
   public TypeElement boxedClass(PrimitiveType p) {
      switch (p.getKind()) {
         case BOOLEAN:
            return CoreReflectionElements.INSTANCE.getTypeElement(Boolean.class);
         case BYTE:
            return CoreReflectionElements.INSTANCE.getTypeElement(Byte.class);
         case SHORT:
            return CoreReflectionElements.INSTANCE.getTypeElement(Short.class);
         case CHAR:
            return CoreReflectionElements.INSTANCE.getTypeElement(Character.class);
         case INT:
            return CoreReflectionElements.INSTANCE.getTypeElement(Integer.class);
         case LONG:
            return CoreReflectionElements.INSTANCE.getTypeElement(Long.class);
         case FLOAT:
            return CoreReflectionElements.INSTANCE.getTypeElement(Float.class);
         case DOUBLE:
            return CoreReflectionElements.INSTANCE.getTypeElement(Double.class);
         default:
            throw new IllegalArgumentException("Unrecognized primitive type kind: " + p.getKind());
      }
   }

   @Override
   public PrimitiveType unboxedType(TypeMirror t) {
      if (t.getKind() != TypeKind.DECLARED) {
         throw new IllegalArgumentException();
      }
      Type type = ((CoreReflectionDeclaredType) t).base().getType();
      if (!(type instanceof Class)) {
         throw new IllegalArgumentException();
      }
      Class<?> boxed = (Class<?>) type;
      Class<?> unboxed = com.apriori.reflect.Types.unbox(boxed);
      if (boxed == unboxed) {
         throw new IllegalArgumentException();
      }
      assert unboxed.isPrimitive();
      return (PrimitiveType) getTypeMirror(unboxed);
   }

   @Override
   public TypeMirror capture(TypeMirror t) {
      // TODO: implement me
      return null;
   }

   @Override
   public PrimitiveType getPrimitiveType(TypeKind kind) {
      switch (kind) {
         case BOOLEAN:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(boolean.class),
                  TypeKind.BOOLEAN);
         case BYTE:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(byte.class),
                  TypeKind.BYTE);
         case SHORT:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(short.class),
                  TypeKind.SHORT);
         case CHAR:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(char.class),
                  TypeKind.CHAR);
         case INT:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(int.class),
                  TypeKind.INT);
         case LONG:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(long.class),
                  TypeKind.LONG);
         case FLOAT:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(float.class),
                  TypeKind.FLOAT);
         case DOUBLE:
            return new CoreReflectionPrimitiveType(
                  com.apriori.reflect.AnnotatedTypes.newAnnotatedType(double.class),
                  TypeKind.DOUBLE);
         default:
            throw new IllegalArgumentException("kind is not a valid primitive type: " + kind);
      }
   }

   @Override
   public NullType getNullType() {
      return CoreReflectionNullType.INSTANCE;
   }

   @Override
   public NoType getNoType(TypeKind kind) {
      if (kind == TypeKind.VOID) {
         return new CoreReflectionVoidType(
               com.apriori.reflect.AnnotatedTypes.newAnnotatedType(void.class));
      } else if (kind == TypeKind.NONE) {
         return CoreReflectionNoneType.INSTANCE;
      } else {
         throw new IllegalArgumentException("Given kind is not valid as NoType: " + kind);
      }
   }

   @Override
   public ArrayType getArrayType(TypeMirror componentType) {
      if (!(componentType instanceof CoreReflectionBaseTypeMirror)) {
         throw new IllegalArgumentException("Invalid kind of type argument: " + componentType);
      }
      AnnotatedType component = ((CoreReflectionBaseTypeMirror) componentType).base();
      AnnotatedTypes.newAnnotatedArrayType(component);
      // TODO: implement me
      return null;
   }

   @Override
   public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
      // TODO: implement me
      return null;
   }

   @Override
   public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
      return getDeclaredType(null, typeElem, typeArgs);
   }

   @Override
   public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
         TypeMirror... typeArgs) {
      
      AnnotatedType owner = containing == null
            ? null : ((CoreReflectionDeclaredType) containing).base();
      Class<?> rawType = ((CoreReflectionTypeElement) typeElem).base();
      if (typeArgs.length != rawType.getTypeParameters().length) {
         throw new IllegalArgumentException("wrong number of type arguments: expecting "
               + rawType.getTypeParameters().length + " but received " + Arrays.toString(typeArgs));
      }
      AnnotatedType[] args = new AnnotatedType[typeArgs.length];
      for (int i = 0; i < typeArgs.length; i++) {
         if (!(typeArgs[i] instanceof CoreReflectionBaseTypeMirror)) {
            throw new IllegalArgumentException("Invalid kind of type argument: " + typeArgs[i]);
         }
         args[i] = ((CoreReflectionBaseTypeMirror) typeArgs[i]).base();
      }
      return new CoreReflectionDeclaredType(owner, AnnotatedTypes.newAnnotatedType(rawType), args);
   }

   @Override
   public TypeMirror asMemberOf(DeclaredType containing, Element element) {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeMirror getTypeMirror(AnnotatedType type) {
      if (type instanceof AnnotatedArrayType) {
         return getArrayTypeMirror((AnnotatedArrayType) type);
      } else if (type instanceof AnnotatedParameterizedType) {
         return getParameterizedTypeMirror((AnnotatedParameterizedType) type);
      } else if (type instanceof AnnotatedWildcardType) {
         return getWildcardTypeMirror((AnnotatedWildcardType) type);
      } else if (type instanceof AnnotatedTypeVariable) {
         return getTypeVariableMirror((AnnotatedTypeVariable) type);
      } else {
         Class<?> clazz = (Class<?>) type.getType();
         assert !clazz.isArray();
         if (clazz.isPrimitive()) {
            if (clazz == void.class) {
               return new CoreReflectionVoidType(type);
            } else if (clazz == boolean.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.BOOLEAN);
            } else if (clazz == byte.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.BYTE);
            } else if (clazz == short.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.SHORT);
            } else if (clazz == char.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.CHAR);
            } else if (clazz == int.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.INT);
            } else if (clazz == long.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.LONG);
            } else if (clazz == float.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.FLOAT);
            } else if (clazz == double.class) {
               return new CoreReflectionPrimitiveType(type, TypeKind.DOUBLE);
            } else {
               throw new AssertionError("Unsupported primitive type " + clazz);
            }
         }
         Class<?> owner = Modifier.isStatic(clazz.getModifiers())
               ? null : clazz.getEnclosingClass();
         AnnotatedType annotatedOwner = owner == null
               ? null : AnnotatedTypes.newAnnotatedType(owner);
         return new CoreReflectionDeclaredType(annotatedOwner, type);
      }
   }

   @Override
   public ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType) {
      // TODO: implement me
      return null;
   }

   @Override
   public DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType) {
      // TODO(jh): use an AnnotatedType once AnnotatedParameterizedType is fixed to expose its
      // annotated owner type
      Type owner;
      Type type = parameterizedType.getType();
      if (type instanceof Class) {
         Class<?> clazz = (Class<?>) type;
         owner = Modifier.isStatic(clazz.getModifiers())
               ? null : clazz.getEnclosingClass();
      } else {
         owner = ((ParameterizedType) type).getOwnerType();
      }
      AnnotatedType annotatedOwner = owner == null
            ? null : AnnotatedTypes.newAnnotatedType(owner);
      return new CoreReflectionDeclaredType(annotatedOwner, parameterizedType,
            parameterizedType.getAnnotatedActualTypeArguments());
   }

   @Override
   public WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType) {
      // TODO: implement me
      return null;
   }

   @Override
   public TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar) {
      return new CoreReflectionTypeVariable(typeVar);
   }
}
