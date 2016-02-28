package com.bluegosling.reflect;

import static java.util.Objects.requireNonNull;

import com.bluegosling.collections.FilteringCollection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Checks if a given set of type annotations is assignable to another set. If an annotation
 * {@code A1} is assignable from annotation {@code A2}, then a given annotated type {@code @A1 T1}
 * is assignable from another type {@code @A2 T2} as long as {@code T1} is assignable from
 * {@code T2} according to the Java type system. (See {@link Types#isAssignable} for more info.)
 *  
 * <p>This provides functionality similar to the Checker Framework, except that it works at runtime
 * instead of compile-time and is backed by core reflection.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: more doc, examples, comparisons with checker framework
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
    * <p>An argument that is empty indicates the absence of any type annotations.
    *
    * @param from annotations that appear on the type of the assignment source (RHS)
    * @param to annotations that appear on the type of the assignment target (LHS)
    * @return true if a type annotated with {@code to} annotations is assignable from a type
    *       annotated with {@code from} annotations
    * @throws IllegalArgumentException if either collection of annotations represents an illegal
    *       combination of type annotations (for example, contains both {@code NotNull} and
    *       {@code Nullable})
    */
   boolean isAssignable(Collection<? extends Annotation> from, Collection<? extends Annotation> to);
   
   /**
    * Determines if the first given annotations are "assignable from" the second given annotations.
    * This is a convenience method for use with core reflection, where most methods that query
    * annotations return arrays, not collections.
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
    * <p>It is valid to pass {@code null} as an annotation type to the builder methods. A
    * {@code null} type annotation corresponds to an unannotated type, or an "unqualified" type. A
    * {@code null} type is considered to have the same structure as a non-{@code null} annotation
    * that has no methods.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class Builder {
      private static final Method[] EMPTY_METHODS = new Method[0];
      
      /**
       * A structure with annotations that represent supertypes of some annotation. This also
       * includes equivalences (two equivalent annotations are both supertypes and subtypes of
       * one another).
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private static class SupertypeInfo {
         /**
          * The set of annotations to which some annotation is assignable. The keys are the
          * annotation types and the values are mapping functions, for mapping attribute names on
          * the supertype to those on some annotation.
          */
         final Map<Class<? extends Annotation>, Function<String, String>> assignableTo;

         /**
          * The set of annotations with which some annotation is equivalent. The keys are the
          * annotation types and the values are mapping functions, for mapping attribute names on
          * the equivalent annotation to those on some annotation.
          */
         final Map<Class<? extends Annotation>, Function<String, String>> equivalences;
         
         /** Create empty sources. */
         SupertypeInfo() {
            assignableTo = new LinkedHashMap<>();
            equivalences = new LinkedHashMap<>();
         }

         /** Create a copy of the given sources. */
         SupertypeInfo(SupertypeInfo s) {
            this.assignableTo = new LinkedHashMap<>(s.assignableTo);
            this.equivalences = new LinkedHashMap<>(s.equivalences);
         }
      }

      /**
       * The map of type relationships. The keys are assignment sources and the values contain
       * allowed assignment targets. Allowed assignment targets are annotations that represent
       * supertypes of the key annotation and those that are considered equivalent to the key
       * annotation.
       */
      private final Map<Class<? extends Annotation>, SupertypeInfo> supertypes =
            new LinkedHashMap<>();

      /**
       * Defines an equivalence relationship between the given two type annotations. The structure
       * of the annotations (method names and their return values) must be the same.
       *
       * @param anno1 a type annotation
       * @param anno2 another type annotation
       * @return {@code this}, for method chaining
       * @throws IllegalArgumentException if the two types do not have compatible structure or if
       *       the two given types are the same type
       * @throws IllegalStateException if introducing the relationship would form a cycle in the
       *       relationships already defined in this builder
       */
      public Builder equivalent(Class<? extends Annotation> anno1,
            Class<? extends Annotation> anno2) {
         if (anno1 == anno2) {
            throw new IllegalArgumentException("Two given types are the same: " + anno1);
         }
         equivalent(anno1, anno2, Function.identity(), Function.identity());
         return this;
      }

      /**
       * Defines an equivalence relationship between the given two type annotations. The signatures
       * of the annotations' methods must be the same, though the names can vary. The given map
       * defines the mapping in structure, from method names in the first annotation to
       * corresponding method names in the second. The map must be invertible -- e.g. the mappings
       * must be 1-to-1 (multiple keys with the same value are not permitted).
       *
       * @param anno1 a type annotation
       * @param anno2 another type annotation
       * @param attributeMap a map of method names in {@code anno1} to their corresponding method
       *       name in {@code anno2}
       * @return {@code this}, for method chaining
       * @throws IllegalArgumentException if the two types do not have compatible structure, if the
       *       two given types are the same type, or if the given mapping is invalid (e.g. missing
       *       a method, has keys and values that do not correspond to valid methods, or does not
       *       have 1-to-1 mappings)
       * @throws IllegalStateException if introducing the relationship would form a cycle in the
       *       relationships already defined in this builder
       */
      public Builder equivalent(Class<? extends Annotation> anno1,
            Class<? extends Annotation> anno2, Map<String, String> attributeMap) {
         if (anno1 == anno2) {
            throw new IllegalArgumentException("Two given types are the same: " + anno1);
         }
         Map<String, String> safeCopy = new LinkedHashMap<>(attributeMap);
         Map<String, String> inverse = invert(safeCopy);
         checkAttributes(anno1, safeCopy.keySet(), true);
         checkAttributes(anno2, inverse.keySet(), true);
         equivalent(anno1, anno2, inverse::get, safeCopy::get);
         return this;
      }

      private void equivalent(Class<? extends Annotation> anno1,
            Class<? extends Annotation> anno2,
            Function<String, String> attributeMapperTo,
            Function<String, String> attributeMapperFrom) {
         checkNotAssignable(anno1, anno2);
         checkNotAssignable(anno2, anno1);
         checkNoCycle(anno1, anno2, true);
         checkNoCycle(anno2, anno1, true);
         checkStructure(anno1, anno2, attributeMapperTo);
         checkStructure(anno2, anno1, attributeMapperFrom);
         // and store two pairs, one for each direction
         supertypes.compute(anno1, (k, v) -> {
            if (v == null) {
               v = new SupertypeInfo();
            }
            v.equivalences.put(anno2, attributeMapperTo);
            return v;
         });
         supertypes.compute(anno2, (k, v) -> {
            if (v == null) {
               v = new SupertypeInfo();
            }
            v.equivalences.put(anno1, attributeMapperFrom);
            return v;
         });
      }
      
      /**
       * Defines an assignability relationship (or subtype relationship) between the given two type
       * annotations. The first type (or source) is assignable to the second type (or target); it
       * can be interpreted as the first type being a subtype of the second type. The structure of
       * the annotations (method names and their return values) must be the compatible: all methods
       * present in the target type are present (with the same return type) in the source type. The
       * source type may have methods that do not appear in the target type, but not vice versa.
       *
       * @param from a source annotation
       * @param to a target annotation
       * @return {@code this}, for method chaining
       * @throws IllegalArgumentException if the two types do not have compatible structure or if
       *       the two given types are the same type
       * @throws IllegalStateException if introducing the relationship would form a cycle in the
       *       relationships already defined in this builder
       */
      public Builder assignable(Class<? extends Annotation> from,
            Class<? extends Annotation> to) {
         if (from == to) {
            throw new IllegalArgumentException("Two given types are the same: " + from);
         }
         assignable(from, to, Function.identity());
         return this;
      }

      /**
       * Defines an assignability relationship (or subtype relationship) between the given two type
       * annotations. The first type (or source) is assignable to the second type (or target); it
       * can be interpreted as the first type being a subtype of the second type. The signatures of
       * the annotations' methods must be the compatible: all methods present in the target type
       * must have a corresponding method with the same return type in the source type. The
       * source type may have methods that do not appear in the target type, but not vice versa. The
       * given map defines the mapping in structure, from method names in the source annotation to
       * corresponding method names in the target. The map must be invertible -- e.g. the mappings
       * must be 1-to-1 (multiple keys with the same value are not permitted).
       *
       * @param from a source annotation
       * @param to a target annotation
       * @param attributeMap a map of method names in {@code from} to their corresponding method
       *       name in {@code to}
       * @return {@code this}, for method chaining
       * @throws IllegalArgumentException if the two types do not have compatible structure, if the
       *       two given types are the same type, or if the given mapping is invalid (e.g. missing a
       *       mapping for the target annotation, has keys and values that do not correspond to
       *       valid methods, or does not have 1-to-1 mappings)
       * @throws IllegalStateException if introducing the relationship would form a cycle in the
       *       relationships already defined in this builder
       */
      public Builder assignable(Class<? extends Annotation> from,
            Class<? extends Annotation> to, Map<String, String> attributeMap) {
         if (from == to) {
            throw new IllegalArgumentException("Two given types are the same: " + from);
         }
         Map<String, String> map = invert(attributeMap);
         checkAttributes(to, map.keySet(), true);
         checkAttributes(from, map.values(), false);
         assignable(from, to, map::get);
         return this;
      }

      private void assignable(Class<? extends Annotation> from,
            Class<? extends Annotation> to, Function<String, String> attributeMapper) {
         checkNotAssignable(from, to);
         checkNotEquivalent(from, to);
         checkNoCycle(from, to, false);
         checkStructure(from, to, attributeMapper);
         supertypes.compute(from, (k, v) -> {
            if (v == null) {
               v = new SupertypeInfo();
            }
            v.assignableTo.put(to, attributeMapper);
            return v;
         });
         supertypes.computeIfAbsent(to, k -> new SupertypeInfo());
      }

      private void checkNotEquivalent(Class<? extends Annotation> anno1,
            Class<? extends Annotation> anno2) {
         SupertypeInfo s = supertypes.get(anno1);
         if (s != null && s.equivalences.containsKey(anno2)) {
            throw new IllegalStateException(getName(anno1) + " and " + getName(anno2)
                  + " are already defined as equivalent");
         }
      }

      private void checkNotAssignable(Class<? extends Annotation> from,
            Class<? extends Annotation> to) {
         SupertypeInfo s = supertypes.get(from);
         if (s != null && s.assignableTo.containsKey(to)) {
            throw new IllegalStateException(getName(to) + " is already defined as assignable from "
                  + getName(from));
         }
      }

      private void checkNoCycle(Class<? extends Annotation> from, Class<? extends Annotation> to,
            boolean forEquivalence) {
         if (hasCycle(from, to, forEquivalence, new HashSet<>())) {
            String combineMessage = forEquivalence ? " equivalent to " : " assignable from ";
            throw new IllegalStateException("Making " + getName(to) + combineMessage
                  + getName(from) + " would result in a cycle");
         }
      }
      
      private boolean hasCycle(Class<? extends Annotation> from, Class<? extends Annotation> to,
            boolean forEquivalence, Set<Class<? extends Annotation>> alreadySeen) {
         if (Objects.equals(to, from)) {
            return true;
         }
         if (!alreadySeen.add(to)) {
            return false;
         }
         SupertypeInfo s = supertypes.get(to);
         if (s != null) {
            for (Class<? extends Annotation> supertype : s.assignableTo.keySet()) {
               if (hasCycle(from, supertype, false, alreadySeen)) {
                  return true;
               }
            }
            for (Class<? extends Annotation> eq : s.equivalences.keySet()) {
               if (forEquivalence && Objects.equals(to, eq)) {
                  // skip existing info about their equivalence (otherwise, this would look like
                  // there's a cycle when it's actually a relationship we're going to overwrite)
                  continue;
               }
               if (hasCycle(from, eq, true, alreadySeen)) {
                  return true;
               }
            }
         }
         return false;
      }

      private static void checkAttributes(Class<? extends Annotation> annoType,
            Collection<String> names, boolean namesAreExhaustive) {
         Set<String> annotationAttributes = annoType == null
               ? Collections.emptySet()
               : Arrays.stream(annoType.getDeclaredMethods())
                     .map(Method::getName)
                     .collect(Collectors.toSet());
         Iterator<String> notAttribute = names.stream()
               .filter(s -> !annotationAttributes.contains(s))
               .iterator();
         Iterator<String> notInMap = namesAreExhaustive
               ? annotationAttributes.stream()
                     .filter(s -> !names.contains(s))
                     .iterator()
               : Collections.emptyIterator();
         if (!notAttribute.hasNext() && !notInMap.hasNext()) {
            // attribute map is valid!
            return;
         }
         
         // Build error message about why the attributes are no good.
         StringBuilder sb = new StringBuilder();
         if (notAttribute.hasNext()) {
            sb.append("Given attribute map contains names that are not valid attributes of ")
                  .append(getName(annoType))
                  .append(": ");
            boolean first = true;
            while (notAttribute.hasNext()) {
               if (first) {
                  first = false;
               } else {
                  sb.append(", ");
               }
               sb.append(notAttribute.next());
            }
            sb.append('.');
         }
         if (notInMap.hasNext()) {
            if (sb.length() > 0) {
               sb.append(' ');
            }
            sb.append("Given attribute map is missing attributes of ")
                  .append(getName(annoType))
                  .append(": ");
            boolean first = true;
            while (notInMap.hasNext()) {
               if (first) {
                  first = false;
               } else {
                  sb.append(", ");
               }
               sb.append(notInMap.next());
            }
            sb.append('.');
         }
         throw new IllegalArgumentException(sb.toString());
      }
      
      private static <K, V> Map<V, K> invert(Map<? extends K, ? extends V> map) {
         Map<V, K> inverse = new LinkedHashMap<>();
         for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            K prev = inverse.put(requireNonNull(entry.getValue()), requireNonNull(entry.getKey()));
            if (prev != null) {
               throw new IllegalArgumentException("Attribute map does not provide 1-to-1"
                     + " mapping. Two source attributes, " + entry.getKey() + " and " + prev + ","
                     + " map to the same target attribute, " + entry.getValue());
            }
         }
         return inverse;         
      }
      
      private static void checkStructure(Class<? extends Annotation> from,
            Class<? extends Annotation> to, Function<String, String> attributeMapper) {
         Method methods[] = to == null ? EMPTY_METHODS : to.getDeclaredMethods();
         for (Method m : methods) {
            String fromName = attributeMapper.apply(m.getName());
            try {
               if (from == null) {
                  throw new NoSuchMethodException();
               }
               Method fromMethod = from.getDeclaredMethod(fromName);
               if (!Types.equals(fromMethod.getGenericReturnType(), m.getGenericReturnType())) {
                  throw new IllegalArgumentException(getName(to) + " cannot be assignable from "
                        + getName(from) + " because they have incompatible structure: attribute "
                        + getName(from) + "#" + fromName + " has wrong type. Expecting "
                        + m.getGenericReturnType().getTypeName() + "; Actual "
                        + fromMethod.getGenericReturnType().getTypeName());
               }
            } catch (NoSuchMethodException e) {
               throw new IllegalArgumentException(getName(to) + " cannot be assignable from "
                     + getName(from) + " because they have incompatible structure: "
                     + getName(from) + " has no attribute " + fromName);
            }
         }
      }

      private static String getName(Class<? extends Annotation> anno) {
         return anno == null ? "unqualified" : "@" + anno.getName();
      }

      public TypeAnnotationChecker build() {
         // defensive copy
         Map<Class<? extends Annotation>, SupertypeInfo> map =
               new LinkedHashMap<>(supertypes.size() * 4 / 3);
         for (Entry<Class<? extends Annotation>, SupertypeInfo> entry : supertypes.entrySet()) {
            map.put(entry.getKey(), new SupertypeInfo(entry.getValue()));
         }
         
         return (from, to) -> {
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
            if (Objects.equals(aFrom, aTo)) {
               return true;
            }
            Class<? extends Annotation> typeTo = aTo == null ? null : aTo.annotationType();
            Class<? extends Annotation> typeFrom = aFrom == null ? null : aFrom.annotationType();
            Function<String, String> mapper = getAttributeMapper(typeFrom, typeTo, map);
            return mapper != null && compatibleValues(aFrom, aTo, typeFrom, typeTo, mapper);
         };
      }

      private static Function<String, String> getAttributeMapper(Class<? extends Annotation> from,
            Class<? extends Annotation> to,
            Map<Class<? extends Annotation>, SupertypeInfo> supertypes) {
         return from == to
               ? Function.identity()
               : getAttributeMapper(from, to, supertypes, new HashSet<>());
      }

      private static Function<String, String> getAttributeMapper(Class<? extends Annotation> from,
            Class<? extends Annotation> to,
            Map<Class<? extends Annotation>, SupertypeInfo> supertypes,
            Set<Class<? extends Annotation>> alreadySeen) {
         if (!alreadySeen.add(from)) {
            return null;
         }
         SupertypeInfo s = supertypes.get(from);
         if (s == null) {
            return null;
         }
         Function<String, String> mapper = s.assignableTo.get(to);
         if (mapper != null) {
            return mapper;
         }
         mapper = s.equivalences.get(to);
         if (mapper != null) {
            return mapper;
         }
         for (Entry<Class<? extends Annotation>, Function<String, String>> entry
               : s.assignableTo.entrySet()) {
            mapper = getAttributeMapper(entry.getKey(), to, supertypes, alreadySeen);
            if (mapper != null) {
               if (mapper == Function.<String>identity()) {
                  return entry.getValue();
               } else if (entry.getValue() == Function.<String>identity()) {
                  return mapper;
               } else {
                  return mapper.andThen(entry.getValue());
               }
            }
         }
         for (Entry<Class<? extends Annotation>, Function<String, String>> entry
               : s.equivalences.entrySet()) {
            mapper = getAttributeMapper(entry.getKey(), to, supertypes, alreadySeen);
            if (mapper != null) {
               if (mapper == Function.<String>identity()) {
                  return entry.getValue();
               } else if (entry.getValue() == Function.<String>identity()) {
                  return mapper;
               } else {
                  return mapper.andThen(entry.getValue());
               }
            }
         }
         return null;
      }

      private static boolean compatibleValues(Annotation from, Annotation to,
            Class<? extends Annotation> fromType, Class<? extends Annotation> toType,
            Function<String, String> attributeMapper) {
         if (to == null) {
            // unqualified type has no values to check
            return true;
         }
         Method[] toMethods = toType.getDeclaredMethods();
         if (toMethods.length == 0) {
            return true;
         }
         
         for (Method toMethod : toMethods) {
            Object toValue = Annotations.getAnnotationFieldValue(toMethod, to);
            Method fromMethod;
            if (fromType == toType) {
               fromMethod = toMethod;
            } else {
               String fromName = attributeMapper.apply(toMethod.getName());
               try {
                  fromMethod = from.annotationType().getDeclaredMethod(fromName);
               } catch (NoSuchMethodException e) {
                  throw new RuntimeException(e);
               }
            }
            Object fromValue = Annotations.getAnnotationFieldValue(fromMethod, from);
            if (toValue.getClass().isArray()) {
               assert fromValue.getClass().isArray();
               if (!ArrayUtils.equals(fromValue, toValue)) {
                  return false;
               }
            } else if (!fromValue.equals(toValue)) {
               return false;
            }
         }
         return true;
      }
   }
}
