package com.apriori.reflect.model;

import com.apriori.reflect.AnnotatedTypes;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;


class CoreReflectionTypeParameterElement extends CoreReflectionBaseElement<TypeVariable<?>>
implements TypeParameterElement {
   
   private final CoreReflectionTypeVariable typeVar;

   CoreReflectionTypeParameterElement(TypeVariable<?> base) {
      super(base, base.getName());
      this.typeVar =
            new CoreReflectionTypeVariable(this, AnnotatedTypes.newAnnotatedTypeVariable(base));
   }
   
   CoreReflectionTypeParameterElement(CoreReflectionTypeVariable typeMirror,
         TypeVariable<?> base) {
      super(base, base.getName());
      this.typeVar = typeMirror;
   }

   @Override
   public TypeMirror asType() {
      return typeVar;
   }

   @Override
   public ElementKind getKind() {
      return ElementKind.TYPE_PARAMETER;
   }

   @Override
   public Set<Modifier> getModifiers() {
      return Collections.emptySet();
   }

   @Override
   public List<? extends Element> getEnclosedElements() {
      return Collections.emptyList();
   }

   @Override
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitTypeParameter(this, p);
   }

   @Override
   public Element getGenericElement() {
      GenericDeclaration decl = base().getGenericDeclaration();
      if (decl instanceof Class) {
         return CoreReflectionElements.INSTANCE.getTypeElement((Class<?>) decl);
      } else if (decl instanceof Constructor || decl instanceof Method) {
         return CoreReflectionElements.INSTANCE.getExecutableElement((Executable) decl);
      } else {
         throw new IllegalStateException("Unsupported GenericDeclaration: " + decl);
      }
   }

   @Override
   public List<? extends TypeMirror> getBounds() {
      AnnotatedType[] bounds = base().getAnnotatedBounds();
      List<TypeMirror> result = new ArrayList<>(bounds.length);
      for (AnnotatedType type : bounds) {
         result.add(CoreReflectionTypes.INSTANCE.getTypeMirror(type));
      }
      return result;
   }

   @Override
   public Element getEnclosingElement() {
      return getGenericElement();
   }

}
