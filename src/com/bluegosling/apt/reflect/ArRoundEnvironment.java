package com.bluegosling.apt.reflect;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * The environment for a single round of annotation processing. This differs from the standard
 * {@link RoundEnvironment} in that its API is in terms of the reflection API in this package
 * instead of in terms of elements, annotation mirrors, and type mirrors.
 *
 * @see RoundEnvironment
 * @see ArProcessor
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: javadoc!
public class ArRoundEnvironment {
   
   private final RoundEnvironment env;
   
   /**
    * Wraps a standard round environment.
    *
    * @param env a round environment
    */
   public ArRoundEnvironment(RoundEnvironment env) {
      this.env = env;
   }

   /**
    * Returns true if the previous round of processing had errors.
    *
    * @return true if the previous round of processing had errors
    * 
    * @see RoundEnvironment#errorRaised()
    */
   public boolean hasErrorFromPreviousRound() {
      return env.errorRaised();
   }
   
   /**
    * Returns true if this is the last round of processing.
    *
    * @return true if this is the last round of processing
    * 
    * @see RoundEnvironment#processingOver()
    */
   public boolean isProcessingOver() {
      return env.processingOver();
   }

   private Set<? extends ArAnnotatedElement> getRootElements(boolean includeClasses,
         boolean includePackages) {
      Set<? extends Element> rootElements = env.getRootElements();
      Set<ArAnnotatedElement> ret = new HashSet<ArAnnotatedElement>(rootElements.size() * 4 / 3);
      for (Element e : rootElements) {
         ArClass clazz = e instanceof TypeElement ? ArClass.forElement((TypeElement) e) : null;
         if (clazz != null) {
            if (includeClasses) {
               ret.add(clazz);
            }
         } else if (includePackages) {
            ArPackage pkg = e instanceof PackageElement
                  ? ArPackage.forElement((PackageElement) e) : null;
            if (pkg != null) {
               ret.add(pkg);
            }
         }
      }
      return Collections.unmodifiableSet(ret);
   }
   
   public Set<? extends ArAnnotatedElement> allElements() {
      return getRootElements(true, true);
   }

   @SuppressWarnings("unchecked")
   public Set<ArPackage> allPackages() {
      return (Set<ArPackage>) getRootElements(false, true);
   }
   
   @SuppressWarnings("unchecked")
   public Set<ArClass> allClasses() {
      return (Set<ArClass>) getRootElements(true, false);
   }
   
   public Set<ArClass> allClasses(Set<ArClass.Kind> classTypes) {
      Set<ArClass> classes = new HashSet<ArClass>();
      for (ArClass clazz : allClasses()) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }
   
   /**
    * A function that transforms an {@link Element} to an {@link ArAnnotatedElement}.
    * 
    * <p>The function will return instances of {@link ArClass}, {@link ArField}, {@link ArMethod},
    * {@link ArConstructor}, {@link ArParameter}, or {@link ArPackage}. If the specified input element
    * does not represent one of these types of elements then {@code null} is returned.
    */
   private Function<Element, ArAnnotatedElement> FROM_ELEMENT =
         (e) -> ReflectionVisitors.ANNOTATED_ELEMENT_VISITOR.visit(e);

   
   private <T extends ArAnnotatedElement> Set<T> convertElements(Set<? extends Element> elements,
         Class<T> clazz) {
      Set<T> ret = new HashSet<T>();
      for (Element element : elements) {
         ArAnnotatedElement a = FROM_ELEMENT.apply(element);
         if (a != null && clazz.isInstance(a)) {
            ret.add(clazz.cast(a));
         }
         // TODO: log or throw if element returned is null?
      }
      return Collections.unmodifiableSet(ret);
   }
   
   private <T extends ArAnnotatedElement> Set<T> getElements(Class<? extends Annotation> annotation,
         Class<T> clazz) {
      return convertElements(env.getElementsAnnotatedWith(annotation), clazz);
   }

   private <T extends ArAnnotatedElement> Set<T> getElements(ArClass annotation, Class<T> clazz) {
      if (!annotation.isAnnotation()) {
         throw new IllegalArgumentException("specified type is not an annotation type");
      }
      return convertElements(env.getElementsAnnotatedWith(annotation.asElement()), clazz);
   }

   public Set<ArAnnotatedElement> annotatedElements(Class<? extends Annotation> annotation) {
      return getElements(annotation, ArAnnotatedElement.class);
   }

   public Set<ArAnnotatedElement> annotatedElements(ArClass annotation) {
      return getElements(annotation, ArAnnotatedElement.class);
   }

   public Set<ArClass> annotatedClasses(Class<? extends Annotation> annotation) {
      return getElements(annotation, ArClass.class);
   }

   public Set<ArClass> annotatedClasses(ArClass annotation) {
      return getElements(annotation, ArClass.class);
   }

   public Set<ArClass> annotatedClasses(Class<? extends Annotation> annotation,
         Set<ArClass.Kind> classTypes) {
      Set<ArClass> classes = new HashSet<ArClass>();
      for (ArClass clazz : annotatedClasses(annotation)) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<ArClass> annotatedClasses(ArClass annotation, Set<ArClass.Kind> classTypes) {
      Set<ArClass> classes = new HashSet<ArClass>();
      for (ArClass clazz : annotatedClasses(annotation)) {
         if (classTypes.contains(clazz.getClassKind())) {
            classes.add(clazz);
         }
      }
      return classes;
   }

   public Set<ArPackage> annotatedPackages(Class<? extends Annotation> annotation) {
      return getElements(annotation, ArPackage.class);
   }

   public Set<ArPackage> annotatedPackages(ArClass annotation) {
      return getElements(annotation, ArPackage.class);
   }

   public Set<ArField> annotatedFields(Class<? extends Annotation> annotation) {
      return getElements(annotation, ArField.class);
   }

   public Set<ArField> annotatedFields(ArClass annotation) {
      return getElements(annotation, ArField.class);
   }

   public Set<ArMethod> annotatedMethods(Class<? extends Annotation> annotation) {
      return getElements(annotation, ArMethod.class);
   }

   public Set<ArMethod> annotatedMethods(ArClass annotation) {
      return getElements(annotation, ArMethod.class);
   }

   public Set<ArConstructor> annotatedConstructors(Class<? extends Annotation> annotation) {
      return getElements(annotation, ArConstructor.class);
   }

   public Set<ArConstructor> annotatedConstructors(ArClass annotation) {
      return getElements(annotation, ArConstructor.class);
   }
}
