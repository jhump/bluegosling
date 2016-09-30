package com.bluegosling.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bluegosling.collections.views.FilteringCollection;
import com.bluegosling.collections.views.FilteringMap;
import com.bluegosling.collections.views.TransformingMap;
import com.google.common.collect.Collections2;

/**
 * Models a type hierarchy for type annotations. The same basic assignability concepts apply as for
 * a {@link TypeAnnotationChecker} except that a hierarchy can be queried for all super-types of a
 * given type.
 *  
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: more tests
public interface TypeAnnotationHierarchy extends TypeAnnotationChecker {
   
   /**
    * Returns the set of type annotations that qualify supertypes of types with the given
    * annotation. If an annotation {@code A1} is a supertype annotation of an annotation {@code A2},
    * then for any type {@code T}, {@code A1 T} is a supertype of {@code A2 T}.
    * 
    * <p>If two annotations are equivalent, they are each supertypes of the other (and thus also
    * subtypes of the other).
    * 
    * <p>If this hierarchy does not have knowledge of the given annotation, it should return an
    * empty set. If an unqualified type (e.g. without annotations) is a direct supertype of one
    * with the given annotation, the returned set should include a {@code null}. Similarly, if the
    * provided annotation is {@code null}, the returned set should be annotations for the direct
    * supertypes of an unqualified type.
    * 
    * @param annotation a type annotation
    * @return the supertype annotations of the given annotation 
    */
   Set<Annotation> getDirectSupertypeAnnotations(Annotation annotation);
   
   /**
    * Returns the set of supertypes of the given annotated type. This includes all
    * {@linkplain AnnotatedTypes#getAnnotatedDirectSupertypes(AnnotatedType) supertypes} with the
    * same annotations as given. It also includes formulations of the same type, but with
    * {@linkplain #getDirectSupertypeAnnotations(Annotation) supertype annotations}.
    * 
    * <p>The default implementation uses {@link #getDirectSupertypeAnnotations(Annotation)} and
    * {@link AnnotatedTypes#getAnnotatedDirectSupertypes(AnnotatedType)} to compute the full set
    * of supertypes.
    * 
    * @param type an annotated type
    * @return the annotated supertypes of the given type
    */
   default Set<AnnotatedType> getDirectSupertypes(AnnotatedType type) {
      Set<AnnotatedType> supertypes = new LinkedHashSet<>();
      supertypes.addAll(Arrays.asList(AnnotatedTypes.getAnnotatedDirectSupertypes(type)));
      List<Annotation> annotations = Arrays.asList(type.getDeclaredAnnotations());
      Builder.exhaustAllAnnotationSupertypes(this, annotations, annotations.listIterator(), type,
            supertypes);
      return Collections.unmodifiableSet(supertypes);
   }
   
   /**
    * A builder for creating instances of {@link TypeAnnotationHierarchy}. This provides a simple
    * API for defining the hierarchy via assignment compatibility of type annotations. Assignability
    * is defined in terms of pairs of annotation types: an assignment target and an assignment
    * source. In the hierarchy, if an annotation {@code A1} is assignable from another {@code A2}
    * then {@code A1} is a super-type of {@code A2}. Conversely, {@code A2} is a sub-type of
    * {@code A1} in that example.
    * 
    * <p>Annotations with fields are further checked for value compatibility by checking that
    * the values on a target annotation match those of a source annotation.
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
               if (!Types.isAssignableStrict(fromMethod.getGenericReturnType(),
                     m.getGenericReturnType())) {
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

      public TypeAnnotationHierarchy build() {
         // defensive copy
         Map<Class<? extends Annotation>, SupertypeInfo> map =
               new LinkedHashMap<>(supertypes.size() * 4 / 3);
         for (Entry<Class<? extends Annotation>, SupertypeInfo> entry : supertypes.entrySet()) {
            map.put(entry.getKey(), new SupertypeInfo(entry.getValue()));
         }
         
         return new TypeAnnotationHierarchy() {
            @Override
            public Set<Annotation> getDirectSupertypeAnnotations(Annotation annotation) {
               SupertypeInfo info = map.get(annotation.annotationType());
               if (info == null) {
                  return Collections.emptySet();
               }
               Set<Annotation> superAnnos = new LinkedHashSet<>(
                     (info.assignableTo.size() + info.equivalences.size()) * 4 / 3);
               for (Entry<Class<? extends Annotation>, Function<String, String>> entry
                     : info.assignableTo.entrySet()) {
                  superAnnos.add(computeAnnotation(annotation, entry.getKey(), entry.getValue()));
               }
               for (Entry<Class<? extends Annotation>, Function<String, String>> entry
                     : info.equivalences.entrySet()) {
                  superAnnos.add(computeAnnotation(annotation, entry.getKey(), entry.getValue()));
               }
               return superAnnos;
            }
            
            @Override
            public void validateAnnotations(Collection<? extends Annotation> annotations) {
               Collection<? extends Annotation> coll = new FilteringCollection<>(annotations,
                     a -> map.containsKey(a.annotationType()));
               // TODO: allow dups if they are equivalent
               if (coll.size() > 1) {
                  throw new IllegalArgumentException(
                        "Annotated type contains mutually exclusive annotations: " + coll);
               }
            }
            
            @Override
            public boolean isEquivalent(Annotation a1, Annotation a2) {
               return isCompatible(a1, a2, true);
            }
            
            @Override
            public boolean isAssignable(Annotation from, Annotation to) {
               return isCompatible(from, to, false);
            }
            
            private boolean isCompatible(Annotation from, Annotation to, boolean equivalencesOnly) {
               Class<? extends Annotation> typeFrom = from == null ? null : from.annotationType();
               Class<? extends Annotation> typeTo = to == null ? null : to.annotationType();

               // if we don't recognize an annotation, we ignore it and treat as if unqualified
               if (!map.containsKey(typeFrom)) {
                  typeFrom = null;
                  from = null;
               }
               if (!map.containsKey(typeTo)) {
                  typeTo = null;
                  to = null;
               }

               if (Objects.equals(from, to)) {
                  return true;
               }

               Function<String, String> mapper =
                     getAttributeMapper(typeFrom, typeTo, equivalencesOnly, map);
               return mapper != null && compatibleValues(from, to, typeFrom, typeTo, mapper);
            }
            
            @Override
            public boolean isAssignable(Collection<? extends Annotation> from,
                  Collection<? extends Annotation> to) {
               Collection<? extends Annotation> collFrom = new FilteringCollection<>(from,
                     a -> map.containsKey(a.annotationType()));
               Iterator<? extends Annotation> iterFrom = collFrom.iterator();
               Annotation aFrom = iterFrom.hasNext() ? iterFrom.next() : null;
               // TODO: allow dups if they are equivalent
               if (iterFrom.hasNext()) {
                  throw new IllegalArgumentException(
                        "To type contains mutually exclusive annotations: " + collFrom);
               }
               
               Collection<? extends Annotation> collTo = new FilteringCollection<>(to,
                     a -> map.containsKey(a.annotationType()));
               Iterator<? extends Annotation> iterTo = collTo.iterator();
               Annotation aTo = iterTo.hasNext() ? iterTo.next() : null;
               // TODO: allow dups if they are equivalent
               if (iterTo.hasNext()) {
                  throw new IllegalArgumentException(
                        "From type contains mutually exclusive annotations: " + collTo);
               }
               
               return isAssignable(aFrom, aTo);
            }
            
            @Override
            public boolean isEquivalent(Collection<? extends Annotation> a1,
                  Collection<? extends Annotation> a2) {
               Collection<? extends Annotation> coll1 = new FilteringCollection<>(a1,
                     a -> map.containsKey(a.annotationType()));
               Iterator<? extends Annotation> iter1 = coll1.iterator();
               Annotation anno1 = iter1.hasNext() ? iter1.next() : null;
               // TODO: allow dups if they are equivalent
               if (iter1.hasNext()) {
                  throw new IllegalArgumentException(
                        "To type contains mutually exclusive annotations: " + coll1);
               }
               
               Collection<? extends Annotation> coll2 = new FilteringCollection<>(a2,
                     a -> map.containsKey(a.annotationType()));
               Iterator<? extends Annotation> iter2 = coll2.iterator();
               Annotation anno2 = iter2.hasNext() ? iter2.next() : null;
               // TODO: allow dups if they are equivalent
               if (iter2.hasNext()) {
                  throw new IllegalArgumentException(
                        "From type contains mutually exclusive annotations: " + coll2);
               }
               
               return isEquivalent(anno1, anno2);
            }
         };
      }

      private static <T extends Annotation> T computeAnnotation(Annotation source, Class<T> target,
            Function<String, String> attributeMapper) {
         Map<String, Object> asMap = Annotations.toMap(source);
         asMap = TransformingMap.transformingKeys(asMap, attributeMapper::apply);
         asMap = FilteringMap.filteringKeys(asMap, t -> t != null);
         return Annotations.create(target, asMap);
      }
      
      private static Function<String, String> getAttributeMapper(Class<? extends Annotation> from,
            Class<? extends Annotation> to, boolean equivalencesOnly,
            Map<Class<? extends Annotation>, SupertypeInfo> supertypes) {
         return from == to
               ? Function.identity()
               : getAttributeMapper(from, to, equivalencesOnly, supertypes, new HashSet<>());
      }

      private static Function<String, String> getAttributeMapper(Class<? extends Annotation> from,
            Class<? extends Annotation> to, boolean equivalencesOnly,
            Map<Class<? extends Annotation>, SupertypeInfo> supertypes,
            Set<Class<? extends Annotation>> alreadySeen) {
         if (!alreadySeen.add(from)) {
            return null;
         }
         SupertypeInfo s = supertypes.get(from);
         if (s == null) {
            return null;
         }
         Function<String, String> mapper = s.equivalences.get(to);
         if (mapper != null) {
            return mapper;
         }
         if (!equivalencesOnly) {
            mapper = s.assignableTo.get(to);
            if (mapper != null) {
               return mapper;
            }
         }
         for (Entry<Class<? extends Annotation>, Function<String, String>> entry
               : s.equivalences.entrySet()) {
            mapper =
                  getAttributeMapper(entry.getKey(), to, equivalencesOnly, supertypes, alreadySeen);
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
         if (!equivalencesOnly) {
            for (Entry<Class<? extends Annotation>, Function<String, String>> entry
                  : s.assignableTo.entrySet()) {
               mapper =
                     getAttributeMapper(entry.getKey(), to, equivalencesOnly, supertypes, alreadySeen);
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
      
      /**
       * Computes known permutations of annotations that represent supertype annotations of the
       * given annotated type. The specified hierarchy is consulted to find supertype annotations.
       * 
       * <p>The given list of annotations is a mutable copy of the type's annotations. (This
       * function is recursive and the list will be perturbed with each recursive invocation.) The
       * given iterator indicates this invocation's starting point in the list. It will proceed
       * from this point, replacing annotations with their direct supertype annotations to compute
       * supertype permutations. The given set of supertypes is the result set. As new supertypes
       * are computed, they are added to the set.
       * 
       * <p>The method iterates through the given iterator. For each element (a type annotation),
       * the hierarchy is queried for its supertype annotations. If any, the list is updated to
       * reflect the given annotation being replaced with its supertype. The resulting list of
       * annotations is used to construct an annotated type and added to the list of supertype
       * results. The method then recurses, with the subsequent invocation having an iterator that
       * starts at the next element in the list.
       * 
       * @param hierarchy the hierarchy which can provide direct supertype annotations
       * @param annotations the list of annotations for the type and its computed supertypes
       * @param iter an iterator which enumerates the elements of the given list of annotations
       *       whose supertype annotations are to be explored
       * @param type the type whose supertypes are being computed
       * @param supertypes the result set of supertypes, to which supertypes are added as they
       *       are computed
       */
      private static void exhaustAllAnnotationSupertypes(TypeAnnotationHierarchy hierarchy,
            List<Annotation> annotations, ListIterator<Annotation> iter, AnnotatedType type,
            Set<AnnotatedType> supertypes) {
         while (iter.hasNext()) {
            Annotation a = iter.next();
            for (Annotation superAnno : hierarchy.getDirectSupertypeAnnotations(a)) {
               iter.set(superAnno);
               AnnotatedType newType =
                     AnnotatedTypes.replaceAnnotations(type,
                           Collections2.filter(annotations, v -> v != null));
               supertypes.add(newType);
               // recurse to compute all permutations
               exhaustAllAnnotationSupertypes(hierarchy, annotations,
                     annotations.listIterator(iter.nextIndex()), type, supertypes);
            }
            iter.set(a); // restore
         }
      }
   }
}
