package com.apriori.reflect.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor8;

/**
 * An implementation of {@link Types} backed by an annotation processing environment. This delegates
 * most methods to a {@link javax.lang.model.util.Types} instance.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class ProcessingEnvironmentTypes implements Types {
   private final javax.lang.model.util.Types base;
   private final Elements elementUtils;
   private final AnnotationMirrors annotationUtils;
   
   ProcessingEnvironmentTypes(ProcessingEnvironment env) {
      this.base = env.getTypeUtils();
      this.elementUtils = new ProcessingEnvironmentElements(env.getElementUtils(), this);
      this.annotationUtils = AnnotationMirrors.fromProcessingEnvironment(env);
   }
   
   ProcessingEnvironmentTypes(javax.lang.model.util.Types base, Elements elementUtils) {
      this.base = base;
      this.elementUtils = elementUtils;
      this.annotationUtils = new AnnotationMirrors(elementUtils, this);
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
         TypeMirror baseType;
         if (clazz.isPrimitive()) {
            if (clazz == void.class) {
               baseType = base.getNoType(TypeKind.VOID);
            } else if (clazz == boolean.class) {
               baseType = base.getPrimitiveType(TypeKind.BOOLEAN);
            } else if (clazz == byte.class) {
               baseType = base.getPrimitiveType(TypeKind.BYTE);
            } else if (clazz == short.class) {
               baseType = base.getPrimitiveType(TypeKind.SHORT);
            } else if (clazz == char.class) {
               baseType = base.getPrimitiveType(TypeKind.CHAR);
            } else if (clazz == int.class) {
               baseType = base.getPrimitiveType(TypeKind.INT);
            } else if (clazz == long.class) {
               baseType = base.getPrimitiveType(TypeKind.LONG);
            } else if (clazz == float.class) {
               baseType = base.getPrimitiveType(TypeKind.FLOAT);
            } else if (clazz == double.class) {
               baseType = base.getPrimitiveType(TypeKind.DOUBLE);
            } else {
               throw new AssertionError("Unsupported primitive type " + clazz);
            }
         } else {
            Class<?> owner = Modifier.isStatic(clazz.getModifiers())
                  ? null : clazz.getEnclosingClass();
            baseType = owner == null
                  ? getDeclaredType((DeclaredType) getTypeMirror(owner),
                        elementUtils.getTypeElement(clazz))
                  : getDeclaredType(elementUtils.getTypeElement(clazz));
         }
         return makeAnnotatedMirror(baseType, type);
      }
   }

   @Override
   public ArrayType getArrayTypeMirror(AnnotatedArrayType arrayType) {
      TypeMirror componentType = getTypeMirror(arrayType.getAnnotatedGenericComponentType());
      return makeAnnotatedMirror(base.getArrayType(componentType), arrayType);
   }

   @Override
   public DeclaredType getParameterizedTypeMirror(AnnotatedParameterizedType parameterizedType) {
      // TODO: handle annotations on owner type once core reflection APIs provide access to them
      ParameterizedType pType = (ParameterizedType) parameterizedType.getType();
      Type owner = pType.getOwnerType();
      AnnotatedType[] args = parameterizedType.getAnnotatedActualTypeArguments();
      TypeMirror[] argMirrors = new TypeMirror[args.length];
      for (int i = 0; i < args.length; i++) {
         argMirrors[i] = getTypeMirror(args[i]);
      }
      TypeElement rawType = elementUtils.getTypeElement((Class<?>) pType.getRawType());
      DeclaredType baseMirror = owner == null
            ? base.getDeclaredType(rawType, argMirrors)
            : base.getDeclaredType((DeclaredType) getTypeMirror(owner), rawType, argMirrors);
      return makeAnnotatedMirror(baseMirror, parameterizedType);
   }

   @Override
   public WildcardType getWildcardTypeMirror(AnnotatedWildcardType wildcardType) {
      AnnotatedType[] lowerBounds = wildcardType.getAnnotatedLowerBounds();
      TypeMirror superBound;
      if (lowerBounds == null || lowerBounds.length == 0) {
         superBound = null;
      } else {
         assert lowerBounds.length == 1;
         superBound = getTypeMirror(lowerBounds[0]);
      }
      TypeMirror extendsBound;
      if (superBound != null) {
         AnnotatedType[] upperBounds = wildcardType.getAnnotatedUpperBounds();
         if (upperBounds == null || upperBounds.length == 0) {
            extendsBound = null;
         } else {
            assert upperBounds.length == 1;
            extendsBound = getTypeMirror(upperBounds[0]);
         }
      } else {
         extendsBound = null;
      }
      return makeAnnotatedMirror(base.getWildcardType(extendsBound, superBound), wildcardType);
   }

   @Override
   public TypeVariable getTypeVariableMirror(AnnotatedTypeVariable typeVar) {
      TypeParameterElement element = elementUtils.getTypeParameterElement(
            (java.lang.reflect.TypeVariable<?>) typeVar.getType()); 
      return makeAnnotatedMirror((TypeVariable) element.asType(), typeVar);
   }

   private class AnnotationMakerVisitor
   extends SimpleTypeVisitor8<TypeMirror, AnnotatedElement> {
      AnnotationMakerVisitor() {
      }
      
      @Override
      protected TypeMirror defaultAction(TypeMirror t, AnnotatedElement p) {
         throw new IllegalArgumentException("Unsupported type kind: " + t.getKind());
      }

      @Override
      public PrimitiveType visitPrimitive(PrimitiveType t, AnnotatedElement p) {
         return makeAnnotatedMirror(t, p);
      }

      @Override
      public ArrayType visitArray(ArrayType t, AnnotatedElement p) {
         return makeAnnotatedMirror(t, p);
      }

      @Override
      public DeclaredType visitDeclared(DeclaredType t, AnnotatedElement p) {
         return makeAnnotatedMirror(t, p);
      }

      @Override
      public TypeVariable visitTypeVariable(TypeVariable t, AnnotatedElement p) {
         return makeAnnotatedMirror(t, p);
      }

      @Override
      public WildcardType visitWildcard(WildcardType t, AnnotatedElement p) {
         return makeAnnotatedMirror(t, p);
      }

      @Override
      public NoType visitNoType(NoType t, AnnotatedElement p) {
         if (t.getKind() != TypeKind.VOID) {
            throw new IllegalArgumentException("Unsupported type kind: " + t.getKind());
         }
         return makeAnnotatedMirror(t, p);
      }
   }
   
   TypeMirror makeAnnotatedMirror(TypeMirror typeMirror, AnnotatedElement annotationSource) {
      return typeMirror.accept(new AnnotationMakerVisitor(), annotationSource);
   }

   NoType makeAnnotatedMirror(NoType typeMirror, AnnotatedElement annotationSource) {
      Annotation[] annotations = annotationSource.getAnnotations();
      if (annotations.length == 0) {
         return typeMirror;
      }
      assert typeMirror.getKind() == TypeKind.VOID;
      return new WrappedNoType(typeMirror, annotationSource, makeMirrors(annotations));
   }

   PrimitiveType makeAnnotatedMirror(PrimitiveType typeMirror,
         AnnotatedElement annotationSource) {
      Annotation[] annotations = annotationSource.getAnnotations();
      if (annotations.length == 0) {
         return typeMirror;
      }
      return new WrappedPrimitiveType(typeMirror, annotationSource, makeMirrors(annotations));
   }

   DeclaredType makeAnnotatedMirror(DeclaredType typeMirror,
         AnnotatedElement annotationSource) {
      Annotation[] annotations = annotationSource.getAnnotations();
      if (annotations.length == 0) {
         return typeMirror;
      }
      return new WrappedDeclaredType(typeMirror, annotationSource, makeMirrors(annotations));
   }

   ArrayType makeAnnotatedMirror(ArrayType typeMirror, AnnotatedElement annotationSource) {
      Annotation[] annotations = annotationSource.getAnnotations();
      if (annotations.length == 0) {
         return typeMirror;
      }
      return new WrappedArrayType(typeMirror, annotationSource, makeMirrors(annotations));
   }

   TypeVariable makeAnnotatedMirror(TypeVariable typeMirror,
         AnnotatedElement annotationSource) {
      Annotation[] annotations = annotationSource.getAnnotations();
      if (annotations.length == 0) {
         return typeMirror;
      }
      return new WrappedTypeVariable(typeMirror, annotationSource, makeMirrors(annotations));
   }

   WildcardType makeAnnotatedMirror(WildcardType typeMirror,
         AnnotatedElement annotationSource) {
      Annotation[] annotations = annotationSource.getAnnotations();
      if (annotations.length == 0) {
         return typeMirror;
      }
      return new WrappedWildcardType(typeMirror, annotationSource, makeMirrors(annotations));
   }

   private List<AnnotationMirror> makeMirrors(Annotation[] annotations) {
      List<AnnotationMirror> mirrors = new ArrayList<>(annotations.length);
      for (Annotation a : annotations) {
         mirrors.add(annotationUtils.getAnnotationAsMirror(a));
      }
      return Collections.unmodifiableList(mirrors);
   }
   
   private static abstract class AnnotatedWrapper<T extends TypeMirror>
   implements TypeMirror {
      private final T wrapped;
      private final AnnotatedElement annotationSource;
      private final List<AnnotationMirror> annotations;
      
      AnnotatedWrapper(T wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         this.wrapped = wrapped;
         this.annotationSource = annotationSource;
         this.annotations = annotations;
      }
      
      T wrapped() {
         return wrapped;
      }
      
      @Override
      public List<? extends AnnotationMirror> getAnnotationMirrors() {
         return annotations;
      }

      @Override
      public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
         return annotationSource.getAnnotation(annotationType);
      }

      @Override
      public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
         return annotationSource.getAnnotationsByType(annotationType);
      }

      @Override
      public TypeKind getKind() {
         return wrapped.getKind();
      }
      
      // TODO: how should we handle these?
      /*
      @Override
      public int hashCode() {
         
      }
      
      @Override
      public boolean equals() {
         
      }
      
      @Override
      public String toString() {
         
      }
      */
   }
   
   private static class WrappedNoType extends AnnotatedWrapper<NoType> implements NoType {
      WrappedNoType(NoType wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         super(wrapped, annotationSource, annotations);
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
         return v.visitNoType(this, p);
      }
   }

   private static class WrappedPrimitiveType extends AnnotatedWrapper<PrimitiveType>
   implements PrimitiveType {
      WrappedPrimitiveType(PrimitiveType wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         super(wrapped, annotationSource, annotations);
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
         return v.visitPrimitive(this, p);
      }
   }

   private static class WrappedArrayType extends AnnotatedWrapper<ArrayType>
   implements ArrayType {
      WrappedArrayType(ArrayType wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         super(wrapped, annotationSource, annotations);
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
         return v.visitArray(this, p);
      }

      @Override
      public TypeMirror getComponentType() {
         return wrapped().getComponentType();
      }
   }

   private static class WrappedDeclaredType extends AnnotatedWrapper<DeclaredType>
   implements DeclaredType {
      WrappedDeclaredType(DeclaredType wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         super(wrapped, annotationSource, annotations);
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
         return v.visitDeclared(this, p);
      }

      @Override
      public Element asElement() {
         return wrapped().asElement();
      }

      @Override
      public TypeMirror getEnclosingType() {
         return wrapped().getEnclosingType();
      }

      @Override
      public List<? extends TypeMirror> getTypeArguments() {
         return wrapped().getTypeArguments();
      }
   }
   
   private static class WrappedWildcardType extends AnnotatedWrapper<WildcardType>
   implements WildcardType {
      WrappedWildcardType(WildcardType wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         super(wrapped, annotationSource, annotations);
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
         return v.visitWildcard(this, p);
      }

      @Override
      public TypeMirror getExtendsBound() {
         return wrapped().getExtendsBound();
      }

      @Override
      public TypeMirror getSuperBound() {
         return wrapped().getSuperBound();
      }
   }
   
   private static class WrappedTypeVariable extends AnnotatedWrapper<TypeVariable>
   implements TypeVariable {
      WrappedTypeVariable(TypeVariable wrapped, AnnotatedElement annotationSource,
            List<AnnotationMirror> annotations) {
         super(wrapped, annotationSource, annotations);
      }

      @Override
      public <R, P> R accept(TypeVisitor<R, P> v, P p) {
         return v.visitTypeVariable(this, p);
      }

      @Override
      public Element asElement() {
         return wrapped().asElement();
      }

      @Override
      public TypeMirror getUpperBound() {
         return wrapped().getUpperBound();
      }

      @Override
      public TypeMirror getLowerBound() {
         return wrapped().getLowerBound();
      }
   }
}
