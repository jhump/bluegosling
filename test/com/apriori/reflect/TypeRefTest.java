package com.apriori.reflect;

import java.io.Serializable;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
      assertTrue(tr.getTypeParameterNames().isEmpty());
   }

   /**
    * Tests {@link TypeRef}s for simple (non-generic) array types.
    */
   public void testSimpleArrayType() {
      TypeRef<String[]> tr = new TypeRef<String[]>() {};
      assertSame(String[].class, tr.asClass());
      assertTrue(tr.getTypeParameterNames().isEmpty());
   }

   /**
    * Tests {@link TypeRef}s for generic types.
    */
   public void testGenericType() {
      TypeRef<Map<String, Object>> tr = new TypeRef<Map<String, Object>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());
      // and verify resolution of generic type info
      TypeRef<?> typeVarRef = tr.resolveTypeParameterNamed("K");
      assertSame(String.class, typeVarRef.asClass());
      typeVarRef = tr.resolveTypeParameterNamed("V");
      assertSame(Object.class, typeVarRef.asClass());
   }

   /**
    * Tests trying to explore type variable resolutions for {@link TypeRef}s with invalid type
    * variable names.
    */
   public void testGenericTypeAndInvalidTypeVariable() {
      TypeRef<List<Number>> tr = new TypeRef<List<Number>>() {};
      assertSame(List.class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeParameterNames());
      try {
         // case sensitive!
         tr.resolveTypeParameterNamed("e");
         fail();
      } catch (IllegalArgumentException expected) {}
      try {
         tr.resolveTypeParameterNamed("X");
         fail();
      } catch (IllegalArgumentException expected) {}
      try {
         tr.resolveTypeParameterNamed(null);
         fail();
      } catch (NullPointerException expected) {}
   }

   /**
    * Tests {@link TypeRef}s for generic types using wildcards during construction.
    */
   public void testGenericTypeAndAcceptableWildcard() {
      TypeRef<Map<?, ?>> tr = new TypeRef<Map<?, ?>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());
      // acknowledge that we cannot resolve the type variables here
      assertFalse(tr.resolveTypeParameterNamed("K").isResolved());
      assertFalse(tr.resolveTypeParameterNamed("V").isResolved());
   }

   /**
    * Tests {@link TypeRef}s for generic types using type parameters during construction.
    * 
    * @param <X> dummy type variable
    */
   public <X> void testGenericTypeAndAcceptableTypeParam() {
      TypeRef<Map<X, Object>> tr = new TypeRef<Map<X, Object>>() {};
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());
      // acknowledge that we cannot resolve both type variables here
      assertFalse(tr.resolveTypeParameterNamed("K").isResolved());
      // but we know this one
      TypeRef<?> typeVarRef = tr.resolveTypeParameterNamed("V");
      assertSame(Object.class, typeVarRef.asClass());
   }

   /**
    * Tests constructing {@link TypeRef}s using type parameters and wildcard types. These prevent
    * resolution of the type.
    * 
    * @param <X> dummy type variable
    * @param <Y> dummy type variable
    * @param <Z> dummy type variable
    */
   public <X, Y, Z extends Map<X, Y>> void testTypeParamsAndWildcards() {
      // type parameter
      TypeRef<Z> trFromVar = new TypeRef<Z>() {};
      assertFalse(trFromVar.isResolved());
      TypeVariable<?> var = (TypeVariable<?>) trFromVar.asType();
      assertEquals("Z", var.getName());
      Method m = (Method) var.getGenericDeclaration();
      assertEquals("testTypeParamsAndWildcards", m.getName());
      assertTrue(Arrays.equals(new Class<?>[0], m.getParameterTypes()));
      
      TypeRef<?> trFromWildcard = TypeRef.forType(Types.newExtendsWildcardType(List.class));
      assertFalse(trFromWildcard.isResolved());
      WildcardType wc = (WildcardType) trFromWildcard.asType();
      assertEquals(0, wc.getLowerBounds().length);
      assertTrue(Arrays.equals(new Class<?>[] { List.class }, wc.getUpperBounds()));
   }

   /**
    * Tests {@link TypeRef}s for a very complex generic type (whose type parameters are themselves
    * generic, etc).
    */
   public void testComplexGenericType() {
      TypeRef<?> tr = new TypeRef<Map<Comparable<? super Number>, List<Set<String>[]>>>() {};
      assertTrue(tr.isResolved());
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());

      TypeRef<?> kType = tr.resolveTypeParameterNamed("K");
      assertTrue(kType.isResolved());
      assertSame(Comparable.class, kType.asClass());
      assertEquals(Arrays.asList("T"), kType.getTypeParameterNames());
      // this one's not available since it was defined via wildcard
      assertFalse(kType.resolveTypeParameterNamed("T").isResolved());

      TypeRef<?> vType = tr.resolveTypeParameterNamed("V");
      assertTrue(vType.isResolved());
      assertSame(List.class, vType.asClass());
      assertEquals(Arrays.asList("E"), vType.getTypeParameterNames());
      vType = vType.resolveTypeParameterNamed("E");
      assertTrue(vType.isResolved());
      assertSame(Set[].class, vType.asClass());
      vType = vType.getComponentTypeRef();
      assertEquals(Arrays.asList("E"), vType.getTypeParameterNames());
      vType = vType.resolveTypeParameterNamed("E");
      assertTrue(vType.isResolved());
      assertSame(String.class, vType.asClass());
      assertTrue(vType.getTypeParameterNames().isEmpty());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is unresolvable.
    */
   public void testUnresolvableConstructionViaSubclass1() {
      TypeRef<?> tr = new TypeReferenceGenericSubclass<String>();
      assertFalse(tr.isResolved());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is invalid (initial type resolution
    * fails).
    */
   public void testUnresolvableConstructionViaSubclass2() {
      TypeRef<?> tr = new TypeReferenceComplex1<String, Object, HashMap<String, List<Object>>>();
      assertFalse(tr.isResolved());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass1() {
      TypeReferenceSubclass tr = new TypeReferenceSubclass() {};
      assertTrue(tr.isResolved());
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
      assertEquals(Arrays.asList("E"), tr.getTypeParameterNames());
      assertFalse(tr.resolveTypeParameterNamed("E").isResolved()); // wildcards can't be resolved
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
      assertEquals(Arrays.asList("E"), tr.getTypeParameterNames());
      assertFalse(tr.resolveTypeParameterNamed("E").isResolved()); // wildcards can't be resolved
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass3() {
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>() {};
      assertSame(Map.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());
      TypeRef<?> type = tr.resolveTypeParameterNamed("K");
      assertSame(Object.class, type.asClass());
      assertTrue(type.getTypeParameterNames().isEmpty());
      TypeRef<?> listType = tr.resolveTypeParameterNamed("V");
      assertSame(List.class, listType.asClass());
      assertEquals(Arrays.asList("E"), listType.getTypeParameterNames());
      type = listType.resolveTypeParameterNamed("E");
      assertSame(String.class, type.asClass());
      assertTrue(type.getTypeParameterNames().isEmpty());
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
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());
      assertFalse(tr.resolveTypeParameterNamed("K").isResolved()); // this type variable can't be resolved
      TypeRef<?> listType = tr.resolveTypeParameterNamed("V");
      assertSame(List.class, listType.asClass());
      assertEquals(Arrays.asList("E"), listType.getTypeParameterNames());
      assertFalse(listType.resolveTypeParameterNamed("E").isResolved());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass4() {
      TypeReferenceArraySubclass<String> tr = new TypeReferenceArraySubclass<String>() {};
      assertSame(String[].class, tr.asClass());
      // look at type variable info
      assertTrue(tr.getTypeParameterNames().isEmpty());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that is valid.
    */
   public void testConstructionViaSubclass5() {
      TypeReferenceGenericSubclass<Collection<?>> tr = new TypeReferenceGenericSubclass<Collection<?>>() {};
      assertSame(Collection.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeParameterNames());
      assertFalse(tr.resolveTypeParameterNamed("E").isResolved()); // can't resolve wildcard
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
      assertEquals(Arrays.asList("K", "V"), tr.getTypeParameterNames());
      TypeRef<?> k = tr.resolveTypeParameterNamed("K");
      assertSame(Collection.class, k.asClass());
      assertEquals(Arrays.asList("E"), k.getTypeParameterNames());
      TypeRef<?> e = k.resolveTypeParameterNamed("E");
      assertSame(String.class, e.asClass());
      assertTrue(e.getTypeParameterNames().isEmpty());
      TypeRef<?> v = tr.resolveTypeParameterNamed("V");
      assertSame(List.class, v.asClass());
      assertEquals(Arrays.asList("E"), v.getTypeParameterNames());
      e = v.resolveTypeParameterNamed("E");
      assertSame(Boolean[].class, e.asClass());
      assertTrue(e.getTypeParameterNames().isEmpty());
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
      assertEquals("java.util.List<java.lang.Number>", tr.toString());
   }

   /**
    * Tests {@link TypeRef#toString()} with an elaborate and complex generic type.
    * 
    * @param <X> dummy type variable
    */
   public <X> void testToStringComplex() {
      TypeRef<?> tr = new TypeRef<Map<List<X>, Map<Integer, SortedSet<Class<?>>>>>() {};
      assertEquals(
            "java.util.Map<java.util.List<X>,java.util.Map<java.lang.Integer,java.util.SortedSet<java.lang.Class<?>>>>",
            tr.toString());
   }

   /**
    * Tests {@link TypeRef#getTypeParameterNamed(String)}.
    */
   public void testGetTypeVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      TypeVariable<Class<?>> typeVarKey = tr.getTypeParameterNamed("K");
      assertEquals("K", typeVarKey.getName());
      assertSame(Map.class, typeVarKey.getGenericDeclaration());
      assertSame(Map.class.getTypeParameters()[0], typeVarKey);
      TypeVariable<Class<?>> typeVarVal = tr.getTypeParameterNamed("V");
      assertEquals("V", typeVarVal.getName());
      assertSame(Map.class, typeVarVal.getGenericDeclaration());
      assertSame(Map.class.getTypeParameters()[1], typeVarVal);
      tr = tr.resolveTypeParameterNamed("V");
      TypeVariable<Class<?>> typeVarElement = tr.getTypeParameterNamed("E");
      assertEquals("E", typeVarElement.getName());
      assertSame(List.class, typeVarElement.getGenericDeclaration());
      assertSame(List.class.getTypeParameters()[0], typeVarElement);
   }

   /**
    * Tests {@link TypeRef#getTypeParameterNamed(String)} with invalid variable names specified.
    */
   public void testGetTypeVariableWithInvalidVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      try {
         tr.getTypeParameterNamed("k"); // case-sensitive
         fail();
      } catch (IllegalArgumentException expected) {}
      try {
         tr.getTypeParameterNamed("X");
         fail();
      } catch (IllegalArgumentException expected) {}
      try {
         tr.getTypeParameterNamed(null);
         fail();
      } catch (NullPointerException expected) {}
   }

   /**
    * Tests {@link TypeRef#canResolveTypeVariable(String)}.
    */
   public void testCanResolveTypeVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      assertFalse(tr.resolveTypeParameterNamed("K").isResolved());
      assertTrue(tr.resolveTypeParameterNamed("V").isResolved());
      assertSame(List.class, tr.resolveTypeParameterNamed("V").asClass());
      tr = tr.resolveTypeParameterNamed("V");
      assertFalse(tr.resolveTypeParameterNamed("E").isResolved());
   }

   /**
    * Tests {@link TypeRef#canResolveTypeVariable(String)} with invalid variable names specified.
    */
   public void testCanResolveTypeVariableWithInvalidVariable() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      try {
         tr.resolveTypeParameterNamed("k"); // case-sensitive
         fail();
      } catch (IllegalArgumentException expected) {}
      try {
         tr.resolveTypeParameterNamed("X");
         fail();
      } catch (IllegalArgumentException expected) {}
      try {
         tr.resolveTypeParameterNamed(null);
         fail();
      } catch (NullPointerException expected) {}
   }

   /**
    * Tests that {@link TypeRef#isFullyResolved()} correctly identifies when the current object is not
    * resolved.
    */
   public void testIsResolvedFalse() {
      TypeRef<?> tr = new TypeRef<Map<? extends Number, List<?>>>() {};
      assertFalse(tr.isFullyResolved());
      assertFalse(tr.resolveTypeParameterNamed("V").isFullyResolved());
   }

   /**
    * Tests that {@link TypeRef#isFullyResolved()} correctly identifies when the current object is
    * resolved.
    */
   public void testIsResolvedTrue() {
      TypeRef<?> tr = new TypeRef<Map<Comparable<Integer>, List<String>>>() {};
      assertTrue(tr.isFullyResolved());
      TypeRef<?> varRef = tr.resolveTypeParameterNamed("K");
      assertTrue(varRef.isFullyResolved());
      assertTrue(varRef.resolveTypeParameterNamed("T").isFullyResolved());
      varRef = tr.resolveTypeParameterNamed("V");
      assertTrue(varRef.isFullyResolved());
      assertTrue(varRef.resolveTypeParameterNamed("E").isFullyResolved());
   }

   /**
    * Tests that {@link TypeRef#isFullyResolved()} correctly identifies an object "mixed" resolution.
    * A "mixed" resolution is one where an object is <em>not</em> resolved but one of its type
    * variables is. This asserts that the type variable returns true but that the composite/parent
    * type returns false.
    */
   public void testIsResolvedMixed() {
      TypeRef<?> tr = new TypeRef<Map<? extends Comparable<Integer>, List<String>>>() {};
      assertFalse(tr.isFullyResolved());
      TypeRef<?> varRef = tr.resolveTypeParameterNamed("V");
      assertTrue(varRef.isFullyResolved());
      assertTrue(varRef.resolveTypeParameterNamed("E").isFullyResolved());
   }

   /**
    * Tests that {@link TypeRef#isFullyResolved()} correctly identifies a primitive or non-generic type
    * as resolved.
    */
   public void testIsResolvedSimpleTypes() {
      TypeRef<?> tr = new TypeRef<Object>() {};
      assertTrue(tr.isFullyResolved());
      tr = TypeRef.forClass(int.class);
      assertTrue(tr.isFullyResolved());
   }

   /**
    * Tests {@link TypeRef#getSuperclassTypeRef()}, {@link TypeRef#resolveSuperTypeRef(Class)}, and
    * {@link TypeRef#getInterfaceTypeRefs()}.
    */
   public void testSuperTypeAndInterfaceTypeRefs() {
      @SuppressWarnings("serial")
      abstract class ComparableList extends Number
            implements List<CharSequence>, Comparable<Collection<Number>> {}

      TypeRef<ComparableList> tr = new TypeRef<ComparableList>() {};
      assertTrue(tr.getTypeParameterNames().isEmpty());
      // superclass
      assertSame(Number.class, tr.getSuperclassTypeRef().asClass());
      //   again, but this time using superTypeRefFor
      assertSame(Number.class, tr.resolveSuperTypeRef(Number.class).asClass());
      // interface
      List<TypeRef<?>> ifaceTypes = new ArrayList<TypeRef<?>>(tr.getInterfaceTypeRefs());
      assertEquals(2, ifaceTypes.size());
      TypeRef<?> superRef = ifaceTypes.get(0);
      assertSame(List.class, superRef.asClass());
      assertSame(CharSequence.class, superRef.resolveTypeParameterNamed("E").asClass());
      //   again, but this time using superTypeRefFor
      superRef = tr.resolveSuperTypeRef(List.class);
      assertSame(List.class, superRef.asClass());
      assertSame(CharSequence.class, superRef.resolveTypeParameterNamed("E").asClass());
      // another interface
      superRef = ifaceTypes.get(1);
      assertSame(Comparable.class, superRef.asClass());
      TypeRef<?> varRef = superRef.resolveTypeParameterNamed("T");
      assertSame(Collection.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeParameterNamed("E").asClass());
      //   again, but this time using superTypeRefFor
      superRef = tr.resolveSuperTypeRef(Comparable.class);
      assertSame(Comparable.class, superRef.asClass());
      varRef = superRef.resolveTypeParameterNamed("T");
      assertSame(Collection.class, varRef.asClass());
      assertSame(Number.class, varRef.resolveTypeParameterNamed("E").asClass());
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
    * Tests {@link TypeRef#getSuperclassTypeRef()} and {@link TypeRef#resolveSuperTypeRef(Class)} with a very
    * complex example to hopefully catch any possible edge cases in the lookup/resolution of
    * super types.
    */
   public void testSuperTypeRefForComplex() {
      @SuppressWarnings("serial")
      abstract class MyCrazierSet<F> extends MyCrazySet<Number, Callable<F>> {}

      // first w/ wild-card
      {
         TypeRef<MyCrazierSet<?>> tr = new TypeRef<MyCrazierSet<?>>() {};
         assertEquals(Arrays.asList("F"), tr.getTypeParameterNames());
         assertFalse(tr.resolveTypeParameterNamed("F").isResolved());
   
         TypeRef<?> superRef = tr.getSuperclassTypeRef();
         assertSame(MyCrazySet.class, superRef.asClass());
         assertEquals(Arrays.asList("S", "T"), superRef.getTypeParameterNames());
         assertSame(Number.class, superRef.resolveTypeParameterNamed("S").asClass());
         assertSame(Callable.class, superRef.resolveTypeParameterNamed("T").asClass());
   
         superRef = tr.resolveSuperTypeRef(CrazySet.class);
         assertSame(CrazySet.class, superRef.asClass());
         assertEquals(Arrays.asList("W", "X", "Y", "Z"), superRef.getTypeParameterNames());
         assertSame(Class.class, superRef.resolveTypeParameterNamed("W").asClass());
         assertSame(Number.class, superRef.resolveTypeParameterNamed("X").asClass());
         assertSame(String.class, superRef.resolveTypeParameterNamed("Y").asClass());
         assertSame(Callable.class, superRef.resolveTypeParameterNamed("Z").asClass());
   
         superRef = tr.resolveSuperTypeRef(Map.class);
         assertSame(Map.class, superRef.asClass());
         assertEquals(Arrays.asList("K", "V"), superRef.getTypeParameterNames());
         assertSame(Pair.class, superRef.resolveTypeParameterNamed("K").asClass());
         assertSame(SortedSet.class, superRef.resolveTypeParameterNamed("V").asClass());
   
         superRef = tr.resolveSuperTypeRef(TypeVariable.class);
         assertSame(TypeVariable.class, superRef.asClass());
         assertEquals(Arrays.asList("D"), superRef.getTypeParameterNames());
         assertSame(Class.class, superRef.resolveTypeParameterNamed("D").asClass());
      }

      {
         // and with a concrete value (now we'll go crazy verifying the *full*
         // generic type -- type variables and all)
         TypeRef<MyCrazierSet<RuntimeException>> tr =
               new TypeRef<MyCrazierSet<RuntimeException>>() {};
         assertEquals(Arrays.asList("F"), tr.getTypeParameterNames());
         assertSame(RuntimeException.class, tr.resolveTypeParameterNamed("F").asClass());
   
         TypeRef<?> superRef = tr.getSuperclassTypeRef();
         assertSame(MyCrazySet.class, superRef.asClass());
         assertEquals(Arrays.asList("S", "T"), superRef.getTypeParameterNames());
         assertSame(Number.class, superRef.resolveTypeParameterNamed("S").asClass());
         TypeRef<?> varRef = superRef.resolveTypeParameterNamed("T");
         assertSame(Callable.class, varRef.asClass());
         assertSame(RuntimeException.class, varRef.resolveTypeParameterNamed("V").asClass());
   
         superRef = tr.resolveSuperTypeRef(CrazySet.class);
         assertEquals(Arrays.asList("W", "X", "Y", "Z"), superRef.getTypeParameterNames());
         varRef = superRef.resolveTypeParameterNamed("W");
         assertSame(Class.class, varRef.asClass());
         assertSame(Number.class, varRef.resolveTypeParameterNamed("T").asClass());
         assertSame(Number.class, superRef.resolveTypeParameterNamed("X").asClass());
         assertSame(String.class, superRef.resolveTypeParameterNamed("Y").asClass());
         varRef = superRef.resolveTypeParameterNamed("Z");
         assertSame(Callable.class, varRef.asClass());
         assertSame(RuntimeException.class, varRef.resolveTypeParameterNamed("V").asClass());
   
         superRef = tr.resolveSuperTypeRef(Map.class);
         assertEquals(Arrays.asList("K", "V"), superRef.getTypeParameterNames());
         varRef = superRef.resolveTypeParameterNamed("K");
         assertSame(Pair.class, varRef.asClass());
         assertSame(Number.class, varRef.resolveTypeParameterNamed("A").asClass());
         assertSame(String.class, varRef.resolveTypeParameterNamed("B").asClass());
         // and get super-type of this Pair type
         TypeRef<?> pairSuperRef = varRef.resolveSuperTypeRef(Tuple5.class);
         assertEquals(pairSuperRef, varRef.getInterfaceTypeRefs().get(0));
         assertSame(Number.class, pairSuperRef.resolveTypeParameterNamed("M1").asClass());
         assertSame(String.class, pairSuperRef.resolveTypeParameterNamed("M2").asClass());
         assertSame(Void.class, pairSuperRef.resolveTypeParameterNamed("M3").asClass());
         assertSame(Void.class, pairSuperRef.resolveTypeParameterNamed("M4").asClass());
         assertSame(Void.class, pairSuperRef.resolveTypeParameterNamed("M5").asClass());
         varRef = superRef.resolveTypeParameterNamed("V");
         assertSame(SortedSet.class, varRef.asClass());
         varRef = varRef.resolveTypeParameterNamed("E");
         assertSame(Callable.class, varRef.asClass());
         assertSame(RuntimeException.class, varRef.resolveTypeParameterNamed("V").asClass());
   
         superRef = tr.resolveSuperTypeRef(TypeVariable.class);
         assertEquals(Arrays.asList("D"), superRef.getTypeParameterNames());
         varRef = superRef.resolveTypeParameterNamed("D");
         assertSame(Class.class, varRef.asClass());
         assertSame(Number.class, varRef.resolveTypeParameterNamed("T").asClass());
      }
   }

   /**
    * Tests {@link TypeRef#resolveSuperTypeRef(Class)} when an invalid type is queried.
    */
   public void testSuperTypeRefForInvalidType() {
      TypeRef<?> tr = new TypeRef<ArrayList<String>>() {};

      try {
         tr.resolveSuperTypeRef(Set.class);
         fail();
      } catch (IllegalArgumentException expected) {}

      try {
         tr.resolveSuperTypeRef(Number.class);
         fail();
      } catch (IllegalArgumentException expected) {}

      try {
         tr.resolveSuperTypeRef(null);
         fail();
      } catch (NullPointerException expected) {}
   }

   /**
    * Tests that {@link TypeRef#getSuperclassTypeRef()} returns {@code null} for interfaces, {@code Object},
    * primitives, and {@code void}.
    */
   public void testSuperTypeRefNull() {
      // root of hierarchy
      TypeRef<?> tr = new TypeRef<Object>() {};
      assertNull(tr.getSuperclassTypeRef());
      // interface
      tr = new TypeRef<List<?>>() {};
      assertNull(tr.getSuperclassTypeRef());
      // primitive
      tr = TypeRef.forClass(boolean.class);
      assertNull(tr.getSuperclassTypeRef());
      // void
      tr = TypeRef.forClass(void.class);
      assertNull(tr.getSuperclassTypeRef());
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
      TypeRef<List<?>> subRef4 = new TypeRef<List<?>>() {};
      TypeRef<ArrayList> subRef5 = new TypeRef<ArrayList>() {};
      
      assertTrue(ref.isSubTypeOf(subRef1));
      assertTrue(ref.isSubTypeOf(subRef2));
      assertTrue(ref.isSubTypeOf(subRef3));
      assertTrue(ref.isSubTypeOf(subRef4));
      assertTrue(ref.isSubTypeOf(subRef5));
      
      TypeRef<List<Map<?, String>>> notSubRef1 = new TypeRef<List<Map<?, String>>>() {};
      TypeRef<List<Map<String, Object>>> notSubRef2 = new TypeRef<List<Map<String, Object>>>() {};
      TypeRef<Set<?>> notSubRef3 = new TypeRef<Set<?>>() {};
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
    * Tests {@link TypeRef#isAssignableFrom(TypeRef)} under normal conditions.
    */
   public void testIsAssignableFrom() {
      TypeRef<Collection<Map<String, String>>> ref =
            new TypeRef<Collection<Map<String, String>>>() {};
            
      TypeRef<List<Map<String, String>>> superRef1 = new TypeRef<List<Map<String, String>>>() {};
      TypeRef<LinkedHashSet<Map<String, String>>> superRef2 =
            new TypeRef<LinkedHashSet<Map<String, String>>>() {};
      TypeRef<Collection<Map<String, String>>> superRef3 =
            new TypeRef<Collection<Map<String, String>>>() {};
      
      assertTrue(ref.isAssignableFrom(superRef1));
      assertTrue(ref.isAssignableFrom(superRef2));
      assertTrue(ref.isAssignableFrom(superRef3));
      
      TypeRef<Collection<Map<?, String>>> notSuperRef1 =
            new TypeRef<Collection<Map<?, String>>>() {};
      TypeRef<Collection<Map<String, Object>>> notSuperRef2 =
            new TypeRef<Collection<Map<String, Object>>>() {};
      TypeRef<Collection<?>> notSuperRef3 = new TypeRef<Collection<?>>() {};
      TypeRef<Map<String, Number>> notSuperRef4 = new TypeRef<Map<String, Number>>() {};

      assertFalse(ref.isAssignableFrom(notSuperRef1));
      assertFalse(ref.isAssignableFrom(notSuperRef2));
      assertFalse(ref.isAssignableFrom(notSuperRef3));
      assertFalse(ref.isAssignableFrom(notSuperRef4));
      
      TypeRef<Collection<?>> wildcardType1 = new TypeRef<Collection<?>>() {};
      TypeRef<Collection<? extends Map<?, String>>> wildcardType2 =
            new TypeRef<Collection<? extends Map<?, String>>>() {};
      assertTrue(wildcardType1.isAssignableFrom(ref));
      assertTrue(wildcardType2.isAssignableFrom(ref));
   }

   /**
    * Tests {@link TypeRef#isAssignableFrom(TypeRef)} with array types.
    */
   public void testIsAssignableFromArray() {
      TypeRef<Object> ref1 = new TypeRef<Object>() {};
      TypeRef<Object[]> ref2 = new TypeRef<Object[]>() {};
      TypeRef<Collection<String>[]> ref3 = new TypeRef<Collection<String>[]>() {};
      
      assertTrue(ref1.isAssignableFrom(ref2));
      assertTrue(ref1.isAssignableFrom(ref3));
      assertTrue(ref2.isAssignableFrom(ref3));

      assertFalse(ref2.isAssignableFrom(ref1));
      assertFalse(ref3.isAssignableFrom(ref1));
      assertFalse(ref3.isAssignableFrom(ref2));
   }
      
   /**
    * Tests that {@link TypeRef#isAssignableFrom(TypeRef)} throws an exception on {@code null} input.
    */
   public void testIsAssignableFromNull() {
      TypeRef<ArrayList<? extends Comparable<? extends Number>>> ref =
            new TypeRef<ArrayList<? extends Comparable<? extends Number>>>() {};
            
      try {
         assertFalse(ref.isAssignableFrom(null));
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
      assertTrue(ref.isFullyResolved());

      ref = TypeRef.forClass(String[].class);
      assertSame(String[].class, ref.asClass());
      assertTrue(ref.isFullyResolved());

      ref = TypeRef.forClass(LinkedHashSet.class);
      assertSame(LinkedHashSet.class, ref.asClass());
      assertEquals(Arrays.asList("E"), ref.getTypeParameterNames());
      // raw class means we have no info on generic type parameters
      assertFalse(ref.isFullyResolved());
      assertFalse(ref.resolveTypeParameterNamed("E").isResolved());
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
      assertFalse(ref.isFullyResolved());
      assertFalse(ref.resolveTypeParameterNamed("E").isResolved());
      
      ref = TypeRef.forType(NumberList.class.getDeclaredFields()[0].getGenericType());
      assertSame(Map.class, ref.asClass());
      assertFalse(ref.isFullyResolved());
      assertFalse(ref.resolveTypeParameterNamed("V").isResolved());
      ref = ref.resolveTypeParameterNamed("K");
      assertSame(Class.class, ref.asClass());
      assertFalse(ref.resolveTypeParameterNamed("T").isResolved());

      ref = TypeRef.forType(BigDecimalList.class.getGenericSuperclass());
      assertSame(NumberList.class, ref.asClass());
      assertTrue(ref.isFullyResolved());
      assertSame(BigDecimal.class, ref.resolveTypeParameterNamed("T").asClass());
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
