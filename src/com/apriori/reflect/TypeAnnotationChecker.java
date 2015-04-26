package com.apriori.reflect;

import com.apriori.collections.FilteringCollection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

// TODO: more doc, examples, comparisons with checker framework
// TODO: tests
@FunctionalInterface
public interface TypeAnnotationChecker {
   /**
    * Determines if the first given annotations are "assignable from" the second given annotations.
    * If an annotation {@code A1} is assignable from annotation {@code A2}, then a given annotated
    * type {@code @A1 T1} is assignable from another type {@code @A2 T2} as long as {@code T1} is
    * assignable from {@code T2} according to the Java type system. (See {@link Types#isAssignable}
    * for more info.) 
    * 
    * <p>Note that two "equivalent" types are mutually assignable. So if two annotations indicate
    * the same type, then this method should return true regardless of the order of arguments.
    * 
    * <p>An empty collection is passed if the corresponding type is not annotated.
    *
    * @param target annotations that appear on the type of the assignment target (LHS)
    * @param source annotations that appear on the type of the assignment source (RHS)
    * @return true if a type annotated with {@code target} annotations is assignable from a type
    *       annotated with {@code source} annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    */
   boolean isAssignable(Collection<? extends Annotation> target,
         Collection<? extends Annotation> source);
  
   /**
    * Combines two checkers into one. Sets of annotations are only assignable if both this and the
    * given checker both agree they are assignable.
    *
    * @param other another annotation checker
    * @return a new checker that considers annotations assignable only when both this and the
    *       given checker agree they are assignable
    */
   default TypeAnnotationChecker and(TypeAnnotationChecker other) {
      return (a1, a2) -> this.isAssignable(a1, a2) && other.isAssignable(a1, a2);
   }
   
   /**
    * A builder for creating instances of {@link TypeAnnotationChecker}. This provides a simple
    * API for defining assignment compatibility of type annotations. Assignability is defined in
    * terms of pairs of annotation types: an assignment target and an assignment source. Annotations
    * with fields are further checked for value compatibility by checking that the values on a
    * target annotation match those of a source annotation. 
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Builder {
      private static final Method[] EMPTY_METHODS = new Method[0];
      
      /**
       * A structure with annotations that represent super-types of a given annotation. This also
       * includes equivalences (two equivalent annotations are both super-types and sub-types of
       * one another).
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private static class SuperTypeInfo {
         final Set<Class<? extends Annotation>> assignableTo;
         final Set<Class<? extends Annotation>> equivalences;
         
         /** Create empty sources. */
         SuperTypeInfo() {
            assignableTo = new LinkedHashSet<>();
            equivalences = new LinkedHashSet<>();
         }

