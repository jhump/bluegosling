package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

/**
 * The implementation of {@link Types} that is backed by core reflection.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
enum CoreReflectionTypes implements Types {
   INSTANCE;
   
   private static final TypeMirror OBJECT_TYPE =
         new CoreReflectionDeclaredType(null, AnnotatedTypes.newAnnotatedType(Object.class));
   
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
         // if all bounds are the same, then the two intersections are the same
         List<? extends TypeMirror> b1 = ((IntersectionType) t1).getBounds();
         List<? extends TypeMirror> b2 = ((IntersectionType) t2).getBounds();
         if (b1.size() != b2.size()) {
            return false;
         }
         for (Iterator<? extends TypeMirror> iter1 = b1.iterator(), iter2 = b2.iterator();
               iter1.hasNext(); ) {
            if (!isSameType(iter1.next(), iter2.next())) {
               return false;
            }
         }
         return true;
      } else if (k == TypeKind.UNION) {         
         // if all alternatives are the same, then the two unions are the same
         List<? extends TypeMirror> a1 = ((UnionType) t1).getAlternatives();
         List<? extends TypeMirror> a2 = ((UnionType) t2).getAlternatives();
         if (a1.size() != a2.size()) {
            return false;
         }
         for (Iterator<? extends TypeMirror> iter1 = a1.iterator(), iter2 = a2.iterator();
               iter1.hasNext(); ) {
            if (!isSameType(iter1.next(), iter2.next())) {
               return false;
            }
         }
         return true;
      } else if (k == TypeKind.ARRAY || k == TypeKind.DECLARED || k == TypeKind.TYPEVAR
            || k == TypeKind.WILDCARD) {
         AnnotatedType at1 = asAnnotatedType(t1, "first type");
         AnnotatedType at2 = asAnnotatedType(t2, "second type");
         return com.apriori.reflect.Types.isSameType(at1.getType(), at2.getType());
      } else {
         // the rest are primitive types and special "none" types, in which case, simply
         // having the same kind means they are the same type
         return true;
      }
   }

   @Override
   public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
      TypeKind k1 = t1.getKind();
      TypeKind k2 = t2.getKind();
      if (k1 == TypeKind.EXECUTABLE || k1 == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid type kind: " + k1);
      } else if (k2 == TypeKind.EXECUTABLE || k2 == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid type kind: " + k2);
      } else if (k1 == TypeKind.INTERSECTION) {
         IntersectionType i = checkType(t1, CoreReflectionIntersectionType.class, "first type");
         for (TypeMirror m : i.getBounds()) {
            if (isSubtype(m, t2)) {
               return true;
            }
         }
         return false;
      } else if (k1 == TypeKind.UNION) {
         UnionType u = checkType(t1, CoreReflectionUnionType.class, "first type");
         for (TypeMirror m : u.getAlternatives()) {
            if (!isSubtype(m, t2)) {
               return false;
            }
         }
         return true;
      } else if (k2 == TypeKind.INTERSECTION) {
         IntersectionType i = checkType(t2, CoreReflectionIntersectionType.class, "second type");
         for (TypeMirror m : i.getBounds()) {
            if (!isSubtype(t1, m)) {
               return false;
            }
         }
         return true;
      } else if (k2 == TypeKind.UNION) {
         UnionType u = checkType(t2, CoreReflectionUnionType.class, "second type");
         for (TypeMirror m : u.getAlternatives()) {
            if (isSubtype(t1, m)) {
               return true;
            }
         }
         return false;
      }

      AnnotatedType at1 = asAnnotatedTypeAllowVoid(t1, "first type");
      AnnotatedType at2 = asAnnotatedTypeAllowVoid(t1, "second type");
      return com.apriori.reflect.Types.isSubtype(at1.getType(), at2.getType());
   }

   @Override
   public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
      TypeKind k1 = t1.getKind();
      TypeKind k2 = t2.getKind();
      if (k1 == TypeKind.EXECUTABLE || k1 == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid type kind: " + k1);
      } else if (k2 == TypeKind.EXECUTABLE || k2 == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid type kind: " + k2);
      } else if (k1 == TypeKind.INTERSECTION) {
         IntersectionType i = checkType(t1, CoreReflectionIntersectionType.class, "first type");
         for (TypeMirror m : i.getBounds()) {
            if (isAssignable(m, t2)) {
               return true;
            }
         }
         return false;
      } else if (k1 == TypeKind.UNION) {
         UnionType u = checkType(t1, CoreReflectionUnionType.class, "first type");
         for (TypeMirror m : u.getAlternatives()) {
            if (!isAssignable(m, t2)) {
               return false;
            }
         }
         return true;
      } else if (k2 == TypeKind.INTERSECTION) {
         IntersectionType i = checkType(t2, CoreReflectionIntersectionType.class, "second type");
         for (TypeMirror m : i.getBounds()) {
            if (!isAssignable(t1, m)) {
               return false;
            }
         }
         return true;
      } else if (k2 == TypeKind.UNION) {
         UnionType u = checkType(t2, CoreReflectionUnionType.class, "second type");
         for (TypeMirror m : u.getAlternatives()) {
            if (isAssignable(t1, m)) {
               return true;
            }
         }
         return false;
      }

      AnnotatedType at1 = asAnnotatedTypeAllowVoid(t1, "first type");
      AnnotatedType at2 = asAnnotatedTypeAllowVoid(t1, "second type");
      return com.apriori.reflect.Types.isAssignable(at1.getType(), at2.getType());
   }

   @Override
   public boolean contains(TypeMirror t1, TypeMirror t2) {
      
      // TODO: implement me!
      return false;
   }
   
   @Override
   public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
      ExecutableSignature e1 =
            checkType(m1, CoreReflectionExecutableType.class, "first executable").base();
      ExecutableSignature e2 =
            checkType(m2, CoreReflectionExecutableType.class, "second executable").base();
      return e1.isSubsignatureOf(e2);
   }

   @Override
   public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
      TypeKind k = t.getKind();
      if (k == TypeKind.EXECUTABLE || k == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid type kind: " + k);
      
      } else if (k == TypeKind.INTERSECTION) {
         IntersectionType i = checkType(t, CoreReflectionIntersectionType.class, "given type");
         return i.getBounds();
         
      } else if (k == TypeKind.UNION) {
         UnionType u = checkType(t, CoreReflectionUnionType.class, "given type");
         List<? extends TypeMirror> alternativeMirrors = u.getAlternatives();
         List<AnnotatedType> alternatives = new ArrayList<>(alternativeMirrors.size());
         for (TypeMirror m : alternativeMirrors) {
            alternatives.add(asAnnotatedType(m, "member of given union type"));
         }
         AnnotatedType[] lubs = AnnotatedTypes.getAnnotatedLeastUpperBounds(alternatives);
         return Collections.singletonList(new CoreReflectionIntersectionType(lubs));
      }
      
      AnnotatedType at = asAnnotatedTypeAllowVoid(t, "given type");
      AnnotatedType[] supertypes = AnnotatedTypes.getAnnotatedDirectSupertypes(at);
      List<TypeMirror> mirrors = new ArrayList<>(supertypes.length);
      for (AnnotatedType st : supertypes) {
         mirrors.add(getTypeMirror(st));
      }
      return Collections.unmodifiableList(mirrors);
   }

   @Override
   public TypeMirror erasure(TypeMirror t) {
      if (t.getKind() == TypeKind.NULL ||
            t.getKind() == TypeKind.NONE) {
         return t;
      } else if (t.getKind() == TypeKind.PACKAGE) {
         throw new IllegalArgumentException("Invalid kind has no erasure: " + t.getKind());
      } else if (t.getKind() == TypeKind.EXECUTABLE) {
         ExecutableSignature e =
               checkType(t, CoreReflectionExecutableType.class, "given type").base();
         
         boolean generic = e.getTypeParameters().length > 0
               || isGeneric(e.getAnnotatedReturnType())
               || isGeneric(e.getAnnotatedReceiverType())
               || isAnyGeneric(e.getAnnotatedParameterTypes())
               || isAnyGeneric(e.getAnnotatedExceptionTypes());
         // return the same object if there is nothing to erase
         return generic ? new CoreReflectionExecutableType(e.erased()) : t;
      } else if (t.getKind() == TypeKind.INTERSECTION) {
         IntersectionType i = checkType(t, CoreReflectionIntersectionType.class, "given type");
         return erasure(i.getBounds().get(0));
      } else if (t.getKind() == TypeKind.UNION) {
         UnionType u = checkType(t, CoreReflectionUnionType.class, "given type");
         List<? extends TypeMirror> alternativeMirrors = u.getAlternatives();
         List<AnnotatedType> alternatives = new ArrayList<>(alternativeMirrors.size());
         for (TypeMirror m : alternativeMirrors) {
            alternatives.add(asAnnotatedType(m, "member of given union type"));
         }
         AnnotatedType lub = AnnotatedTypes.getAnnotatedLeastUpperBounds(alternatives)[0];
         Type lubType = lub.getType();
         Type erasedType = com.apriori.reflect.Types.getErasure(lubType);
         AnnotatedType erased = erasedType == lubType
               ? lub : AnnotatedTypes.newAnnotatedType(erasedType, lub.getAnnotations());
         return getTypeMirror(erased);
      } else {
         AnnotatedType at = asAnnotatedTypeAllowVoid(t, "given type");
         Class<?> erased = com.apriori.reflect.Types.getErasure(at.getType());
         return getTypeMirror(AnnotatedTypes.newAnnotatedType(erased, at.getAnnotations()));
      }
   }
   
   private static boolean isGeneric(AnnotatedType t) {
      return !(t.getType() instanceof Class);
   }

   private static boolean isAnyGeneric(AnnotatedType[] types) {
      for (AnnotatedType t : types) {
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

   private static AnnotatedType asAnnotatedType(TypeMirror mirror, String description) {
      // ugly type gymnastics to work around issues with class tokens for generic types...
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Class<CoreReflectionBaseTypeMirror<AnnotatedType>> type =
            (Class) CoreReflectionBaseTypeMirror.class;
      return asAnnotatedType(mirror, type, description);
   }
   
   private static AnnotatedType asAnnotatedTypeAllowVoid(TypeMirror mirror, String description) {
      // ugly type gymnastics to work around issues with class tokens for generic types...
      @SuppressWarnings({ "unchecked", "rawtypes" })
      Class<CoreReflectionBaseTypeMirror<AnnotatedType>> type =
            (Class) CoreReflectionBaseTypeMirror.class;
      return asAnnotatedTypeAllowVoid(mirror, type, description);
   }
   
   private static <T extends AnnotatedType> T asAnnotatedType(TypeMirror mirror,
         Class<? extends CoreReflectionBaseTypeMirror<? extends T>> expectedType,
         String description) {
      T result = asAnnotatedTypeAllowVoid(mirror, expectedType, description);
      if (result.getType() == void.class) {
         throw new IllegalArgumentException("Invalid kind of " + description + ": " + mirror);
      }
      return result;
   }
   
   private static <T> T asAnnotatedTypeAllowVoid(TypeMirror mirror,
         Class<? extends CoreReflectionBaseTypeMirror<? extends T>> expectedType,
         String description) {
      return checkType(mirror, expectedType, description).base();
   }
   
   private static <M extends CoreReflectionMarker> M checkType(Object obj, Class<M> expectedType,
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

   private static String initCap(String string) {
      if (string.isEmpty()) {
         return string;
      }
      StringBuilder sb = new StringBuilder(string);
      sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
      return sb.toString();
   }

   @Override
   public TypeMirror asMemberOf(DeclaredType containing, Element element) {
      AnnotatedType containingType = asAnnotatedType(containing, "given containing type");
      assert !(containingType instanceof AnnotatedArrayType
            || containingType instanceof AnnotatedWildcardType
            || containingType instanceof AnnotatedTypeVariable);
      Class<?> containingClass =
            com.apriori.reflect.Types.getErasure(containingType.getType());
      
      switch (element.getKind()) {
         case CLASS: case INTERFACE: case ENUM: case ANNOTATION_TYPE:
            Class<?> clazz =
                  checkType(element, CoreReflectionTypeElement.class, "given element").base();
            if (!clazz.getEnclosingClass().isAssignableFrom(containingClass)) {
               throw new IllegalArgumentException("Given element, " + clazz
                     + ", cannot be a member of given containing type, "
                     + containingClass.getName());
            }
            if (clazz.isAnonymousClass()) {
               // Instead of just returning the type, we'll resolve its superclass (or interface)
               // so "new List<T>() {}" resolved in context where T is String returns List<String>.
               AnnotatedType[] parents = clazz.getAnnotatedInterfaces();
               AnnotatedType supertype;
               if (parents.length == 0) {
                  supertype = clazz.getAnnotatedSuperclass();
               } else {
                  assert parents.length == 1;
                  supertype = parents[0];
               }
               return getTypeMirror(AnnotatedTypes.resolveType(containingType, supertype));
            } else {
               // Nothing to do -- return the type
               return getTypeMirror(clazz);
            }
            
         case METHOD: case CONSTRUCTOR:
            Executable ex =
                  checkType(element, CoreReflectionExecutableElement.class, "given element").base();
            if (!ex.getDeclaringClass().isAssignableFrom(containingClass)) {
               throw new IllegalArgumentException("Given element, " + ex
                     + ", cannot be a member of given containing type, "
                     + containingClass.getName());
            }
            return new CoreReflectionExecutableType(
                  ExecutableSignature.of(ex).resolveTypes(containingType));
            
         case PARAMETER:
            Parameter parm = 
                  checkType(element, CoreReflectionParameterElement.class, "given element").base();
            Executable declarerEx = parm.getDeclaringExecutable();
            if (!declarerEx.getDeclaringClass().isAssignableFrom(containingClass)) {
               throw new IllegalArgumentException("Given element, " + parm
                     + ", cannot be a member of given containing type, "
                     + containingClass.getName());
            }
            return getTypeMirror(
                  AnnotatedTypes.resolveType(containingType, parm.getAnnotatedType()));
            
         case ENUM_CONSTANT: case FIELD:
            Field field =
                  checkType(element, CoreReflectionFieldElement.class, "given element").base();
            if (!field.getDeclaringClass().isAssignableFrom(containingClass)) {
               throw new IllegalArgumentException("Given element, " + field
                     + ", cannot be a member of given containing type, "
                     + containingClass.getName());
            }
            return getTypeMirror(
                  AnnotatedTypes.resolveType(containingType, field.getAnnotatedType()));
            
         case TYPE_PARAMETER:
            java.lang.reflect.TypeVariable<?> typeVar =
                  checkType(element, CoreReflectionTypeParameterElement.class, "given element")
                  .base();
            GenericDeclaration declarer = typeVar.getGenericDeclaration();
            if (!(declarer instanceof Class
                  && ((Class<?>) declarer).isAssignableFrom(containingClass))) {
               throw new IllegalArgumentException("Given element, " + typeVar
                     + ", cannot be a member of given containing type, "
                     + containingClass.getName());
            }
            return getTypeMirror(AnnotatedTypes.resolveTypeVariable(containingType, typeVar));
            
         default:
            throw new IllegalArgumentException("Given element, " + element
                  + ", cannot be a member of given containing type, "
                  + containingClass.getName());
      }
   }

   @Override
   public TypeMirror getTypeMirror(Type type) {
      return getTypeMirror(AnnotatedTypes.newAnnotatedType(type));
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
   public ReferenceType getReferenceTypeMirror(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is a primitive: " + clazz.getName());
      }
      return (ReferenceType) getTypeMirror(AnnotatedTypes.newAnnotatedType(clazz));
   }
   
   @Override
   public DeclaredType getDeclaredTypeMirror(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is a primitive: " + clazz.getName());
      } else if (clazz.isArray()) {
         throw new IllegalArgumentException("Given class is an array: " + clazz.getName());
      }
      return (DeclaredType) getTypeMirror(AnnotatedTypes.newAnnotatedType(clazz));
   }
   
   @Override
   public PrimitiveType getPrimitiveTypeMirror(Class<?> clazz) {
      if (!clazz.isPrimitive()) {
         throw new IllegalArgumentException("Given class is not a primitive: " + clazz.getName());
      }
      return (PrimitiveType) getTypeMirror(AnnotatedTypes.newAnnotatedType(clazz));
   }
   
   @Override
   public ArrayType getArrayTypeMirror(Class<?> clazz) {
      if (!clazz.isArray()) {
         throw new IllegalArgumentException("Given class is not an array: " + clazz.getName());
      }
      return getArrayTypeMirror((AnnotatedArrayType) AnnotatedTypes.newAnnotatedType(clazz));
   }
   
   @Override
   public ArrayType getArrayTypeMirror(GenericArrayType arrayType) {
      return getArrayTypeMirror(AnnotatedTypes.newAnnotatedArrayType(arrayType));
   }
   
   @Override
   public ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType) {
      return new CoreReflectionArrayType(arrayType);
   }

   @Override
   public DeclaredType getParameterizedTypeMirror(ParameterizedType parameterizedType) {
      return getParameterizedTypeMirror(
            AnnotatedTypes.newAnnotatedParameterizedType(parameterizedType));
   }

   @Override
   public DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType) {
      // TODO: use an AnnotatedType once AnnotatedParameterizedType is fixed to expose its
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
   public WildcardType getWildcardTypeMirror(java.lang.reflect.WildcardType wildcardType) {
      return getWildcardTypeMirror(AnnotatedTypes.newAnnotatedWildcardType(wildcardType)); 
   }

   @Override
   public WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType) {
      return new CoreReflectionWildcardType(wildcardType);
   }

   @Override
   public TypeVariable getTypeVariableMirror(java.lang.reflect.TypeVariable<?> typeVar) {
      return getTypeVariableMirror(AnnotatedTypes.newAnnotatedTypeVariable(typeVar));
   }
   
   @Override
   public TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar) {
      return typeVar.getType() instanceof AnnotatedCapturedType.CapturedTypeVariable
            ? new CoreReflectionCapturedType(typeVar)
            : new CoreReflectionTypeVariable(typeVar);
   }
   
   /**
    * Returns a type mirror that represents the given bounds for a type (which could be a wildcard
    * or a type variable). If the given array has more than one type then an intersection type is
    * returned. If the given array is empty then a mirror representing {@link Object} is returned.
    * 
    * <p>This is used to compute a type mirror for upper (extends) bounds of a type.
    *
    * @param bounds an array of bounds
    * @return a type mirror that represents the given bounds
    */
   static TypeMirror toTypeMirrorOrObject(AnnotatedType[] bounds) {
      int l = bounds.length;
      if (l == 0) {
         return OBJECT_TYPE;
      } else if (l == 1) {
         return CoreReflectionTypes.INSTANCE.getTypeMirror(bounds[0]);
      } else {
         return new CoreReflectionIntersectionType(bounds);
      }
   }
   
   /**
    * Returns a type mirror that represents the given bounds for a type (which could be a wildcard
    * or a type variable). If the given array has more than one type then an intersection type is
    * returned. If the given array is empty then the {@linkplain TypeKind#NULL null} type is
    * returned. 
    * 
    * <p>This is used to compute a type mirror for lower (super) bounds of a type.
    *
    * @param bounds an array of bounds
    * @return a type mirror that represents the given bounds
    */
   static TypeMirror toTypeMirrorOrNull(AnnotatedType[] bounds) {
      int l = bounds.length;
      if (l == 0) {
         return CoreReflectionNullType.INSTANCE;
      } else if (l == 1) {
         return CoreReflectionTypes.INSTANCE.getTypeMirror(bounds[0]);
      } else {
         return new CoreReflectionIntersectionType(bounds);
      }
   }
}
