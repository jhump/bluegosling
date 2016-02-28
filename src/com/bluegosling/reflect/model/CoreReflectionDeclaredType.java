package com.bluegosling.reflect.model;

import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;

/**
 * A {@link DeclaredType} backed by a core reflection {@link AnnotatedType}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionDeclaredType extends CoreReflectionBaseTypeMirror<AnnotatedType>
implements DeclaredType {
   
   private final TypeMirror owner;
   private final TypeElement element;
   private final List<TypeMirror> args;

   CoreReflectionDeclaredType(AnnotatedType owner, AnnotatedType base, AnnotatedType... args) {
      this(owner, base, Arrays.asList(args));
   }
   
   CoreReflectionDeclaredType(AnnotatedType owner, AnnotatedType base,
         List<? extends AnnotatedType> args) {
      super(base);
      
      assert !com.bluegosling.reflect.Types.isPrimitive(base.getType());
      assert !com.bluegosling.reflect.Types.isArray(base.getType());
      assert owner == null || !com.bluegosling.reflect.Types.isPrimitive(owner.getType());
      assert owner == null || !com.bluegosling.reflect.Types.isArray(owner.getType());
      assert args.size() == com.bluegosling.reflect.Types.getTypeParameters(base.getType()).length;
      
      this.owner = owner == null
            ? CoreReflectionNoneType.INSTANCE
            : CoreReflectionTypes.INSTANCE.getTypeMirror(owner);
      if (args.isEmpty()) {
         this.args = Collections.emptyList();
      } else {
         List<TypeMirror> list = new ArrayList<>(args.size());
         for (AnnotatedType a : args) {
            list.add(CoreReflectionTypes.INSTANCE.getTypeMirror(a));
         }
         this.args = Collections.unmodifiableList(list);
      }

      Class<?> clazz = com.bluegosling.reflect.Types.getErasure(base.getType());
      this.element = new CoreReflectionTypeElement(clazz);
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
   public TypeElement asElement() {
      return element;
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