         /** Create a copy of the given sources. */
         SuperTypeInfo(SuperTypeInfo s) {
            this.assignableTo = new LinkedHashSet<>(s.assignableTo);
            this.equivalences = new LinkedHashSet<>(s.equivalences);
         }
      }

      /**
       * The map of type relationships. The keys are assignment sources and the values contain
       * allowed assignment targets. Allowed assignment targets are annotations that represent
       * super-types of the key annotation.
       */
      private final Map<Class<? extends Annotation>, SuperTypeInfo> superTypes =
            new LinkedHashMap<>();

      public Builder equivalent(Class<? extends Annotation> anno1,
            Class<? extends Annotation> anno2) {
         checkNotAssignable(anno1, anno2);
         checkNotAssignable(anno2, anno1);
         checkNoCycle(anno1, anno2, true);
         checkNoCycle(anno2, anno1, true);
         checkStructure(anno1, anno2);
         checkStructure(anno2, anno1);
         // and store two pairs, one for each direction
         superTypes.compute(anno1, (k, v) -> {
            if (v == null) {
               v = new SuperTypeInfo();
            }
            v.equivalences.add(anno2);
            return v;
         });
         superTypes.compute(anno2, (k, v) -> {
            if (v == null) {
               v = new SuperTypeInfo();
            }
            v.equivalences.add(anno1);
            return v;
         });
         return this;
      }
      
      public Builder assignable(Class<? extends Annotation> to,
            Class<? extends Annotation> from) {
         checkNotEquivalent(to, from);
         checkNoCycle(to, from, false);
         checkStructure(to, from);
         superTypes.compute(from, (k, v) -> {
            if (v == null) {
               v = new SuperTypeInfo();
            }
            v.assignableTo.add(to);
            return v;
         });
         superTypes.computeIfAbsent(to, k -> new SuperTypeInfo());
         return this;
      }

      private void checkNotEquivalent(Class<? extends Annotation> anno1,
            Class<? extends Annotation> anno2) {
         SuperTypeInfo s = superTypes.get(anno1);
         if (s != null && s.equivalences.contains(anno2)) {
            throw new IllegalStateException(getName(anno1) + " and " + getName(anno2)
                  + " are already defined as equivalent");
         }
      }

      private void checkNotAssignable(Class<? extends Annotation> to,
            Class<? extends Annotation> from) {
         SuperTypeInfo s = superTypes.get(from);
         if (s != null && s.assignableTo.contains(to)) {
            throw new IllegalStateException(getName(to) + " is already defined as assignable from "
                  + getName(from));
         }
      }

      private void checkNoCycle(Class<? extends Annotation> to, Class<? extends Annotation> from,
            boolean forEquivalence) {
         if (hasCycle(to, from, forEquivalence, new HashSet<>())) {
            String combineMessage = forEquivalence ? " equivalent to " : " assignable from ";
            throw new IllegalArgumentException("Making " + getName(to) + combineMessage
                  + getName(from) + " would result in a cycle");
         }
      }
      
      private boolean hasCycle(Class<? extends Annotation> to, Class<? extends Annotation> from,
            boolean forEquivalence, Set<Class<? extends Annotation>> alreadySeen) {
         if (Objects.equals(to, from)) {
            return true;
         }
         if (!alreadySeen.add(to)) {
            return false;
         }
         SuperTypeInfo s = superTypes.get(to);
         if (s != null) {
            for (Class<? extends Annotation> superType : s.assignableTo) {
               if (hasCycle(superType, from, false, alreadySeen)) {
                  return true;
               }
            }
            for (Class<? extends Annotation> eq : s.equivalences) {
               if (forEquivalence && Objects.equals(to, eq)) {
                  // skip existing info about their equivalence (otherwise, this would look like
                  // there's a cycle when it's actually a relationship we're going to overwrite)
                  continue;
               }
               if (hasCycle(eq, from, true, alreadySeen)) {
                  return true;
               }
            }
         }
         return false;
      }

      private static void checkStructure(Class<? extends Annotation> to,
            Class<? extends Annotation> from) {
         Method methods[] = to == null ? EMPTY_METHODS : to.getDeclaredMethods();
         for (Method m : methods) {
            try {
               if (from == null) {
                  throw new NoSuchMethodException();
               }
               Method fromMethod = from.getDeclaredMethod(m.getName());
               if (!Types.equals(fromMethod.getGenericReturnType(), m.getGenericReturnType())) {
                  throw new IllegalArgumentException(getName(to) + " cannot be assignable from "
                        + getName(from) + " because they have incompatible structure: attribute "
                        + getName(from) + "#" + m.getName() + " has wrong type. Expecting "
                        + m.getGenericReturnType().getTypeName() + "; Actual "
                        + fromMethod.getGenericReturnType().getTypeName());
               }
            } catch (NoSuchMethodException e) {
               throw new IllegalArgumentException(getName(to) + " cannot be assignable from "
                     + getName(from) + " because they have incompatible structure: "
                     + getName(from) + " has no attribute " + m.getName());
            }
         }
      }

      private static String getName(Class<? extends Annotation> anno) {
         return anno == null ? "unqualified" : "@" + anno.getName();
      }

      public TypeAnnotationChecker build() {
         // defensive copy
         Map<Class<? extends Annotation>, SuperTypeInfo> map =
               new LinkedHashMap<>(superTypes.size() * 4 / 3);
         for (Entry<Class<? extends Annotation>, SuperTypeInfo> entry : superTypes.entrySet()) {
            map.put(entry.getKey(), new SuperTypeInfo(entry.getValue()));
         }
         
         return (to, from) -> {
            Collection<? extends Annotation> collTo = new FilteringCollection<>(to,
                  a -> map.containsKey(a.annotationType()));
            Iterator<? extends Annotation> iterTo = collTo.iterator();
            Annotation aTo = iterTo.hasNext() ? iterTo.next() : null;
            if (iterTo.hasNext()) {
               throw new IllegalArgumentException(
                     "Annotated type contains mutually exclusive annotations: " + collTo);
            }
            Collection<? extends Annotation> collFrom = new FilteringCollection<>(from,
                  a -> map.containsKey(a.annotationType()));
            Iterator<? extends Annotation> iterFrom = collFrom.iterator();
            Annotation aFrom = iterFrom.hasNext() ? iterFrom.next() : null;
            if (iterFrom.hasNext()) {
               throw new IllegalArgumentException(
                     "Annotated type contains mutually exclusive annotations: " + collFrom);
            }
            if (Objects.equals(aTo, aFrom)) {
               return true;
            }
            Class<? extends Annotation> typeTo = aTo == null ? null : aTo.annotationType();
            Class<? extends Annotation> typeFrom = aFrom == null ? null : aFrom.annotationType();
            return isAssignable(typeTo, typeFrom, map) && compatibleValues(aTo, aFrom);
         };
      }

      private static boolean isAssignable(Class<? extends Annotation> to,
            Class<? extends Annotation> from,
            Map<Class<? extends Annotation>, SuperTypeInfo> superTypes) {
         return Objects.equals(to, from) || isAssignable(to, from, superTypes, new HashSet<>());
      }
      
      private static boolean isAssignable(Class<? extends Annotation> to,
            Class<? extends Annotation> from,
            Map<Class<? extends Annotation>, SuperTypeInfo> superTypes,
            Set<Class<? extends Annotation>> alreadySeen) {
         if (!alreadySeen.add(from)) {
            return false;
         }
         SuperTypeInfo s = superTypes.get(from);
         if (s == null) {
            return false;
         }
         if (s.assignableTo.contains(to) || s.equivalences.contains(to)) {
            return true;
         }
         for (Class<? extends Annotation> superType : s.assignableTo) {
            if (isAssignable(to, superType, superTypes, alreadySeen)) {
               return true;
            }
         }
         for (Class<? extends Annotation> eq : s.equivalences) {
            if (isAssignable(to, eq, superTypes, alreadySeen)) {
               return true;
            }
         }
         return false;
      }

      private static boolean compatibleValues(Annotation to, Annotation from) {
         if (to == null || to.annotationType().getDeclaredMethods().length == 0) {
            return true;
         }
         Map<String, Object> toAsMap = Annotations.toMap(to);
         Map<String, Object> fromAsMap =
               Annotations.toMap(from, m -> toAsMap.containsKey(m.getName()));
         for (Entry<String, Object> toEntry : toAsMap.entrySet()) {
            if (!toEntry.getValue().equals(fromAsMap.get(toEntry.getKey()))) {
               return false;
            }
         }
         return true;
      }
   }
}
