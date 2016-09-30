package com.bluegosling.reflect;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Checks if a given type annotation is assignable to another. If an annotation {@code A1} is
 * assignable to annotation {@code A2}, then a given annotated type {@code @A1 T1} is assignable to
 * another type {@code @A2 T2} as long as {@code T1} is assignable to {@code T2} according to the
 * Java type system. (See {@link Types#isAssignable} for more info.)
 * 
 * <p>This can provide functionality similar to the Checker Framework, except that it works at
 * runtime instead of compile-time and is backed by core reflection.
 * 
 * @see TypeAnnotationHierarchy
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@FunctionalInterface
public interface TypeAnnotationChecker {
   
   /**
    * Determines if the given annotation is assignable to the other. Either or both annotations
    * could be {@code null}, indicating unannotated types.
    * 
    * <p>Note that two equivalent types are mutually assignable. So if two annotations indicate the
    * same type, then this method should return true regardless of the order of arguments.
    *
    * @param from annotation that appears on the type of the assignment source (RHS)
    * @param to annotation that appears on the type of the assignment target (LHS)
    * @return true if a type annotated with {@code to} is assignable from a type annotated with
    *       {@code from}
    */
   boolean isAssignable(Annotation from, Annotation to);
   
   /**
    * Determines if the given two annotations are equivalent. Either or both annotations could be
    * {@code null}, indicating unannotated types.
    * 
    * <p>The default implementation verifies that the first annotation is assignable to the second
    * and vice versa: that the second is assignable to the first.
    * 
    * @param a1 a type annotation
    * @param a2 another type annotation
    * @return true if the the two type annotations are equivalent
    */
   default boolean isEquivalent(Annotation a1, Annotation a2) {
      return isAssignable(a1, a2) && isAssignable(a2, a1);
   }

   /**
    * Determines if the one set of annotations is assignable to a second set of annotations. If an
    * annotation {@code A1} is assignable from annotation {@code A2}, then a given annotated
    * type {@code @A1 T1} is assignable from another type {@code @A2 T2} as long as {@code T1} is
    * assignable from {@code T2} according to the Java type system. (See {@link Types#isAssignable}
    * for more info.) 
    * 
    * <p>Note that two equivalent types are mutually assignable. So if two annotations indicate the
    * same type, then this method should return true regardless of the order of arguments.
    *
    * <p>An argument that is empty indicates the absence of any type annotations.
    * 
    * <p>The default implementation reduces the two sets of annotations to single annotations. If
    * either set contains more than one annotation which is not equivalent to the others, then an
    * exception is thrown since this indicates an invalid (or incompatible) set of annotations. Upon
    * reduction, the single from and to annotations are checked via
    * {@link #isAssignable(Annotation, Annotation)}.
    *
    * @param from annotations that appear on the type of the assignment source (RHS)
    * @param to annotations that appear on the type of the assignment target (LHS)
    * @return true if a type annotated with {@code to} annotations is assignable from a type
    *       annotated with {@code from} annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    */
   default boolean isAssignable(Collection<? extends Annotation> from,
         Collection<? extends Annotation> to) {
      Iterator<? extends Annotation> iterFrom = from.iterator();
      Annotation fromAnnotation = iterFrom.hasNext() ? iterFrom.next() : null;
      while (iterFrom.hasNext()) {
         Annotation a = iterFrom.next();
         if (!isEquivalent(fromAnnotation, a)) {
            throw new IllegalArgumentException(
                  "From type contains mutually exclusive annotations: " + fromAnnotation
                  + " and " + a);
         }
      }
      
      Iterator<? extends Annotation> iterTo = to.iterator();
      Annotation toAnnotation = iterTo.hasNext() ? iterTo.next() : null;
      while (iterTo.hasNext()) {
         Annotation a = iterTo.next();
         if (!isEquivalent(toAnnotation, a)) {
            throw new IllegalArgumentException(
                  "To type contains mutually exclusive annotations: " + toAnnotation
                  + " and " + a);
         }
      }
      
      return isAssignable(fromAnnotation, toAnnotation);
   }
   
   /**
    * Determines if a given set of annotations is assignable to a second set of annotations. This is
    * a convenience method for use with core reflection, where most methods that query annotations
    * return arrays, not collections.
    *
    * @param from annotations that appear on the type of the assignment source (RHS)
    * @param to annotations that appear on the type of the assignment target (LHS)
    * @return true if a type annotated with {@code to} annotations is assignable from a type
    *       annotated with {@code from} annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    * @see #isAssignable(Collection, Collection)
    */
   default boolean isAssignable(Annotation[] from, Annotation[] to) {
      return isAssignable(Arrays.asList(from), Arrays.asList(to));
   }
   
   /**
    * Determines if a given set of annotations is equivalent to a second set of annotations. Note
    * that two equivalent types are mutually assignable. So if two annotations indicate the same
    * type, then this method should return true regardless of the order of arguments.
    * 
    * <p>An argument that is empty indicates the absence of any type annotations.
    * 
    * <p>The default implementation reduces the two sets of annotations to single annotations. If
    * either set contains more than one annotation which is not equivalent to the others, then an
    * exception is thrown since this indicates an invalid (or incompatible) set of annotations. Upon
    * reduction, the single from and to annotations are checked via
    * {@link #isEquivalent(Annotation, Annotation)}.
    * 
    * @param a1 a set of annotations associated with a type
    * @param a2 another set of annotations associated with a type
    * @return true if a type annotated with {@code a1} annotations is equivalent to a type
    *       annotated with {@code a2} annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    */
   default boolean isEquivalent(Collection<? extends Annotation> a1,
         Collection<? extends Annotation> a2) {
      Iterator<? extends Annotation> iter1 = a1.iterator();
      Annotation anno1 = iter1.hasNext() ? iter1.next() : null;
      while (iter1.hasNext()) {
         Annotation a = iter1.next();
         if (!isEquivalent(anno1, a)) {
            throw new IllegalArgumentException(
                  "From type contains mutually exclusive annotations: " + anno1
                  + " and " + a);
         }
      }
      
      Iterator<? extends Annotation> iter2 = a2.iterator();
      Annotation anno2 = iter2.hasNext() ? iter2.next() : null;
      while (iter2.hasNext()) {
         Annotation a = iter2.next();
         if (!isEquivalent(anno2, a)) {
            throw new IllegalArgumentException(
                  "To type contains mutually exclusive annotations: " + anno2
                  + " and " + a);
         }
      }
      
      return isEquivalent(anno1, anno2);
   }
   
   /**
    * Determines if a given set of annotations is equivalent to a second set of annotations. This is
    * a convenience method for use with core reflection, where most methods that query annotations
    * return arrays, not collections.
    *
    * @param a1 a set of annotations associated with a type
    * @param a2 another set of annotations associated with a type
    * @return true if a type annotated with {@code a1} annotations is equivalent to a type
    *       annotated with {@code a2} annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    * @see #isEquivalent(Collection, Collection)
    */
   default boolean isEquivalent(Annotation[] a1, Annotation[] a2) {
      return isEquivalent(Arrays.asList(a1), Arrays.asList(a2));
   }

   /**
    * Validates that the given set of annotations is consistent. This is a convenience method for
    * use with core reflection, where most methods that query annotations return arrays, not
    * collections.
    * 
    * @param annotations a set of type annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    * @see #validateAnnotations(Collection)
    */
   default void validateAnnotations(Annotation[] annotations) {
      validateAnnotations(Arrays.asList(annotations));
   }
   
   /**
    * Validates that the given set of annotations is consistent. If the given annotations contains
    * incompatible type annotations (e.g. both {@code Nullable} and {@code NonNull}) then an
    * exception is thrown
    * 
    * <p>The default implementation verifies that all recognized annotations in the given set are
    * {@linkplain #isEquivalent(Annotation, Annotation) equivalent}. Otherwise, the set represents
    * an invalid combination, and an exception is thrown.
    * 
    * @param annotations a set of type annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    */
   default void validateAnnotations(Collection<? extends Annotation> annotations) {
      Iterator<? extends Annotation> iter = annotations.iterator();
      Annotation annotation = iter.hasNext() ? iter.next() : null;
      while (iter.hasNext()) {
         Annotation a = iter.next();
         if (!isEquivalent(annotation, a)) {
            throw new IllegalArgumentException(
                  "Annotated type contains mutually exclusive annotations: " + annotation
                  + " and " + a);
         }
      }
   }
   
   /**
    * Combines two checkers into one. Sets of annotations are only assignable if both this and the
    * given checker both agree they are assignable.
    *
    * @param other another annotation checker
    * @return a new checker that considers annotations assignable only when both this and the
    *       given checker agree they are assignable
    */
   default TypeAnnotationChecker and(TypeAnnotationChecker other) {
      TypeAnnotationChecker self = this;
      return new TypeAnnotationChecker() {
         @Override
         public boolean isAssignable(Annotation from, Annotation to) {
            return self.isAssignable(from, to) && other.isAssignable(from, to);
         }

         @Override
         public boolean isEquivalent(Annotation a1, Annotation a2) {
            return self.isEquivalent(a1, a2) && other.isEquivalent(a1, a2);
         }

         @Override
         public boolean isAssignable(Collection<? extends Annotation> from,
               Collection<? extends Annotation> to) {
            return self.isAssignable(from, to) && other.isAssignable(from, to);
         }

         @Override
         public void validateAnnotations(Collection<? extends Annotation> annotations) {
            self.validateAnnotations(annotations);
            other.validateAnnotations(annotations);
         }
      };
   }
}
