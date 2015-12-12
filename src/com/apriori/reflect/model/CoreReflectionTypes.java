package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
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

/**
 * The implementation of {@link Types} that is backed by core reflection.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
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
         Executable e = ((CoreReflectionExecutableType) t).base();
         boolean generic = e.getTypeParameters().length > 0
               || isGeneric(e.getAnnotatedReturnType().getType())
               || isGeneric(e.getAnnotatedReceiverType().getType())
               || isAnyGeneric(e.getGenericParameterTypes())
               || isAnyGeneric(e.getGenericExceptionTypes());
         // return the same object if there is nothing to erase
         return generic ? new CoreReflectionErasedExecutableType(e) : t;
      } else if (t.getKind() == TypeKind.INTERSECTION) {
         return erasure(((CoreReflectionIntersectionType) t).getBounds().get(0));
      } else if (t.getKind() == TypeKind.UNION) {
         // TODO
         return null;
      } else {
         AnnotatedType at = ((CoreReflectionBaseTypeMirror<?>) t).base();
         Class<?> erased = com.apriori.reflect.Types.getRawType(at.getType());
         return CoreReflectionTypes.INSTANCE.getTypeMirror(
               AnnotatedTypes.newAnnotatedType(erased, at.getAnnotations()));
      }
   }
   
   private static boolean isGeneric(Type t) {
      return !(t instanceof Class);
   }

   private static boolean isAnyGeneric(Type[] types) {
      for (Type t : types) {
         if (isGeneric(t)) {
            return true;
         }
      }
      return false;
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
      AnnotatedType type = asAnnotatedType(t, "captured type");
      if (!(type instanceof AnnotatedParameterizedType)) {
         return t;
      }
      AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type.getType();
      AnnotatedType[] args = pType.getAnnotatedActualTypeArguments();
      boolean needCapture = false;
      for (AnnotatedType arg : args) {
         if (arg instanceof AnnotatedWildcardType) {
            needCapture = true;
            break;
         }
      }
      if (!needCapture) {
         return t;
      }
      List<AnnotatedType> capturedArgs = new ArrayList<>(args.length);
      for (AnnotatedType arg : args) {
         if (arg instanceof AnnotatedWildcardType) {
            capturedArgs.add(new AnnotatedCapturedType((AnnotatedWildcardType) arg));
         } else {
            capturedArgs.add(arg);
         }
      }
      // TODO: fix once AnnotatedParameterizedType supports getting annotated owner type
      ParameterizedType pt = (ParameterizedType) pType.getType();
      Type ownerType = pt.getOwnerType();
      AnnotatedParameterizedType owner = ownerType instanceof ParameterizedType
            ? AnnotatedTypes.newAnnotatedParameterizedType((ParameterizedType) ownerType)
            : null;
      AnnotatedType raw = AnnotatedTypes.newAnnotatedType(pt.getRawType(), type.getAnnotations());
      return new CoreReflectionDeclaredType(owner, raw, capturedArgs);
   }

   @Override
   public PrimitiveType getPrimitiveType(TypeKind kind) {
      switch (kind) {
         case BOOLEAN:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(boolean.class),
                  TypeKind.BOOLEAN);
         case BYTE:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(byte.class),
                  TypeKind.BYTE);
         case SHORT:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(short.class),
                  TypeKind.SHORT);
         case CHAR:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(char.class),
                  TypeKind.CHAR);
         case INT:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(int.class),
                  TypeKind.INT);
         case LONG:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(long.class),
                  TypeKind.LONG);
         case FLOAT:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(float.class),
                  TypeKind.FLOAT);
         case DOUBLE:
            return new CoreReflectionPrimitiveType(
                  AnnotatedTypes.newAnnotatedType(double.class),
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
         return new CoreReflectionVoidType(AnnotatedTypes.newAnnotatedType(void.class));
      } else if (kind == TypeKind.NONE) {
         return CoreReflectionNoneType.INSTANCE;
      } else {
         throw new IllegalArgumentException("Given kind is not valid as NoType: " + kind);
      }
   }

   @Override
   public ArrayType getArrayType(TypeMirror componentType) {
      AnnotatedType component = asAnnotatedType(componentType, "component type");
      return new CoreReflectionArrayType(AnnotatedTypes.newAnnotatedArrayType(component));
   }

   @Override
   public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
      if ((extendsBound == null) == (superBound == null)) {
         throw new IllegalArgumentException("Exactly one of extends or super bound should be "
               + "present, but found " + (extendsBound == null ? "none": "both"));
      }
      if (extendsBound != null) {
         assert superBound == null;
         AnnotatedType extendsType = asAnnotatedType(extendsBound, "extends bound");
         return new CoreReflectionWildcardType(
               AnnotatedTypes.newExtendsAnnotatedWildcardType(extendsType));
      } else {
         assert superBound != null;
         AnnotatedType superType = asAnnotatedType(superBound, "super bound");
         return new CoreReflectionWildcardType(
               AnnotatedTypes.newSuperAnnotatedWildcardType(superType));
      }
   }

   @Override
   public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
      return getDeclaredType(null, typeElem, typeArgs);
   }

   @Override
   public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem,
         TypeMirror... typeArgs) {
      AnnotatedType owner = containing != null
            ? asAnnotatedType(containing, CoreReflectionDeclaredType.class, "containing type")
            : null;
      Class<?> rawType = checkType(typeElem, CoreReflectionTypeElement.class, "given type").base();
      if (typeArgs.length != rawType.getTypeParameters().length) {
         throw new IllegalArgumentException("wrong number of type arguments: expecting "
               + rawType.getTypeParameters().length + " but received " + Arrays.toString(typeArgs));
      }
      AnnotatedType[] args = new AnnotatedType[typeArgs.length];
      for (int i = 0; i < typeArgs.length; i++) {
         args[i] = asAnnotatedType(typeArgs[i], "type argument");
      }
      return new CoreReflectionDeclaredType(owner, AnnotatedTypes.newAnnotatedType(rawType), args);
   }

   private AnnotatedType asAnnotatedType(TypeMirror mirror, String description) {
      // ugly type gymnastics to work around issues with class tokens for generic types...
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Class<CoreReflectionBaseTypeMirror<AnnotatedType>> type =
            (Class) CoreReflectionBaseTypeMirror.class;
      return asAnnotatedType(mirror, type, description);
   }
   
   private <T extends AnnotatedType> T asAnnotatedType(TypeMirror mirror,
         Class<? extends CoreReflectionBaseTypeMirror<? extends T>> expectedType,
         String description) {
      T result = asAnnotatedTypeAllowVoid(mirror, expectedType, description);
      if (((AnnotatedType) result).getType() == void.class) {
         throw new IllegalArgumentException("Invalid kind of " + description + ": " + mirror);
      }
      return result;
   }
   
   private <T> T asAnnotatedTypeAllowVoid(TypeMirror mirror,
         Class<? extends CoreReflectionBaseTypeMirror<? extends T>> expectedType,
         String description) {
      return checkType(mirror, expectedType, description).base();
   }
   
   private <M extends CoreReflectionBase<?>> M checkType(Object obj, Class<M> expectedType,
         String description) {
      if (!expectedType.isInstance(obj)) {
         if (!(obj instanceof CoreReflectionMarker)) {
            throw new IllegalArgumentException(
                  initCap(description) + " is not backed by core reflection: " + obj);
         } else {
            // when expectedType is CoreReflectionBaseTypeMirror, this error will occur for
            // TypeKinds EXECUTABLE, PACKAGE, NONE, NULL, INTERSECTION, and UNION
            throw new IllegalArgumentException(
                  "Invalid kind of " + description + ": " + obj);
         }
      }
      return expectedType.cast(obj);
   }

   private String initCap(String string) {
      if (string.isEmpty()) {
         return string;
      }
      StringBuilder sb = new StringBuilder(string);
      sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
      return sb.toString();
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
      return new CoreReflectionArrayType(arrayType);
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
      return new CoreReflectionWildcardType(wildcardType);
   }

   @Override
   public TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar) {
      return typeVar.getType() instanceof AnnotatedCapturedType.CapturedTypeVariable
            ? new CoreReflectionCapturedType(typeVar)
            : new CoreReflectionTypeVariable(typeVar);
   }
}
