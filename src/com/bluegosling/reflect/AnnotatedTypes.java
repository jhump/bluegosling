package com.bluegosling.reflect;

import static java.util.Objects.requireNonNull;

import com.bluegosling.collections.MoreIterables;
import com.bluegosling.collections.MapBuilder;
import com.bluegosling.collections.views.FilteringCollection;
import com.bluegosling.collections.views.TransformingCollection;
import com.bluegosling.collections.views.TransformingMap;
import com.bluegosling.function.Predicates;
import com.bluegosling.reflect.Types.ParameterizedTypeImpl;
import com.bluegosling.reflect.Types.TypePathElement;
import com.bluegosling.reflect.Types.WildcardTypeImpl;
import com.google.common.collect.Iterators;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Numerous utility methods for using, constructing, and inspecting annotated types.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotatedType
 */
// TODO: tests, fix up javadoc
public final class AnnotatedTypes {
   static final AnnotatedType OBJECT = newAnnotatedType(Object.class);
   static final AnnotatedType[] JUST_OBJECT = new AnnotatedType[] { OBJECT };
   static final AnnotatedType EXTENDS_ANY =
         new AnnotatedWildcardTypeImpl(OBJECT, true, Collections.emptyList());
   static final AnnotatedType[] EMPTY_TYPES = new AnnotatedType[0];
   private static final AnnotatedType[] ARRAY_INTERFACES = new AnnotatedType[] {
      newAnnotatedType(Cloneable.class), newAnnotatedType(Serializable.class)
   };
   private static final AnnotatedType[] ARRAY_SUPERTYPES = new AnnotatedType[] {
      newAnnotatedType(Object.class), ARRAY_INTERFACES[0], ARRAY_INTERFACES[1]
   };
   private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

   private AnnotatedTypes() {}
   
