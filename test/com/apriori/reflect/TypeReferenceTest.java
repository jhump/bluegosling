package com.apriori.reflect;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Tests {@link TypeReference}.
 * 
 * @author jhumphries
 */
public class TypeReferenceTest extends TestCase {
   
   // Numerous sub-classes of TypeReference to test various scenarios under
   // which the TypeReference determines the resolved runtime type for its
   // type parameter.
   
   private static class TypeReferenceSubclass extends TypeReference<String> {
   }

   private static class TypeReferenceArraySubclass<T> extends TypeReference<T[]> {
   }

   private static class TypeReferenceGenericSubclass<T> extends TypeReference<T> {
   }
   
   private static class TypeReferenceSubclass2 extends TypeReferenceGenericSubclass<List<? extends Serializable>> {
   }

   private static class TypeReferenceComplex1<K, E, M extends Map<K, List<E>>> extends TypeReference<M> {
   }

   private static class TypeReferenceComplex2<K, V> extends TypeReference<Map<V, List<K>>> {
   }

   public void testSimpleType() {
      TypeReference<Number> tr = new TypeReference<Number>() { };
      assertSame(Number.class, tr.asClass());
      assertTrue(tr.getTypeVariableNames().isEmpty());
      assertSame(Number.class, tr.getType());
   }
   
   public void testSimpleArrayType() {
      TypeReference<String[]> tr = new TypeReference<String[]>() { };
      assertSame(String[].class, tr.asClass());
      assertTrue(tr.getTypeVariableNames().isEmpty());
      // now verify generic type information
      Type type = tr.getType();
      assertTrue(type instanceof GenericArrayType);
      GenericArrayType arrayType = (GenericArrayType) type;
      assertSame(String.class, arrayType.getGenericComponentType());
   }

   public void testGenericType() {
      TypeReference<Map<String, Object>> tr = new TypeReference<Map<String, Object>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      
      // now verify generic type information
      Type mapType = tr.getType();
      assertTrue(mapType instanceof ParameterizedType);
      ParameterizedType ptMapType = (ParameterizedType) mapType;
      assertNull(ptMapType.getOwnerType());
      assertSame(Map.class, ptMapType.getRawType());
      Type mapTypeParams[] = ptMapType.getActualTypeArguments(); 
      assertEquals(2, mapTypeParams.length);
      assertSame(String.class, mapTypeParams[0]);
      assertSame(Object.class, mapTypeParams[1]);
      
      // and verify resolution of generic type info
      TypeReference<?> typeVarRef = tr.resolveTypeVariable("K");
      assertSame(String.class, typeVarRef.asClass());
      typeVarRef = tr.resolveTypeVariable("V");
      assertSame(Object.class, typeVarRef.asClass());
   }

