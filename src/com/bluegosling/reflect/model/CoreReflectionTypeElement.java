package com.bluegosling.reflect.model;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link TypeElement} that is backed by a core reflection {@linkplain Class class token}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionTypeElement extends CoreReflectionBaseElement<Class<?>> implements TypeElement {

   CoreReflectionTypeElement(Class<?> clazz) {
      super(clazz, clazz.getSimpleName());
   }
   
   @Override
   public TypeMirror asType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(parameterize(base()));
   }
   
   private static Type parameterize(Class<?> clazz) {
      Class<?> ownerClass = clazz.getDeclaringClass();
      Type owner = ownerClass == null ? null : parameterize(ownerClass);
      TypeVariable<?>[] typeVars = clazz.getTypeParameters();
      if (owner == null || owner instanceof Class) {
         return typeVars.length == 0
               ? clazz
               : com.bluegosling.reflect.Types.newParameterizedType(clazz, typeVars);
      } else {
         return com.bluegosling.reflect.Types.newParameterizedType(
               (ParameterizedType) owner, clazz, typeVars);
      }
   }

   @Override
   public ElementKind getKind() {
      if (base().isAnnotation()) {
         return ElementKind.ANNOTATION_TYPE;
      } else if (base().isInterface()) {
         return ElementKind.INTERFACE;
      } else if (base().isEnum()) {
         return ElementKind.ENUM;
      } else {
         return ElementKind.CLASS;
      }
   }

   @Override
   public Set<Modifier> getModifiers() {
      return CoreReflectionModifiers.asSet(base().getModifiers());
   }

   @Override
   public <R, P> R accept(ElementVisitor<R, P> v, P p) {
      return v.visitType(this, p);
   }

   @Override
   public List<? extends Element> getEnclosedElements() {
      Field[] fields = base().getDeclaredFields();
      Constructor<?>[] ctors = base().getDeclaredConstructors();
      Method[] methods = base().getDeclaredMethods();
      Class<?>[] classes = base().getDeclaredClasses();
      List<Element> result =
            new ArrayList<>(fields.length + ctors.length + methods.length + classes.length);
      for (Field f : fields) {
         result.add(CoreReflectionElements.INSTANCE.getFieldElement(f));
      }
      for (Constructor<?> c : ctors) {
         result.add(CoreReflectionElements.INSTANCE.getExecutableElement(c));
      }
      for (Method m : methods) {
         result.add(CoreReflectionElements.INSTANCE.getExecutableElement(m));
      }
      for (Class<?> c : classes) {
         result.add(CoreReflectionElements.INSTANCE.getTypeElement(c));
      }
      return result;
   }

   @Override
   public NestingKind getNestingKind() {
      if (base().isAnonymousClass()) {
         return NestingKind.ANONYMOUS;
      } else if (base().isLocalClass()) {
         return NestingKind.LOCAL;
      } else if (base().isMemberClass()) {
         return NestingKind.MEMBER;
      } else {
         return NestingKind.TOP_LEVEL;
      }
   }

   @Override
   public Name getQualifiedName() {
      String n = base().getCanonicalName();
      if (n == null) {
         n = "";
      }
      return CoreReflectionName.of(n);
   }

   @Override
   public TypeMirror getSuperclass() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedSuperclass());
   }

   @Override
   public List<? extends TypeMirror> getInterfaces() {
      AnnotatedType[] ifaces = base().getAnnotatedInterfaces();
      List<TypeMirror> result = new ArrayList<>(ifaces.length);
      for (AnnotatedType iface : ifaces) {
         result.add(CoreReflectionTypes.INSTANCE.getTypeMirror(iface));
      }
      return result;
   }

   @Override
   public List<? extends TypeParameterElement> getTypeParameters() {
      TypeVariable<?>[] typeVars = base().getTypeParameters();
      List<TypeParameterElement> result = new ArrayList<>(typeVars.length);
      for (TypeVariable<?> typeVar : typeVars) {
         result.add(CoreReflectionElements.INSTANCE.getTypeParameterElement(typeVar));
      }
      return result;
   }

   @Override
   public Element getEnclosingElement() {
      Executable ex = base().getEnclosingConstructor();
      if (ex != null) {
         return CoreReflectionElements.INSTANCE.getExecutableElement(ex);
      }
      ex = base().getEnclosingMethod();
      if (ex != null) {
         return CoreReflectionElements.INSTANCE.getExecutableElement(ex);
      }
      Class<?> cl = base().getEnclosingClass();
      if (cl != null) {
         return CoreReflectionElements.INSTANCE.getTypeElement(cl);
      }
      Package pkg = base().getPackage();
      return pkg == null
            ? new CoreReflectionSyntheticPackageElement(determinePackageName(base()))
            : new CoreReflectionPackageElement(pkg);
   }
   
   private static String determinePackageName(Class<?> clazz) {
      while (true) {
         Class<?> enclosing = clazz.getEnclosingClass();
         if (enclosing == null) {
            break;
         }
         clazz = enclosing;
      }
      String n = clazz.getCanonicalName();
      String s = clazz.getSimpleName();
      if (n.equals(s)) {
         return "";
      } else {
         assert n.endsWith("." + s);
         int p = n.length() - s.length() - 1;
         assert p > 0;
         assert n.charAt(p) == '.';
         return n.substring(0, p);
      }
   }
}
