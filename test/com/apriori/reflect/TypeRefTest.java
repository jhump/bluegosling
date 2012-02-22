package com.apriori.reflect;

import java.io.Serializable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;

import junit.framework.TestCase;

/**
 * Tests {@link TypeRef}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TypeRefTest extends TestCase {

   // Numerous sub-classes of TypeReference to test various scenarios under
   // which the TypeReference determines the resolved runtime type for its
   // type parameter.

   private static class TypeReferenceSubclass extends TypeRef<String> {
      public TypeReferenceSubclass() {}
   }

   private static class TypeReferenceArraySubclass<T> extends TypeRef<T[]> {
      public TypeReferenceArraySubclass() {}
   }

   private static class TypeReferenceGenericSubclass<T> extends TypeRef<T> {
      public TypeReferenceGenericSubclass() {}
   }

   private static class TypeReferenceSubclass2 extends
         TypeReferenceGenericSubclass<List<? extends Serializable>> {
      public TypeReferenceSubclass2() {}
   }

   private static class TypeReferenceComplex1<K, E, M extends Map<K, List<E>>> extends TypeRef<M> {
      public TypeReferenceComplex1() {}
   }

   private static class TypeReferenceComplex2<K, V> extends TypeRef<Map<V, List<K>>> {
      public TypeReferenceComplex2() {}
   }

   /**
    * Tests {@link TypeRef}s for simple (non-generic) types.
    */
   public void testSimpleType() {
      TypeRef<Number> tr = new TypeRef<Number>() {};
      assertSame(Number.class, tr.asClass());
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests {@link TypeRef}s for simple (non-generic) array types.
    */
   public void testSimpleArrayType() {
      TypeRef<String[]> tr = new TypeRef<String[]>() {};
      assertSame(String[].class, tr.asClass());
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests {@link TypeRef}s for generic types.
    */
   public void testGenericType() {
      TypeRef<Map<String, Object>> tr = new TypeRef<Map<String, Object>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // and verify resolution of generic type info
      TypeRef<?> typeVarRef = tr.resolveTypeVariable("K");
      assertSame(String.class, typeVarRef.asClass());
      typeVarRef = tr.resolveTypeVariable("V");
      assertSame(Object.class, typeVarRef.asClass());
   }

   /**
    * Tests {@link TypeRef}s for generic array types.
    */
   public void testGenericArrayType() {
      TypeRef<List<Integer>[]> tr = new TypeRef<List<Integer>[]>() {};
      assertSame(List[].class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      // and verify resolution of generic type info
      TypeRef<?> typeVarRef = tr.resolveTypeVariable("E");
      assertSame(Integer.class, typeVarRef.asClass());
   }

   /**
    * Tests {@link TypeRef}s for multi-dimensional (i.e. nested) array types.
    */
   public void test2DArrayType() {
      TypeRef<List<Date>[][][]> tr = new TypeRef<List<Date>[][][]>() {};
      assertSame(List[][][].class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      // and verify resolution of generic type info
      TypeRef<?> typeVarRef = tr.resolveTypeVariable("E");
      assertSame(Date.class, typeVarRef.asClass());
   }

   /**
    * Tests trying to explore type variable resolutions for {@link TypeRef}s with invalid type
    * variable names.
    */
   public void testGenericTypeAndInvalidTypeVariable() {
      TypeRef<List<Number>> tr = new TypeRef<List<Number>>() {};
      assertSame(List.class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      try {
         // case sensitive!
         tr.resolveTypeVariable("e");
         fail();
      }
      catch (IllegalArgumentException expected) {}
      try {
         tr.resolveTypeVariable("X");
         fail();
      }
      catch (IllegalArgumentException expected) {}
      try {
         tr.resolveTypeVariable(null);
         fail();
      }
      catch (NullPointerException expected) {}
   }

   /**
    * Tests {@link TypeRef}s for generic types using wildcards during construction.
    */
   public void testGenericTypeAndAcceptableWildcard() {
      TypeRef<Map<?, ?>> tr = new TypeRef<Map<?, ?>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // acknowledge that we cannot resolve the type variables here
      assertFalse(tr.canResolveTypeVariable("K"));
      assertFalse(tr.canResolveTypeVariable("V"));
   }

   /**
    * Tests {@link TypeRef}s for generic types using type parameters during construction.
    * 
    * @param <X> dummy type variable
    */
   public <X> void testGenericTypeAndAcceptableTypeParam() {
      TypeRef<Map<X, Object>> tr = new TypeRef<Map<X, Object>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // acknowledge that we cannot resolve both type variables here
      assertFalse(tr.canResolveTypeVariable("K"));
      // but we know this one
      TypeRef<?> typeVarRef = tr.resolveTypeVariable("V");
      assertSame(Object.class, typeVarRef.asClass());
   }

   /**
    * Tests {@link TypeRef}s for generic types using invalid type parameters during construction
    * (which prevent initial resolution of the type).
    * 
    * @param <X> dummy type variable
    * @param <Y> dummy type variable
    * @param <Z> dummy type variable
    */
   public <X, Y, Z extends Map<X, Y>> void testGenericTypeAndUnacceptableTypeParam() {
      try {
         @SuppressWarnings("unused")
         TypeRef<Z> tr = new TypeRef<Z>() {};
         fail();
      }
      catch (IllegalArgumentException expected) {}
   }

   /**
    * Tests {@link TypeRef}s for a very complex generic type (whose type parameters are themselves
    * generic, etc).
    */
   public void testComplexGenericType() {
      TypeRef<?> tr = new TypeRef<Map<Comparable<? super Number>, List<Set<String>[]>>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());

      TypeRef<?> kType = tr.resolveTypeVariable("K");
      assertSame(Comparable.class, kType.asClass());
      assertEquals(Arrays.asList("T"), kType.getTypeVariableNames());
      // this one's not available since it was defined via wildcard
      assertFalse(kType.canResolveTypeVariable("T"));

      TypeRef<?> vType = tr.resolveTypeVariable("V");
      assertSame(List.class, vType.asClass());
      assertEquals(Arrays.asList("E"), vType.getTypeVariableNames());
      vType = vType.resolveTypeVariable("E");
      assertSame(Set[].class, vType.asClass());
      assertEquals(Arrays.asList("E"), vType.getTypeVariableNames());
      vType = vType.resolveTypeVariable("E");
      assertSame(String.class, vType.asClass());
      assertTrue(vType.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is invalid (initial type resolution
    * fails).
    */
   public void testInvalidConstructionViaSubclass1() {
      try {
         @SuppressWarnings("unused")
         TypeRef<?> tr = new TypeReferenceGenericSubclass<String>();
         fail();
      }
      catch (IllegalArgumentException expected) {}
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is invalid (initial type resolution
    * fails).
    */
   public void testInvalidConstructionViaSubclass2() {
      try {
         @SuppressWarnings("unused")
         TypeRef<?> tr = new TypeReferenceComplex1<String, Object, HashMap<String, List<Object>>>();
         fail();
      }
      catch (IllegalArgumentException expected) {}
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass1() {
      TypeReferenceSubclass tr = new TypeReferenceSubclass() {};
      assertSame(String.class, tr.asClass());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid and doesn't even require the
    * use of an anonymous subclass (as would be required if instantiating {@link TypeRef} directly).
    */
   public void testConstructionViaSubclassNoAnonymousType1() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as String.
      TypeReferenceSubclass tr = new TypeReferenceSubclass();
      assertSame(String.class, tr.asClass());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass2() {
      TypeReferenceSubclass2 tr = new TypeReferenceSubclass2() {};
      assertSame(List.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertFalse(tr.canResolveTypeVariable("E")); // wildcards can't be resolved
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid and doesn't even require the
    * use of an anonymous subclass (as would be required if instantiating {@link TypeRef} directly).
    */
   public void testConstructionViaSubclassNoAnonymousType2() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as
      // List<? extends Serializable>.
      TypeReferenceSubclass2 tr = new TypeReferenceSubclass2();
      assertSame(List.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertFalse(tr.canResolveTypeVariable("E")); // wildcards can't be resolved
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass3() {
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>() {};
      assertSame(Map.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      TypeRef<?> type = tr.resolveTypeVariable("K");
      assertSame(Object.class, type.asClass());
      assertTrue(type.getTypeVariableNames().isEmpty());
      TypeRef<?> listType = tr.resolveTypeVariable("V");
      assertSame(List.class, listType.asClass());
      assertEquals(Arrays.asList("E"), listType.getTypeVariableNames());
      type = listType.resolveTypeVariable("E");
      assertSame(String.class, type.asClass());
      assertTrue(type.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid and doesn't even require the
    * use of an anonymous subclass (as would be required if instantiating {@link TypeRef} directly).
    */
   public void testConstructionViaSubclassNoAnonymousType3() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as
      // Map<V, List<K>>.
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>();
      assertSame(Map.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      assertFalse(tr.canResolveTypeVariable("K")); // this type variable can't be resolved
      TypeRef<?> listType = tr.resolveTypeVariable("V");
      assertSame(List.class, listType.asClass());
      assertEquals(Arrays.asList("E"), listType.getTypeVariableNames());
      assertFalse(listType.canResolveTypeVariable("E"));
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass4() {
      TypeReferenceArraySubclass<String> tr = new TypeReferenceArraySubclass<String>() {};
      assertSame(String[].class, tr.asClass());
      // look at type variable info
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass5() {
      TypeReferenceGenericSubclass<Collection<?>> tr = new TypeReferenceGenericSubclass<Collection<?>>() {};
      assertSame(Collection.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertFalse(tr.canResolveTypeVariable("E")); // can't resolve wildcard
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass6() {
      // this behemoth results in a TypeReference for
      // Map<Collection<String>, List<Boolean[]>>
      TypeReferenceComplex2<Boolean[], Collection<String>> tr = new TypeReferenceComplex2<Boolean[], Collection<String>>() {};
      assertSame(Map.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      TypeRef<?> k = tr.resolveTypeVariable("K");
      assertSame(Collection.class, k.asClass());
      assertEquals(Arrays.asList("E"), k.getTypeVariableNames());
      TypeRef<?> e = k.resolveTypeVariable("E");
      assertSame(String.class, e.asClass());
      assertTrue(e.getTypeVariableNames().isEmpty());
      TypeRef<?> v = tr.resolveTypeVariable("V");
      assertSame(List.class, v.asClass());
      assertEquals(Arrays.asList("E"), v.getTypeVariableNames());
      e = v.resolveTypeVariable("E");
      assertSame(Boolean[].class, e.asClass());
      assertTrue(e.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests that {@link TypeRef#equals(Object)} properly treats an instance as equal to itself.
    */
   public void testEqualsSameInstance() {
      TypeRef<?> tr = new TypeRef<Map<List<String>, Map<Integer, SortedSet<Class<?>>>>>() {};
      assertEquals(tr.hashCode(), tr.hashCode());
      assertTrue(tr.equals(tr));
   }

   /**
    * Tests that {@link TypeRef#equals(Object)} properly recognizes that two equal objects are equal
    * and that {@link TypeRef#hashCode()} is consistent with it.
    */
   public void testEquals() {
      TypeRef<?> tr1 = new TypeRef<Map<List<String>, Map<Integer, SortedSet<Number>>>>() {};
      TypeRef<?> tr2 = new TypeRef<Map<List<String>, Map<Integer, SortedSet<Number>>>>() {};
      assertEquals(tr1.hashCode(), tr2.hashCode());
      assertTrue(tr1.equals(tr2));
      assertTrue(tr2.equals(tr1));
   }

   /**
    * Tests that {@link TypeRef#equals(Object)} properly recognizes that two unequal objects are not
    * equal.
    */
   public void testNotEquals() {
      TypeRef<?> tr1 = new TypeRef<Map<List<String>, Map<Integer, SortedSet<Number>>>>() {};
      TypeRef<?> tr2 = new TypeRef<Map<List<String>, ? extends Object>>() {};
      assertFalse(tr1.equals(tr2));
      assertFalse(tr2.equals(tr1));
   }

   /**
    * Tests that {@link TypeRef#equals(Object)} properly recognizes that two seemingly equal but
    * actually unequal (due to semantics of wildcards) objects are not equal.
    */
   public void testNotEqualsWildcards() {
      // they look the same, but wildcard prevents us from being able to
      // say they are equal
      TypeRef<?> tr1 = new TypeRef<Map<List<String>, ?>>() {};
      TypeRef<?> tr2 = new TypeRef<Map<List<String>, ?>>() {};
      assertFalse(tr1.equals(tr2));
      assertFalse(tr2.equals(tr1));
   }

   /**
    * Tests {@link TypeRef#toString()} with a non-generic type.
    */
   public void testToStringNoTypeVars() {
      TypeRef<?> tr = new TypeRef<Double>() {};
      assertEquals("java.lang.Double", tr.toString());
   }

   /**
    * Tests {@link TypeRef#toString()} with a generic type.
    */
   public void testToStringWithTypeVars() {
      TypeRef<?> tr = new TypeRef<List<Number>>() {};
      assertEquals("java.util.List<E=java.lang.Number>", tr.toString());
   }

   /**
    * Tests {@link TypeRef#toString()} with an elaborate and complex generic type.
    * 
    * @param <X> dummy type variable
    */
   public <X> void testToStringComplex() {
      TypeRef<?> tr = new TypeRef<Map<List<X>, Map<Integer, SortedSet<Class<?>>>>>() {};
      assertEquals(
            "java.util.Map<K=java.util.List<E=?>,V=java.util.Map<K=java.lang.Integer,V=java.util.SortedSet<E=java.lang.Class<T=?>>>>",
            tr.toString());
   }

   /**
    * Tests {@link TypeRef#getTypeVariable(String)}.
    */
   public void testGetTypeVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      TypeVariable<Class<?>> typeVarKey = tr.getTypeVariable("K");
      assertEquals("K", typeVarKey.getName());
      assertSame(Map.class, typeVarKey.getGenericDeclaration());
      assertSame(Map.class.getTypeParameters()[0], typeVarKey);
      TypeVariable<Class<?>> typeVarVal = tr.getTypeVariable("V");
      assertEquals("V", typeVarVal.getName());
      assertSame(Map.class, typeVarVal.getGenericDeclaration());
      assertSame(Map.class.getTypeParameters()[1], typeVarVal);
      tr = tr.resolveTypeVariable("V");
      TypeVariable<Class<?>> typeVarElement = tr.getTypeVariable("E");
      assertEquals("E", typeVarElement.getName());
      assertSame(List.class, typeVarElement.getGenericDeclaration());
      assertSame(List.class.getTypeParameters()[0], typeVarElement);
   }

   /**
    * Tests {@link TypeRef#getTypeVariable(String)} with invalid variable names specified.
    */
   public void testGetTypeVariableWithInvalidVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      try {
         tr.getTypeVariable("k"); // case-sensitive
         fail();
      }
      catch (IllegalArgumentException expected) {}
      try {
         tr.getTypeVariable("X");
         fail();
      }
      catch (IllegalArgumentException expected) {}
      try {
         tr.getTypeVariable(null);
         fail();
      }
      catch (NullPointerException expected) {}
   }

   /**
    * Tests {@link TypeRef#canResolveTypeVariable(String)}.
    */
   public void testCanResolveTypeVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      assertFalse(tr.canResolveTypeVariable("K"));
      try {
         tr.resolveTypeVariable("K");
         fail();
      }
      catch (IllegalStateException expected) {}
      assertTrue(tr.canResolveTypeVariable("V"));
      assertSame(List.class, tr.resolveTypeVariable("V").asClass());
      tr = tr.resolveTypeVariable("V");
      assertFalse(tr.canResolveTypeVariable("E"));
      try {
         tr.resolveTypeVariable("E");
         fail();
      }
      catch (IllegalStateException expected) {}
   }

   /**
    * Tests {@link TypeRef#canResolveTypeVariable(String)} with invalid variable names specified.
    */
   public void testCanResolveTypeVariableWithInvalidVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      try {
         tr.canResolveTypeVariable("k"); // case-sensitive
         fail();
      }
      catch (IllegalArgumentException expected) {}
      try {
         tr.canResolveTypeVariable("X");
         fail();
      }
      catch (IllegalArgumentException expected) {}
      try {
         tr.canResolveTypeVariable(null);
         fail();
      }
      catch (NullPointerException expected) {}
   }

   /**
    * Tests that {@link TypeRef#isResolved()} correctly identifies when the current object is not
    * resolved.
    */
   public void testIsResolvedFalse() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      assertFalse(tr.isResolved());
      assertFalse(tr.resolveTypeVariable("V").isResolved());
   }

   /**
    * Tests that {@link TypeRef#isResolved()} correctly identifies when the current object is
    * resolved.
    */
   public void testIsResolvedTrue() {
      TypeRef<?> tr = new TypeRef<Map<Comparable<Integer>, List<String>>>() {};
      assertTrue(tr.isResolved());
      TypeRef<?> varRef = tr.resolveTypeVariable("K");
      assertTrue(varRef.isResolved());
      assertTrue(varRef.resolveTypeVariable("T").isResolved());
      varRef = tr.resolveTypeVariable("V");
      assertTrue(varRef.isResolved());
      assertTrue(varRef.resolveTypeVariable("E").isResolved());
   }

   /**
    * Tests that {@link TypeRef#isResolved()} correctly identifies an object "mixed" resolution.
    * A "mixed" resolution is one where an object is <em>not</em> resolved but one of its type
    * variables is. This asserts that the type variable returns true but that the composite/parent
    * type returns false.
    */
   public void testIsResolvedMixed() {
      TypeRef<?> tr = new TypeRef<Map<? extends Comparable<Integer>, List<String>>>() {};
      assertFalse(tr.isResolved());
      TypeRef<?> varRef = tr.resolveTypeVariable("V");
      assertTrue(varRef.isResolved());
      assertTrue(varRef.resolveTypeVariable("E").isResolved());
   }

   /**
    * Tests that {@link TypeRef#isResolved()} correctly identifies a primitive or non-generic type
    * as resolved.
    */
   public void testIsResolvedSimpleTypes() {
      TypeRef<?> tr = new TypeRef<Object>() {};
      assertTrue(tr.isResolved());
      tr = TypeRef.forClass(int.class);
      assertTrue(tr.isResolved());
   }

   /**
    * Tests {@link TypeRef#superTypeRef()}, {@link TypeRef#superTypeRefFor(Class)}, and
    * {@link TypeRef#interfaceTypeRefs()}.
    */
   public void testSuperTypeAndInterfaceTypeRefs() {
      @SuppressWarnings("serial")
      abstract class ComparableList extends Number
            implements List<CharSequence>, Comparable<Collection<Number>> {}

      TypeRef<?> tr = new TypeRef<ComparableList>() {};
      assertTrue(tr.getTypeVariableNames().isEmpty());
      // superclass
      assertSame(Number.class, tr.superTypeRef().asClass());
      //   again, but this time using superTypeRefFor
      assertSame(Number.class, tr.superTypeRefFor(Number.class).asClass());
      // interface
      List<TypeRef<?>> ifaceTypes = new ArrayList<TypeRef<?>>(tr.interfaceTypeRefs());
      assertEquals(2, ifaceTypes.size());
      TypeRef<?> superRef = ifaceTypes.get(0);
      assertSame(List.class, superRef.asClass());
      assertSame(CharSequence.class, superRef.resolveTypeVariable("E").asClass());
      //   again, but this time using superTypeRefFor
      superRef = tr.superTypeRefFor(List.class);
      assertSame(List.class, superRef.asClass());
      assertSame(CharSequence.class, superRef.resolveTypeVariable("E").asClass());
      // another interface
      superRef = ifaceTypes.get(1);
      assertSame(Comparable.class, superRef.asClass());
      TypeRef<?> varRef = superRef.resolveTypeVariable("T");
      assertSame(Collection.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeVariable("E").asClass());
      //   again, but this time using superTypeRefFor
      superRef = tr.superTypeRefFor(Comparable.class);
      assertSame(Comparable.class, superRef.asClass());
      varRef = superRef.resolveTypeVariable("T");
      assertSame(Collection.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeVariable("E").asClass());
   }

   // convoluted definitions to really stress test
   // TypeRef.resolveTypeVariable(String) and TypeRef.superTypeRefFor(Class)
   interface Tuple5<M1, M2, M3, M4, M5> {}

   interface Pair<A, B> extends Tuple5<A, B, Void, Void, Void> {}

   interface CrazySet<W extends GenericDeclaration, X, Y, Z> extends
         Serializable, TypeVariable<W>, Map<Pair<X, Y>, SortedSet<Z>> {}

   @SuppressWarnings("serial")
   static abstract class MyCrazySet<S, T> implements CrazySet<Class<S>, S, String, T> {}

   /**
    * Tests {@link TypeRef#superTypeRef()} and {@link TypeRef#superTypeRefFor(Class)} with a very
    * complex example to hopefully catch any possible edge cases in the lookup/resolution of
    * super types.
    */
   public void testSuperTypeRefForComplex() {
      @SuppressWarnings("serial")
      abstract class MyCrazierSet<F> extends MyCrazySet<Number, Callable<F>> {}

      // first w/ wild-card
      TypeRef<?> tr = new TypeRef<MyCrazierSet<?>>() {};
      assertEquals(Arrays.asList("F"), tr.getTypeVariableNames());
      assertFalse(tr.canResolveTypeVariable("F"));

      TypeRef<?> superRef = tr.superTypeRef();
      assertSame(MyCrazySet.class, superRef.asClass());
      assertEquals(Arrays.asList("S", "T"), superRef.getTypeVariableNames());
      assertSame(Number.class, superRef.resolveTypeVariable("S").asClass());
      assertSame(Callable.class, superRef.resolveTypeVariable("T").asClass());

      superRef = tr.superTypeRefFor(CrazySet.class);
      assertSame(CrazySet.class, superRef.asClass());
      assertEquals(Arrays.asList("W", "X", "Y", "Z"), superRef.getTypeVariableNames());
      assertSame(Class.class, superRef.resolveTypeVariable("W").asClass());
      assertSame(Number.class, superRef.resolveTypeVariable("X").asClass());
      assertSame(String.class, superRef.resolveTypeVariable("Y").asClass());
      assertSame(Callable.class, superRef.resolveTypeVariable("Z").asClass());

      superRef = tr.superTypeRefFor(Map.class);
      assertSame(Map.class, superRef.asClass());
      assertEquals(Arrays.asList("K", "V"), superRef.getTypeVariableNames());
      assertSame(Pair.class, superRef.resolveTypeVariable("K").asClass());
      assertSame(SortedSet.class, superRef.resolveTypeVariable("V").asClass());

      superRef = tr.superTypeRefFor(TypeVariable.class);
      assertSame(TypeVariable.class, superRef.asClass());
      assertEquals(Arrays.asList("D"), superRef.getTypeVariableNames());
      assertSame(Class.class, superRef.resolveTypeVariable("D").asClass());

      // and with a concrete value (now we'll go crazy verifying the *full*
      // generic type -- type variables and all)
      tr = new TypeRef<MyCrazierSet<RuntimeException>>() {};
      assertEquals(Arrays.asList("F"), tr.getTypeVariableNames());
      assertSame(RuntimeException.class, tr.resolveTypeVariable("F").asClass());

      superRef = tr.superTypeRef();
      assertSame(MyCrazySet.class, superRef.asClass());
      assertEquals(Arrays.asList("S", "T"), superRef.getTypeVariableNames());
      assertSame(Number.class, superRef.resolveTypeVariable("S").asClass());
      TypeRef<?> varRef = superRef.resolveTypeVariable("T");
      assertSame(Callable.class, varRef.asClass());
      assertSame(RuntimeException.class, varRef.resolveTypeVariable("V").asClass());

      superRef = tr.superTypeRefFor(CrazySet.class);
      assertEquals(Arrays.asList("W", "X", "Y", "Z"), superRef.getTypeVariableNames());
      varRef = superRef.resolveTypeVariable("W");
      assertSame(Class.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeVariable("T").asClass());
      assertSame(Number.class, superRef.resolveTypeVariable("X").asClass());
      assertSame(String.class, superRef.resolveTypeVariable("Y").asClass());
      varRef = superRef.resolveTypeVariable("Z");
      assertSame(Callable.class, varRef.asClass());
      assertSame(RuntimeException.class, varRef.resolveTypeVariable("V").asClass());

      superRef = tr.superTypeRefFor(Map.class);
      assertEquals(Arrays.asList("K", "V"), superRef.getTypeVariableNames());
      varRef = superRef.resolveTypeVariable("K");
      assertSame(Pair.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeVariable("A").asClass());
      assertSame(String.class, varRef.resolveTypeVariable("B").asClass());
      // and get super-type of this Pair type
      TypeRef<?> pairSuperRef = varRef.superTypeRefFor(Tuple5.class);
      assertEquals(pairSuperRef, varRef.interfaceTypeRefs().get(0));
      assertSame(Number.class, pairSuperRef.resolveTypeVariable("M1").asClass());
      assertSame(String.class, pairSuperRef.resolveTypeVariable("M2").asClass());
      assertSame(Void.class, pairSuperRef.resolveTypeVariable("M3").asClass());
      assertSame(Void.class, pairSuperRef.resolveTypeVariable("M4").asClass());
      assertSame(Void.class, pairSuperRef.resolveTypeVariable("M5").asClass());
      varRef = superRef.resolveTypeVariable("V");
      assertSame(SortedSet.class, varRef.asClass());
      varRef = varRef.resolveTypeVariable("E");
      assertSame(Callable.class, varRef.asClass());
      assertSame(RuntimeException.class, varRef.resolveTypeVariable("V").asClass());

      superRef = tr.superTypeRefFor(TypeVariable.class);
      assertEquals(Arrays.asList("D"), superRef.getTypeVariableNames());
      varRef = superRef.resolveTypeVariable("D");
      assertSame(Class.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeVariable("T").asClass());
   }

   /**
    * Tests {@link TypeRef#superTypeRefFor(Class)} when an invalid type is queried.
    */
   public void testSuperTypeRefForInvalidType() {
      TypeRef<?> tr = new TypeRef<ArrayList<String>>() {};

      try {
         tr.superTypeRefFor(Set.class);
         fail();
      }
      catch (IllegalArgumentException expected) {}

      try {
         tr.superTypeRefFor(Number.class);
         fail();
      }
      catch (IllegalArgumentException expected) {}

      try {
         tr.superTypeRefFor(null);
         fail();
      }
      catch (NullPointerException expected) {}
   }

   /**
    * Tests that {@link TypeRef#superTypeRef()} returns {@code null} for interfaces, {@code Object},
    * primitives, and {@code void}.
    */
   public void testSuperTypeRefNull() {
      // root of hierarchy
      TypeRef<?> tr = new TypeRef<Object>() {};
      assertNull(tr.superTypeRef());
      // interface
      tr = new TypeRef<List<?>>() {};
      assertNull(tr.superTypeRef());
      // primitive
      tr = TypeRef.forClass(boolean.class);
      assertNull(tr.superTypeRef());
      // void
      tr = TypeRef.forClass(void.class);
      assertNull(tr.superTypeRef());
   }

   /**
    * Tests {@link TypeRef#isSubTypeOf(TypeRef)} under normal conditions.
    */
   public void testIsSubTypeOf() {
      TypeRef<ArrayList<Map<String, String>>> ref =
            new TypeRef<ArrayList<Map<String, String>>>() {};
            
      assertTrue(ref.isSubTypeOf(ref));

      TypeRef<Serializable> subRef1 = new TypeRef<Serializable>() {};
      TypeRef<List<Map<String, String>>> subRef2 = new TypeRef<List<Map<String, String>>>() {};
      TypeRef<ArrayList<Map<String, String>>> subRef3 =
            new TypeRef<ArrayList<Map<String, String>>>() {};
      
      assertTrue(ref.isSubTypeOf(subRef1));
      assertTrue(ref.isSubTypeOf(subRef2));
      assertTrue(ref.isSubTypeOf(subRef3));
      
      TypeRef<List<Map<?, String>>> notSubRef1 = new TypeRef<List<Map<?, String>>>() {};
      TypeRef<List<Map<String, Object>>> notSubRef2 = new TypeRef<List<Map<String, Object>>>() {};
      TypeRef<List<?>> notSubRef3 = new TypeRef<List<?>>() {};
      TypeRef<Double> notSubRef4 = new TypeRef<Double>() {};

      assertFalse(ref.isSubTypeOf(notSubRef1));
      assertFalse(ref.isSubTypeOf(notSubRef2));
      assertFalse(ref.isSubTypeOf(notSubRef3));
      assertFalse(ref.isSubTypeOf(notSubRef4));
      
      // sneak one more useful one in here:
      abstract class StringList implements List<String> {}
      assertTrue(new TypeRef<StringList>() {}.isSubTypeOf(new TypeRef<List<String>>() {}));
   }

   /**
    * Tests {@link TypeRef#isSubTypeOf(TypeRef)} with array types.
    */
   public void testIsSubTypeOfArray() {
      TypeRef<Object> ref1 = new TypeRef<Object>() {};
      TypeRef<Object[]> ref2 = new TypeRef<Object[]>() {};
      TypeRef<Collection<String>[]> ref3 = new TypeRef<Collection<String>[]>() {};
      
      assertTrue(ref2.isSubTypeOf(ref1));
      assertTrue(ref3.isSubTypeOf(ref1));
      assertTrue(ref3.isSubTypeOf(ref2));

      assertFalse(ref1.isSubTypeOf(ref2));
      assertFalse(ref1.isSubTypeOf(ref3));
      assertFalse(ref2.isSubTypeOf(ref3));
   }
      
   /**
    * Tests {@link TypeRef#isSubTypeOf(TypeRef)} for a type that is not fully resolved (which means
    * no other type can be considered a sub-type).
    */
   public void testIsSubTypeOfWildcard() {
      TypeRef<ArrayList<? extends Comparable<? extends Number>>> ref =
            new TypeRef<ArrayList<? extends Comparable<? extends Number>>>() {};

      // a type is always a sub-type of itself, and if they're the same instance, then wildcard
      // are allowed
      assertTrue(ref.isSubTypeOf(ref));

      TypeRef<List<? extends Comparable<? extends Number>>> notSubRef1 =
            new TypeRef<List<? extends Comparable<? extends Number>>>() {};
      TypeRef<ArrayList<? extends Comparable<? extends Number>>> notSubRef2 =
            new TypeRef<ArrayList<? extends Comparable<? extends Number>>>() {};

      assertFalse(ref.isSubTypeOf(notSubRef1));
      assertFalse(ref.isSubTypeOf(notSubRef2));
   }
   
   /**
    * Tests that {@link TypeRef#isSubTypeOf(TypeRef)} throws an exception on {@code null} input.
    */
   public void testIsSubTypeOfNull() {
      TypeRef<ArrayList<? extends Comparable<? extends Number>>> ref =
            new TypeRef<ArrayList<? extends Comparable<? extends Number>>>() {};
            
      try {
         assertFalse(ref.isSubTypeOf(null));
         fail();
      } catch (NullPointerException expected) {
      }
   }

   /**
    * Tests {@link TypeRef#isSuperTypeOf(TypeRef)} under normal conditions.
    */
   public void testIsSuperTypeOf() {
      TypeRef<Collection<Map<String, String>>> ref =
            new TypeRef<Collection<Map<String, String>>>() {};
            
      TypeRef<List<Map<String, String>>> superRef1 = new TypeRef<List<Map<String, String>>>() {};
      TypeRef<LinkedHashSet<Map<String, String>>> superRef2 =
            new TypeRef<LinkedHashSet<Map<String, String>>>() {};
      TypeRef<Collection<Map<String, String>>> superRef3 =
            new TypeRef<Collection<Map<String, String>>>() {};
      
      assertTrue(ref.isSuperTypeOf(superRef1));
      assertTrue(ref.isSuperTypeOf(superRef2));
      assertTrue(ref.isSuperTypeOf(superRef3));
      
      TypeRef<Collection<Map<?, String>>> notSuperRef1 =
            new TypeRef<Collection<Map<?, String>>>() {};
      TypeRef<Collection<Map<String, Object>>> notSuperRef2 =
            new TypeRef<Collection<Map<String, Object>>>() {};
      TypeRef<Collection<?>> notSuperRef3 = new TypeRef<Collection<?>>() {};
      TypeRef<Map<String, Number>> notSuperRef4 = new TypeRef<Map<String, Number>>() {};

      assertFalse(ref.isSuperTypeOf(notSuperRef1));
      assertFalse(ref.isSuperTypeOf(notSuperRef2));
      assertFalse(ref.isSuperTypeOf(notSuperRef3));
      assertFalse(ref.isSuperTypeOf(notSuperRef4));
   }

   /**
    * Tests {@link TypeRef#isSuperTypeOf(TypeRef)} with array types.
    */
   public void testIsSuperTypeOfArray() {
      TypeRef<Object> ref1 = new TypeRef<Object>() {};
      TypeRef<Object[]> ref2 = new TypeRef<Object[]>() {};
      TypeRef<Collection<String>[]> ref3 = new TypeRef<Collection<String>[]>() {};
      
      assertTrue(ref1.isSuperTypeOf(ref2));
      assertTrue(ref1.isSuperTypeOf(ref3));
      assertTrue(ref2.isSuperTypeOf(ref3));

      assertFalse(ref2.isSuperTypeOf(ref1));
      assertFalse(ref3.isSuperTypeOf(ref1));
      assertFalse(ref3.isSuperTypeOf(ref2));
   }
      
   /**
    * Tests {@link TypeRef#isSuperTypeOf(TypeRef)} for a type that is not fully resolved (which
    * means no other type can be considered a super type).
    */
   public void testIsSuperTypeOfWildcard() {
      TypeRef<List<? extends Comparable<? extends Number>>> ref =
            new TypeRef<List<? extends Comparable<? extends Number>>>() {};

      // a type is always a super type of itself, and if they're the same instance, then wildcard
      // are allowed
      assertTrue(ref.isSuperTypeOf(ref));
            
      TypeRef<ArrayList<? extends Comparable<? extends Number>>> notSuperRef1 =
            new TypeRef<ArrayList<? extends Comparable<? extends Number>>>() {};
      TypeRef<List<? extends Comparable<? extends Number>>> notSuperRef2 =
            new TypeRef<List<? extends Comparable<? extends Number>>>() {};

      assertFalse(ref.isSuperTypeOf(notSuperRef1));
      assertFalse(ref.isSuperTypeOf(notSuperRef2));
   }
   
   /**
    * Tests that {@link TypeRef#isSuperTypeOf(TypeRef)} throws an exception on {@code null} input.
    */
   public void testIsSuperTypeOfNull() {
      TypeRef<ArrayList<? extends Comparable<? extends Number>>> ref =
            new TypeRef<ArrayList<? extends Comparable<? extends Number>>>() {};
            
      try {
         assertFalse(ref.isSuperTypeOf(null));
         fail();
      } catch (NullPointerException expected) {
      }
   }

   /**
    * Tests {@link TypeRef#forClass(Class)}.
    */
   public void testForClass() {
      TypeRef<?> ref = TypeRef.forClass(int.class);
      assertSame(int.class, ref.asClass());
      assertTrue(ref.isResolved());

      ref = TypeRef.forClass(String[].class);
      assertSame(String[].class, ref.asClass());
      assertTrue(ref.isResolved());

      ref = TypeRef.forClass(LinkedHashSet.class);
      assertSame(LinkedHashSet.class, ref.asClass());
      assertEquals(Arrays.asList("E"), ref.getTypeVariableNames());
      // raw class means we have no info on generic type parameters
      assertFalse(ref.isResolved());
      assertFalse(ref.canResolveTypeVariable("E"));
   }

   /**
    * Tests {@link TypeRef#forClass(Class)} with {@code null} input.
    */
   public void testForClassNull() {
      try {
         TypeRef.forClass(null);
         fail();
      } catch (NullPointerException expected) {
      }
   }
   
   /**
    * Tests {@link TypeRef#forType(Type)}.
    */
   public void testForType() {
      abstract class NumberList<T extends Number> implements List<T> {
         @SuppressWarnings("unused")
         Map<Class<? extends Number>, T> field;
      }
      abstract class BigDecimalList extends NumberList<BigDecimal> {
      }
      
      TypeRef<?> ref = TypeRef.forType(NumberList.class.getGenericInterfaces()[0]);
      assertSame(List.class, ref.asClass());
      assertFalse(ref.isResolved());
      assertFalse(ref.canResolveTypeVariable("E"));
      
      ref = TypeRef.forType(NumberList.class.getDeclaredFields()[0].getGenericType());
      assertSame(Map.class, ref.asClass());
      assertFalse(ref.isResolved());
      assertFalse(ref.canResolveTypeVariable("V"));
      ref = ref.resolveTypeVariable("K");
      assertSame(Class.class, ref.asClass());
      assertFalse(ref.canResolveTypeVariable("T"));

      ref = TypeRef.forType(BigDecimalList.class.getGenericSuperclass());
      assertSame(NumberList.class, ref.asClass());
      assertTrue(ref.isResolved());
      assertSame(BigDecimal.class, ref.resolveTypeVariable("T").asClass());
   }

   /**
    * Tests {@link TypeRef#forType(Type)} with input that cannot actually be resolved into a
    * {@code TypeRef}.
    */
   public void testForTypeInvalid() {
      // type parameter
      try {
         TypeRef.forType(Comparable.class.getTypeParameters()[0]);
      } catch (IllegalArgumentException expected) {
      }
      // wildcard
      try {
         TypeRef.forType(new WildcardType() {
            @Override
            public Type[] getUpperBounds() {
               return new Type[] { Object.class, ArrayList.class.getGenericSuperclass() };
            }
            @Override
            public Type[] getLowerBounds() {
               return new Type[0];
            }
         });
      } catch (IllegalArgumentException expected) {
      }
   }

   /**
    * Tests {@link TypeRef#forType(Type)} with {@code null} input.
    */
   public void testForTypeNull() {
      try {
         TypeRef.forType(null);
         fail();
      } catch (NullPointerException expected) {
      }
   }
}