   public void testGenericArrayType() {
      TypeReference<List<Integer>[]> tr = new TypeReference<List<Integer>[]>() { };
      assertSame(List[].class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      
      // now verify generic type information
      Type type = tr.getType();
      assertTrue(type instanceof GenericArrayType);
      GenericArrayType arrayType = (GenericArrayType) type;
      type = arrayType.getGenericComponentType();
      assertTrue(type instanceof ParameterizedType);
      ParameterizedType parmType = (ParameterizedType) type;
      assertNull(parmType.getOwnerType());
      assertSame(List.class, parmType.getRawType());
      Type typeParms[] = parmType.getActualTypeArguments();
      assertEquals(1, typeParms.length);
      assertSame(Integer.class, typeParms[0]);
      
      // and verify resolution of generic type info
      TypeReference<?> typeVarRef = tr.resolveTypeVariable("E");
      assertSame(Integer.class, typeVarRef.asClass());
   }

   public void test2DArrayType() {
      TypeReference<List<Date>[][][]> tr = new TypeReference<List<Date>[][][]>() { };
      assertSame(List[][][].class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      
      // now verify generic type information
      Type type = tr.getType();
      assertTrue(type instanceof GenericArrayType);
      GenericArrayType arrayType = (GenericArrayType) type;
      type = arrayType.getGenericComponentType();
      assertTrue(type instanceof GenericArrayType);
      arrayType = (GenericArrayType) type;
      type = arrayType.getGenericComponentType();
      assertTrue(type instanceof GenericArrayType);
      arrayType = (GenericArrayType) type;
      type = arrayType.getGenericComponentType();
      assertTrue(type instanceof ParameterizedType);
      ParameterizedType parmType = (ParameterizedType) type;
      assertNull(parmType.getOwnerType());
      assertSame(List.class, parmType.getRawType());
      Type typeParms[] = parmType.getActualTypeArguments();
      assertEquals(1, typeParms.length);
      assertSame(Date.class, typeParms[0]);
      
      // and verify resolution of generic type info
      TypeReference<?> typeVarRef = tr.resolveTypeVariable("E");
      assertSame(Date.class, typeVarRef.asClass());
   }

   public void testGenericTypeAndInvalidTypeVariable() {
      TypeReference<List<Number>> tr = new TypeReference<List<Number>>() { };
      assertSame(List.class, tr.asClass());
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      try {
         // case sensitive!
         TypeReference<?> typeVarRef = tr.resolveTypeVariable("e");
         fail();
      } catch (IllegalArgumentException expected) {
      }
      try {
         TypeReference<?> typeVarRef = tr.resolveTypeVariable("X");
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   public void testGenericTypeAndAcceptableWildcard() {
      TypeReference<Map<?, ?>> tr = new TypeReference<Map<?, ?>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // acknowledge that we cannot resolve the type variables here
      assertNull(tr.resolveTypeVariable("K"));
      assertNull(tr.resolveTypeVariable("V"));
   }

   public <X> void testGenericTypeAndAcceptableTypeParam() {
      TypeReference<Map<X, Object>> tr = new TypeReference<Map<X, Object>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      // acknowledge that we cannot resolve both type variables here
      assertNull(tr.resolveTypeVariable("K"));
      // but we know this one
      TypeReference<?> typeVarRef = tr.resolveTypeVariable("V");
      assertSame(Object.class, typeVarRef.asClass());
   }
   
   public <X, Y, Z extends Map<X, Y>> void testGenericTypeAndUnacceptableTypeParam() {
      try {
         new TypeReference<Z>() { };
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   public void testComplexGenericType() {
      TypeReference<?> tr = new TypeReference<Map<Comparable<? super Number>, List<Set<String>[]>>>() { };
      assertSame(Map.class, tr.asClass());
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());

      TypeReference<?> kType = tr.resolveTypeVariable("K");
      assertSame(Comparable.class, kType.asClass());
      assertEquals(Arrays.asList("T"), kType.getTypeVariableNames());
      // this one's not available since it was defined via wildcard
      assertNull(kType.resolveTypeVariable("T"));
      
      TypeReference<?> vType = tr.resolveTypeVariable("V");
      assertSame(List.class, vType.asClass());
      assertEquals(Arrays.asList("E"), vType.getTypeVariableNames());
      vType = vType.resolveTypeVariable("E");
      assertSame(Set[].class, vType.asClass());
      assertEquals(Arrays.asList("E"), vType.getTypeVariableNames());
      vType = vType.resolveTypeVariable("E");
      assertSame(String.class, vType.asClass());
      assertTrue(vType.getTypeVariableNames().isEmpty());
   }
   
   public void testInvalidConstructionViaSubclass1() {
      try {
         new TypeReferenceGenericSubclass<String>();
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   public void testInvalidConstructionViaSubclass2() {
      try {
         new TypeReferenceComplex1<String, Object, HashMap<String, List<Object>>>();
         fail();
      } catch (IllegalArgumentException expected) {
      }
   }

   public void testConstructionViaSubclass1() {
      TypeReferenceSubclass tr = new TypeReferenceSubclass() { };
      assertSame(String.class, tr.asClass());
      assertSame(String.class, tr.getType());
   }
   
   public void testConstructionViaSubclassNoAnonymousType1() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as String.
      TypeReferenceSubclass tr = new TypeReferenceSubclass();
      assertSame(String.class, tr.asClass());
      assertSame(String.class, tr.getType());
   }

   public void testConstructionViaSubclass2() {
      TypeReferenceSubclass2 tr = new TypeReferenceSubclass2() { };
      assertSame(List.class, tr.asClass());
      // we'll assert a lot more in the next one...
   }

   public void testConstructionViaSubclassNoAnonymousType2() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as
      // List<? extends Serializable>.
      TypeReferenceSubclass2 tr = new TypeReferenceSubclass2();
      assertSame(List.class, tr.asClass());
      
      // now verify generic type information
      Type listType = tr.getType();
      assertTrue(listType instanceof TypeVariable);
      TypeVariable<?> tv = (TypeVariable<?>) listType;
      // all we have is a type variable -- actual type resolution would require
      // looking at additional generic type information for the intermediate
      // class TypeReferenceGenericSubclass
      assertEquals("T", tv.getName());
      assertSame(tr.getClass().getSuperclass(), tv.getGenericDeclaration());
      
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("E")); // wildcards can't be resolved
   }

   public void testConstructionViaSubclass3() {
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>() { };
      assertSame(Map.class, tr.asClass());
      // we'll assert a lot more in the next one...
   }

   public void testConstructionViaSubclassNoAnonymousType3() {
      // no need for anonymous subclass since this class statically
      // resolves type parameter (in "implements" clause) as
      // Map<V, List<K>>.
      TypeReferenceComplex2<?, ?> tr = new TypeReferenceComplex2<String, Object>();
      assertSame(Map.class, tr.asClass());
      
      // now verify generic type information
      Type mapType = tr.getType();
      assertTrue(mapType instanceof ParameterizedType);
      ParameterizedType parameterizedType = (ParameterizedType) mapType;
      assertNull(parameterizedType.getOwnerType());
      assertSame(Map.class, parameterizedType.getRawType());
      Type typeParams[] = parameterizedType.getActualTypeArguments();
      assertEquals(2, typeParams.length);
      // we don't actually have these resolved, but that's okay
      assertTrue(typeParams[0] instanceof TypeVariable);
      TypeVariable<?> typeVar = (TypeVariable<?>) typeParams[0];
      assertEquals("V", typeVar.getName());
      assertSame(TypeReferenceComplex2.class, typeVar.getGenericDeclaration());
      assertTrue(typeParams[1] instanceof ParameterizedType);
      parameterizedType = (ParameterizedType) typeParams[1];
      assertNull(parameterizedType.getOwnerType());
      assertSame(List.class, parameterizedType.getRawType());
      typeParams = parameterizedType.getActualTypeArguments();
      assertEquals(1, typeParams.length);
      assertTrue(typeParams[0] instanceof TypeVariable);
      typeVar = (TypeVariable<?>) typeParams[0];
      assertEquals("K", typeVar.getName());
      assertSame(TypeReferenceComplex2.class, typeVar.getGenericDeclaration());
      
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("K")); // this type variable can't be resolved
      TypeReference<?> listType = tr.resolveTypeVariable("V");
      assertSame(List.class, listType.asClass());
      assertEquals(Arrays.asList("E"), listType.getTypeVariableNames());
      assertNull(listType.resolveTypeVariable("E"));
   }

   public void testConstructionViaSubclass4() {
      TypeReferenceArraySubclass<String> tr = new TypeReferenceArraySubclass<String>() { };
      assertSame(String[].class, tr.asClass());
      
      // now verify generic type information
      Type arrayType = tr.getType();
      assertTrue(arrayType instanceof GenericArrayType);
      GenericArrayType gat = (GenericArrayType) arrayType;
      Type componentType = gat.getGenericComponentType();
      assertTrue(componentType instanceof TypeVariable);
      TypeVariable<?> componentTypeVar = (TypeVariable<?>) componentType;
      // this is as far as we can go...
      assertEquals("T", componentTypeVar.getName());
      assertSame(TypeReferenceArraySubclass.class, componentTypeVar.getGenericDeclaration());
      
      // look at type variable info
      assertTrue(tr.getTypeVariableNames().isEmpty());
   }
   
   public void testConstructionViaSubclass5() {
      TypeReferenceGenericSubclass<Collection<?>> tr = new TypeReferenceGenericSubclass<Collection<?>>() { };
      assertSame(Collection.class, tr.asClass());
      
      // now verify generic type information
      Type type = tr.getType();
      assertTrue(type instanceof TypeVariable);
      TypeVariable<?> typeVar = (TypeVariable<?>) type;
      // this is as far as we can go...
      assertEquals("T", typeVar.getName());
      assertSame(TypeReferenceGenericSubclass.class, typeVar.getGenericDeclaration());
      
      // look at type variable info
      assertEquals(Arrays.asList("E"), tr.getTypeVariableNames());
      assertNull(tr.resolveTypeVariable("E")); // can't resolve wildcard
   }
   
   public void testConstructionViaSubclass6() {
      // this behemoth results in a TypeReference for
      // Map<Collection<String>, List<Boolean[]>>
      TypeReferenceComplex2<Boolean[], Collection<String>> tr = new TypeReferenceComplex2<Boolean[], Collection<String>>() { };
      assertSame(Map.class, tr.asClass());
      
      // now verify generic type information
      Type type = tr.getType();
      assertTrue(type instanceof ParameterizedType);
      ParameterizedType pType = (ParameterizedType) type;
      assertNull(pType.getOwnerType());
      assertSame(Map.class, pType.getRawType());
      Type typeParams[] = pType.getActualTypeArguments();
      assertEquals(2, typeParams.length);
      Type keyType = typeParams[0];
      Type valType = typeParams[1];
      assertTrue(keyType instanceof TypeVariable);
      TypeVariable<?> typeVar = (TypeVariable<?>) keyType;
      // as far as we can go here...
      assertEquals("V", typeVar.getName());
      assertSame(TypeReferenceComplex2.class, typeVar.getGenericDeclaration());
      // now look at the other type parameter...
      assertTrue(valType instanceof ParameterizedType);
      pType = (ParameterizedType) valType;
      assertNull(pType.getOwnerType());
      assertSame(List.class, pType.getRawType());
      typeParams = pType.getActualTypeArguments();
      assertEquals(1, typeParams.length);
      assertTrue(typeParams[0] instanceof TypeVariable);
      typeVar = (TypeVariable<?>) typeParams[0];
      // and the end of the line here, too
      assertEquals("K", typeVar.getName());
      assertSame(TypeReferenceComplex2.class, typeVar.getGenericDeclaration());
      
      // look at type variable info
      assertEquals(Arrays.asList("K", "V"), tr.getTypeVariableNames());
      TypeReference<?> k = tr.resolveTypeVariable("K");
      assertSame(Collection.class, k.asClass());
      assertEquals(Arrays.asList("E"), k.getTypeVariableNames());
      TypeReference<?> e = k.resolveTypeVariable("E");
      assertSame(String.class, e.asClass());
      assertTrue(e.getTypeVariableNames().isEmpty());
      TypeReference<?> v = tr.resolveTypeVariable("V");
      assertSame(List.class, v.asClass());
      assertEquals(Arrays.asList("E"), v.getTypeVariableNames());
      e = v.resolveTypeVariable("E");
      assertSame(Boolean[].class, e.asClass());
      assertTrue(e.getTypeVariableNames().isEmpty());
   }
}
