package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;


class CoreReflectionWildcardType extends CoreReflectionBaseTypeMirror<AnnotatedWildcardType>
implements WildcardType {
   private final TypeMirror extendsBound;
   private final TypeMirror superBound;

   CoreReflectionWildcardType(AnnotatedWildcardType wildcardType) {
      super(wildcardType);
      
      AnnotatedType[] extendsBounds = wildcardType.getAnnotatedUpperBounds();
      assert extendsBounds.length <= 1;
      AnnotatedType[] superBounds = wildcardType.getAnnotatedUpperBounds();
      assert superBounds.length <= 1;
      
      this.extendsBound = extendsBounds.length == 0
            ? new CoreReflectionDeclaredType(null, AnnotatedTypes.newAnnotatedType(Object.class))
            : CoreReflectionTypes.INSTANCE.getTypeMirror(extendsBounds[0]); 
      this.superBound = superBounds.length == 0
            ? CoreReflectionNullType.INSTANCE
            : CoreReflectionTypes.INSTANCE.getTypeMirror(superBounds[0]); 
   }
   
   @Override
   public TypeKind getKind() {
      return TypeKind.WILDCARD;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitWildcard(this, p);
   }

   @Override
   public TypeMirror getExtendsBound() {
      return extendsBound;
   }

   @Override
   public TypeMirror getSuperBound() {
      return superBound;
   }
}
