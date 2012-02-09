package com.apriori.reflect;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Tests {@link TypeRef}.
 * 
 * @author jhumphries
 */
public class TypeRefTest extends TestCase {
   
   // Numerous sub-classes of TypeReference to test various scenarios under
   // which the TypeReference determines the resolved runtime type for its
   // type parameter.
   
   private static class TypeReferenceSubclass extends TypeRef<String> {
      public TypeReferenceSubclass() {
      }
   }

   private static class TypeReferenceArraySubclass<T> extends TypeRef<T[]> {
      public TypeReferenceArraySubclass() {
      }
   }

   private static class TypeReferenceGenericSubclass<T> extends TypeRef<T> {
      public TypeReferenceGenericSubclass() {
      }
   }
   
   private static class TypeReferenceSubclass2 extends TypeReferenceGenericSubclass<List<? extends Serializable>> {
      public TypeReferenceSubclass2() {
      }
   }

   private static class TypeReferenceComplex1<K, E, M extends Map<K, List<E>>> extends TypeRef<M> {
      public TypeReferenceComplex1() {
      }
   }

   private static class TypeReferenceComplex2<K, V> extends TypeRef<Map<V, List<K>>> {
      public TypeReferenceComplex2() {
      }
   }

   /**
    * Tests {@link TypeRef}s for simple (non-generic) types.
    */
   public void testSimpleType() {
      TypeRef<Number> tr = new TypeRef<Number>() { };
      assertSame(Number.class, tr.asClass());
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }
   
   /**
    * Tests {@link TypeRef}s for simple (non-generic) array types.
    */
   public void testSimpleArrayType() {
      TypeRef<String[]> tr = new TypeRef<String[]>() { };
      assertSame(String[].class, tr.asClass());
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }

