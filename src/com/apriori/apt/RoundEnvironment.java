package com.apriori.apt;

import com.apriori.apt.reflect.AnnotatedElement;
import com.apriori.apt.reflect.Class;
import com.apriori.apt.reflect.Constructor;
import com.apriori.apt.reflect.Field;
import com.apriori.apt.reflect.Method;
import com.apriori.apt.reflect.Package;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

//TODO: javadoc!
public class RoundEnvironment {
   
   private final javax.annotation.processing.RoundEnvironment env;
   
   public RoundEnvironment(javax.annotation.processing.RoundEnvironment env) {
      this.env = env;
   }

   public boolean hasErrorFromPreviousRound() {
      return env.errorRaised();
   }
   
   public boolean isProcessingOver() {
      return env.processingOver();
   }

   private Set<? extends AnnotatedElement> getRootElements(boolean includeClasses, boolean includePackages) {
      Set<? extends Element> rootElements = env.getRootElements();
      Set<AnnotatedElement> ret = new HashSet<AnnotatedElement>(rootElements.size());
      for (Element e : rootElements) {
         Class clazz = e instanceof TypeElement ? Class.forElement((TypeElement) e) : null;
         if (clazz != null) {
            if (includeClasses) {
               ret.add(clazz);
            }
         } else if (includePackages) {
            Package pkg = e instanceof PackageElement ? Package.forElement((PackageElement) e) : null;
            if (pkg != null) {
               ret.add(pkg);
            }
         }
      }
      return Collections.unmodifiableSet(ret);
   }
   
   public Set<? extends AnnotatedElement> allElements() {
      return getRootElements(true, true);
   }

   @SuppressWarnings("unchecked")
   public Set<Package> allPackages() {
      return (Set<Package>) getRootElements(false, true);
   }
   
   @SuppressWarnings("unchecked")
   public Set<Class> allClasses() {
      return (Set<Class>) getRootElements(true, false);
   }
   
   public Set<Class> allClasses(Set<Class.Kind> classTypes) {
      Set<Class> classes = new HashSet<Class>();
      for (Class clazz : allClasses()) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }
   
   private <T extends AnnotatedElement> Set<T> convertElements(Set<? extends Element> elements,
         java.lang.Class<T> clazz) {
      Set<T> ret = new HashSet<T>();
      for (Element element : elements) {
         AnnotatedElement a = AnnotatedElement.FROM_ELEMENT.apply(element);
         if (a != null) {
            if (clazz.isInstance(a)) {
               @SuppressWarnings("unchecked")
               T t = (T) a;
               ret.add(t);
            }
         }
         // TODO: log or throw if element returned is null?
      }
      return Collections.unmodifiableSet(ret);
   }
   
   private <T extends AnnotatedElement> Set<T> getElements(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation,
         java.lang.Class<T> clazz) {
      return convertElements(env.getElementsAnnotatedWith(annotation), clazz);
   }

   private <T extends AnnotatedElement> Set<T> getElements(Class annotation,
         java.lang.Class<T> clazz) {
      if (!annotation.isAnnotation()) {
         throw new IllegalArgumentException("specified type is not an annotation type");
      }
      return convertElements(env.getElementsAnnotatedWith(annotation.asElement()), clazz);
   }

   public Set<AnnotatedElement> annotatedElements(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      return getElements(annotation, AnnotatedElement.class);
   }

   public Set<AnnotatedElement> annotatedElements(Class annotation) {
      return getElements(annotation, AnnotatedElement.class);
   }

   public Set<Class> annotatedClasses(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      return getElements(annotation, Class.class);
   }

   public Set<Class> annotatedClasses(Class annotation) {
      return getElements(annotation, Class.class);
   }

   public Set<Class> annotatedClasses(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation,
         Set<Class.Kind> classTypes) {
      Set<Class> classes = new HashSet<Class>();
      for (Class clazz : annotatedClasses(annotation)) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<Class> annotatedClasses(Class annotation, Set<Class.Kind> classTypes) {
      Set<Class> classes = new HashSet<Class>();
      for (Class clazz : annotatedClasses(annotation)) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<Package> annotatedPackages(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      return getElements(annotation, Package.class);
   }

   public Set<Package> annotatedPackages(Class annotation) {
      return getElements(annotation, Package.class);
   }

   public Set<Field> annotatedFields(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      return getElements(annotation, Field.class);
   }

   public Set<Field> annotatedFields(Class annotation) {
      return getElements(annotation, Field.class);
   }

   public Set<Method> annotatedMethods(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      return getElements(annotation, Method.class);
   }

   public Set<Method> annotatedMethods(Class annotation) {
      return getElements(annotation, Method.class);
   }

   public Set<Constructor> annotatedConstructors(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotation) {
      return getElements(annotation, Constructor.class);
   }

   public Set<Constructor> annotatedConstructors(Class annotation) {
      return getElements(annotation, Constructor.class);
   }
}