   /**
    * Returns the component type of the given array type. If the given type does not represent an
    * array type then {@code null} is returned.
    *
    * @param type a generic type
    * @return the component type of given array type or {@code null} if the given type does not
    *       represent an array type
    */
   public static AnnotatedType getComponentType(AnnotatedType type) {
      requireNonNull(type);
      if (type instanceof AnnotatedArrayType) {
         return ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
      } else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = type instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) type).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) type).getAnnotatedBounds();
         if (bounds.length > 0) {
            AnnotatedType componentType = getComponentType(bounds[0]);
            // We synthesize a new wildcard type. So a wildcard type <? extends Number[]> will
            // return a component type of <? extends Number> instead of simply Number. Similarly, a
            // type variable <T extends Number[]> returns a component type of <? extends Number>.
            return componentType != null ? newExtendsAnnotatedWildcardType(componentType) : null;
         }
      }
      // type is not an array type
      return null;
   }
   
   public static AnnotatedType getErasure(AnnotatedType type) {
      return newAnnotatedType(Types.getErasure(type.getType()), type.getDeclaredAnnotations());
   }

   /**
    * Returns the generic superclass of the given type. If the given type is one of the eight
    * primitive types or {@code void}, if it is {@code Object}, or if it is an interface then
    * {@code null} is returned. If the given type is an array type then {@code Object} is the
    * returned superclass.
    * 
    * <p>If the given type is a wildcard or type variable then its first upper bound, if not an
    * interface, is its superclass. If the first upper bound is an interface then, like other
    * interface types, {@code null} is returned.
    *
    * @param type a generic type
    * @return the superclass of the given type
    * 
    * @see Class#getAnnotatedSuperclass()
    */
   // TODO: revise doc
   public static AnnotatedType getAnnotatedSuperclass(AnnotatedType type) {
      requireNonNull(type);
      if (type instanceof AnnotatedParameterizedType) {
         Class<?> superClass = Types.getErasure(type.getType()).getSuperclass();
         if (superClass == null) {
            return null;
         }
         AnnotatedType superType = resolveSupertype(type, superClass);
         assert superType != null;
         return superType;
      } else if (type instanceof AnnotatedArrayType) {
         return newAnnotatedType(Object.class, type.getDeclaredAnnotations());
      } else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = type instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) type).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) type).getAnnotatedBounds();
         assert bounds.length > 0;
         if (Types.isInterface(bounds[0].getType())) {
            return null;
         }
         return addAnnotations(bounds[0], type.getDeclaredAnnotations());
      } else {
         Class<?> clazz = (Class<?>) type.getType();
         AnnotatedType superType = ((Class<?>) type.getType()).getAnnotatedSuperclass();
         if (clazz.getTypeParameters().length == 0) {
            return addAnnotations(superType, type.getDeclaredAnnotations());
         }
         // if the given type is the raw form of a generic type, then its supertype
         // is also a raw type
         return newAnnotatedType(Types.getErasure(superType.getType()),
               combined(superType.getDeclaredAnnotations(),
                     Arrays.asList(type.getDeclaredAnnotations())));
      }
   }
   
   /**
    * Returns the generic interfaces implemented by the given type. If the given type is an
    * interface then the interfaces it directly extends are returned. If the given type is an array
    * then an array containing {@code Serializable} and {@code Cloneable} is returned. If the given
    * type is a class that does not directly implement any interfaces (including primitive types)
    * then an empty array is returned.
    * 
    * <p>If the given type is a wildcard or type variable, then this returns an array containing any
    * upper bounds that are interfaces.
    *
    * @param type a generic type
    * @return the interfaces directly implemented by the given type
    * 
    * @see Class#getAnnotatedInterfaces()
    */
   // TODO: revise doc
   public static AnnotatedType[] getAnnotatedInterfaces(AnnotatedType type) {
      requireNonNull(type);
      if (type instanceof AnnotatedParameterizedType) {
         Class<?> interfaces[] = Types.getErasure(type.getType()).getInterfaces();
         if (interfaces.length == 0) {
            return EMPTY_TYPES;
         }
         int len = interfaces.length;
         AnnotatedType[] annotatedInterfaces = new AnnotatedType[len];
         for (int i = 0; i < len; i++) {
            annotatedInterfaces[i] = resolveSupertype(type, interfaces[i]);
            assert annotatedInterfaces[i] != null;
         }
         return annotatedInterfaces;
      } else if (type instanceof AnnotatedArrayType) {
         Annotation[] annos = type.getDeclaredAnnotations();
         AnnotatedType[] annotatedInterfaces = ARRAY_INTERFACES.clone();
         if (annos.length != 0) {
            for (int i = 0; i < ARRAY_INTERFACES.length; i++) {
               annotatedInterfaces[i] = addAnnotations(annotatedInterfaces[i], annos);
            }
         }
         return annotatedInterfaces;
      } else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = type instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) type).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) type).getAnnotatedBounds();
         assert bounds.length > 0;
         AnnotatedType annotatedInterfaces[] =
               Arrays.stream(bounds)
                     .filter(t -> Types.isInterface(t.getType()))
                     .toArray(sz -> new AnnotatedType[sz]);
         Annotation[] annos = type.getDeclaredAnnotations();
         for (int i = 0; i < annotatedInterfaces.length; i++) {
            annotatedInterfaces[i] = addAnnotations(annotatedInterfaces[i], annos);
         }
         return annotatedInterfaces;
      } else {
         Class<?> clazz = (Class<?>) type.getType();
         AnnotatedType interfaces[] = ((Class<?>) type.getType()).getAnnotatedInterfaces();
         Annotation[] annos = type.getDeclaredAnnotations();
         if (clazz.getTypeParameters().length == 0) {
            for (int i = 0; i < interfaces.length; i++) {
               interfaces[i] = addAnnotations(interfaces[i], annos);
            }
            return interfaces;
         }
         // if the given type is the raw form of a generic type, then its supertype
         // is also a raw type
         List<Annotation> annoList = Arrays.asList(annos);
         for (int i = 0; i < interfaces.length; i++) {
            interfaces[i] = newAnnotatedType(Types.getErasure(interfaces[i].getType()),
                  combined(interfaces[i].getDeclaredAnnotations(), annoList));
         }
         return interfaces;
      }
   }

   /**
    * Returns the set of direct supertypes for the given type. The direct supertypes are determined
    * using the rules described in <em>Subtyping</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10">JLS
    * 4.10</a>).
    * 
    * <p>If invoked for the type {@code Object}, an empty array is returned.
    *
    * @param type a type
    * @return its direct supertypes
    */
   // TODO: revise doc
   public static AnnotatedType[] getAnnotatedDirectSupertypes(AnnotatedType type) {
      Annotation[] annotations = type.getDeclaredAnnotations();
      Type theType = type.getType();
      if (theType instanceof Class) {
         Class<?> clazz = (Class<?>) theType;
         if (clazz.isPrimitive()) {
            Class<?> superType = Types.getPrimitiveSupertype(clazz);
            if (superType == null) {
               return EMPTY_TYPES;
            }
            return new AnnotatedType[] { newAnnotatedType(superType, annotations) };
         }
      }
      
      if (type instanceof AnnotatedArrayType) {
         AnnotatedType component =
               ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
         Type componentType = component.getType();
         if (componentType == Object.class || Types.isPrimitive(componentType)) {
            // base case supertypes for Object[] and primitive arrays
            AnnotatedType[] supertypes = ARRAY_SUPERTYPES.clone();
            if (annotations.length != 0) {
               for (int i = 0; i < supertypes.length; i++) {
                  supertypes[i] = addAnnotations(supertypes[i], annotations);
               }
            }
            return supertypes;
         }
         // create array types for all of the element's supertypes
         AnnotatedType[] superTypes = getAnnotatedDirectSupertypes(component);
         for (int i = 0; i < superTypes.length; i++) {
            superTypes[i] = newAnnotatedArrayType(superTypes[i], annotations);
         }
         return superTypes;
      }
      
      AnnotatedType superClass;
      AnnotatedType[] superInterfaces;
      AnnotatedType rawType;
      if (theType instanceof Class) {
         Class<?> clazz = (Class<?>) theType;
         superClass = clazz.getAnnotatedSuperclass();
         superInterfaces = clazz.getAnnotatedInterfaces();
         rawType = null;
         if (Types.isGeneric(clazz)) {
            // raw type use
            superClass = getErasure(superClass);
            for (int i = 0; i < superInterfaces.length; i++) {
               superInterfaces[i] = getErasure(superInterfaces[i]);
            }
         }
         superClass = addAnnotations(superClass, annotations);
         for (int i = 0; i < superInterfaces.length; i++) {
            superInterfaces[i] = addAnnotations(superInterfaces[i], annotations);
         }
      } else {
         superClass = getAnnotatedSuperclass(type);
         superInterfaces = getAnnotatedInterfaces(type);
         // a raw type is a supertype of a parameterized type 
         rawType = theType instanceof ParameterizedType ? getErasure(type) : null;
      }
      if (superClass == null && superInterfaces.length == 0 && Types.isInterface(theType)) {
         // direct supertype of an interface that has no direct super-interfaces is Object
         superClass = newAnnotatedType(Object.class, annotations);
      }

      // now construct array of results
      if (superClass == null && rawType == null) {
         return superInterfaces;
      }
      int addl = 0;
      if (superClass != null) {
         addl++;
      }
      if (rawType != null) {
         addl++;
      }
      AnnotatedType[] superTypes = new AnnotatedType[superInterfaces.length + addl];
      if (superClass != null) {
         superTypes[0] = superClass;
         System.arraycopy(superInterfaces, 0, superTypes, 1, superInterfaces.length);
      } else {
         System.arraycopy(superInterfaces, 0, superTypes, 0, superInterfaces.length);
      }
      if (rawType != null) {
         superTypes[superTypes.length - 1] = rawType;
      }
      return superTypes;
   }

   /**
    * Returns the set of all supertypes for the given type. This returned set has the same elements
    * as if {@link #getAnnotatedDirectSupertypes(AnnotatedType)} were invoked, and then repeatedly
    * invoked recursively on the returned supertypes until the entire hierarchy is exhausted (e.g.
    * reach {@code Object}, which has no supertypes).
    * 
    * <p>This method uses a breadth-first search and returns a set with deterministic iteration
    * order so that types "closer" to the given type appear first when iterating through the set.
    * 
    * <p>If invoked for the type {@code Object}, an empty set is returned.
    *
    * @param type a type
    * @return the set all of the given type's supertypes
    */
   // TODO: revise doc
   public static Set<AnnotatedType> getAllAnnotatedSupertypes(AnnotatedType type) {
      return Collections.unmodifiableSet(
            new LinkedHashSet<>(getAllAnnotatedSupertypesInternal(type, false, false).values()));
   }
   
   /**
    * Breadth-first searches the type hierarchy, returning all supertypes for the given type. If
    * so instructed, the returned set will also include the given type. If so instructed, all types
    * in the returned set will be erased (e.g. raw) types.
    *
    * @param type a type
    * @param includeType if true, the given type is included in the returned set
    * @param erased if true, only erasures for the supertypes are included in the returned set
    * @return the set of all of the given type's supertypes
    */
   // TODO: revise doc
   private static Map<Class<?>, AnnotatedType> getAllAnnotatedSupertypesInternal(AnnotatedType type,
         boolean includeType, boolean erased) {
      Queue<AnnotatedType> pending = new ArrayDeque<>();
      if (erased) {
         type = getErasure(type);
      }
      pending.add(type);
      Map<Class<?>, AnnotatedType> results = new LinkedHashMap<>();
      while (!pending.isEmpty()) {
         AnnotatedType t = wrap(pending.poll());
         if (t != type || includeType) {
            Class<?> key = Types.getErasure(t.getType());
            AnnotatedType existing = results.putIfAbsent(key, t);
            if (existing != null) {
               if (AnnotatedTypes.equals(t, existing)) {
                  // move along
                  continue;
               }
               results.put(key, addAnnotations(existing, t.getDeclaredAnnotations()));
            }
            AnnotatedType[] superTypes = getAnnotatedDirectSupertypes(t);
            for (AnnotatedType st : superTypes) {
               pending.add(erased ? getErasure(st) : st);
            }
         }
      }
      return results;
   }

   private static Map<Class<?>, AnnotatedType> getAllErasedSupertypesInternal(AnnotatedType type,
         boolean includeType) {
      return getAllAnnotatedSupertypesInternal(type, includeType, true);
   }
   
   /**
    * Computes least upper bounds for the given array of types. This is a convenience method that is
    * shorthand for {@code Types.getLeastUpperBounds(Arrays.asList(types))}.
    *
    * @param types the types whose least upper bounds are computed
    * @return the least upper bounds for the given types
    * 
    * @see #getAnnotatedLeastUpperBounds(Iterable)
    */
   // TODO: revise doc
   public static AnnotatedType[] getAnnotatedLeastUpperBounds(AnnotatedType... types) {
      return getAnnotatedLeastUpperBounds(Arrays.asList(types));
   }

   /**
    * Computes least upper bounds for the given types. The algorithm used is detailed in
    * <em>Least Upper Bound</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.10.4">JLS
    * 4.10.4</a>).
    * 
    * <p>The least upper bounds can include up to one class type but multiple interface types. When
    * the bounds include a class type, it will be the first element of the given array (and all
    * subsequent elements interface types).
    * 
    * <p>The JLS indicates that recursive types, which could result in infinite recursion in
    * computing the least upper bounds, should result in cyclic data structures. Core reflection type
    * interfaces are generally not expected to be cyclic. So if the computed least upper bound
    * {@code `lub`} were, for example, to result in {@code Comparable<`lub`>} then a non-cyclic
    * type, {@code Comparable<?>} would be returned instead.
    * 
    * <p>If a mix of reference and primitive types are given, an empty array is returned since
    * primitive types and reference types have no shared bound.
    *
    * @param types the types whose least upper bounds are computed
    * @return the least upper bounds for the given types
    */
   // TODO: revise doc
   public static AnnotatedType[] getAnnotatedLeastUpperBounds(
         Iterable<? extends AnnotatedType> types) {
      Iterator<? extends AnnotatedType> iter = types.iterator();
      if (!iter.hasNext()) {
         throw new IllegalArgumentException();
      }
      AnnotatedType first = iter.next();
      if (!iter.hasNext()) {
         // if just one type given, it is its own least upper bound
         return new AnnotatedType[] { first };
      }
      
      // Move the types into a set to de-dup. Even if given iterable is a set, we still do this so
      // we have a defensive copy of the set for all subsequent operations.
      OptionalInt numTypes = MoreIterables.trySize(types);
      Set<AnnotatedType> typesSet = new LinkedHashSet<>(numTypes.orElse(6) * 4 / 3);
      typesSet.add(wrap(first));
      while (iter.hasNext()) {
         typesSet.add(wrap(iter.next()));
      }
      if (typesSet.size() == 1) {
         // all other types were duplicates of the first, so least upper bound is the one type
         return new AnnotatedType[] { first };
      }
      
      return leastUpperBounds(typesSet, new HashMap<>());
   }

   /**
    * Computes least upper bounds for the given types, using the given sets to track recursion and
    * prevent recursive types from causing infinite recursion.
    *
    * @param types the types whose least upper bounds are computed
    * @param setsSeen sets of types already observed, mapped to memoized results (to prevent
    *       duplicated work during recursion and also to prevent infinite recursion)
    * @return the least upper bounds for the given types
    */
   // TODO: revise doc
   private static AnnotatedType[] leastUpperBounds(Set<AnnotatedType> types,
         Map<Set<AnnotatedType>, AnnotatedType[]> setsSeen) {
      return leastUpperBounds(types, setsSeen, false);
   }

   /**
    * Computes least upper bounds for the given types or reduces them via similar logic.
    * 
    * <p>In addition to computing least upper bounds, it can also just reduce the given types, using
    * the same logic in computing least upper bounds (as if given types are the intersection of
    * supertypes and then computing the <em>minimal erased candidate set</em> and the best
    * <em>candidate parameterization</em> for candidates that are generic types).
    * 
    * <p>This also uses another given set to track recursions and thereby prevent cycles from
    * causing infinite recursion. If a cycle is detected, a least upper bound of {@code Object} is
    * returned instead of trying to construct cyclic data structures.
    *
    * @param types the types whose least upper bounds are computed (or are reduced)
    * @param setsSeen sets of types already observed, mapped to memoized results (to prevent
    *       duplicated work during recursion and also to prevent infinite recursion)
    * @param reduceTypesDirectly if true, just reduce the given types instead of computing their
    *       least upper bounds
    * @return the least upper bounds for the given types (or the reduction of the given types using
    *       similar logic)
    */
   // TODO: revise doc
   private static AnnotatedType[] leastUpperBounds(Set<AnnotatedType> types,
         Map<Set<AnnotatedType>, AnnotatedType[]> setsSeen, boolean reduceTypesDirectly) {
      AnnotatedType[] cachedResult = setsSeen.get(types);
      if (cachedResult != null) {
         return cachedResult;
      }
      // We seed the map with Object. That way we can avoid infinite recursion if we have recursive
      // types, like Foo extends Comparable<Foo>, and the least upper bound wants to also be
      // recursive (e.g. `lub` == Comparable<`lub`>). The JLS says compilers must model this
      // situation with cyclic data structures. But this is core reflection, not a compiler. We
      // don't want cyclic types because they'd likely cause infinite recursion with algorithms that
      // expect (for good reason) reflection types to be a dag or a tree. So instead of a cyclic
      // data structure, we terminate the cycle with the mother-of-all-upper-bounds: Object
      setsSeen.put(types, justObject(intersectAnnotations(types)));

      // Build erased candidate set
      Map<Class<?>, AnnotatedType> candidateSet;
      if (reduceTypesDirectly) {
         candidateSet = new LinkedHashMap<>(types.size() * 4 / 3);
         for (AnnotatedType t : types) {
            candidateSet.put(Types.getErasure(t.getType()), t);
         }
      } else {
         Iterator<AnnotatedType> iter = types.iterator();
         AnnotatedType first = iter.next();
         candidateSet = getAllErasedSupertypesInternal(first, true);
         while (iter.hasNext()) {
            Map<Class<?>, AnnotatedType> nextCandidates =
                  getAllErasedSupertypesInternal(iter.next(), true);
            for (Iterator<Entry<Class<?>, AnnotatedType>> i = candidateSet.entrySet().iterator();
                  i.hasNext();) {
               Entry<Class<?>, AnnotatedType> entry = i.next();
               AnnotatedType other = nextCandidates.get(entry.getKey());
               if (other == null) {
                  i.remove();
               } else {
                  AnnotatedType current = entry.getValue();
                  entry.setValue(replaceAnnotations(current,
                        intersectAnnotations(current, other)));
               }
            }
         }
      }
      
      if (candidateSet.isEmpty()) {
         // this can only happen if given types include a mix of reference and non-reference types
         assert StreamSupport.stream(types.spliterator(), false)
               .anyMatch(t -> Types.getErasure(t.getType()).isPrimitive());
         assert StreamSupport.stream(types.spliterator(), false)
               .anyMatch(t -> !Types.getErasure(t.getType()).isPrimitive());
         return EMPTY_TYPES;
      }
      
      // Now compute "minimal candidate set" by filtering out redundant supertypes
      for (Iterator<Class<?>> csIter = candidateSet.keySet().iterator(); csIter.hasNext();) {
         Class<?> t1 = csIter.next();
         for (Class<?> t2 : candidateSet.keySet()) {
            if (t1 == t2) {
               continue;
            }      
            if (t1.isAssignableFrom(t2)) {
               csIter.remove();
               break;
            }
         }
      }
      
      // Determine final result by computing parameters for any generic supertypes
      AnnotatedType[] results = new AnnotatedType[candidateSet.size()];
      int index = 0;
      for (Entry<Class<?>, AnnotatedType> entry : candidateSet.entrySet()) {
         Class<?> rawCandidate = entry.getKey();
         AnnotatedType parameterizedCandidate;
         if (!Types.isGeneric(rawCandidate)) {
            // not generic, so no type parameters to compute
            parameterizedCandidate = entry.getValue();
         } else {
            // we have to compute type parameter values
            parameterizedCandidate = null;
            for (AnnotatedType t : types) {
               AnnotatedType resolved;
               if (reduceTypesDirectly) {
                  if (Types.getErasure(t.getType()) != rawCandidate) {
                     continue;
                  }
                  resolved = t;
               } else {
                  resolved = resolveSupertype(t, rawCandidate);
                  assert resolved != null;
               }
               if (resolved.getType() instanceof Class) {
                  // raw type use trumps, so skip parameters
                  parameterizedCandidate = resolved;
                  break;
               }
               assert resolved instanceof AnnotatedParameterizedType;
               
               // reduce this resolution and the current parameterization
               if (parameterizedCandidate == null) {
                  parameterizedCandidate = resolved;
               } else {
                  parameterizedCandidate = leastContainingInvocation(
                        (AnnotatedParameterizedType) parameterizedCandidate,
                        (AnnotatedParameterizedType) resolved,
                        setsSeen);
               }
            }
            assert parameterizedCandidate != null;
         }
         results[index++] = parameterizedCandidate;
      }
      
      // should not end up with more than one class in the results
      assert Arrays.stream(results).filter(t -> Types.isInterface(t.getType())).count()
            >= results.length - 1;
      
      for (int i = 0; i < results.length; i++) {
         if (!Types.isInterface(results[i].getType())) {
            if (i > 0) {
               // move class to beginning of array and shift the rest down
               AnnotatedType tmp = results[i];
               System.arraycopy(results, 0, results, 1, i);
               results[0] = tmp;
            }
            break;
         }
      }
      
      // overwrite the seeded result with the actual
      setsSeen.put(types, results);
      return results;
   }

   private static Set<Annotation> intersectAnnotations(AnnotatedType... types) {
      return intersectAnnotations(Arrays.asList(types));
   }

   private static Set<Annotation> intersectAnnotations(Iterable<AnnotatedType> types) {
      Set<Annotation> annos = new LinkedHashSet<>();
      Iterator<AnnotatedType> iter = types.iterator();
      AnnotatedType type = iter.next();
      addAll(type.getDeclaredAnnotations(), annos);
      while (iter.hasNext()) {
         type = iter.next();
         annos.retainAll(Arrays.asList(type.getDeclaredAnnotations()));
      }
      return annos;
   }
   
   private static AnnotatedType[] justObject(Set<Annotation> annotations) {
      return annotations.isEmpty()
            ? JUST_OBJECT.clone()
            : new AnnotatedType[] { newAnnotatedType(Object.class, annotations) };
   }
   
   /**
    * Computes {@code lcp()}, "least containing invocation", for the given relevant
    * parameterizations (as described in JLS 4.10.4).
    *
    * @param t1 a relevant parameterization
    * @param t2 another relevant parameterization
    * @param setsSeen sets of types already observed during calculation of least upper bounds, to
    *       eliminate duplicated work during recursion and also to prevent infinite recursion
    * @return the most specific parameterization that contains both given
    */
   // TODO: revise doc
   private static AnnotatedType leastContainingInvocation(AnnotatedParameterizedType t1,
         AnnotatedParameterizedType t2, Map<Set<AnnotatedType>, AnnotatedType[]> setsSeen) {
      assert Types.getErasure(t1.getType()) == Types.getErasure(t2.getType());
      AnnotatedType pt1Owner = getOwnerType(t1);
      AnnotatedType pt2Owner = getOwnerType(t2);
      AnnotatedType resultOwner;
      assert (pt1Owner == null) == (pt2Owner == null);
      if (pt1Owner != null) {
         if (pt1Owner.getType() instanceof Class) {
            resultOwner = pt1Owner;
         } else if (pt2Owner.getType() instanceof Class) {
            resultOwner = pt2Owner;
         } else {
            resultOwner = leastContainingInvocation((AnnotatedParameterizedType) pt1Owner,
                  (AnnotatedParameterizedType) pt2Owner, setsSeen);
         }
      } else {
         resultOwner = null;
      }
      AnnotatedType[] pt1Args = t1.getAnnotatedActualTypeArguments();
      AnnotatedType[] pt2Args = t2.getAnnotatedActualTypeArguments();
      assert pt1Args.length == pt2Args.length;
      AnnotatedType[] resultArgs = new AnnotatedType[pt1Args.length];
      for (int i = 0; i < pt1Args.length; i++) {
         resultArgs[i] = leastContainingTypeArgument(pt1Args[i], pt2Args[i], setsSeen);
      }
      return new AnnotatedParameterizedTypeImpl(resultOwner, Types.getErasure(t1.getType()),
            Arrays.asList(resultArgs), intersectAnnotations(t1, t2));
   }
   
   /**
    * Computes {@code lcta()}, "least containing type argument", for the given type argument values
    * (as described in JLS 4.10.4).
    *
    * @param t1 a value for the type argument being resolved
    * @param t2 another value for the type argument being resolved
    * @param setsSeen sets of types already observed during calculation of least upper bounds, to
    *       eliminate duplicated work during recursion and also to prevent infinite recursion
    * @return the most specific type argument that contains both the given values
    */
   // TODO: revise doc
   private static AnnotatedType leastContainingTypeArgument(AnnotatedType t1, AnnotatedType t2,
         Map<Set<AnnotatedType>, AnnotatedType[]> setsSeen) {
      if (t1 instanceof AnnotatedWildcardType) {
         return leastContainingTypeArgument((AnnotatedWildcardType) t1, t2, setsSeen);
      } else if (t2 instanceof WildcardType) {
         return leastContainingTypeArgument((AnnotatedWildcardType) t2, t1, setsSeen);
      } else {
         // JLS: lcta(U, V) = U if U = V, otherwise ? extends lub(U, V)
         Set<AnnotatedType> asSet = new LinkedHashSet<>(4);
         asSet.add(wrap(t1));
         asSet.add(wrap(t2));
         if (asSet.size() == 1) {
            return t1;
         }
         AnnotatedType[] lubs = leastUpperBounds(asSet, setsSeen);
         return new AnnotatedWildcardTypeImpl(Arrays.asList(lubs), Collections.emptyList(),
               Collections.emptyList());
      }
   }

   /**
    * Computes {@code lcta()} for type arguments when one is a wildcard type (as described in JLS
    * 4.10.4).
    *
    * @param t1 a wildcard type value for the type argument being resolved
    * @param t2 another value (may or may not be a wildcard) for the type argument being resolved
    * @param setsSeen sets of types already observed during calculation of least upper bounds, to
    *       eliminate duplicated work during recursion and also to prevent infinite recursion
    * @return the most specific type argument that contains both the given values
    */
   // TODO: revise doc
   private static AnnotatedType leastContainingTypeArgument(AnnotatedWildcardType t1,
         AnnotatedType t2, Map<Set<AnnotatedType>, AnnotatedType[]> setsSeen) {
      AnnotatedType[] superBounds = t1.getAnnotatedLowerBounds();
      AnnotatedType[] extendsBounds = t1.getAnnotatedUpperBounds();
      assert extendsBounds.length > 0;
      
      // Lower bounds
      if (superBounds.length != 0) {
         if (!isExtendsAny(extendsBounds)) {
            // a wildcard with both super and extends: treat as if it were "?"
            superBounds = EMPTY_TYPES;
            extendsBounds = JUST_OBJECT;
         } else {
            if (t2 instanceof AnnotatedWildcardType) {
               AnnotatedType[] superBounds2 = t1.getAnnotatedLowerBounds();
               AnnotatedType[] extendsBounds2 = t1.getAnnotatedUpperBounds();
               assert extendsBounds2.length > 0;
               if (superBounds2.length != 0) {
                  if (isExtendsAny(extendsBounds2)) {
                     // a wildcard with both super and extends: treat as if it were "?"
                     superBounds2 = EMPTY_TYPES;
                     extendsBounds2 = JUST_OBJECT;
                     // fall-through...
                  } else {
                     // JLS: lcta(? super U, ? super V) = ? super glb(U, V)
                     for (AnnotatedType st : superBounds2) {
                        superBounds = greatestLowerBounds(superBounds, st);
                     }
                     return new AnnotatedWildcardTypeImpl(Arrays.asList(extendsBounds),
                           Arrays.asList(superBounds), Collections.emptyList());
                  }
               }
               // JLS: lcta(? extends U, ? super V) = U if U = V, otherwise ?
               if (superBounds.length == 1 && extendsBounds2.length == 1
                     && equals(superBounds[0], extendsBounds2[0])) {
                  return superBounds[0];
               }
               return EXTENDS_ANY;
            } else {
               // JLS: lcta(U, ? super V) = ? super glb(U, V)
               superBounds = greatestLowerBounds(superBounds, t2);
               return new AnnotatedWildcardTypeImpl(Arrays.asList(extendsBounds),
                     Arrays.asList(superBounds), Collections.emptyList());
            }
         }
      }
      
      // Upper bounds
      if (t2 instanceof AnnotatedWildcardType) {
         AnnotatedType[] superBounds2 = t1.getAnnotatedLowerBounds();
         AnnotatedType[] extendsBounds2 = t1.getAnnotatedUpperBounds();
         assert extendsBounds2.length > 0;
         if (superBounds2.length != 0) {
            if (isExtendsAny(extendsBounds2)) {
               // a wildcard with both super and extends: treat as if it were "?"
               superBounds2 = EMPTY_TYPES;
               extendsBounds2 = JUST_OBJECT;
               // fall-through...
            } else {
               // JLS: lcta(? extends U, ? super V) = U if U = V, otherwise ?
               if (superBounds2.length == 1 && extendsBounds.length == 1
                     && equals(superBounds2[0], extendsBounds[0])) {
                  return superBounds2[0];
               }
               return EXTENDS_ANY;
            }
         }
         // JLS: lcta(? extends U, ? extends V) = ? extends lub(U, V)
         Set<AnnotatedType> typeSet =
               new LinkedHashSet<>((extendsBounds.length + extendsBounds2.length) * 4 /3);
         for (AnnotatedType t : extendsBounds) {
            typeSet.add(wrap(t));
         }
         for (AnnotatedType t : extendsBounds2) {
            typeSet.add(wrap(t));
         }
         AnnotatedType[] lub = leastUpperBounds(typeSet, setsSeen);
         return new AnnotatedWildcardTypeImpl(Arrays.asList(lub), Collections.emptyList(),
               Collections.emptyList());
      } else {
         // JLS: lcta(U, ? extends V) = ? extends lub(U, V)
         Set<AnnotatedType> typeSet =
               new LinkedHashSet<>((extendsBounds.length + 1) * 4 /3);
         for (AnnotatedType t : extendsBounds) {
            typeSet.add(wrap(t));
         }
         typeSet.add(wrap(t2));
         AnnotatedType[] lub = leastUpperBounds(typeSet, setsSeen);
         return new AnnotatedWildcardTypeImpl(Arrays.asList(lub), Collections.emptyList(),
               Collections.emptyList());
      }
   }

   private static AnnotatedType[] greatestLowerBounds(AnnotatedType[] bounds,
         AnnotatedType newBound) {
      // Section 5.1.10 of the JLS seems to define the glb function as a simple intersection of the
      // given arguments. But we also need to reduce the set to eliminate redundant types (where one
      // is a subtype of another).
      
      // NB: Intersections cannot contain conflicting types; e.g. two classes (not interfaces) where
      // one is *not* a subtype of another OR different parameterizations of the same generic type.
      // The natural resolution when faced with a conflict would be a union type, but there is no
      // such thing in Java (at least not in core reflection). So instead, we merge incompatible
      // types via least-upper-bounds. So an attempt to intersect String and Class results in
      // Serializable since String & Class is not possible. Similarly, an attempt to intersect
      // List<String> and List<Class<?>> results in List<? extends Serializable>.
      Set<AnnotatedType> interfaceBounds = new LinkedHashSet<>((bounds.length + 1) * 4 / 3);
      boolean interfacesNeedReduction = false;
      AnnotatedType classBound = null;
      boolean newIsClass = !Types.isInterface(newBound.getType());
      boolean existingHasClass = !Types.isInterface(bounds[0].getType());
      
      // Merge the given bounds array and the new bound into an optional class bound and zero or
      // more interface bounds.
      if (newIsClass && existingHasClass) {
         for (int i = 1; i < bounds.length; i++) {
            interfaceBounds.add(wrap(bounds[i]));
         }
         // compute least upper bounds for possibly-conflicting class types
         AnnotatedType lubs[] = getAnnotatedLeastUpperBounds(bounds[0], newBound);
         if (!Types.isInterface(lubs[0].getType())) {
            classBound = lubs[0];
            if (lubs.length > 1) {
               interfacesNeedReduction = !interfaceBounds.isEmpty();
               for (int i = 1; i < lubs.length; i++) {
                  interfaceBounds.add(wrap(lubs[i]));
               }
            }
         } else {
            interfacesNeedReduction = !interfaceBounds.isEmpty();
            for (AnnotatedType t : lubs) {
               interfaceBounds.add(wrap(t));
            }
         }
      } else {
         if (newIsClass) {
            classBound = newBound;
            for (AnnotatedType t : bounds) {
               interfaceBounds.add(wrap(t));
            }
         } else if (existingHasClass) {
            classBound = bounds[0];
            if (bounds.length > 1) {
               interfacesNeedReduction = true;
               for (int i = 1; i < bounds.length; i++) {
                  interfaceBounds.add(wrap(bounds[i]));
               }
            }
            interfaceBounds.add(wrap(newBound));
         }
      }
      
      AnnotatedType[] reducedInterfaces;
      if (interfacesNeedReduction) {
         // instead of gathering supertypes and reducing to least upper bounds, the last argument
         // being "true" means this will just reduce the given input types
         reducedInterfaces = leastUpperBounds(interfaceBounds, new HashMap<>(), true);
      } else {
         reducedInterfaces = interfaceBounds.toArray(EMPTY_TYPES);
      }
      
      if (classBound == null) {
         return reducedInterfaces;
      } else if (reducedInterfaces.length == 0) {
         return new AnnotatedType[] { classBound };
      } else {
         AnnotatedType[] ret = new AnnotatedType[reducedInterfaces.length + 1];
         ret[0] = classBound;
         System.arraycopy(reducedInterfaces, 0, ret, 1, reducedInterfaces.length);
         return ret;
      }
   }
   
   /**
    * Returns the owner of the given type. The owner is the type's declaring class. If the given
    * type is a top-level type then the owner is {@code null}. Array types, wildcard types, and
    * type variables do not have owners, though their component types / bounds might. So this method
    * return {@code null} if given such a type.
    * 
    * <p>For non-static inner classes, the owner could be a parameterized type. In other cases, the
    * owner type will be a raw type (e.g. a {@code Class} token)
    * 
    * <p><strong>Note:</strong> If the given type was not created using factory methods in this
    * class, the returned type will have no annotations. This is because the reflection APIs do not
    * actually provide access to annotations on the owner types through the {@link AnnotatedType}
    * API (as of Java 8; to be amended in Java 9).
    *
    * @param type the generic type
    * @return the owner of the given type or {@code null} if it has no owner
    * 
    * @see Class#getDeclaringClass()
    * @see ParameterizedType#getOwnerType()
    */
   public static AnnotatedType getOwnerType(AnnotatedType type) {
      requireNonNull(type);
      // TODO: in JRE9 can just use type.getAnnotatedOwnerType() and not need any of this
      if (type instanceof AnnotatedArrayType || type instanceof AnnotatedWildcardType
            || type instanceof AnnotatedTypeVariable) {
         return null;
      } else if (type instanceof AnnotatedDeclaredTypeImpl) {
         return ((AnnotatedDeclaredTypeImpl) type).getAnnotatedOwnerType();
      } else if (type instanceof AnnotatedParameterizedTypeImpl) {
         return ((AnnotatedParameterizedTypeImpl) type).getAnnotatedOwnerType();
      } else if (type instanceof AnnotatedParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType) type.getType(); 
         Type ownerType = parameterizedType.getOwnerType();
         if (ownerType == null) {
            ownerType = Types.getErasure(parameterizedType.getRawType()).getDeclaringClass();
         }
         return ownerType == null ? null : newAnnotatedType(ownerType);
      } else {
         Class<?> clazz = ((Class<?>) type.getType()).getDeclaringClass();
         return clazz == null ? null : newAnnotatedType(clazz);
      }
   }
   
   /**
    * Returns true if both of the given types refer to equal types and have equivalent annotations
    * according to the given annotation checker.
    *
    * @param a a type
    * @param b another type
    * @param checker an annotation checker
    * @return true if the two given types and their annotations are equivalent; false otherwise
    */
   public static boolean equivalent(AnnotatedType a, AnnotatedType b,
         TypeAnnotationChecker checker) {
      return checkCompatible(a, b, checker::isEquivalent);
   }

   /**
    * Returns true if both of the given types refer to equal types and have the same annotations.
    *
    * @param a a type
    * @param b another type
    * @return true if the two given types are equal; false otherwise
    */
   public static boolean equals(AnnotatedType a, AnnotatedType b) {
      return checkCompatible(a, b, (a1, a2) -> {
         if (a1.length != a2.length) {
            return false;
         }
         // TODO: threshold at which point we should dump the array contents into sets to
         // avoid quadratic behavior... for expected cases of small arrays, this should be fine
         for (Annotation anno1 : a1) {
            boolean found = false;
            for (Annotation anno2 : a2) {
               if (anno1.equals(anno2)) {
                  found = true;
                  break;
               }
            }
            if (!found) {
               return false;
            }
         }
         return true;
      });
   }

   private static boolean checkCompatible(AnnotatedType a, AnnotatedType b,
         BiPredicate<Annotation[], Annotation[]> checker) {
      if (requireNonNull(a) == requireNonNull(b)) {
         return true;
      } else if (a instanceof AnnotatedParameterizedType) {
         if (!(b instanceof AnnotatedParameterizedType)) {
            return false;
         }
         AnnotatedParameterizedType p1 = (AnnotatedParameterizedType) a;
         AnnotatedParameterizedType p2 = (AnnotatedParameterizedType) b;
         AnnotatedType owner1 = getOwnerType(p1);
         AnnotatedType owner2 = getOwnerType(p2);
         if (!(owner1 == null ? owner2 == null : checkCompatible(owner1, owner2, checker))) {
            return false;
         }
         ParameterizedType pt1 = (ParameterizedType) p1.getType();
         ParameterizedType pt2 = (ParameterizedType) p2.getType();
         if (!Types.equals(pt1.getRawType(), pt2.getRawType())) {
            return false;
         }
         if (!checker.test(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())) {
            return false;
         }
         AnnotatedType[] args1 = p1.getAnnotatedActualTypeArguments();
         AnnotatedType[] args2 = p2.getAnnotatedActualTypeArguments();
         if (args1.length != args2.length) {
            return false;
         }
         for (int i = 0; i < args1.length; i++) {
            if (!checkCompatible(args1[i], args2[i], checker)) {
               return false;
            }
         }
         return true;
      } else if (a instanceof AnnotatedArrayType) {
         if (!(b instanceof AnnotatedArrayType)) {
            return false;
         }
         AnnotatedArrayType a1 = (AnnotatedArrayType) a;
         AnnotatedArrayType a2 = (AnnotatedArrayType) b;
         return checker.test(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())
               && checkCompatible(a1.getAnnotatedGenericComponentType(),
                     a2.getAnnotatedGenericComponentType(), checker);
      } else if (a instanceof AnnotatedWildcardType) {
         if (!(b instanceof AnnotatedWildcardType)) {
            return false;
         }
         AnnotatedWildcardType w1 = (AnnotatedWildcardType) a;
         AnnotatedWildcardType w2 = (AnnotatedWildcardType) b;
         if (!checker.test(a.getDeclaredAnnotations(), b.getDeclaredAnnotations())) {
            return false;
         }
         AnnotatedType[] bounds1 = w1.getAnnotatedLowerBounds();
         AnnotatedType[] bounds2 = w2.getAnnotatedLowerBounds();
         if (bounds1.length != bounds2.length) {
            return false;
         }
         for (int i = 0; i < bounds1.length; i++) {
            if (!checkCompatible(bounds1[i], bounds2[i], checker)) {
               return false;
            }
         }
         bounds1 = w1.getAnnotatedUpperBounds();
         bounds2 = w2.getAnnotatedUpperBounds();
         if (bounds1.length != bounds2.length) {
            return false;
         }
         for (int i = 0; i < bounds1.length; i++) {
            if (!checkCompatible(bounds1[i], bounds2[i], checker)) {
               return false;
            }
         }
         return true;
      } else if (a instanceof AnnotatedTypeVariable) {
         return b instanceof AnnotatedTypeVariable && Types.equals(a.getType(), b.getType())
               && checker.test(a.getDeclaredAnnotations(), b.getDeclaredAnnotations());
      } else {
         AnnotatedType owner1 = getOwnerType(a);
         AnnotatedType owner2 = getOwnerType(b);
         if (!(owner1 == null ? owner2 == null : checkCompatible(owner1, owner2, checker))) {
            return false;
         }
         return Types.equals(a.getType(), b.getType())
               && checker.test(a.getDeclaredAnnotations(), b.getDeclaredAnnotations());
      }
   }
   
   /**
    * Computes a hash code for the given generic type. The generic type interfaces do not document
    * {@code equals(Object)} or {@code hashCode()} definitions. So this method computes a stable
    * hash, regardless of the underlying type implementation.
    *
    * @param type a generic type
    * @return a hash code for the given type
    */
   public static final int hashCode(AnnotatedType type) {
      // NB: AnnotatedType implementations in JRE do not override equals and hashCode and just
      // use identity-based implementations inherited from Object.
      requireNonNull(type);
      int hash = Types.hashCode(type.getType()) + 37 * hashCode(type.getDeclaredAnnotations());
      AnnotatedType owner = getOwnerType(type);
      if (owner != null) {
         hash = owner.hashCode() + 37 * hash; 
      }
      if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pt = (AnnotatedParameterizedType) type;
         return hash ^ hashCode(pt.getAnnotatedActualTypeArguments());
      } else if (type instanceof AnnotatedArrayType) {
         AnnotatedArrayType gat = (AnnotatedArrayType) type;
         return hash ^ hashCode(gat.getAnnotatedGenericComponentType());
      } else if (type instanceof AnnotatedTypeVariable) {
         // don't need to also include bounds here, so the hash so far suffices
         return hash;
      } else if (type instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType wt = (AnnotatedWildcardType) type;
         return hash ^ hashCode(wt.getAnnotatedLowerBounds())
               ^ hashCode(wt.getAnnotatedUpperBounds());
      } else {
         return hash;
      }      
   }
   
   private static int hashCode(Annotation annotations[]) {
      // computes a hash for the array that, unlike Arrays.hashCode, is *not* order-dependent
      // (so same result as first putting array contents into a Set and then getting its hashCode)
      int result = 0;
      for (Annotation a : annotations) {
         result += a.hashCode();
      }
      return result;
   }
   
   private static int hashCode(AnnotatedType types[]) {
      int result = 1;
      for (AnnotatedType type : types) {
         result = 31 * result + (type == null ? 0 : hashCode(type));
      }
      return result;
   }

   /**
    * Constructs a string representation of the given type. Since the generic type interfaces do not
    * document a {@code toString()} definition, this method can be used to construct a suitable
    * string representation, regardless of the underlying type implementation.
    *
    * @param type a generic type
    * @return a string representation of the given type
    */
   public static String toString(AnnotatedType type) {
      requireNonNull(type);
      StringBuilder sb = new StringBuilder();
      toStringBuilder(type, sb);
      return sb.toString();
   }
   
   private static void toStringBuilder(AnnotatedType type, StringBuilder sb) {
      if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pt = (AnnotatedParameterizedType) type;
         AnnotatedType owner = getOwnerType(pt);
         if (owner == null) {
            for (Annotation a : type.getDeclaredAnnotations()) {
               sb.append(Annotations.toString(a));
               sb.append(" ");
            }
            sb.append(Types.toString(Types.getErasure(pt.getType())));
         } else {
            toStringBuilder(owner, sb);
            sb.append(".");
            for (Annotation a : type.getDeclaredAnnotations()) {
               sb.append(Annotations.toString(a));
               sb.append(" ");
            }
            Class<?> rawType = Types.getErasure(pt.getType());
            String simpleName = rawType.getSimpleName();
            if (simpleName.isEmpty()) {
               // Anonymous class? This shouldn't really be possible: the Java language doesn't
               // allow parameterized anonymous classes. But just in case: use the class name suffix
               // (e.g. "$1") as its simple name
               Class<?> enclosing = rawType.getEnclosingClass();
               assert rawType.getName().startsWith(enclosing.getName());
               simpleName = rawType.getName().substring(enclosing.getName().length());
               assert !simpleName.isEmpty();
            }
            sb.append(simpleName);
         }
         AnnotatedType args[] = pt.getAnnotatedActualTypeArguments();
         if (args.length > 0) {
            sb.append("<");
            boolean first = true;
            for (AnnotatedType arg : args) {
               if (first) {
                  first = false;
               } else {
                  sb.append(",");
               }
               toStringBuilder(arg, sb);
            }
            sb.append(">");
         }
      } else if (type instanceof AnnotatedArrayType) {
         // Doing the nested arrays recursively would result in the reverse order we want since
         // annotations are root component type first, and then outer-most to inner-most. So we 
         // start by finding root component type.
         AnnotatedType componentType = type;
         while (componentType instanceof AnnotatedArrayType) {
            AnnotatedArrayType arrayType = (AnnotatedArrayType) componentType;
            componentType = arrayType.getAnnotatedGenericComponentType();
         }
         toStringBuilder(componentType, sb);
         // then outer-most to inner-most
         while (type instanceof AnnotatedArrayType) {
            Annotation annotations[] = type.getDeclaredAnnotations();
            if (annotations.length > 0) {
               sb.append(" ");
               for (Annotation a : annotations) {
                  sb.append(Annotations.toString(a));
                  sb.append(" ");
               }
            }
            sb.append("[]");
            AnnotatedArrayType arrayType = (AnnotatedArrayType) type;
            type = arrayType.getAnnotatedGenericComponentType();
         }
      } else if (type instanceof AnnotatedWildcardType) {
         for (Annotation a : type.getDeclaredAnnotations()) {
            sb.append(Annotations.toString(a));
            sb.append(" ");
         }
         AnnotatedWildcardType wc = (AnnotatedWildcardType) type;
         AnnotatedType bounds[] = wc.getAnnotatedLowerBounds();
         if (bounds.length > 0) {
            sb.append("? super ");
         } else {
            bounds = wc.getAnnotatedUpperBounds();
            if (bounds.length == 1 && bounds[0].getType() == Object.class
                  && bounds[0].getDeclaredAnnotations().length == 0) {
               sb.append("?");
               return;
            }
            sb.append("? extends ");
         }
         boolean first = true;
         for (AnnotatedType bound : bounds) {
            if (first) {
               first = false;
            } else {
               sb.append("&");
            }
            toStringBuilder(bound, sb);
         }
      } else {
         AnnotatedType owner = getOwnerType(type);
         if (owner == null) {
            toStringBuilder(owner, sb);
            sb.append(".");
         }
         for (Annotation a : type.getDeclaredAnnotations()) {
            sb.append(Annotations.toString(a));
            sb.append(" ");
         }
         sb.append(Types.toString(type.getType()));
      }
   }
   
   /**
    * Determines if a given annotated type is assignable from another. This uses the same rules as
    * {@link Types#isAssignable} (Assignment Conversions, JLS 5.2) for determining whether the types
    * are assignable and uses {@linkplain TypeAnnotationChecker#isAssignable(Collection, Collection)
    * the checker} for determining whether the types' annotations are also assignable.
    * 
    * <p>This crawls the types recursively, testing for assignment-compatibility of enclosed
    * annotated types where appropriate. For example, if given parameterized types,
    * {@code Collection<@NotNull String>} and {@code List<@Nullable String>}, the actual type
    * arguments (and their annotations) are examined. In the example, assuming this checker
    * disallows assigning {@code @Nullable} to {@code NotNull}, false would be returned because the
    * type arguments are not compatible.
    * 
    * <p>Similarly, this crawls up a type's hierarchy. So if a type were declared using
    * {@code class TypeA extends @Blah TypeB}, then {@code TypeA} implicitly is annotated with
    * {@code @Blah}, even if that annotation is not present on the given {@link AnnotatedType}. 
    *
    * @param from the RHS of assignment
    * @param to the LHS of assignment
    * @param checker a type annotation checker
    * @return true if the assignment is allowed
    */
   // TODO: revise doc
   public static boolean isAssignable(AnnotatedType from, AnnotatedType to,
         TypeAnnotationChecker checker) {
      return isAssignable(from, to, checker, Predicates.alwaysAccept(), Collections.emptyList(),
            Predicates.alwaysAccept(), Collections.emptyList());
   }
   
   // TODO: docs
   
   public static boolean isAssignable(AnnotatedType from, AnnotatedType to,
         TypeAnnotationChecker checker, Predicate<? super Annotation> unboxingFilter,
         Predicate<? super Annotation> boxingFilter) {
      return isAssignable(from, to, checker, unboxingFilter, Collections.emptyList(),
            boxingFilter, Collections.emptyList());
   }

   public static boolean isAssignable(AnnotatedType from, AnnotatedType to,
         TypeAnnotationChecker checker, Collection<? extends Annotation> unboxingAdditions,
         Collection<? extends Annotation> boxingAdditions) {
      return isAssignable(from, to, checker, Predicates.alwaysAccept(), unboxingAdditions,
            Predicates.alwaysAccept(), boxingAdditions);
   }

   public static boolean isAssignable(AnnotatedType from, AnnotatedType to,
         TypeAnnotationChecker checker,
         Predicate<? super Annotation> unboxingFilter,
         Collection<? extends Annotation> unboxingAdditions,
         Predicate<? super Annotation> boxingFilter,
         Collection<? extends Annotation> boxingAdditions) {
      // This helper will test identity conversions, widening reference conversions, and unchecked
      // conversions.
      if (isAssignableReference(from, Collections.emptyList(), to, Collections.emptyList(), checker,
            true)) {
         return true;
      }
      // If that fails, we still need to try widening primitive conversion and boxing/unboxing
      // conversion. All of these require from to be a raw class token (either a primitive type OR a
      // boxed primitive type, none which of are generic).
      Type fromType = from.getType();
      Type toType = to.getType();
      if (fromType instanceof Class) {
         Class<?> fromClass = (Class<?>) fromType;
         if (toType instanceof ParameterizedType && fromClass.isPrimitive()) {
            // try a boxing conversion.
            Collection<Annotation> boxedAnnotations =
                  filterAnnotations(from.getDeclaredAnnotations(), boxingFilter);
            AnnotatedType boxedFrom = newAnnotatedType(Types.box(fromClass), boxedAnnotations);
            return isAssignableReference(boxedFrom, boxingAdditions, to, Collections.emptyList(),
                  checker, true); 
         } else if (toType instanceof Class) {
            Class<?> toClass = (Class<?>) toType;
            if (fromClass.isPrimitive()) {
               if (toClass.isPrimitive() && Types.isPrimitiveSubtype(fromClass, toClass)) {
                  // primitive widening conversions
                  return checkAnnotations(from, Collections.emptyList(),
                        to, Collections.emptyList(), checker);
               }
               // boxing conversion
               Collection<Annotation> boxedAnnotations =
                     filterAnnotations(from.getDeclaredAnnotations(), boxingFilter);
               AnnotatedType boxedFrom = newAnnotatedType(Types.box(fromClass), boxedAnnotations);
               return isAssignableReference(boxedFrom, boxingAdditions, to, Collections.emptyList(),
                     checker, true);
            } else if (toClass.isPrimitive()) {
               // unboxing conversion
               Class<?> unboxedFromClass = Types.unbox(fromClass);
               if (unboxedFromClass == fromClass) {
                  // no unboxing can be done
                  return false;
               }
               Collection<Annotation> unboxedAnnotations =
                     filterAnnotations(from.getDeclaredAnnotations(), unboxingFilter);
               AnnotatedType unboxedFrom = newAnnotatedType(unboxedFromClass, unboxedAnnotations);
               return isAssignableReference(unboxedFrom, unboxingAdditions,
                     to, Collections.emptyList(), checker, true);
            }
         }
      }
      return false;
   }
   
   private static Collection<Annotation> filterAnnotations(Annotation[] annotations,
         Predicate<? super Annotation> filter) {
      Collection<Annotation> coll = Arrays.asList(annotations);
      return new FilteringCollection<>(coll, filter);
   }
   
   /**
    * Determines if a given annotated type is strictly assignable to another. This is just like
    * {@link #isAssignable(AnnotatedType, AnnotatedType, TypeAnnotationChecker)} except that it is
    * more restrictive. In particular, it uses the same rules as {@link Class#isAssignableFrom} and
    * {@link Types#isAssignableStrict}, which only consider Identity Conversion (JLS 5.1.1) and
    * Widening Reference Conversion (JLS 5.1.4) when determining if a type can be assigned to
    * another.
    * 
    * @param from the RHS of assignment
    * @param to the LHS of assignment
    * @param checker a type annotation checker
    * @return true if the assignment is allowed
    */
   // TODO: revise doc
   public static boolean isAssignableStrict(AnnotatedType from, AnnotatedType to,
         TypeAnnotationChecker checker) {
      return isAssignableReference(from, Collections.emptyList(), to, Collections.emptyList(),
            checker, false);
   }

   private static boolean isAssignableReference(
         AnnotatedType from, Collection<? extends Annotation> extraFromAnnotations,
         AnnotatedType to, Collection<? extends Annotation> extraToAnnotations,
         TypeAnnotationChecker checker, boolean allowUncheckedConversion) {
      if (requireNonNull(from) == requireNonNull(to)) {
         return true;
      }
      Type fromType = from.getType();
      Type toType = to.getType();
      if (fromType instanceof Class && toType instanceof Class) {
         return ((Class<?>) toType).isAssignableFrom((Class<?>) fromType)
               && checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations, checker);
      } else if (toType == Object.class) {
         return checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations, checker);
      } else if (from instanceof AnnotatedWildcardType
            || from instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = from instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) from).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) from).getAnnotatedBounds();
         for (AnnotatedType bound : bounds) {
            if (isAssignableReference(bound, join(extraFromAnnotations, from.getAnnotations()),
                  to, extraToAnnotations, checker, allowUncheckedConversion)) {
               return true;
            }
         }
         // they might still be assignable if they refer to the same type variable 
         return from instanceof AnnotatedTypeVariable && to instanceof AnnotatedTypeVariable
               && Types.equals(from.getType(), to.getType())
               && checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations, checker);
      } else if (to instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType toParamType = (AnnotatedParameterizedType) to;
         Class<?> toRawType = Types.getErasure(toType);
         if (fromType instanceof Class) {
            Class<?> fromClass = (Class<?>) fromType;
            if (!toRawType.isAssignableFrom(fromClass)) {
               // Raw types aren't even compatible? Abort!
               return false;
            }
            if (fromClass.getTypeParameters().length > 0) {
               // Both types are generic, but RHS has no type arguments (e.g. raw). This requires
               // an unchecked cast
               return allowUncheckedConversion
                     && checkAnnotations(from, extraFromAnnotations,
                           to, extraToAnnotations, checker);
            }
         } else if (fromType instanceof ParameterizedType) {
            ParameterizedType fromParamType = (ParameterizedType) fromType;
            Class<?> fromRawType = (Class<?>) fromParamType.getRawType();
            if (!toRawType.isAssignableFrom(fromRawType)) {
               // Raw types aren't even compatible? Abort!
               return false;
            }
         } else {
            // We handle "from" being a WildcardType or TypeVariable above. If it's
            // a GenericArrayType (only remaining option), return false since arrays
            // cannot be parameterized (only their component types can be).
            return false;
         }
         AnnotatedType resolvedToType = resolveSupertype(from, toRawType);
         AnnotatedType args[] = toParamType.getAnnotatedActualTypeArguments();
         AnnotatedType resolvedArgs[] = getActualTypeArguments(resolvedToType);
         if (resolvedArgs.length == 0) {
            // assigning from raw type to parameterized type requires unchecked cast, so no go
            return allowUncheckedConversion
                  && checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations, checker);
         }
         assert args.length == resolvedArgs.length;
         // check each type argument
         for (int i = 0, len = args.length; i < len; i++) {
            AnnotatedType toArg = args[i];
            AnnotatedType fromArg = resolvedArgs[i];
            if (toArg instanceof AnnotatedWildcardType) {
               AnnotatedWildcardType wildcardArg = (AnnotatedWildcardType) toArg;
               for (AnnotatedType upperBound : wildcardArg.getAnnotatedUpperBounds()) {
                  if (!isAssignableReference(fromArg, Collections.emptyList(), upperBound,
                        Collections.emptyList(), checker, allowUncheckedConversion)) {
                     return false;
                  }
               }
               for (AnnotatedType lowerBound : wildcardArg.getAnnotatedLowerBounds()) {
                  if (!isAssignableReference(lowerBound, Collections.emptyList(), fromArg, 
                        Collections.emptyList(), checker, allowUncheckedConversion)) {
                     return false;
                  }
               }
            } else if (!equivalent(toArg, fromArg, checker)) {
               return false;
            }
         }
         return checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations, checker);
      } else if (to instanceof AnnotatedArrayType) {
         AnnotatedArrayType toArrayType = (AnnotatedArrayType) to;
         if (!checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations,
               checker)) {
            return false;
         }
         if (from instanceof AnnotatedArrayType) {
            return isAssignableReference(
                  ((AnnotatedArrayType) from).getAnnotatedGenericComponentType(),
                  Collections.emptyList(), toArrayType.getAnnotatedGenericComponentType(),
                  Collections.emptyList(), checker, allowUncheckedConversion);
         } else if (fromType instanceof Class) {
            Class<?> fromClass = (Class<?>) fromType;
            return fromClass.isArray() && isAssignableReference(
                  newAnnotatedType(fromClass.getComponentType()), Collections.emptyList(),
                  toArrayType.getAnnotatedGenericComponentType(), Collections.emptyList(), checker,
                  allowUncheckedConversion);
         } else {
            return false;
         }
      } else if (to instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType toWildcard = (AnnotatedWildcardType) to;
         AnnotatedType lowerBounds[] = toWildcard.getAnnotatedLowerBounds();
         if (lowerBounds.length == 0) {
            // Can only assign to a wildcard type based on its lower bounds
            return false;
         }
         for (AnnotatedType bound : lowerBounds) {
            if (!isAssignableReference(from, extraFromAnnotations,
                  bound, join(extraToAnnotations, to.getAnnotations()), checker,
                  allowUncheckedConversion)) {
               return false;
            }
         }
         return checkAnnotations(from, extraFromAnnotations, to, extraToAnnotations, checker);
      } else if (to instanceof AnnotatedTypeVariable) {
         // We don't actually know the type bound to this variable. So we can only assign to it from
         // another instance of the same type variable or some other variable or wildcard that
         // extends it. Both of those cases are handled above (check for `from` being a TypeVariable
         // or WildcardType). So if we get here, it's not assignable.
         return false;
      } else {
         Class<?> toClass = (Class<?>) to.getType();
         if (from instanceof AnnotatedArrayType) {
            if (toClass == Cloneable.class || toClass == Serializable.class) {
               return checkAnnotations(from, extraFromAnnotations, to,
                     extraToAnnotations, checker);
            }
            AnnotatedArrayType fromArrayType = (AnnotatedArrayType) from;
            return toClass.isArray()
                  && isAssignableReference(fromArrayType.getAnnotatedGenericComponentType(),
                        Collections.emptyList(), newAnnotatedType(toClass.getComponentType()),
                        Collections.emptyList(), checker, allowUncheckedConversion)
                  && checkAnnotations(from, extraFromAnnotations,
                        to, extraToAnnotations, checker);
         } else if (from instanceof AnnotatedParameterizedType) {
            Class<?> fromRaw = Types.getErasure(((AnnotatedParameterizedType) from).getType());
            return toClass.isAssignableFrom(fromRaw)
                  && checkAnnotations(from, extraFromAnnotations, to,
                        extraToAnnotations, checker);
         } else {
            return false;
         }
      }
   }
   
   private static <T> Collection<T> join(Collection<? extends T> head, T[] tail) {
      return new AbstractCollection<T>() {
         @Override
         public Iterator<T> iterator() {
            return Iterators.concat(head.iterator(), Iterators.forArray(tail));
         }

         @Override
         public int size() {
            return head.size() + tail.length;
         }
      };
   }

   private static boolean checkAnnotations(
         AnnotatedType from, Collection<? extends Annotation> extraFromAnnotations,
         AnnotatedType to, Collection<? extends Annotation> extraToAnnotations,
         TypeAnnotationChecker checker) {
      return checker.isAssignable(getAllAnnotations(from, extraFromAnnotations),
            getAllAnnotations(to, extraToAnnotations));
   }
   
   private static Collection<Annotation> getAllAnnotations(AnnotatedType type,
         Collection<? extends Annotation> extras) {
      Set<Annotation> allAnnotations = new HashSet<>();
      allAnnotations.addAll(extras);
      addAllAnnotationsFromHierarchy(type, allAnnotations);
      return allAnnotations;
   }
   
   private static void addAllAnnotationsFromHierarchy(AnnotatedType type,
         Set<Annotation> allAnnotations) {
      addAll(type.getAnnotations(), allAnnotations);
      if (type instanceof AnnotatedWildcardType) {
         // add annotations for upper bounds
         AnnotatedType[] bounds = ((AnnotatedWildcardType) type).getAnnotatedUpperBounds();
         for (AnnotatedType b : bounds) {
            addAllAnnotationsFromHierarchy(b, allAnnotations);
         }
      } else if (type instanceof AnnotatedTypeVariable) {
         // add annotations for bounds
         AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) type;
         AnnotatedType[] bounds = typeVar.getAnnotatedBounds();
         for (AnnotatedType b : bounds) {
            addAllAnnotationsFromHierarchy(b, allAnnotations);
         }
         // as well as annotations present on the type variable declaration
         addAll(((TypeVariable<?>) typeVar.getType()).getAnnotations(), allAnnotations); 
      } else {
         // add annotations for super-types
         Class<?> raw = Types.getErasure(type.getType());
         AnnotatedType s = raw.getAnnotatedSuperclass();
         if (s != null) {
            addAllAnnotationsFromHierarchy(s, allAnnotations);
         }
         for (AnnotatedType i : raw.getAnnotatedInterfaces()) {
            addAllAnnotationsFromHierarchy(i, allAnnotations);
         }
      }
   }
   
   private static void addAll(Annotation[] annotations, Set<Annotation> allAnnotations) {
      for (Annotation a : annotations) {
         allAnnotations.add(a);
      }
   }
   
   /**
    * Resolves the given type variable in the context of the given type. For example, if the given
    * type variable is {@code Collection.<E>} and the given type is the parameterized type
    * {@code List<Optional<String>>}, then this will return {@code Optional<String>}.
    * 
    * <p>If the given type variable cannot be resolved then {@code null} is returned. For example,
    * if the type variable given is {@code Map.<K>} and the given type is {@code List<Number>}, then
    * the variable cannot be resolved.
    *
    * @param context the generic type whose context is used to resolve the given variable
    * @param variable the type variable to resolve
    * @return the resolved value of the given variable or {@code null} if it cannot be resolved
    */
   // TODO: revise doc
   public static AnnotatedType resolveTypeVariable(AnnotatedType context,
         TypeVariable<?> variable) {
      GenericDeclaration declaration = variable.getGenericDeclaration();
      if (!(declaration instanceof Class)) {
         return null; // can only resolve variables declared on classes
      }
      while (true) {
         AnnotatedType componentType;
         if (context instanceof AnnotatedArrayType) {
            componentType = ((AnnotatedArrayType) context).getAnnotatedGenericComponentType();
         } else {
            Type contextType = context.getType();
            if (contextType instanceof Class) {
               Class<?> contextClass = (Class<?>) contextType;
               componentType = contextClass.isArray()
                     ? newAnnotatedType(contextClass.getComponentType())
                     : null;
            } else {
               componentType = null;
            }
         }
         if (componentType == null) {
            break;
         }
         context = componentType;
      }
      AnnotatedType superType = resolveSupertype(context, (Class<?>) declaration);
      if (superType == null || superType.getType() instanceof Class) {
         return null; // cannot resolve
      }
      TypeVariable<?>[] vars = ((Class<?>) declaration).getTypeParameters();
      AnnotatedType[] actualArgs =
            ((AnnotatedParameterizedType) superType).getAnnotatedActualTypeArguments();
      assert actualArgs.length == vars.length;
      for (int i = 0, len = vars.length; i < len; i++) {
         if (Types.equals(vars[i], variable)) {
            AnnotatedType value = actualArgs[i];
            // if actual type argument equals the type variable itself, it isn't resolved
            return Types.equals(vars[i], value.getType()) ? null : value;
         }
      }
      throw new AssertionError("should not be reachable");
   }

   /**
    * Resolves the given type in the context of another type. Any type variable references in the
    * type will be resolved using the given context. For example, if the given type is
    * {@code Map<? extends K, ? extends V>} (where {@code K} and {@code V} are the type variables
    * of interface {@code Map}) and the given context is the parameterized type
    * {@code TreeMap<String, List<String>>} then the type returned will be
    * {@code Map<? super String, ? super List<String>>}.
    * 
    * <p>If any type variables present in the given type cannot be resolved, they will be unchanged
    * and continue to refer to type variables in the returned type.
    *
    * @param context the generic type whose context is used to resolve the given type
    * @param typeToResolve the generic type to resolve
    * @return the resolved type
    */
   // TODO: revise doc
   public static AnnotatedType resolveType(AnnotatedType context, AnnotatedType typeToResolve) {
      Map<TypeVariable<?>, AnnotatedType> resolvedVariableValues = new HashMap<>();
      Set<TypeVariable<?>> resolvedVariables = new HashSet<>();
      resolveTypeVariables(context, typeToResolve, resolvedVariableValues, resolvedVariables);
      return replaceTypeVariablesInternal(typeToResolve, resolvedVariableValues);
   }
   
   private static void resolveTypeVariables(AnnotatedType context, AnnotatedType type,
         Map<TypeVariable<?>, AnnotatedType> resolvedVariableValues,
         Set<TypeVariable<?>> resolvedVariables) {
      if (type instanceof AnnotatedParameterizedType) {
         for (AnnotatedType arg
               : ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()) {
            resolveTypeVariables(context, arg, resolvedVariableValues, resolvedVariables);
         }
      } else if (type instanceof AnnotatedArrayType) {
         resolveTypeVariables(context,
               ((AnnotatedArrayType) type).getAnnotatedGenericComponentType(),
               resolvedVariableValues, resolvedVariables);
      } else if (type instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType wt = (AnnotatedWildcardType) type;
         for (AnnotatedType bound : wt.getAnnotatedUpperBounds()) {
            resolveTypeVariables(context, bound, resolvedVariableValues, resolvedVariables);
         }
         for (AnnotatedType bound : wt.getAnnotatedLowerBounds()) {
            resolveTypeVariables(context, bound, resolvedVariableValues, resolvedVariables);
         }
      } else if (type instanceof AnnotatedTypeVariable) {
         TypeVariable<?> tv = (TypeVariable<?>) type.getType();
         TypeVariable<?> wrapper = Types.wrap(tv);
         if (tv.getGenericDeclaration() instanceof Class) {
            // don't bother re-resolving occurrences of variables we've already seen
            if (resolvedVariables.add(wrapper)) {
               AnnotatedType resolvedValue = resolveTypeVariable(context, tv);
               if (resolvedValue != null) {
                  resolvedVariableValues.put(wrapper, resolvedValue);
               }
            }
         }
      }
   }
   
   /**
    * Computes a new version of the given type by replacing all occurrences of a given type variable
    * with a given value for that variable.
    *
    * @param type the type to be resolved
    * @param typeVariable the type variable 
    * @param typeValue the value that will replace the type variable
    * @return the given type, but with references to the given type variable replaced with the given
    *       value
    */
   // TODO: revise doc
   public static AnnotatedType replaceTypeVariable(AnnotatedType type, TypeVariable<?> typeVariable,
         AnnotatedType typeValue) {
      // wrap the variable to make sure its hashCode and equals are well-behaved
      HashMap<TypeVariable<?>, AnnotatedType> resolvedVariables = new HashMap<>();
      resolvedVariables.put(Types.wrap(typeVariable), typeValue);
      // extract additional context from the given type, in case it has resolved type arguments
      // necessary for validating bounds of given type variables
      collectTypeParameters(type, resolvedVariables);
      // check type bounds
      checkTypeValue(typeVariable, typeValue, resolvedVariables);
      return replaceTypeVariablesInternal(type, resolvedVariables);
   }
   
   /**
    * Computes a new version of the given type by replacing all occurrences of mapped type variables
    * with the given mapped values. This provides a way to more efficiently resolve a batch of type
    * variables instead of iteratively resolving one variable at a time.
    *
    * @param type the type to be resolved
    * @param typeVariables a map of type variables to values
    * @return the given type, but with references to the given type variables replaced with the
    *       given mapped values
    */
   // TODO: revise doc
   public static AnnotatedType replaceTypeVariables(AnnotatedType type,
         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      // wrap the variables to make sure their hashCode and equals are well-behaved
      HashMap<TypeVariable<?>, AnnotatedType> resolvedVariables =
            new HashMap<>(typeVariables.size() * 4 / 3);
      for (Entry<TypeVariable<?>, AnnotatedType> entry : typeVariables.entrySet()) {
         TypeVariable<?> typeVariable = entry.getKey();
         AnnotatedType typeValue = entry.getValue();
         resolvedVariables.put(Types.wrap(typeVariable), typeValue);
      }
      // extract additional context from the given type, in case it has resolved type arguments
      // necessary for validating bounds of given type variables
      collectTypeParameters(type, resolvedVariables);
      // check type bounds
      for (Entry<TypeVariable<?>, AnnotatedType> entry : typeVariables.entrySet()) {
         checkTypeValue(entry.getKey(), entry.getValue(), resolvedVariables);
      }
      return replaceTypeVariablesInternal(type, resolvedVariables);
   }
   
   private static AnnotatedType replaceTypeVariablesInternal(AnnotatedType type,
         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      return replaceTypeVariablesInternal(type, typeVariables, false);
   }

   private static AnnotatedType replaceTypeVariablesInternal(AnnotatedType type,
         Map<TypeVariable<?>, AnnotatedType> typeVariables, boolean wildcardForMissingTypeVars) {
      // NB: if replacing variable references in this type result in no changes (e.g. no references
      // to replace), then return the given type object as is. Do *not* return a different-but-equal
      // type since that could cause a lot of excess work to re-construct a complex type for a
      // no-op replacement operation.
      if (typeVariables.isEmpty()) {
         return type;
      }
      
      if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type;
         AnnotatedType initialOwner = getOwnerType(pType);
         AnnotatedType resolvedOwner = initialOwner == null ? null
               : replaceTypeVariablesInternal(initialOwner, typeVariables);
         boolean different = initialOwner != resolvedOwner;
         AnnotatedType initialArgs[] = pType.getAnnotatedActualTypeArguments();
         List<AnnotatedType> resolvedArgs = new ArrayList<>(initialArgs.length);
         for (AnnotatedType initial : initialArgs) {
            AnnotatedType resolved = replaceTypeVariablesInternal(initial, typeVariables);
            resolvedArgs.add(resolved);
            if (initial != resolved) {
               different = true;
            }
         }
         return different
               ? new AnnotatedParameterizedTypeImpl(resolvedOwner,
                     Types.getErasure(pType.getType()), resolvedArgs,
                     Arrays.asList(pType.getDeclaredAnnotations()))
               : type;
         
      } else if (type instanceof AnnotatedArrayType) {
         AnnotatedType initialComponent =
               ((AnnotatedArrayType) type).getAnnotatedGenericComponentType();
         AnnotatedType resolvedComponent =
               replaceTypeVariablesInternal(initialComponent, typeVariables);
         return resolvedComponent != initialComponent
               ? new AnnotatedArrayTypeImpl(resolvedComponent,
                     Arrays.asList(type.getDeclaredAnnotations()))
               : type;
         
      } else if (type instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType wtType = (AnnotatedWildcardType) type;
         boolean different = false;
         AnnotatedType initialUpperBounds[] = wtType.getAnnotatedUpperBounds();
         List<AnnotatedType> resolvedUpperBounds = new ArrayList<>(initialUpperBounds.length);
         for (AnnotatedType initial : initialUpperBounds) {
            AnnotatedType resolved = replaceTypeVariablesInternal(initial, typeVariables);
            resolvedUpperBounds.add(resolved);
            if (initial != resolved) {
               different = true;
            }
         }
         AnnotatedType initialLowerBounds[] = wtType.getAnnotatedLowerBounds();
         List<AnnotatedType> resolvedLowerBounds = new ArrayList<>(initialLowerBounds.length);
         for (AnnotatedType initial : initialLowerBounds) {
            AnnotatedType resolved = replaceTypeVariablesInternal(initial, typeVariables);
            resolvedLowerBounds.add(resolved);
            if (initial != resolved) {
               different = true;
            }
         }
         return different
               ? new AnnotatedWildcardTypeImpl(resolvedUpperBounds, resolvedLowerBounds,
                     Arrays.asList(type.getDeclaredAnnotations()))
               : type;
         
      } else if (type instanceof AnnotatedTypeVariable) {
         AnnotatedTypeVariable annotatedTypeVar = (AnnotatedTypeVariable) type;
         TypeVariable<?> typeVar = (TypeVariable<?>) type.getType();
         AnnotatedType resolvedType = typeVariables.get(Types.wrap(typeVar));
         if (resolvedType == null) {
            if (wildcardForMissingTypeVars) {
               resolvedType = new AnnotatedWildcardTypeImpl(
                     Arrays.asList(annotatedTypeVar.getAnnotatedBounds()), Collections.emptyList(),
                     Arrays.asList(type.getDeclaredAnnotations()));
            } else {
               resolvedType = type;
            }
         } else {
            resolvedType = addAnnotations(resolvedType, type.getDeclaredAnnotations());
         }
         return resolvedType;
         
      } else {
         return type;
      }
   }

   /**
    * Checks whether a parameterization is valid by determining whether the given type argument is
    * compatible with the given type variable.
    *
    * @param variable a type variable
    * @param argument a proposed argument for the type variable
    * @param resolvedVariables current type context, as a map of type variables to their actual
    *       arguments, for resolving types that may reference these variables
    * @throws IllegalArgumentException if the given argument is not a valid value for the given
    *       type variable
    */
   // TODO: revise doc
   private static void checkTypeValue(TypeVariable<?> variable, AnnotatedType argument,
         Map<TypeVariable<?>, AnnotatedType> resolvedVariables) {
      Types.checkTypeValue(variable, argument.getType(),
            TransformingMap.transformingValues(resolvedVariables, AnnotatedType::getType));
   }
   
   /**
    * Returns true if the given wildcard bounds allow any type. This is the case when the given
    * types define no bounds or solely {@code Object}.
    *
    * @param bounds array of type bounds
    * @return true if the given wildcard bounds allow any type.
    */
   private static boolean isExtendsAny(AnnotatedType[] bounds) {
      return bounds.length == 0 || (bounds.length == 1 && bounds[0].getType() == Object.class);
   }
   
   /**
    * For the given generic type, computes the generic super-type corresponding to the given raw
    * class token. If the given generic type is not actually assignable to the given super-type
    * token then {@code null} is returned. 
    * 
    * <p>For example, if the given generic type is {@code List<String>} and the given raw class
    * token is {@code Collection.class}, then this method will resolve type parameters and return a
    * parameterized type: {@code Collection<String>}.
    * 
    * <p>If the given generic type is a raw class token but represents a type with type parameters,
    * then raw types are returned. For example, if the generic type is {@code HashMap.class} and
    * the given raw class token is {@code Map.class}, then this method simply returns the raw type
    * {@code Map.class}. This is also done if any super-type traversed uses raw types. For example,
    * if the given type's super-class were defined as {@code class Xyz extends HashMap}, then the
    * type arguments to {@code HashMap} are lost due to raw type usage and a raw type is returned.
    * 
    * <p>If the given generic type is a raw class token that does <em>not</em> have any type
    * parameters, then the returned value can still be a generic type. For example, if the given
    * type is {@code Xyz.class} and that class is defined as {@code class Xyz extends
    * ArrayList<String>}, then querying for a super-type of {@code List.class} will return a
    * parameterized type: {@code List<String>}.
    * 
    * <p>If the given generic type is a wildcard type or type variable, its super-types include all
    * upper bounds (and their super-types), and the type's hierarchy is traversed as such.
    * 
    * <p>Technically, a generic array type's only super-types are {@code Object}, {@code Cloneable},
    * and {@code Serializable}. However, since array types are co-variant, this method can resolve
    * other super-types to which the given type is assignable. For example, if the given type is
    * {@code HashMap<String, Number>[]} and the given raw class token queried is {@code Map[].class}
    * then this method will resolve type parameters and return a generic type:
    * {@code Map<String, Number>[]}.
    *
    * @param type a generic type
    * @param superClass a class token for the super-type to query
    * @return a generic type that represents the given super-type token resolved in the context of
    *       the given type or {@code null} if the given token is not a super-type of the given
    *       generic type
    */
   // TODO: revise doc
   public static AnnotatedType resolveSupertype(AnnotatedType type, Class<?> superClass) {
      requireNonNull(type);
      requireNonNull(superClass);
      Map<TypeVariable<?>, AnnotatedType> typeVariables = new HashMap<>();
      AnnotatedType superType = findAnnotatedSupertype(type, superClass, typeVariables);
      return superType != null ? replaceTypeVariablesInternal(superType, typeVariables) : null;
   }

   /**
    * Finds the generic super type for the given generic type and super-type token and accumulates
    * type variables and actual type arguments in the given map.
    *
    * @param type the generic type
    * @param superClass the class token for the super-type being queried
    * @param typeVariables a map of type variables that accumulates type variables and actual type
    *       arguments as types are traversed from the given type to the given super-type 
    * @return the generic type corresponding to the given super-type token as returned by
    *       {@link Class#getGenericSuperclass()} or {@link Class#getGenericInterfaces()} 
    */
   // TODO: revise doc
   private static AnnotatedType findAnnotatedSupertype(AnnotatedType type, Class<?> superClass,
         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      if (type instanceof AnnotatedArrayType) {
         if (superClass == Object.class || superClass == Serializable.class
               || superClass == Cloneable.class) {
            return newAnnotatedType(superClass, type.getDeclaredAnnotations());
         }
         if (!superClass.isArray()) {
            return null;
         }
         AnnotatedType resolvedComponentType = findAnnotatedSupertype(
               ((AnnotatedArrayType) type).getAnnotatedGenericComponentType(),
               superClass.getComponentType(),
               typeVariables);
         return resolvedComponentType == null ? null : newAnnotatedArrayType(resolvedComponentType);
         
      } else if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type;
         Class<?> rawType = Types.getErasure(pType.getType());
         if (rawType == superClass) {
            return type;
         }
         AnnotatedType superType = findAnnotatedSupertype(type, superClass, false, typeVariables);
         mergeTypeVariables(pType, rawType, typeVariables);
         return superType;
         
      } else if (type instanceof AnnotatedWildcardType || type instanceof AnnotatedTypeVariable) {
         AnnotatedType bounds[] = type instanceof AnnotatedWildcardType
               ? ((AnnotatedWildcardType) type).getAnnotatedUpperBounds()
               : ((AnnotatedTypeVariable) type).getAnnotatedBounds();
         for (AnnotatedType bound : bounds) {
            AnnotatedType superType = findAnnotatedSupertype(bound, superClass, typeVariables);
            if (superType != null) {
               return superType;
            }
         }
         return null;
         
      } else {
         Class<?> clazz = Types.getErasure(type.getType());
         // If this is a raw type reference to a generic type, just forget generic type information
         // and use raw types from here on out.
         // NOTE: This is how the Java compiler works. It would be kind of nice to instead support
         // a case like so though:
         //    interface X<T> extends List<String> { ... }
         //    X x; // raw type
         //    List<String> l = x; // generates an unchecked cast warning!
         // The Java compiler insists that the last line is an unchecked cast since raw types cause
         // it to ignore generic type information. (Even though the missing type argument to X isn't
         // actually necessary in this example to statically know the parameterized supertype...)
         boolean useRawTypes = clazz.getTypeParameters().length > 0; 
         return findAnnotatedSupertype(type, superClass, useRawTypes, typeVariables);
         
      }
   }
   
   private static AnnotatedType findAnnotatedSupertype(AnnotatedType type, Class<?> superClass,
         boolean useRawTypes, Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      if (!superClass.isAssignableFrom(Types.getErasure(type.getType()))) {
         return null;
      }
      AnnotatedType superType = superClass.isInterface()
             ? findAnnotatedInterface(type, superClass, typeVariables, new HashSet<>())
             : findAnnotatedSuperclass(type, superClass, typeVariables);
      if (useRawTypes) {
         superType = getErasure(superType);
      }
      return addAnnotations(superType, type.getDeclaredAnnotations());
   }
   
   private static AnnotatedType findAnnotatedSuperclass(AnnotatedType type, Class<?> superClass,
         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      Class<?> clazz = Types.getErasure(type.getType());
      Class<?> actualSuper = clazz.getSuperclass();
      assert actualSuper != null;
      AnnotatedType annotatedSuper = clazz.getAnnotatedSuperclass();
      AnnotatedType ret;
      if (actualSuper == superClass) {
         ret = annotatedSuper;
      } else {
         // recurse until we find it
         ret = findAnnotatedSuperclass(annotatedSuper, superClass, typeVariables);
         if (ret == null) {
            return null;
         }
      }
      if (annotatedSuper instanceof AnnotatedParameterizedType) {
         mergeTypeVariables((AnnotatedParameterizedType) annotatedSuper, actualSuper,
               typeVariables);
      }
      return ret;
   }

   private static AnnotatedType findAnnotatedInterface(AnnotatedType type, Class<?> intrface,
         Map<TypeVariable<?>, AnnotatedType> typeVariables, Set<Class<?>> alreadyChecked) {
      Class<?> clazz = Types.getErasure(type.getType());
      if (alreadyChecked.contains(clazz)) {
         return null;
      }
      Class<?> actualInterfaces[] = clazz.getInterfaces();
      AnnotatedType annotatedInterfaces[] = clazz.getAnnotatedInterfaces();
      Class<?> actualSuper = null;
      AnnotatedType annotatedSuper = null;
      AnnotatedType ret = null;
      // not quite breadth-first -- but first, shallowly check all interfaces before we
      // check their super-interfaces
      for (int i = 0, len = actualInterfaces.length; i < len; i++) {
         if (actualInterfaces[i] == intrface) {
            actualSuper = actualInterfaces[i];
            ret = annotatedSuper = annotatedInterfaces[i];
            break;
         }
      }
      if (ret == null) {
         // didn't find it: check super-interfaces
         for (int i = 0, len = actualInterfaces.length; i < len; i++) {
            ret = findAnnotatedInterface(annotatedInterfaces[i], intrface, typeVariables,
                  alreadyChecked);
            if (ret != null) {
               actualSuper = actualInterfaces[i];
               annotatedSuper = annotatedInterfaces[i];
               break;
            }
         }
      }
      if (ret == null) {
         // still didn't find it: check super-class's interfaces
         if ((actualSuper = clazz.getSuperclass()) == null) {
            return null; // no super-class
         }
         annotatedSuper = clazz.getAnnotatedSuperclass();
         ret = findAnnotatedInterface(annotatedSuper, intrface, typeVariables, alreadyChecked);
      }
      if (ret == null) {
         alreadyChecked.add(clazz);
         return null;
      }
      if (annotatedSuper instanceof AnnotatedParameterizedType) {
         mergeTypeVariables((AnnotatedParameterizedType) annotatedSuper, actualSuper,
               typeVariables);
      }
      return ret;
   }
   
   private static void mergeTypeVariables(AnnotatedParameterizedType type, Class<?> rawType,
         Map<TypeVariable<?>, AnnotatedType> typeVariables) {
      AnnotatedType ownerType = getOwnerType(type);
      if (ownerType instanceof AnnotatedParameterizedType) {
         mergeTypeVariables((AnnotatedParameterizedType) ownerType,
               Types.getErasure(ownerType.getType()), typeVariables);
      }
      Map<TypeVariable<?>, AnnotatedType> currentVars = new HashMap<>();
      TypeVariable<?> vars[] = rawType.getTypeParameters();
      AnnotatedType values[] = type.getAnnotatedActualTypeArguments();
      assert vars.length == values.length;
      for (int i = 0, len = vars.length; i < len; i++) {
         currentVars.put(Types.wrap(vars[i]),
               addAnnotations(values[i], vars[i].getDeclaredAnnotations()));
      }
      // update any existing type variable values in case they refer to these new variables
      for (Entry<TypeVariable<?>, AnnotatedType> entry : typeVariables.entrySet()) {
         entry.setValue(replaceTypeVariablesInternal(entry.getValue(), currentVars));
      }
      typeVariables.putAll(currentVars);
   }
   
   public static AnnotatedArrayType newAnnotatedArrayType(GenericArrayType type,
         Annotation... annotations) {
      return newAnnotatedArrayType(type, Arrays.asList(annotations));
   }
   
   public static AnnotatedArrayType newAnnotatedArrayType(GenericArrayType type,
         Iterable<? extends Annotation> annotations) {
      return newAnnotatedArrayType(newAnnotatedType(type.getGenericComponentType()), annotations);
   }
   
   public static AnnotatedArrayType newAnnotatedArrayType(AnnotatedType componentType,
         Annotation... annotations) {
      return newAnnotatedArrayType(componentType, Arrays.asList(annotations));
   }

   /**
    * Creates a new {@link AnnotatedArrayType} object with the given component type.
    *
    * @param componentType the component type of the array
    * @param annotations the annotations on this array type
    * @return an annotated array type with the given component type
    * @throws NullPointerException if the given argument is {@code null}
    * @throws IllegalArgumentException if the given component type is {@code void.class} or a
    *       wildcard type 
    */
   public static AnnotatedArrayType newAnnotatedArrayType(AnnotatedType componentType,
         Iterable<? extends Annotation> annotations) {
      requireNonNull(componentType);
      return new AnnotatedArrayTypeImpl(componentType, annotations);
   }
   
   /**
    * Returns the actual type arguments if the given type is a parameterized type. Otherwise, it
    * returns an empty array.
    *
    * @param type the generic type
    * @return the actual type arguments if the given types is a parameterized type; an empty array
    *       otherwise
    */
   public static AnnotatedType[] getActualTypeArguments(AnnotatedType type) {
      requireNonNull(type);
      if (type instanceof AnnotatedParameterizedType) {
         return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
      } else {
         return EMPTY_TYPES;
      }
   }
   
   public static AnnotatedType newAnnotatedDeclaredType(Class<?> clazz,
         Annotation... annotations) {
      return newAnnotatedDeclaredType(clazz, Arrays.asList(annotations));
   }
   
   public static AnnotatedType newAnnotatedDeclaredType(Class<?> clazz,
         Iterable<? extends Annotation> annotations) {
      if (clazz.isArray()) {
         throw new IllegalArgumentException("given type cannot be an array");
      }
      Class<?> owner = clazz.getDeclaringClass();
      AnnotatedType annotatedOwner = owner == null ? null : newAnnotatedType(owner);
      return new AnnotatedDeclaredTypeImpl(annotatedOwner, clazz, annotations);
   }
   
   public static AnnotatedType newAnnotatedDeclaredType(AnnotatedType owner, Class<?> clazz,
         Annotation... annotations) {
      return newAnnotatedDeclaredType(owner, clazz, Arrays.asList(annotations));
   }

   public static AnnotatedType newAnnotatedDeclaredType(AnnotatedType owner, Class<?> clazz,
         Iterable<? extends Annotation> annotations) {
      Class<?> ownerClass = Types.getErasure(owner.getType());
      if (ownerClass != clazz.getDeclaringClass()) {
         throw new IllegalArgumentException("given owner is of the wrong type");
      }
      return new AnnotatedDeclaredTypeImpl(owner, clazz, annotations);
   }

   public static AnnotatedType newAnnotatedType(Type type, Annotation... annotations) {
      return newAnnotatedType(type, Arrays.asList(annotations));
   }

   public static AnnotatedType newAnnotatedType(Class<?> clazz, Annotation... annotations) {
      return newAnnotatedType(clazz, Arrays.asList(annotations));
   }

   public static AnnotatedType newAnnotatedType(Class<?> clazz,
         Iterable<? extends Annotation> annotations) {
      if (clazz.isArray()) {
         return newAnnotatedArrayType(newAnnotatedType(clazz.getComponentType()), annotations);
      } else {
         return newAnnotatedDeclaredType(clazz, annotations);
      }
   }
   
   public static AnnotatedType newAnnotatedType(Type type,
         Iterable<? extends Annotation> annotations) {
      if (type instanceof Class) {
         return newAnnotatedType((Class<?>) type, annotations);
      } else if (type instanceof ParameterizedType) {
         return newAnnotatedParameterizedType((ParameterizedType) type, annotations);
      } else if (type instanceof GenericArrayType) {
         return newAnnotatedArrayType((GenericArrayType) type, annotations);
      } else if (type instanceof WildcardType) {
         return newAnnotatedWildcardType((WildcardType) type, annotations);
      } else if (type instanceof TypeVariable) {
         return newAnnotatedTypeVariable((TypeVariable<?>) type, annotations);
      } else {
         throw new UnknownTypeException(type);
      }
   }
   
   private static List<AnnotatedType> asAnnotatedTypes(Type[] types) {
      List<AnnotatedType> a = new ArrayList<>(types.length);
      for (Type t : types) {
         a.add(newAnnotatedType(t));
      }
      return Collections.unmodifiableList(a);
   }
   
   public static AnnotatedType addAnnotations(AnnotatedType type, Annotation... annotations) {
      if (annotations.length == 0) {
         return type;
      }
      return addAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedType addAnnotations(AnnotatedType type,
         Collection<? extends Annotation> annotations) {
      if (type instanceof AnnotatedParameterizedType) {
         return addAnnotations((AnnotatedParameterizedType) type, annotations);
      } else if (type instanceof AnnotatedArrayType) {
         return addAnnotations((AnnotatedArrayType) type, annotations);
      } else if (type instanceof AnnotatedWildcardType) {
         return addAnnotations((AnnotatedWildcardType) type, annotations);
      } else if (type instanceof AnnotatedTypeVariable) {
         return addAnnotations((AnnotatedTypeVariable) type, annotations);
      } else {
         if (annotations.isEmpty()) {
            return type;
         }
         return new AnnotatedDeclaredTypeImpl(getOwnerType(type), (Class<?>) type.getType(),
               combined(type.getDeclaredAnnotations(), annotations));
      }
   }
   
   public static AnnotatedParameterizedType addAnnotations(AnnotatedParameterizedType type,
         Annotation... annotations) {
      if (annotations.length == 0) {
         return type;
      }
      return addAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedParameterizedType addAnnotations(AnnotatedParameterizedType type,
         Collection<? extends Annotation> annotations) {
      if (annotations.isEmpty()) {
         return type;
      }
      return new AnnotatedParameterizedTypeImpl(getOwnerType(type),
            Types.getErasure(type.getType()), Arrays.asList(type.getAnnotatedActualTypeArguments()),
            combined(type.getDeclaredAnnotations(), annotations));
   }

   public static AnnotatedArrayType addAnnotations(AnnotatedArrayType type,
         Annotation... annotations) {
      if (annotations.length == 0) {
         return type;
      }
      return addAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedArrayType addAnnotations(AnnotatedArrayType type,
         Collection<? extends Annotation> annotations) {
      if (annotations.isEmpty()) {
         return type;
      }
      return new AnnotatedArrayTypeImpl(type.getAnnotatedGenericComponentType(),
            combined(type.getDeclaredAnnotations(), annotations));
   }
   
   public static AnnotatedWildcardType addAnnotations(AnnotatedWildcardType type,
         Annotation... annotations) {
      if (annotations.length == 0) {
         return type;
      }
      return addAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedWildcardType addAnnotations(AnnotatedWildcardType type,
         Collection<? extends Annotation> annotations) {
      if (annotations.isEmpty()) {
         return type;
      }
      return new AnnotatedWildcardTypeImpl(
            Arrays.asList(type.getAnnotatedUpperBounds()),
            Arrays.asList(type.getAnnotatedLowerBounds()),
            combined(type.getDeclaredAnnotations(), annotations));
   }
   
   public static AnnotatedTypeVariable addAnnotations(AnnotatedTypeVariable type,
         Annotation... annotations) {
      if (annotations.length == 0) {
         return type;
      }
      return addAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedTypeVariable addAnnotations(AnnotatedTypeVariable type,
         Collection<? extends Annotation> annotations) {
      if (annotations.isEmpty()) {
         return type;
      }
      return new AnnotatedTypeVariableImpl((TypeVariable<?>) type.getType(),
            combined(type.getDeclaredAnnotations(), annotations));
   }
   
   private static Set<Annotation> combined(Annotation[] existing,
         Collection<? extends Annotation> newAnnotations) {
      Set<Annotation> all = new LinkedHashSet<>((existing.length + newAnnotations.size()) * 4 / 3);
      addAll(existing, all);
      all.addAll(newAnnotations);
      return all;
   }
   
   public static AnnotatedType replaceAnnotations(AnnotatedType type, Annotation... annotations) {
      return replaceAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedType replaceAnnotations(AnnotatedType type,
         Collection<? extends Annotation> annotations) {
      if (type instanceof AnnotatedParameterizedType) {
         return replaceAnnotations((AnnotatedParameterizedType) type, annotations);
      } else if (type instanceof AnnotatedArrayType) {
         return replaceAnnotations((AnnotatedArrayType) type, annotations);
      } else if (type instanceof AnnotatedWildcardType) {
         return replaceAnnotations((AnnotatedWildcardType) type, annotations);
      } else if (type instanceof AnnotatedTypeVariable) {
         return replaceAnnotations((AnnotatedTypeVariable) type, annotations);
      } else {
         return new AnnotatedDeclaredTypeImpl(getOwnerType(type), (Class<?>) type.getType(),
               annotations);
      }
   }
   
   public static AnnotatedParameterizedType replaceAnnotations(AnnotatedParameterizedType type,
         Annotation... annotations) {
      return replaceAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedParameterizedType replaceAnnotations(AnnotatedParameterizedType type,
         Collection<? extends Annotation> annotations) {
      return new AnnotatedParameterizedTypeImpl(getOwnerType(type),
            Types.getErasure(type.getType()), Arrays.asList(type.getAnnotatedActualTypeArguments()),
            annotations);
   }

   public static AnnotatedArrayType replaceAnnotations(AnnotatedArrayType type,
         Annotation... annotations) {
      return replaceAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedArrayType replaceAnnotations(AnnotatedArrayType type,
         Collection<? extends Annotation> annotations) {
      return new AnnotatedArrayTypeImpl(type.getAnnotatedGenericComponentType(), annotations);
   }
   
   public static AnnotatedWildcardType replaceAnnotations(AnnotatedWildcardType type,
         Annotation... annotations) {
      return replaceAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedWildcardType replaceAnnotations(AnnotatedWildcardType type,
         Collection<? extends Annotation> annotations) {
      return new AnnotatedWildcardTypeImpl(
            Arrays.asList(type.getAnnotatedUpperBounds()),
            Arrays.asList(type.getAnnotatedLowerBounds()),
            annotations);
   }
   
   public static AnnotatedTypeVariable replaceAnnotations(AnnotatedTypeVariable type,
         Annotation... annotations) {
      return replaceAnnotations(type, Arrays.asList(annotations));
   }

   public static AnnotatedTypeVariable replaceAnnotations(AnnotatedTypeVariable type,
         Collection<? extends Annotation> annotations) {
      return new AnnotatedTypeVariableImpl((TypeVariable<?>) type.getType(), annotations);
   }
   
   public static void validateType(AnnotatedType type, TypeAnnotationChecker checker) {
      // TODO: move into private method that tracks "path" to type, for better error messages
      AnnotatedType owner = getOwnerType(type);
      if (owner != null) {
         validateType(owner, checker);
      }
      if (type instanceof AnnotatedParameterizedType) {
         AnnotatedParameterizedType pType = (AnnotatedParameterizedType) type;
         Class<?> rawType = Types.getErasure(pType.getType());
         TypeVariable<?>[] bounds = rawType.getTypeParameters();
         int i = 0;
         for (AnnotatedType t : pType.getAnnotatedActualTypeArguments()) {
            // recursively validate each argument type
            validateType(t, checker);
            // we combine the annotations from the actual argument and from its bounds to check
            // if the resulting set is valid
            Set<Annotation> annotations = new HashSet<>();
            addAllAnnotationsFromHierarchy(t, annotations);
            for (AnnotatedType b : bounds[i].getAnnotatedBounds()) {
               addAllAnnotationsFromHierarchy(b, annotations);
            }
            checker.validateAnnotations(annotations);
            i++;
         }
         // TODO: check against type hierarchy
      } else if (type instanceof AnnotatedArrayType) {
         validateType(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(), checker);
         checker.validateAnnotations(type.getDeclaredAnnotations());
      } else if (type instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType wtType = (AnnotatedWildcardType) type;
         for (AnnotatedType t : wtType.getAnnotatedLowerBounds()) {
            // TODO: check annotations on bound against those on wildcard itself
            validateType(t, checker);
         }
         for (AnnotatedType t : wtType.getAnnotatedUpperBounds()) {
            // TODO: check annotations on wildcard against those on bound
            validateType(t, checker);
         }
      } else {
         // TODO: check against type hierarchy
         checker.validateAnnotations(type.getDeclaredAnnotations());
      }
   }

   public static AnnotatedParameterizedType newAnnotatedParameterizedType(ParameterizedType type,
         Annotation... annotations) {
      return newAnnotatedParameterizedType(type, Arrays.asList(annotations));
   }
   
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(ParameterizedType type,
         Iterable<? extends Annotation> annotations) {
      Type owner = type.getOwnerType();
      return owner == null
            ? newAnnotatedParameterizedType(Types.getErasure(type),
                  asAnnotatedTypes(type.getActualTypeArguments()),
                  annotations)
            : newAnnotatedParameterizedType(
                  newAnnotatedType(owner),
                  Types.getErasure(type),
                  asAnnotatedTypes(type.getActualTypeArguments()),
                  annotations);
   } 
   
   /**
    * Creates a new {@link ParameterizedType} for a generic top-level or static type. In this case,
    * there is no enclosing instance of a generic type, so the resulting parameterized type has no
    * {@linkplain ParameterizedType#getOwnerType() owner type}.
    *
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type (should not be empty)
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if either of the given arguments is {@code null}
    * @throws IllegalArgumentException if the raw type is a non-generic type or if no type arguments
    *       are given, if the wrong number of type arguments are given, or if any of the type
    *       arguments is outside the bounds for the corresponding type variable
    */
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(Class<?> rawType,
         List<? extends AnnotatedType> typeArguments, Annotation... annotations) {
      return newParameterizedTypeInternal(null, rawType, typeArguments, Arrays.asList(annotations));
   }
   
   /**
    * Creates a new {@link ParameterizedType} for a generic top-level or static type. In this case,
    * there is no enclosing instance of a generic type, so the resulting parameterized type has no
    * {@linkplain ParameterizedType#getOwnerType() owner type}.
    *
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type (should not be empty)
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if either of the given arguments is {@code null}
    * @throws IllegalArgumentException if the raw type is a non-generic type or if no type arguments
    *       are given, if the wrong number of type arguments are given, or if any of the type
    *       arguments is outside the bounds for the corresponding type variable
    */
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(Class<?> rawType, 
         List<? extends AnnotatedType> typeArguments, Iterable<? extends Annotation> annotations) {
      return newParameterizedTypeInternal(null, rawType, typeArguments, annotations);
   }
   
   /**
    * Creates a new {@link ParameterizedType} for a generic enclosed type. This is necessary for
    * representing non-static enclosed types whose enclosing type is generic, e.g. {@code
    * GenericType<T, U>.EnclosedType}. The actual type arguments may be empty if the given type is
    * not generic, and only an enclosing type is generic.
    *
    * @param ownerType the generic enclosing type
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type or an empty list if
    *       the raw type is not a generic type
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if any of the given arguments is {@code null}
    * @throws IllegalArgumentException if the given raw type's owner does not match the given owner
    *       type, if the wrong number of type arguments are given, or if any of the type arguments
    *       is outside the bounds for the corresponding type variable
    */
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(AnnotatedType ownerType,
         Class<?> rawType, List<? extends AnnotatedType> typeArguments,
         Annotation... annotations) {
      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
            typeArguments, Arrays.asList(annotations));
   }

   /**
    * Creates a new {@link ParameterizedType} for a generic enclosed type. This is necessary for
    * representing non-static enclosed types whose enclosing type is generic, e.g. {@code
    * GenericType<T, U>.EnclosedType}. The actual type arguments may be empty if the given raw type
    * is not generic and only its owner type is generic.
    *
    * @param ownerType the generic enclosing type
    * @param rawType the class of the parameterized type
    * @param typeArguments the actual type arguments to the parameterized type or an empty list if
    *       the raw type is not a generic type
    * @return a new parameterized type with the given properties
    * @throws NullPointerException if any of the given arguments is {@code null} or if any of the
    *       elements of {@code typeArguments} is {@code null}
    * @throws IllegalArgumentException if the given raw type's owner does not match the given owner
    *       type, if the wrong number of type arguments are given, if any type argument is a
    *       primitive type or {@code void}, or if any of the type arguments is outside the bounds
    *       for the corresponding type variable
    */
   public static AnnotatedParameterizedType newAnnotatedParameterizedType(AnnotatedType ownerType,
         Class<?> rawType, List<? extends AnnotatedType> typeArguments,
         Iterable<? extends Annotation> annotations) {
      return newParameterizedTypeInternal(requireNonNull(ownerType), rawType,
            typeArguments, annotations);
   }
   
   private static AnnotatedParameterizedType newParameterizedTypeInternal(
         AnnotatedType ownerType, Class<?> rawType, List<? extends AnnotatedType> typeArguments,
         Iterable<? extends Annotation> annotations) {
      requireNonNull(rawType);
      List<AnnotatedType> copyOfArguments = new ArrayList<>(typeArguments); // defensive copy
      for (AnnotatedType type : copyOfArguments) {
         requireNonNull(type);
      }
      AnnotatedType owner;
      if (ownerType != null) {
         if (rawType.getDeclaringClass() != Types.getErasure(ownerType.getType())) {
            throw new IllegalArgumentException("Owner type " + ownerType.getType().getTypeName()
                  + " does not match actual owner of given raw type " + rawType.getTypeName());
         } else if (Modifier.isStatic(rawType.getModifiers())) {
            throw new IllegalArgumentException("Given raw type " + rawType.getTypeName()
                  + " is static so cannot have a parameterized owner type");
         }
         owner = ownerType;
      } else if (copyOfArguments.isEmpty()) {
         throw new IllegalArgumentException("Parameterized type must either have type arguments or "
               + "have a parameterized owner type");
      } else {
         for (Class<?> clazz = rawType, enclosing = null; clazz != null; clazz = enclosing) {
            enclosing = clazz.getDeclaringClass();
            if (Modifier.isStatic(clazz.getModifiers())) {
               break;
            }
            if (enclosing != null && enclosing.getTypeParameters().length > 0) {
               throw new IllegalArgumentException("Non-static parameterized type "
                  + rawType.getTypeName() + " must have parameterized owner");
            }
         }
         owner = newAnnotatedType(rawType.getDeclaringClass());
      }
      TypeVariable<?> typeVariables[] = rawType.getTypeParameters();
      int len = typeVariables.length;
      if (len != copyOfArguments.size()) {
         throw new IllegalArgumentException("Given type " + rawType.getTypeName() + " has " + len
               + " type variable(s), but " + copyOfArguments.size()
               + " argument(s) were specified");
      }
      Map<TypeVariable<?>, AnnotatedType> resolvedVariables = new HashMap<>();
      if (ownerType != null) {
         // resolve owners' type variables
         collectTypeParameters(ownerType, resolvedVariables);
      }
      for (int i = 0; i < len; i++) {
         // add current type variables, in case there are recursive bounds
         resolvedVariables.put(Types.wrap(typeVariables[i]), copyOfArguments.get(i));
      }
      for (int i = 0; i < len; i++) {
         // validate that given arguments are compatible with bounds
         TypeVariable<?> variable = typeVariables[i];
         AnnotatedType argument = copyOfArguments.get(i);
         checkTypeValue(variable, argument, resolvedVariables);
      }
      return new AnnotatedParameterizedTypeImpl(owner, rawType, copyOfArguments, annotations);
   }
   
   private static void collectTypeParameters(AnnotatedParameterizedType type,
         Map<TypeVariable<?>, AnnotatedType> typeParameters) {
      AnnotatedType owner = getOwnerType(type);
      if (owner instanceof AnnotatedParameterizedType) {
         collectTypeParameters((AnnotatedParameterizedType) owner, typeParameters);
      }
      AnnotatedType args[] = type.getAnnotatedActualTypeArguments();
      TypeVariable<?> params[] = Types.getTypeParameters(type.getType());
      assert args.length == params.length;
      for (int i = 0, len = args.length; i < len; i++) {
         typeParameters.put(Types.wrap(params[i]), args[i]);
      }
   }

   private static void collectTypeParameters(AnnotatedType type,
         Map<TypeVariable<?>, AnnotatedType> typeParameters) {
      if (type instanceof ParameterizedType) {
         collectTypeParameters((AnnotatedParameterizedType) type, typeParameters);
      } else if (type instanceof GenericArrayType) {
         collectTypeParameters(((AnnotatedArrayType) type).getAnnotatedGenericComponentType(),
               typeParameters);
      } else if (type instanceof AnnotatedWildcardType) {
         AnnotatedWildcardType wt = (AnnotatedWildcardType) type;
         for (AnnotatedType b : wt.getAnnotatedUpperBounds()) {
            collectTypeParameters(b, typeParameters);
         }
         for (AnnotatedType b : wt.getAnnotatedLowerBounds()) {
            collectTypeParameters(b, typeParameters);
         }
      }
   }

   /**
    * Creates a new {@link WildcardType} with an upper bound, i.e.&nbsp;{@code ? extends T}.
    *
    * @param bound the upper bound for the wildcard type
    * @param annotations optional annotations that are present on the wildcard type
    * @return a new wildcard type with the given bound
    * @throws NullPointerException if the given bound is {@code null}
    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
    *       type
    */
   public static AnnotatedWildcardType newExtendsAnnotatedWildcardType(AnnotatedType bound,
         Annotation... annotations) {
      return newAnnotatedWildcardTypeInternal(bound, Arrays.asList(annotations), true);
   }
   
   public static AnnotatedWildcardType newExtendsAnnotatedWildcardType(AnnotatedType bound,
         Iterable<? extends Annotation> annotations) {
      return newAnnotatedWildcardTypeInternal(bound, annotations, true);
   }

   /**
    * Creates a new {@link WildcardType} with a lower bound, i.e.&nbsp;{@code ? super T}.
    *
    * @param bound the lower bound for the wildcard type
    * @return a new wildcard type with the given bound
    * @throws NullPointerException if the given bound is {@code null}
    * @throws IllegalArgumentException if the given bound is a primitive type or another wildcard
    *       type
    */
   public static AnnotatedWildcardType newSuperAnnotatedWildcardType(AnnotatedType bound,
         Annotation... annotations) {
      return newAnnotatedWildcardTypeInternal(bound, Arrays.asList(annotations), false);
   }

   public static AnnotatedWildcardType newSuperAnnotatedWildcardType(AnnotatedType bound,
         Iterable<? extends Annotation> annotations) {
      return newAnnotatedWildcardTypeInternal(bound, annotations, false);
   }
   
   public static AnnotatedWildcardType newAnnotatedWildcardType(WildcardType type,
         Annotation... annotations) {
      return newAnnotatedWildcardType(type, Arrays.asList(annotations));
   }
   
   public static AnnotatedWildcardType newAnnotatedWildcardType(WildcardType type,
         Iterable<? extends Annotation> annotations) {
      boolean isUpperBound;
      Type[] bounds = type.getLowerBounds();
      if (bounds.length == 0) {
         bounds = type.getUpperBounds();
         isUpperBound = true;
      } else {
         isUpperBound = false;
      }
      assert bounds.length == 1;
      return newAnnotatedWildcardTypeInternal(newAnnotatedType(bounds[0]), annotations,
            isUpperBound);
   }

   private static AnnotatedWildcardType newAnnotatedWildcardTypeInternal(AnnotatedType bound,
         Iterable<? extends Annotation> annotations, boolean isUpperBound) {
      requireNonNull(bound);
      Type boundType = bound.getType();
      if (boundType instanceof Class) {
         Class<?> boundClass = (Class<?>) boundType;
         if (boundClass.isPrimitive()) {
            throw new IllegalArgumentException("Bound for a WildcardType cannot be primitive");
         }
      } else if (boundType instanceof WildcardType) {
         throw new IllegalArgumentException("Bound for a WildcardType cannot be a WildcardType");
      }
      return new AnnotatedWildcardTypeImpl(bound, isUpperBound, createAnnotations(annotations));
   }
   
   public static AnnotatedTypeVariable newAnnotatedTypeVariable(TypeVariable<?> typeVariable,
         Annotation... annotations) {
      return newAnnotatedTypeVariable(typeVariable, Arrays.asList(annotations));
   }
   
   public static AnnotatedTypeVariable newAnnotatedTypeVariable(TypeVariable<?> typeVariable,
         Iterable<? extends Annotation> annotations) {
      return new AnnotatedTypeVariableImpl(typeVariable, annotations);
   }
   
   static Collection<Type> toTypes(Collection<? extends AnnotatedType> coll) {
      return new TransformingCollection<>(coll, AnnotatedType::getType);
   }
   
   private static List<Annotation> createAnnotations(Iterable<? extends Annotation> annotations) {
      Stream<? extends Annotation> stream = annotations instanceof Collection
            ? ((Collection<? extends Annotation>) annotations).stream()
            : StreamSupport.stream(annotations.spliterator(), false);
      Map<Class<? extends Annotation>, List<Annotation>> grouped =
            stream.collect(Collectors.groupingBy(Annotation::annotationType));
      List<Annotation> result = new ArrayList<>(grouped.size());
      for (Entry<Class<? extends Annotation>, List<Annotation>> entry : grouped.entrySet()) {
         Class<? extends Annotation> type = entry.getKey();
         List<Annotation> a = entry.getValue();
         // make sure this annotation is allowed on a type use
         Target target = type.getDeclaredAnnotation(Target.class);
         boolean valid = false;
         if (target != null) {
            for (ElementType t : target.value()) {
               if (t == ElementType.TYPE_USE) {
                  valid = true;
                  break;
               }
            }
         }
         if (!valid) {
            throw new IllegalArgumentException("Annotation " + type
                  + " cannot be used for a type use");
         }
         if (a.size() == 1) {
            result.add(a.get(0));
         } else {
            // If there are multiple annotations of this type, it must be repeatable
            Repeatable r = type.getDeclaredAnnotation(Repeatable.class);
            if (r == null) {
               throw new IllegalArgumentException("Annotation " + type + " is not repeatable but"
                     + a.size() + " occurrences specified");
            }
            result.add(Annotations.create(r.value(),
                  MapBuilder.<String, Object>forHashMap().put("value", a).build()));
         }
      }
      return result;
   }

   /**
    * Wraps the given type as a {@link ProperAnnotatedType}, unless it is already a
    * {@link ProperAnnotatedType}.
    *
    * @param type a type
    * @return a proper type that wraps the given type
    */
   private static AnnotatedType wrap(AnnotatedType type) {
      if (type instanceof ProperAnnotatedType) {
         return type; 
      } else if (type instanceof AnnotatedParameterizedType) {
         return new AnnotatedParameterizedTypeWrapper((AnnotatedParameterizedType) type);
      } else if (type instanceof AnnotatedArrayType) {
         return new AnnotatedArrayTypeWrapper((AnnotatedArrayType) type);
      } else if (type instanceof AnnotatedWildcardType) {
         return new AnnotatedWildcardTypeWrapper((AnnotatedWildcardType) type);
      } else if (type instanceof AnnotatedTypeVariable) {
         return new AnnotatedTypeVariableWrapper((AnnotatedTypeVariable) type);
      } else {
         return new AnnotatedDeclaredTypeWrapper(type);
      }
   }

   /**
    * An {@link AnnotatedType} that has well-defined behavior for {@link Object#equals equals} and
    * {@link Object#hashCode hashCode}.
    * 
    * <p>Since neither {@link AnnotatedType} nor any of its subinterfaces specify semantics for
    * these methods, we cannot rely on their implementations of those methods -- for example, when
    * using them in a set or as map keys.
    * 
    * <p>These operations are defined to use the static methods of the same name on
    * {@link AnnotatedTypes}:
    * <pre>
    * {@literal @}Override
    * public boolean equals(Object o) {
    *   return o instanceof Type && AnnotatedTypes.equals(this, (AnnotatedType) o);
    * }
    * {@literal @}Override
    * public int hashCode() {
    *   return AnnotatedTypes.hashCode(this);
    * }
    * {@literal @}Override
    * public String toString() {
    *   return AnnotatedTypes.toString(this);
    * }
    * </pre>
    */
   private interface ProperAnnotatedType extends AnnotatedType {
   }
   
   // TODO: javadoc
   private abstract static class AnnotatedTypeWrapper<A extends AnnotatedType>
   implements ProperAnnotatedType {
      final A base;
      
      AnnotatedTypeWrapper(A base) {
         this.base = base;
      }
      
      @Override
      public Type getType() {
         return base.getType();
      }

      @Override
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
         return base.getAnnotation(annotationClass);
      }

      @Override
      public Annotation[] getAnnotations() {
         return base.getAnnotations();
      }

      @Override
      public Annotation[] getDeclaredAnnotations() {
         return base.getDeclaredAnnotations();
      }
      
      @Override
      public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
         return base.isAnnotationPresent(annotationClass);
      }

      @Override
      public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
         return base.getAnnotationsByType(annotationClass);
      }

      @Override
      public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
         return base.getDeclaredAnnotation(annotationClass);
      }

      @Override
      public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
         return base.getDeclaredAnnotationsByType(annotationClass);
      }

      @Override
      public int hashCode() {
         return AnnotatedTypes.hashCode(base);
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof AnnotatedType && AnnotatedTypes.equals(base, (AnnotatedType) o);
      }

      @Override
      public String toString() {
         return AnnotatedTypes.toString(base);
      }
   }
   
   private static class AnnotatedDeclaredTypeWrapper extends AnnotatedTypeWrapper<AnnotatedType> {
      AnnotatedDeclaredTypeWrapper(AnnotatedType base) {
         super(base);
      }
   }

   private static class AnnotatedParameterizedTypeWrapper
   extends AnnotatedTypeWrapper<AnnotatedParameterizedType> implements AnnotatedParameterizedType {
      AnnotatedParameterizedTypeWrapper(AnnotatedParameterizedType base) {
         super(base);
      }

      @Override
      public AnnotatedType[] getAnnotatedActualTypeArguments() {
         return base.getAnnotatedActualTypeArguments();
      }
   }

   private static class AnnotatedArrayTypeWrapper extends AnnotatedTypeWrapper<AnnotatedArrayType>
   implements AnnotatedArrayType {
      AnnotatedArrayTypeWrapper(AnnotatedArrayType base) {
         super(base);
      }

      @Override
      public AnnotatedType getAnnotatedGenericComponentType() {
         return base.getAnnotatedGenericComponentType();
      }
   }
   
   private static class AnnotatedWildcardTypeWrapper
   extends AnnotatedTypeWrapper<AnnotatedWildcardType> implements AnnotatedWildcardType {
      AnnotatedWildcardTypeWrapper(AnnotatedWildcardType base) {
         super(base);
      }

      @Override
      public AnnotatedType[] getAnnotatedLowerBounds() {
         return base.getAnnotatedLowerBounds();
      }

      @Override
      public AnnotatedType[] getAnnotatedUpperBounds() {
         return base.getAnnotatedUpperBounds();
      }
   }
   
   private static class AnnotatedTypeVariableWrapper
   extends AnnotatedTypeWrapper<AnnotatedTypeVariable> implements AnnotatedTypeVariable {
      AnnotatedTypeVariableWrapper(AnnotatedTypeVariable base) {
         super(base);
      }

      @Override
      public AnnotatedType[] getAnnotatedBounds() {
         return base.getAnnotatedBounds();
      }
   }
   
   /**
    * Implements {@link AnnotatedType}. This is the base class that the various concrete type
    * implementations extend.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static abstract class AnnotatedTypeImpl implements ProperAnnotatedType, Serializable {
      private static final long serialVersionUID = -1507646636725356215L;
      
      private final Type type;
      private final Annotation annotations[];
      
      AnnotatedTypeImpl(Type type, Iterable<? extends Annotation> annotations) {
         this.type = type;
         this.annotations = MoreIterables.toArray(annotations, EMPTY_ANNOTATIONS);
      }

      @Override
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
         return getDeclaredAnnotation(annotationClass);
      }

      @Override
      public Annotation[] getAnnotations() {
         return getDeclaredAnnotations();
      }

      @Override
      public Annotation[] getDeclaredAnnotations() {
         return annotations.clone();
      }

      @Override
      public Type getType() {
         return type;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof AnnotatedType) {
            return AnnotatedTypes.equals(this, (AnnotatedType) o);
         }
         return false;
      }

      @Override
      public int hashCode() {
         return AnnotatedTypes.hashCode(this);
      }

      @Override
      public String toString() {
         return AnnotatedTypes.toString(this);
      }
   }
   
   /**
    * Implements {@link AnnotatedType} for non-parameterized declared types. This adds an accessor
    * for the annotated owner type (which will be in the JRE's API in Java 9).
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AnnotatedDeclaredTypeImpl extends AnnotatedTypeImpl {
      private static final long serialVersionUID = 6961470718750141275L;

      private final AnnotatedType ownerType;

      AnnotatedDeclaredTypeImpl(AnnotatedType ownerType, Class<?> type,
            Iterable<? extends Annotation> annotations) {
         super(computeType(ownerType == null ? null : ownerType.getType(), type), annotations);
         this.ownerType = ownerType;
      }

      public AnnotatedType getAnnotatedOwnerType() {
         return ownerType;
      }
      
      private static Type computeType(Type owner, Class<?> type) {
         if (owner instanceof ParameterizedType) {
            return Types.newParameterizedType((ParameterizedType) owner, type);
         }
         return type;
      }
   }
   
   /**
    * Implements {@link AnnotatedParameterizedType}. This includes an accessor for the annotated
    * owner type (which will be in the JRE's API in Java 9).
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AnnotatedParameterizedTypeImpl extends AnnotatedTypeImpl
         implements AnnotatedParameterizedType {
      private static final long serialVersionUID = -4933098144775956311L;

      private final AnnotatedType[] typeArguments;
      private final AnnotatedType ownerType;

      AnnotatedParameterizedTypeImpl(AnnotatedType ownerType, Class<?> rawType,
            List<AnnotatedType> typeArguments, Iterable<? extends Annotation> annotations) {
         super(new Types.ParameterizedTypeImpl(ownerType == null ? null : ownerType.getType(),
               rawType, toTypes(typeArguments)), annotations);
         this.typeArguments = typeArguments.toArray(new AnnotatedType[typeArguments.size()]);
         this.ownerType = ownerType;
      }

      @Override
      public AnnotatedType[] getAnnotatedActualTypeArguments() {
         return typeArguments.clone();
      }
      
      public AnnotatedType getAnnotatedOwnerType() {
         return ownerType;
      }
   }
   
   /**
    * Implements {@link AnnotatedArrayType}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AnnotatedArrayTypeImpl extends AnnotatedTypeImpl
         implements AnnotatedArrayType {
      private static final long serialVersionUID = -8335550068623986776L;
      
      private final AnnotatedType componentType;
      
      AnnotatedArrayTypeImpl(AnnotatedType componentType,
            Iterable<? extends Annotation> annotations) {
         super(Types.getArrayType(componentType.getType()), annotations);
         this.componentType = componentType;
      }
      
      @Override
      public AnnotatedType getAnnotatedGenericComponentType() {
         return componentType;
      }
   }
   
   /**
    * Implements {@link AnnotatedWildcardType}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AnnotatedWildcardTypeImpl extends AnnotatedTypeImpl
         implements AnnotatedWildcardType {
      private static final long serialVersionUID = -5371665313248454547L;
      
      private final AnnotatedType upperBounds[];
      private final AnnotatedType lowerBounds[];
      
      AnnotatedWildcardTypeImpl(AnnotatedType bound, boolean isUpperBound,
            Iterable<? extends Annotation> annotations) {
         super(new Types.WildcardTypeImpl(bound.getType(), isUpperBound), annotations);
         upperBounds = new AnnotatedType[] { isUpperBound ? bound : OBJECT };
         lowerBounds = isUpperBound ? EMPTY_TYPES : new AnnotatedType[] { bound };
      }
      
      AnnotatedWildcardTypeImpl(List<AnnotatedType> upperBounds, List<AnnotatedType> lowerBounds,
            Iterable<? extends Annotation> annotations) {
         super(new Types.WildcardTypeImpl(toTypes(upperBounds), toTypes(lowerBounds)), annotations);
         this.upperBounds = upperBounds.toArray(new AnnotatedType[upperBounds.size()]);
         this.lowerBounds = lowerBounds.isEmpty() ? EMPTY_TYPES
               : lowerBounds.toArray(new AnnotatedType[lowerBounds.size()]);
      }

      @Override
      public AnnotatedType[] getAnnotatedLowerBounds() {
         return lowerBounds.clone();
      }

      @Override
      public AnnotatedType[] getAnnotatedUpperBounds() {
         return upperBounds.clone();
      }
   }
   
   /**
    * Implements {@link AnnotatedTypeVariable}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class AnnotatedTypeVariableImpl extends AnnotatedTypeImpl
         implements AnnotatedTypeVariable {
      private static final long serialVersionUID = 2528423947615752140L;
      
      AnnotatedTypeVariableImpl(TypeVariable<?> typeVar,
            Iterable<? extends Annotation> annotations) {
         super(typeVar, annotations);
      }

      @Override
      public AnnotatedType[] getAnnotatedBounds() {
         return ((TypeVariable<?>) getType()).getAnnotatedBounds();
      }
   }
}
