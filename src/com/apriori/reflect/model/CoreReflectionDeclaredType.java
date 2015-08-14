package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;


class CoreReflectionDeclaredType extends CoreReflectionBaseTypeMirror implements DeclaredType {
   
   private final TypeMirror owner;
   private final List<TypeMirror> args;
   
   CoreReflectionDeclaredType(AnnotatedType owner, AnnotatedType base, AnnotatedType... args) {
      super(base);
      this.owner = owner == null
            ? CoreReflectionNoneType.INSTANCE
            : CoreReflectionTypes.INSTANCE.getTypeMirror(owner);
      if (args.length == 0) {
         this.args = Collections.emptyList();
      } else {
         List<TypeMirror> list = new ArrayList<>(args.length);
         for (AnnotatedType a : args) {
            list.add(CoreReflectionTypes.INSTANCE.getTypeMirror(a));
         }
         this.args = Collections.unmodifiableList(list);
      }
   }

   @Override
   public TypeKind getKind() {
      return TypeKind.DECLARED;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitDeclared(this, p);
   }

   @Override
   public Element asElement() {
      Class<?> clazz = com.apriori.reflect.Types.getRawType(base().getType());
      return CoreReflectionElements.INSTANCE.getTypeElement(clazz);
   }

   @Override
   public TypeMirror getEnclosingType() {
      return owner;
   }

   @Override
   public List<? extends TypeMirror> getTypeArguments() {
      return args;
   }
}