   /**
    * Tests {@link TypeRef}s for generic types.
    */
   public void testGenericType() {
      TypeRef<Map<String, Object>> tr = new TypeRef<Map<String, Object>>() { };
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
      TypeRef<List<Integer>[]> tr = new TypeRef<List<Integer>[]>() { };
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
      TypeRef<List<Date>[][][]> tr = new TypeRef<List<Date>[][][]>() { };
      assertSame(List[][][].class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      // and verify resolution of generic type info
      TypeRef<?> typeVarRef = tr.resolveTypeVariable("E");
      assertSame(Date.class, typeVarRef.asClass());
   }

   /**
    * Tests trying to explore type variable resolutions
    * for {@link TypeRef}s with invalid type variable names.
    */
   public void testGenericTypeAndInvalidTypeVariable() {
      TypeRef<List<Number>> tr = new TypeRef<List<Number>>() { };
      assertSame(List.class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      try {
         // case sensitive!
         tr.resolveTypeVariable("e");
         fail();
      } catch (IllegalArgumentException expected) {
      }
      try {
         tr.resolveTypeVariable("X");
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   /**
    * Tests {@link TypeRef}s for generic types using wildcards
    * during construction.
    */
   public void testGenericTypeAndAcceptableWildcard() {
      TypeRef<Map<?, ?>> tr = new TypeRef<Map<?, ?>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // acknowledge that we cannot resolve the type variables here
      assertNull(tr.resolveTypeVariable("K"));
      assertNull(tr.resolveTypeVariable("V"));
   }

   /**
    * Tests {@link TypeRef}s for generic types using type
    * parameters during construction.
    * 
    * @param <X> dummy type variable
    */
   public <X> void testGenericTypeAndAcceptableTypeParam() {
      TypeRef<Map<X, Object>> tr = new TypeRef<Map<X, Object>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // acknowledge that we cannot resolve both type variables here
      assertNull(tr.resolveTypeVariable("K"));
      // but we know this one
      TypeRef<?> typeVarRef = tr.resolveTypeVariable("V");
      assertSame(Object.class, typeVarRef.asClass());
   }
   
   /**
    * Tests {@link TypeRef}s for generic types using invalid type
    * parameters during construction (which prevent initial
    * resolution of the type).
    * 
    * @param <X> dummy type variable
    * @param <Y> dummy type variable
    * @param <Z> dummy type variable
    */
   public <X, Y, Z extends Map<X, Y>> void testGenericTypeAndUnacceptableTypeParam() {
      try {
         @SuppressWarnings("unused")
         TypeRef<Z> tr = new TypeRef<Z>() { };
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   /**
    * Tests {@link TypeRef}s for a very complex generic type
    * (whose type parameters are themselves generic, etc).
    */
   public void testComplexGenericType() {
      TypeRef<?> tr = new TypeRef<Map<Comparable<? super Number>, List<Set<String>[]>>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());

      TypeRef<?> kType = tr.resolveTypeVariable("K");
      assertSame(Comparable.class, kType.asClass());
      assertEquals(Arrays.asList("T"), kType.getTypeVariableNames());
      // this one's not available since it was defined via wildcard
      assertNull(kType.resolveTypeVariable("T"));
      
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
    * Tests construction of {@link TypeRef}s via subclass that
    * is invalid (initial type resolution fails).
    */
   public void testInvalidConstructionViaSubclass1() {
      try {
         @SuppressWarnings("unused")
         TypeRef<?> tr = new TypeReferenceGenericSubclass<String>();
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is invalid (initial type resolution fails).
    */
   public void testInvalidConstructionViaSubclass2() {
      try {
         @SuppressWarnings("unused")
         TypeRef<?> tr = new TypeReferenceComplex1<String, Object, HashMap<String, List<Object>>>();
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid.
    */
   public void testConstructionViaSubclass1() {
      TypeReferenceSubclass tr = new TypeReferenceSubclass() { };
      assertSame(String.class, tr.asClass());
   }
   
   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid and doesn't even require the use of an anonymous
    * subclass (as would be required if instantiating {@link TypeRef}
    * directly).
    */
   public void testConstructionViaSubclassNoAnonymousType1() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as String.
      TypeReferenceSubclass tr = new TypeReferenceSubclass();
      assertSame(String.class, tr.asClass());
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid.
    */
   public void testConstructionViaSubclass2() {
      TypeReferenceSubclass2 tr = new TypeReferenceSubclass2() { };
      assertSame(List.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("E")); // wildcards can't be resolved
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid and doesn't even require the use of an anonymous
    * subclass (as would be required if instantiating {@link TypeRef}
    * directly).
    */
   public void testConstructionViaSubclassNoAnonymousType2() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as
      // List<? extends Serializable>.
      TypeReferenceSubclass2 tr = new TypeReferenceSubclass2();
      assertSame(List.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("E")); // wildcards can't be resolved
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid.
    */
   public void testConstructionViaSubclass3() {
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>() { };
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
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid and doesn't even require the use of an anonymous
    * subclass (as would be required if instantiating {@link TypeRef}
    * directly).
    */
   public void testConstructionViaSubclassNoAnonymousType3() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as
      // Map<V, List<K>>.
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>();
      assertSame(Map.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("K")); // this type variable can't be resolved
      TypeRef<?> listType = tr.resolveTypeVariable("V");
      assertSame(List.class, listType.asClass());
      assertEquals(Arrays.asList("E"), listType.getTypeVariableNames());
      assertNull(listType.resolveTypeVariable("E"));
   }

   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid.
    */
   public void testConstructionViaSubclass4() {
      TypeReferenceArraySubclass<String> tr = new TypeReferenceArraySubclass<String>() { };
      assertSame(String[].class, tr.asClass());
      // look at type variable info
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }
   
   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid.
    */
   public void testConstructionViaSubclass5() {
      TypeReferenceGenericSubclass<Collection<?>> tr = new TypeReferenceGenericSubclass<Collection<?>>() { };
      assertSame(Collection.class, tr.asClass());
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("E")); // can't resolve wildcard
   }
   
   /**
    * Tests construction of {@link TypeRef}s via subclass that
    * is valid.
    */
   public void testConstructionViaSubclass6() {
      // this behemoth results in a TypeReference for
      // Map<Collection<String>, List<Boolean[]>>
      TypeReferenceComplex2<Boolean[], Collection<String>> tr = new TypeReferenceComplex2<Boolean[], Collection<String>>() { };
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
}
