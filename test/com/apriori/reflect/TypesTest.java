package com.apriori.reflect;

import static com.apriori.reflect.TypeTesting.COMPLEX_TYPE;
import static com.apriori.reflect.TypeTesting.EMPTY;
import static com.apriori.reflect.TypeTesting.GENERIC_ARRAY_TYPE;
import static com.apriori.reflect.TypeTesting.GENERIC_ARRAY_TYPE_VARIABLE;
import static com.apriori.reflect.TypeTesting.GENERIC_METHOD;
import static com.apriori.reflect.TypeTesting.PARAM_TYPE;
import static com.apriori.reflect.TypeTesting.TYPE_VAR_T;
import static com.apriori.reflect.TypeTesting.TYPE_VAR_Z;
import static com.apriori.reflect.TypeTesting.WILDCARD_ARRAY;
import static com.apriori.reflect.TypeTesting.WILDCARD_EXTENDS;
import static com.apriori.reflect.TypeTesting.WILDCARD_SUPER;
import static com.apriori.testing.MoreAsserts.assertNotEquals;
import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.apriori.reflect.TypeTesting.Dummy;
import com.apriori.reflect.TypeTesting.InvalidType;
import com.apriori.util.IoStreams;

import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class TypesTest {

   @Test public void isArray() {
      assertFalse(Types.isArray(void.class));
      assertFalse(Types.isArray(long.class));
      assertFalse(Types.isArray(Class.class));
      assertTrue(Types.isArray(Object[].class));
      assertFalse(Types.isArray(PARAM_TYPE));
      assertTrue(Types.isArray(GENERIC_ARRAY_TYPE));
      assertTrue(Types.isArray(GENERIC_ARRAY_TYPE_VARIABLE));
      assertFalse(Types.isArray(TYPE_VAR_Z));
      assertFalse(Types.isArray(TYPE_VAR_T));
      assertFalse(Types.isArray(WILDCARD_EXTENDS));
      assertFalse(Types.isArray(WILDCARD_SUPER));
      assertTrue(Types.isArray(WILDCARD_ARRAY));
      assertTrue(Types.isArray(COMPLEX_TYPE));
      
      assertThrows(NullPointerException.class, () -> Types.isArray(null));
      assertFalse(Types.isArray(InvalidType.INSTANCE));
   }

   @Test public void getComponentType() {
      assertNull(Types.getComponentType(void.class));
      assertNull(Types.getComponentType(long.class));
      assertNull(Types.getComponentType(Class.class));
      assertNull(Types.getComponentType(PARAM_TYPE));
      assertNull(Types.getComponentType(TYPE_VAR_Z));
      assertNull(Types.getComponentType(TYPE_VAR_T));
      assertNull(Types.getComponentType(WILDCARD_EXTENDS));
      assertNull(Types.getComponentType(WILDCARD_SUPER));
      assertEquals(Object.class, Types.getComponentType(Object[].class));
      
      // Map<String, Number>
      assertEquals(Types.newParameterizedType(Map.class, String.class, Number.class),
            Types.getComponentType(GENERIC_ARRAY_TYPE));
      
      // X  (where X -> Dummy#arrayTypeParam.<X>)
      Type expected = Types.getTypeVariable("X", Members.findMethod(Dummy.class, "arrayTypeParam")); 
      assertEquals(expected, Types.getComponentType(GENERIC_ARRAY_TYPE_VARIABLE));
      
      // ? extends List<T>  (where T -> Dummy.<T>)
      expected = Types.newExtendsWildcardType(
            Types.newParameterizedType(List.class, Types.getTypeVariable("T", Dummy.class)));
      assertEquals(expected, Types.getComponentType(WILDCARD_ARRAY));

      // ? extends Z  (where Z -> Dummy#complexType.<Z>)
      expected = Types.newExtendsWildcardType(
            Types.getTypeVariable("Z", Members.findMethod(Dummy.class, "complexType")));
      assertEquals(expected, Types.getComponentType(COMPLEX_TYPE));

      assertThrows(NullPointerException.class, () -> Types.getComponentType(null));
      assertNull(Types.getComponentType(InvalidType.INSTANCE));
   }

   @Test public void getRawType() {
      assertEquals(void.class, Types.getRawType(void.class));
      assertEquals(long.class, Types.getRawType(long.class));
      assertEquals(Class.class, Types.getRawType(Class.class));
      assertEquals(Object[].class, Types.getRawType(Object[].class));
      assertEquals(List.class, Types.getRawType(PARAM_TYPE));
      assertEquals(Map[].class, Types.getRawType(GENERIC_ARRAY_TYPE));
      assertEquals(CharSequence[].class, Types.getRawType(GENERIC_ARRAY_TYPE_VARIABLE));
      assertEquals(Map.class, Types.getRawType(TYPE_VAR_Z));
      assertEquals(Object.class, Types.getRawType(TYPE_VAR_T));
      assertEquals(Number.class, Types.getRawType(WILDCARD_EXTENDS));
      assertEquals(Object.class, Types.getRawType(WILDCARD_SUPER));
      assertEquals(List[].class, Types.getRawType(WILDCARD_ARRAY));
      assertEquals(Map[].class, Types.getRawType(COMPLEX_TYPE));

      assertThrows(NullPointerException.class, () -> Types.getRawType(null));
      assertThrows(UnknownTypeException.class, () -> Types.getRawType(InvalidType.INSTANCE));
   }
   
   @Test public void getActualTypeArguments() {
      assertArrayEquals(EMPTY, Types.getActualTypeArguments(Object.class));
      assertArrayEquals(EMPTY, Types.getActualTypeArguments(Map.class));
      assertArrayEquals(EMPTY, Types.getActualTypeArguments(WILDCARD_EXTENDS));
      assertArrayEquals(EMPTY, Types.getActualTypeArguments(TYPE_VAR_T));
      assertArrayEquals(EMPTY, Types.getActualTypeArguments(GENERIC_ARRAY_TYPE));
      assertArrayEquals(EMPTY, Types.getActualTypeArguments(InvalidType.INSTANCE));
      
      Type expectedArg = Types.newParameterizedType(Map.class, TYPE_VAR_T, Integer.class);
      assertArrayEquals(new Type[] { expectedArg }, Types.getActualTypeArguments(PARAM_TYPE));
      assertArrayEquals(new Type[] { TYPE_VAR_T, Integer.class },
            Types.getActualTypeArguments(expectedArg));

      assertThrows(NullPointerException.class, () -> Types.getTypeParameters(null));
   }
   
   @Test public void getTypeParameters() {
      TypeVariable<?> empty[] = new TypeVariable<?>[0];
      assertArrayEquals(empty, Types.getTypeParameters(Object.class));
      assertArrayEquals(empty, Types.getTypeParameters(WILDCARD_EXTENDS));
      assertArrayEquals(empty, Types.getTypeParameters(TYPE_VAR_T));
      assertArrayEquals(empty, Types.getTypeParameters(GENERIC_ARRAY_TYPE));
      assertArrayEquals(empty, Types.getTypeParameters(InvalidType.INSTANCE));
      
      assertArrayEquals(new TypeVariable<?>[] { TYPE_VAR_T }, Types.getTypeParameters(Dummy.class));
      assertArrayEquals(
            new TypeVariable<?>[] { Types.getTypeVariable("K", Map.class),
                  Types.getTypeVariable("V", Map.class) },
            Types.getTypeParameters(Map.class));

      assertThrows(NullPointerException.class, () -> Types.getTypeParameters(null));
   }
   
   @Test public void testEquals() {
      assertTrue(Types.equals(Class.class, Class.class));
      assertTrue(Types.equals(Object[].class, Types.getArrayType(Object.class)));
      assertTrue(Types.equals(
            Types.newGenericArrayType(
                  Types.newParameterizedType(Map.class, String.class, Number.class)),
            GENERIC_ARRAY_TYPE));
      assertTrue(Types.equals(Types.newExtendsWildcardType(Number.class), WILDCARD_EXTENDS));
      assertTrue(Types.equals(
            Types.newSuperWildcardType(Types.newParameterizedType(List.class, TYPE_VAR_T)),
            WILDCARD_SUPER));
      assertTrue(Types.equals(Types.getTypeVariable("T", Dummy.class), TYPE_VAR_T));
      
      assertFalse(Types.equals(Types.newParameterizedType(Map.class, String.class, Number.class),
            GENERIC_ARRAY_TYPE));
      assertFalse(Types.equals(Class.class, Class[].class));
      assertFalse(Types.equals(int.class, Integer.class));
      assertFalse(Types.equals(TYPE_VAR_T, TYPE_VAR_Z));
      assertFalse(Types.equals(WILDCARD_ARRAY, WILDCARD_EXTENDS));
      assertFalse(Types.equals(PARAM_TYPE, GENERIC_ARRAY_TYPE));
      
      assertThrows(NullPointerException.class, () -> Types.equals(Class.class, null));
      assertThrows(NullPointerException.class, () -> Types.equals(null, Class.class));
      assertThrows(NullPointerException.class, () -> Types.equals(null, null));
      assertFalse(Types.equals(InvalidType.INSTANCE, Object.class));
   }
   
   @Test public void testHashCode() {
      assertEquals(Types.hashCode(Class.class), Types.hashCode(Class.class));
      assertEquals(Types.hashCode(Object[].class),
            Types.hashCode(Types.getArrayType(Object.class)));
      assertEquals(Types.hashCode(
            Types.newGenericArrayType(
                  Types.newParameterizedType(Map.class, String.class, Number.class))),
            Types.hashCode(GENERIC_ARRAY_TYPE));
      assertEquals(Types.hashCode(Types.newExtendsWildcardType(Number.class)),
            Types.hashCode(WILDCARD_EXTENDS));
      assertEquals(Types.hashCode(
            Types.newSuperWildcardType(Types.newParameterizedType(List.class, TYPE_VAR_T))),
            Types.hashCode(WILDCARD_SUPER));
      assertEquals(Types.hashCode(Types.getTypeVariable("T", Dummy.class)),
            Types.hashCode(TYPE_VAR_T));
      
      // We can't assume all unequal types have unequal hash codes since there are bound to be
      // collisions. But we can test a few as a sanity check.
      assertNotEquals(Types.hashCode(Class.class), Types.hashCode(Class[].class));
      assertNotEquals(Types.hashCode(int.class), Types.hashCode(Integer.class));
      assertNotEquals(Types.hashCode(TYPE_VAR_T), Types.hashCode(TYPE_VAR_Z));
      assertNotEquals(Types.hashCode(WILDCARD_ARRAY), Types.hashCode(WILDCARD_EXTENDS));
      assertNotEquals(Types.hashCode(PARAM_TYPE), Types.hashCode(GENERIC_ARRAY_TYPE));
      
      assertThrows(NullPointerException.class, () -> Types.hashCode((Type) null));
      assertEquals(InvalidType.INSTANCE.hashCode(), Types.hashCode(InvalidType.INSTANCE));
   }
   
   @Test public void testToString() {
      assertEquals("void", Types.toString(void.class));
      assertEquals("long[]", Types.toString(long[].class));
      assertEquals("java.lang.Class", Types.toString(Class.class));
      assertEquals("java.lang.Object[]", Types.toString(Object[].class));
      assertEquals("com.apriori.reflect.TypeTesting.Dummy", Types.toString(Dummy.class));
      assertEquals("com.apriori.reflect.TypesTest$1", Types.toString(new Object() { }.getClass()));
      assertEquals("java.util.List<java.util.Map<T,java.lang.Integer>>",
            Types.toString(PARAM_TYPE));
      assertEquals("java.util.Map<java.lang.String,java.lang.Number>[]",
            Types.toString(GENERIC_ARRAY_TYPE));
      assertEquals("X[]", Types.toString(GENERIC_ARRAY_TYPE_VARIABLE));
      assertEquals("Z", Types.toString(TYPE_VAR_Z));
      assertEquals("T", Types.toString(TYPE_VAR_T));
      assertEquals("? extends java.lang.Number", Types.toString(WILDCARD_EXTENDS));
      assertEquals("? super java.util.List<T>", Types.toString(WILDCARD_SUPER));
      assertEquals("? extends java.util.List<T>[]", Types.toString(WILDCARD_ARRAY));
      assertEquals("? extends Z[]", Types.toString(COMPLEX_TYPE));
      
      assertThrows(NullPointerException.class, () -> Types.toString(null));
      assertEquals("INSTANCE", Types.toString(InvalidType.INSTANCE));
   }
   
   @Test public void getTypeVariable() {
      TypeVariable<Class<?>> varT = Types.getTypeVariable("T", Dummy.class);
      assertEquals("T", varT.getName());
      assertEquals(Dummy.class, varT.getGenericDeclaration());
      assertArrayEquals(new Type[] { Object.class }, varT.getBounds());
      
      TypeVariable<Method> varX = Types.getTypeVariable("X", GENERIC_METHOD);
      assertEquals("X", varX.getName());
      assertEquals(GENERIC_METHOD, varX.getGenericDeclaration());
      assertArrayEquals(new Type[] { Number.class }, varX.getBounds());

      TypeVariable<Method> varY = Types.getTypeVariable("Y", GENERIC_METHOD);
      assertEquals("Y", varY.getName());
      assertEquals(GENERIC_METHOD, varY.getGenericDeclaration());
      assertArrayEquals(new Type[] { Types.newParameterizedType(List.class, varT) },
            varY.getBounds());

      TypeVariable<Method> varZ = Types.getTypeVariable("Z", GENERIC_METHOD);
      assertEquals("Z", varZ.getName());
      assertEquals(GENERIC_METHOD, varZ.getGenericDeclaration());
      assertArrayEquals(
            new Type[] { Types.newParameterizedType(Map.class, varX, varY), Serializable.class,
                  Cloneable.class },
            varZ.getBounds());
      
      assertThrows(NullPointerException.class, () -> Types.getTypeVariable(null, Dummy.class));
      assertThrows(NullPointerException.class, () -> Types.getTypeVariable("T", null));
      assertThrows(IllegalArgumentException.class, () -> Types.getTypeVariable("T", Types.class));
   }
   
   @Test public void getTypeVariablesMap() {
      Map<String, TypeVariable<?>> expected = Collections.singletonMap("T", TYPE_VAR_T);
      assertEquals(expected, Types.getTypeVariablesAsMap(Dummy.class));

      expected = new HashMap<>();
      expected.put("X", Types.getTypeVariable("X", GENERIC_METHOD));
      expected.put("Y", Types.getTypeVariable("Y", GENERIC_METHOD));
      expected.put("Z", Types.getTypeVariable("Z", GENERIC_METHOD));
      assertEquals(expected, Types.getTypeVariablesAsMap(GENERIC_METHOD));

      assertEquals(Collections.emptyMap(), Types.getTypeVariablesAsMap(Types.class));

      assertThrows(NullPointerException.class, () -> Types.getTypeVariablesAsMap(null));
   }
   
   @Test public void isAssignable_toObject() {
      // anything assignable to Object
      assertIsAssignable(Integer.class, Object.class);
      assertIsAssignable(Object[].class, Object.class);
      assertIsAssignable(GENERIC_ARRAY_TYPE, Object.class);
      assertIsAssignable(GENERIC_ARRAY_TYPE_VARIABLE, Object.class);
      assertIsAssignable(WILDCARD_EXTENDS, Object.class);
      assertIsAssignable(WILDCARD_SUPER, Object.class);
      assertIsAssignable(WILDCARD_ARRAY, Object.class);
      assertIsAssignable(TYPE_VAR_T, Object.class);
      assertIsAssignable(TYPE_VAR_Z, Object.class);
      assertIsAssignable(PARAM_TYPE, Object.class);
      assertIsAssignable(InvalidType.INSTANCE, Object.class);
      // except primitives
      assertIsNotAssignable(boolean.class, Object.class);
      assertIsNotAssignable(byte.class, Object.class);
      assertIsNotAssignable(char.class, Object.class);
      assertIsNotAssignable(short.class, Object.class);
      assertIsNotAssignable(int.class, Object.class);
      assertIsNotAssignable(long.class, Object.class);
      assertIsNotAssignable(float.class, Object.class);
      assertIsNotAssignable(double.class, Object.class);
      assertIsNotAssignable(void.class, Object.class);
      // primitive arrays are assignable though
      assertIsAssignable(boolean[].class, Object.class);
      assertIsAssignable(byte[].class, Object.class);
      assertIsAssignable(char[].class, Object.class);
      assertIsAssignable(short[].class, Object.class);
      assertIsAssignable(int[].class, Object.class);
      assertIsAssignable(long[].class, Object.class);
      assertIsAssignable(float[].class, Object.class);
      assertIsAssignable(double[].class, Object.class);
   }
   
   @Test public void isAssignable_toObjectArray() {
      // if not an array, not assignable
      assertIsNotAssignable(Integer.class, Object[].class);
      assertIsNotAssignable(WILDCARD_EXTENDS, Object[].class);
      assertIsNotAssignable(WILDCARD_SUPER, Object[].class);
      assertIsNotAssignable(TYPE_VAR_T, Object[].class);
      assertIsNotAssignable(TYPE_VAR_Z, Object[].class);
      assertIsNotAssignable(PARAM_TYPE, Object[].class);
      assertIsNotAssignable(InvalidType.INSTANCE, Object[].class);
      // all array types are assignable to Object[]
      assertIsAssignable(Object[].class, Object[].class);
      assertIsAssignable(GENERIC_ARRAY_TYPE, Object[].class);
      assertIsAssignable(GENERIC_ARRAY_TYPE_VARIABLE, Object[].class);
      assertIsAssignable(WILDCARD_ARRAY, Object[].class);
      assertIsAssignable(fabricateTypeVarThatExtends(Integer[].class), Object[].class);
      // except primitive arrays
      assertIsNotAssignable(boolean[].class, Object[].class);
      assertIsNotAssignable(byte[].class, Object[].class);
      assertIsNotAssignable(char[].class, Object[].class);
      assertIsNotAssignable(short[].class, Object[].class);
      assertIsNotAssignable(int[].class, Object[].class);
      assertIsNotAssignable(long[].class, Object[].class);
      assertIsNotAssignable(float[].class, Object[].class);
      assertIsNotAssignable(double[].class, Object[].class);
   }
   
   @Test public void isAssignable_toClass() {
      // parameterized type can be assigned to raw type if raw types are compatible
      assertIsAssignable(Types.newParameterizedType(List.class, String.class), List.class);
      assertIsAssignable(Types.newParameterizedType(List.class, String.class), Collection.class);
      assertIsNotAssignable(Types.newParameterizedType(Collection.class, String.class), List.class);
      // similarly for generic array types
      assertIsAssignable(
            Types.newGenericArrayType(Types.newParameterizedType(List.class, String.class)),
            List[].class);
      assertIsAssignable(
            Types.newGenericArrayType(Types.newParameterizedType(List.class, String.class)),
            Collection[].class);
      assertIsNotAssignable(
            Types.newGenericArrayType(Types.newParameterizedType(Collection.class, String.class)),
            List[].class);
      // primitives only assignable from themselves
      Class<?> primitives[] = { boolean.class, byte.class, char.class, short.class, int.class,
            long.class, float.class, double.class, void.class };
      for (int i = 0; i < primitives.length; i++) {
         for (int j = 0; j < primitives.length; j++) {
            assertEquals(i == j, Types.isAssignableStrict(primitives[i], primitives[j]));
         }
      }
      // other raw types assignable if compatible (just like Class.isAssignableFrom)
      assertIsAssignable(String.class, CharSequence.class);
      assertIsAssignable(Integer.class, Number.class);
      assertIsNotAssignable(String.class, Number.class);
      assertIsNotAssignable(Double.class, CharSequence.class);
      assertIsAssignable(List.class, List.class);
      assertIsAssignable(List.class, Collection.class);
      assertIsNotAssignable(List.class, String.class);
      // same for simple array types
      assertIsAssignable(String[].class, CharSequence[].class);
      assertIsAssignable(Integer[].class, Number[].class);
      assertIsNotAssignable(String[].class, Number[].class);
      assertIsNotAssignable(Double[].class, CharSequence[].class);
      assertIsAssignable(List[].class, List[].class);
      assertIsAssignable(List[].class, Collection[].class);
      assertIsNotAssignable(List[].class, String[].class);
      assertIsNotAssignable(String[].class, String[][].class);
      assertIsNotAssignable(String[][].class, String[].class);
      // cannot assign from super-bounded wildcard (except to Object.class)
      assertIsNotAssignable(WILDCARD_SUPER, List.class);
      assertIsNotAssignable(WILDCARD_SUPER, Collection.class);
      // but can from extends-bounded wildcards
      assertIsAssignable(WILDCARD_EXTENDS, Number.class);
      assertIsAssignable(WILDCARD_ARRAY, List[].class);
      // and type variables, too
      assertIsAssignable(TYPE_VAR_Z, Map.class);
      assertIsAssignable(TYPE_VAR_Z, Serializable.class);
      assertIsAssignable(TYPE_VAR_Z, Cloneable.class);
      assertIsNotAssignable(TYPE_VAR_Z, String.class);
      assertIsNotAssignable(TYPE_VAR_Z, List.class);
   }

   interface SimpleStringList extends List<String> {
   }
   
   interface TypedStringList<T> extends List<String> {
   }
   
   @Test public void isAssignable_toParameterizedType() {
      // equal types are assignable
      assertIsAssignable(PARAM_TYPE, PARAM_TYPE);
      // compatible types, too
      Type listOfString = Types.newParameterizedType(List.class, String.class);
      assertIsAssignable(Types.newParameterizedType(ArrayList.class, String.class), listOfString);
      assertIsAssignable(listOfString, Types.newParameterizedType(Iterable.class, String.class));
      assertIsNotAssignable(Types.newParameterizedType(Iterable.class, String.class), listOfString);
      assertIsNotAssignable(listOfString, Types.newParameterizedType(ArrayList.class, String.class));
      // array types cannot be parameterized
      assertIsNotAssignable(List[].class, PARAM_TYPE);
      assertIsNotAssignable(Map[].class, PARAM_TYPE);
      assertIsNotAssignable(Types.newGenericArrayType(
            Types.newParameterizedType(Map.class, TYPE_VAR_T, Integer.class)),
            PARAM_TYPE);
      // raw types cannot be assigned to parameterized types (without unchecked cast)
      assertIsNotAssignable(Collection.class, PARAM_TYPE);
      assertIsNotAssignable(List.class, PARAM_TYPE);
      assertIsNotAssignable(ArrayList.class, PARAM_TYPE);
      assertIsNotAssignable(Types.newParameterizedType(List.class, Map.class), PARAM_TYPE);
      // without wildcard as actual type arg, require exact match on parameters
      assertIsNotAssignable(Types.newParameterizedType(List.class,
            Types.newParameterizedType(Map.class,
                  Types.getTypeVariable("T", Dummy.class), Number.class)),
            PARAM_TYPE);
      assertIsNotAssignable(Types.newParameterizedType(List.class,
            Types.newParameterizedType(Map.class,
                  Types.newExtendsWildcardType(TYPE_VAR_T), Integer.class)),
            PARAM_TYPE);
      assertIsAssignable(Types.newParameterizedType(List.class,
            Types.newParameterizedType(Map.class, TYPE_VAR_T, Integer.class)),
            PARAM_TYPE);
      // if super-type includes raw type reference to generic type, we lose type information and
      // would require unchecked casts, so not allowed
      assertIsNotAssignable(TypedStringList.class, listOfString);
      // but any type arg suffices
      assertIsAssignable(Types.newParameterizedType(TypedStringList.class, Object.class),
            listOfString);
      assertIsAssignable(Types.newParameterizedType(TypedStringList.class, Number.class),
            listOfString);
      // simpler case: "from" raw type needs no type args
      assertIsAssignable(SimpleStringList.class, listOfString);
      // must be exact match
      assertIsNotAssignable(SimpleStringList.class,
            Types.newParameterizedType(List.class, CharSequence.class));
      // unless assignment target has wildcard bound
      assertIsAssignable(SimpleStringList.class,
            Types.newParameterizedType(List.class,
                  Types.newExtendsWildcardType(CharSequence.class)));
      // covariance when nested parameterized types have wildcard arguments
      // List<? extends Map<?, ? extends Number>>
      Type listOfWildcardMap = Types.newParameterizedType(List.class,
            Types.newExtendsWildcardType(Types.newParameterizedType(Map.class,
                  Types.extendsAnyWildcard(), Types.newExtendsWildcardType(Number.class))));
      assertIsAssignable(PARAM_TYPE, listOfWildcardMap);
      assertIsAssignable(Types.newParameterizedType(ArrayList.class,
            Types.newParameterizedType(HashMap.class, String.class, Double.class)),
            listOfWildcardMap);
      // not assignable if type args don't match wildcard bounds
      assertIsNotAssignable(Types.newParameterizedType(ArrayList.class,
            Types.newParameterizedType(HashMap.class, String.class, String.class)),
            listOfWildcardMap);
      // if type argument has no wildcard, but nested type arg does, still invariant
      Type listOfMapWildcards = Types.newParameterizedType(List.class,
            Types.newParameterizedType(Map.class,
                  Types.extendsAnyWildcard(), Types.extendsAnyWildcard()));
      assertIsNotAssignable(PARAM_TYPE, listOfMapWildcards);
      assertIsNotAssignable(Types.newParameterizedType(List.class,
            Types.newParameterizedType(HashMap.class,
                  Types.extendsAnyWildcard(), Types.extendsAnyWildcard())),
            listOfMapWildcards);
      assertIsAssignable(Types.newParameterizedType(ArrayList.class,
            Types.newParameterizedType(Map.class,
                  Types.extendsAnyWildcard(), Types.extendsAnyWildcard())),
            listOfMapWildcards);
   }
   
   @Test public void isAssignable_toGenericArrayType() {
      // TODO!!!
   }

   @Test public void isAssignable_toWildcardType() {
      // nothing can be assigned to an extends wildcard
      assertIsNotAssignable(Number.class, WILDCARD_EXTENDS);
      assertIsNotAssignable(Integer.class, WILDCARD_EXTENDS);
      // unless they are the exact *same* type
      assertIsAssignable(WILDCARD_EXTENDS, WILDCARD_EXTENDS);
      // merely *equal* doesn't cut it
      WildcardType w = Types.newExtendsWildcardType(Number.class);
      assertEquals(w, WILDCARD_EXTENDS);
      assertIsNotAssignable(w, WILDCARD_EXTENDS);
      
      // but super wildcard can be assigned from the bound or a sub-type thereof
      assertIsAssignable(Types.newParameterizedType(List.class, TYPE_VAR_T), WILDCARD_SUPER);
      assertIsAssignable(Types.newParameterizedType(ArrayList.class, TYPE_VAR_T), WILDCARD_SUPER);
      // but nothing above
      assertIsNotAssignable(Types.newParameterizedType(Collection.class, TYPE_VAR_T),
            WILDCARD_SUPER);
      assertIsNotAssignable(Types.newParameterizedType(Iterable.class, TYPE_VAR_T), WILDCARD_SUPER);
   }

   @Test public void isAssignable_toTypeVariable() {
      // only a value of the *same* type may be assigned (e.g. reference to same type variable or
      // wildcard/type var that extends it)
      assertIsNotAssignable(fabricateTypeVarThatExtends(Object.class), TYPE_VAR_T);
      assertIsNotAssignable(TYPE_VAR_Z, TYPE_VAR_T);
      assertIsAssignable(Types.getTypeVariable("T", Dummy.class), TYPE_VAR_T);
      assertIsAssignable(fabricateTypeVar("T", Dummy.class, Object.class), TYPE_VAR_T);
   }
   
   /**
    * Asserts the given {@code from} type is assignable to the {@code to} type. This will also check
    * that wildcards and type variables that extend the {@code from} are also assignable.
    */
   private void assertIsAssignable(Type from, Type to) {
      assertTrue(Types.isAssignableStrict(from, to));
      // if we can, check that wildcard or type var that extends given type is also assignable
      // (can't have a wildcard or type var that extends a primitive or other wildcard)
      if (!Types.isPrimitive(from) && !(from instanceof WildcardType)) {
         assertTrue(Types.isAssignableStrict(Types.newExtendsWildcardType(from), to));
         assertTrue(Types.isAssignableStrict(fabricateTypeVarThatExtends(from), to));
      }
      // if we can, do similar check for arrays of the given types (cannot create arrays of
      // wildcard type, void, or unknown type impls)
      if (from != void.class && !(from instanceof WildcardType) && from != InvalidType.INSTANCE
            && to != void.class && !(to instanceof WildcardType)) {
         Type fromArray = from instanceof Class
               ? Types.getArrayType((Class<?>) from)
               : Types.newGenericArrayType(from);
         Type toArray = to instanceof Class
               ? Types.getArrayType((Class<?>) to)
               : Types.newGenericArrayType(to);
         assertTrue(Types.isAssignableStrict(fromArray, toArray));
         assertTrue(Types.isAssignableStrict(Types.newExtendsWildcardType(fromArray), toArray));
         assertTrue(Types.isAssignableStrict(fabricateTypeVarThatExtends(fromArray), toArray));
      }
   }

   /**
    * Asserts the given {@code from} type is <em>not</em> assignable to the {@code to} type. This
    * will also check that wildcards and type variables that extend the {@code from} are also not
    * assignable.
    */
   private void assertIsNotAssignable(Type from, Type to) {
      assertFalse(Types.isAssignableStrict(from, to));
      if (Types.isPrimitive(from) || from instanceof WildcardType) {
         // no more checks can be made (can't have a wildcard or type var that
         // extends a primitive or wildcard type)
         return;
      }
      assertFalse(Types.isAssignableStrict(Types.newExtendsWildcardType(from), to));
      assertFalse(Types.isAssignableStrict(fabricateTypeVarThatExtends(from), to));
   }

   private Type fabricateTypeVarThatExtends(Type... t) {
      return fabricateTypeVar("X", TypesTest.class, t);
   }

   private Type fabricateTypeVar(String name, GenericDeclaration decl, Type... t) {
      return new TypeVariable<GenericDeclaration>() {
         @Override
         public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
         }

         @Override
         public Annotation[] getAnnotations() {
            return new Annotation[0];
         }

         @Override
         public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
         }

         @Override
         public Type[] getBounds() {
            return t.clone();
         }

         @Override
         public GenericDeclaration getGenericDeclaration() {
            return decl;
         }

         @Override
         public String getName() {
            return name;
         }

         @Override
         public AnnotatedType[] getAnnotatedBounds() {
            return new AnnotatedType[0];
         }
         
         @Override
         public boolean equals(Object o) {
            return o instanceof Type && Types.equals(this, (Type) o);
         }
         
         @Override
         public int hashCode() {
            return Types.hashCode(this);
         }
         
         @Override
         public String toString() {
            return Types.toString(this);
         }
      };
   }

   @Test public void resolveSuperType() {
      // TODO!!!
   }

   @Test public void resolveTypeVariable() {
      // TODO!!!
   }

   @Test public void resolveType() {
      // TODO!!!
   }

   @Test public void replaceTypeVariable() {
      // TODO!!!
   }

   @Test public void replaceTypeVariables() {
      // TODO!!!
   }

   static class Owner<A, B, C> {
      static class StaticOwned<T> {
      }
      class Owned {
      }
      class GenericOwned<T> {
      }
      class GenericOwnedOuterBound<T extends Map<A, B>> {
      }
      
      StaticOwned<String> method1() {
         return null;
      }
      GenericOwned<Number> method2() {
         return null;
      }
      Owned method3() {
         return null;
      }
      GenericOwnedOuterBound<LinkedHashMap<A, B>> method4() {
         return null;
      }

      static final Type METHOD1_RETURN_TYPE =
            Members.findMethod(Owner.class, "method1").getGenericReturnType();
      static final Type METHOD2_RETURN_TYPE =
            Members.findMethod(Owner.class, "method2").getGenericReturnType();
      static final Type METHOD3_RETURN_TYPE =
            Members.findMethod(Owner.class, "method3").getGenericReturnType();
      static final Type METHOD4_RETURN_TYPE =
            Members.findMethod(Owner.class, "method4").getGenericReturnType();
   }

   @Test public void newParameterizedType_noOwner() {
      Type param = Types.newParameterizedType(Map.class, TYPE_VAR_T, Integer.class); 
      ParameterizedType paramType = Types.newParameterizedType(List.class, param);
      assertNull(paramType.getOwnerType());
      assertSame(List.class, paramType.getRawType());
      assertArrayEquals(new Type[] { param }, paramType.getActualTypeArguments());
      
      ParameterizedType paramTypeInner =
            Types.newParameterizedType(Owner.StaticOwned.class, String.class);
      assertSame(Owner.class, paramTypeInner.getOwnerType());
      assertSame(Owner.StaticOwned.class, paramTypeInner.getRawType());
      assertArrayEquals(new Type[] { String.class }, paramTypeInner.getActualTypeArguments());
      
      // walks and talks like JRE GenericArrayTypeImpl:
      assertEquals(PARAM_TYPE, paramType);
      assertEquals(Owner.METHOD1_RETURN_TYPE, paramTypeInner);
      assertEquals(paramType, PARAM_TYPE);
      assertEquals(paramTypeInner, Owner.METHOD1_RETURN_TYPE);
      assertEquals(PARAM_TYPE.hashCode(), paramType.hashCode());
      assertEquals(Owner.METHOD1_RETURN_TYPE.hashCode(), paramTypeInner.hashCode());
      
      // can't create type w/ no args and no parameterized owner
      assertThrows(IllegalArgumentException.class, () -> Types.newParameterizedType(Object.class));
      // can't create type w/ wrong number of args
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(List.class, String.class, String.class));
      // can't create type w/ args for non-static enclosed type with raw owner
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(Owner.GenericOwned.class, String.class));
      // can't use primitives as type arguments
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(Owner.GenericOwned.class, int.class));
      
      assertThrows(NullPointerException.class, () -> Types.newParameterizedType(null));
      assertThrows(NullPointerException.class, () -> Types.newParameterizedType(null, TYPE_VAR_T));
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType(null, (List<Type>) null));
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType(List.class, (Type[]) null));
   }
   
   @Test public void newParameterizedType_withOwner() {
      TypeVariable<Class<?>> typeParams[] = Types.getTypeParameters(Owner.class);
      // Owner<A, B, C>
      ParameterizedType owner = Types.newParameterizedType(Owner.class, typeParams);

      // Owner<A, B, C>.GenericOwned<Number>
      ParameterizedType paramType =
            Types.newParameterizedType(owner, Owner.GenericOwned.class, Number.class);
      assertSame(owner, paramType.getOwnerType());
      assertSame(Owner.GenericOwned.class, paramType.getRawType());
      assertArrayEquals(new Type[] { Number.class }, paramType.getActualTypeArguments());

      // Owner<A, B, C>.GenericOwned<Number>
      ParameterizedType paramTypeNoArg = Types.newParameterizedType(owner, Owner.Owned.class);
      assertSame(owner, paramTypeNoArg.getOwnerType());
      assertSame(Owner.Owned.class, paramTypeNoArg.getRawType());
      assertArrayEquals(EMPTY, paramTypeNoArg.getActualTypeArguments());

      // Owner<A, B, C>.GenericOwnedOuterBound<LinkedHashMap<A, B>>
      Type param = Types.newParameterizedType(LinkedHashMap.class, typeParams[0], typeParams[1]);
      ParameterizedType paramTypeComplex =
            Types.newParameterizedType(owner, Owner.GenericOwnedOuterBound.class, param);
      assertSame(owner, paramTypeComplex.getOwnerType());
      assertSame(Owner.GenericOwnedOuterBound.class, paramTypeComplex.getRawType());
      assertArrayEquals(new Type[] { param }, paramTypeComplex.getActualTypeArguments());

      // walks and talks like JRE GenericArrayTypeImpl:
      assertEquals(Owner.METHOD2_RETURN_TYPE, paramType);
      assertEquals(Owner.METHOD3_RETURN_TYPE, paramTypeNoArg);
      assertEquals(Owner.METHOD4_RETURN_TYPE, paramTypeComplex);
      assertEquals(paramType, Owner.METHOD2_RETURN_TYPE);
      assertEquals(paramTypeNoArg, Owner.METHOD3_RETURN_TYPE);
      assertEquals(paramTypeComplex, Owner.METHOD4_RETURN_TYPE);
      assertEquals(Owner.METHOD2_RETURN_TYPE.hashCode(), paramType.hashCode());
      assertEquals(Owner.METHOD3_RETURN_TYPE.hashCode(), paramTypeNoArg.hashCode());
      assertEquals(Owner.METHOD4_RETURN_TYPE.hashCode(), paramTypeComplex.hashCode());

      // Can't specify owner for static type
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(owner,  Owner.StaticOwned.class, Number.class));
      // Can't specify owner for top-level type
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(owner,  List.class, Number.class));
      // Can't create type with wrong owner type
      ParameterizedType wrongOwner = Types.newParameterizedType(List.class, String.class);
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(wrongOwner, Owner.Owned.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(wrongOwner, Owner.GenericOwned.class, Number.class));
      
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType((ParameterizedType) null, List.class, Number.class));
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType(owner, null, Number.class));
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType(owner, Owner.Owned.class, (Type) null));
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType(owner, Owner.Owned.class, (Type[]) null));
      assertThrows(NullPointerException.class,
            () -> Types.newParameterizedType(owner, Owner.Owned.class, (List<Type>) null));
   }
   
   @Test public void newParameterizedType_boundsChecks() {
      class OneBound<T extends Number> {
      }
      class MultipleBounds<T extends ClassLoader & Serializable & Cloneable> {
      }
      class GoodBound extends ClassLoader implements Serializable, Cloneable {
         private static final long serialVersionUID = 1;
      }
      class AlmostGoodBound extends ClassLoader implements Serializable {
         private static final long serialVersionUID = 1;
      }
      class RecursiveBound<T extends Comparable<T>, L extends List<T>> {
      }
      class BadBound implements Comparable<Integer> {
         @Override public int compareTo(Integer o) {
            return 0;
         }
      }
      
      // type var with a single bound
      ParameterizedType paramType = Types.newParameterizedType(OneBound.class, Integer.class);
      assertNull(paramType.getOwnerType());
      assertSame(OneBound.class, paramType.getRawType());
      assertArrayEquals(new Type[] { Integer.class }, paramType.getActualTypeArguments());
      // bad bound
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(OneBound.class, Object.class));

      // type var with several bounds
      paramType = Types.newParameterizedType(MultipleBounds.class, GoodBound.class);
      assertNull(paramType.getOwnerType());
      assertSame(MultipleBounds.class, paramType.getRawType());
      assertArrayEquals(new Type[] { GoodBound.class }, paramType.getActualTypeArguments());
      // doesn't satisfy all bounds
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(MultipleBounds.class, ClassLoader.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(MultipleBounds.class, AlmostGoodBound.class));
      
      // type vars with "recursive" bounds (self-referencing)
      ParameterizedType listParam = Types.newParameterizedType(List.class, Integer.class);
      paramType = Types.newParameterizedType(RecursiveBound.class, Integer.class, listParam);
      assertNull(paramType.getOwnerType());
      assertSame(RecursiveBound.class, paramType.getRawType());
      assertArrayEquals(new Type[] { Integer.class, listParam },
            paramType.getActualTypeArguments());
      // T doesn't match its bounds
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(RecursiveBound.class, BadBound.class,
                  Types.newParameterizedType(List.class, BadBound.class)));
      // L doesn't match its bounds
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(RecursiveBound.class, Integer.class,
                  Types.newParameterizedType(Collection.class, Integer.class)));
      // bounds don't agree on T
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(RecursiveBound.class, Double.class, listParam));

      // must be able to match references to owner type's parameters
      ParameterizedType owner =
            Types.newParameterizedType(Owner.class, String.class, Number.class, Boolean.class);
      Type param =
            Types.newParameterizedType(ConcurrentSkipListMap.class, String.class, Number.class);
      ParameterizedType paramTypeComplex =
            Types.newParameterizedType(owner, Owner.GenericOwnedOuterBound.class, param);
      assertSame(owner, paramTypeComplex.getOwnerType());
      assertSame(Owner.GenericOwnedOuterBound.class, paramTypeComplex.getRawType());
      assertArrayEquals(new Type[] { param }, paramTypeComplex.getActualTypeArguments());
      
      // and reject cases where param doesn't match correctly:
      // ConcurrentSkipListMap<String, Integer> not assignable to Map<String, Number>
      Type badParam =
            Types.newParameterizedType(ConcurrentSkipListMap.class, String.class, Integer.class);
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(owner, Owner.GenericOwnedOuterBound.class, badParam));
   }
   
   @Test public void newGenericArrayType() {
      Type varX = Types.getTypeVariable("X", Members.findMethod(Dummy.class, "arrayTypeParam"));
      GenericArrayType arrayType1 = Types.newGenericArrayType(varX);
      assertEquals(varX, arrayType1.getGenericComponentType());

      Type paramType = Types.newParameterizedType(Map.class, String.class, Number.class);
      GenericArrayType arrayType2 = Types.newGenericArrayType(paramType);
      assertEquals(paramType, arrayType2.getGenericComponentType());

      // walks and talks like JRE GenericArrayTypeImpl:
      assertEquals(GENERIC_ARRAY_TYPE_VARIABLE, arrayType1);
      assertEquals(GENERIC_ARRAY_TYPE, arrayType2);
      assertEquals(arrayType1, GENERIC_ARRAY_TYPE_VARIABLE);
      assertEquals(arrayType2, GENERIC_ARRAY_TYPE);
      assertEquals(GENERIC_ARRAY_TYPE_VARIABLE.hashCode(), arrayType1.hashCode());
      assertEquals(GENERIC_ARRAY_TYPE.hashCode(), arrayType2.hashCode());
            
      assertThrows(IllegalArgumentException.class, () -> Types.newGenericArrayType(Class.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newGenericArrayType(WILDCARD_EXTENDS));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newGenericArrayType(InvalidType.INSTANCE));
      assertThrows(NullPointerException.class, () -> Types.newGenericArrayType(null));
   }
   
   @Test public void newExtendsWildcard() {
      WildcardType wildcard = Types.newExtendsWildcardType(Number.class);
      assertArrayEquals(new Type[] { Number.class }, wildcard.getUpperBounds());
      assertArrayEquals(EMPTY, wildcard.getLowerBounds());

      // walks and talks like JRE WildcardTypeImpl
      assertEquals(WILDCARD_EXTENDS, wildcard);
      assertEquals(wildcard, WILDCARD_EXTENDS);
      assertEquals(WILDCARD_EXTENDS.hashCode(), wildcard.hashCode());
      
      // accepts any type other than a primitive or another wildcard
      wildcard = Types.newExtendsWildcardType(GENERIC_ARRAY_TYPE);
      assertArrayEquals(new Type[] { GENERIC_ARRAY_TYPE }, wildcard.getUpperBounds());
      assertArrayEquals(EMPTY, wildcard.getLowerBounds());
      wildcard = Types.newExtendsWildcardType(TYPE_VAR_Z);
      assertArrayEquals(new Type[] { TYPE_VAR_Z }, wildcard.getUpperBounds());
      assertArrayEquals(EMPTY, wildcard.getLowerBounds());
      wildcard = Types.newExtendsWildcardType(InvalidType.INSTANCE);
      assertArrayEquals(new Type[] { InvalidType.INSTANCE }, wildcard.getUpperBounds());
      assertArrayEquals(EMPTY, wildcard.getLowerBounds());
      
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(void.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newExtendsWildcardType(boolean.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(byte.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(char.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(short.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(int.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(long.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newExtendsWildcardType(float.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newExtendsWildcardType(double.class));

      assertThrows(IllegalArgumentException.class,
            () -> Types.newExtendsWildcardType(WILDCARD_EXTENDS));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newExtendsWildcardType(WILDCARD_SUPER));
      
      assertThrows(NullPointerException.class, () -> Types.newExtendsWildcardType(null));
   }
   
   @Test public void newSuperWildcard() {
      Type bound = Types.newParameterizedType(List.class, TYPE_VAR_T);
      WildcardType wildcard = Types.newSuperWildcardType(bound);
      assertArrayEquals(new Type[] { Object.class }, wildcard.getUpperBounds());
      assertArrayEquals(new Type[] { bound }, wildcard.getLowerBounds());

      // walks and talks like JRE WildcardTypeImpl
      assertEquals(WILDCARD_SUPER, wildcard);
      assertEquals(wildcard, WILDCARD_SUPER);
      assertEquals(WILDCARD_SUPER.hashCode(), wildcard.hashCode());

      // accepts any type other than a primitive or another wildcard
      wildcard = Types.newSuperWildcardType(GENERIC_ARRAY_TYPE);
      assertArrayEquals(new Type[] { Object.class }, wildcard.getUpperBounds());
      assertArrayEquals(new Type[] { GENERIC_ARRAY_TYPE }, wildcard.getLowerBounds());
      wildcard = Types.newSuperWildcardType(TYPE_VAR_Z);
      assertArrayEquals(new Type[] { Object.class }, wildcard.getUpperBounds());
      assertArrayEquals(new Type[] { TYPE_VAR_Z }, wildcard.getLowerBounds());
      wildcard = Types.newSuperWildcardType(InvalidType.INSTANCE);
      assertArrayEquals(new Type[] { Object.class }, wildcard.getUpperBounds());
      assertArrayEquals(new Type[] { InvalidType.INSTANCE }, wildcard.getLowerBounds());

      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(void.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(boolean.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(byte.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(char.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(short.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(int.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(long.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(float.class));
      assertThrows(IllegalArgumentException.class, () -> Types.newSuperWildcardType(double.class));

      assertThrows(IllegalArgumentException.class,
            () -> Types.newSuperWildcardType(WILDCARD_EXTENDS));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newSuperWildcardType(WILDCARD_SUPER));
      
      assertThrows(NullPointerException.class, () -> Types.newSuperWildcardType(null));
   }
   
   @Test public void getArrayType() {
      assertEquals(boolean[].class, Types.getArrayType(boolean.class));
      assertEquals(byte[].class, Types.getArrayType(byte.class));
      assertEquals(char[].class, Types.getArrayType(char.class));
      assertEquals(short[].class, Types.getArrayType(short.class));
      assertEquals(int[].class, Types.getArrayType(int.class));
      assertEquals(long[].class, Types.getArrayType(long.class));
      assertEquals(float[].class, Types.getArrayType(float.class));
      assertEquals(double[].class, Types.getArrayType(double.class));
      assertEquals(Void[].class, Types.getArrayType(Void.class));
      assertEquals(List[].class, Types.getArrayType(List.class));
      assertEquals(double[][].class, Types.getArrayType(double[].class));
      assertEquals(int[][][].class, Types.getArrayType(int[][].class));
      assertEquals(Object[][][][][][][][].class, Types.getArrayType(Object[][][][][][][].class));
      assertEquals(Types[].class, Types.getArrayType(Types.class));
      assertEquals(TypesTest[].class, Types.getArrayType(TypesTest.class));

      // try with customer class loaders
      class Ldr extends ClassLoader {
         Ldr() {
         }

         Ldr(ClassLoader parent) {
            super(parent);
         }
      }
      Object o1 = Proxy.newProxyInstance(new Ldr(), new Class<?>[] { List.class },
            (proxy, method, args) -> null);
      assertEquals(o1.getClass(), Types.getArrayType(o1.getClass()).getComponentType());
      Object o2 = Proxy.newProxyInstance(new Ldr(Types.class.getClassLoader()),
            new Class<?>[] { List.class }, (proxy, method, args) -> null);
      assertEquals(o2.getClass(), Types.getArrayType(o2.getClass()).getComponentType());
      
      // try with some local/anonymous classes, too
      assertEquals(Ldr[].class, Types.getArrayType(Ldr.class));
      Object o3 = new Cloneable() {};
      assertEquals(o3.getClass(), Types.getArrayType(o3.getClass()).getComponentType());
      
      // failure cases
      assertThrows(IllegalArgumentException.class, () -> Types.getArrayType(void.class));
      assertThrows(NullPointerException.class, () -> Types.getArrayType(null));
   }
   
   @Test public void getArrayType_multipleClassLoaders() throws Exception {
      byte typesTestClassBytes[] = IoStreams.toByteArray(TypesTest.class.getClassLoader()
            .getResourceAsStream(TypesTest.class.getName().replace('.', '/') + ".class"));
      class Ldr extends ClassLoader {
         Class<?> getUniqueClass() {
            return defineClass(TypesTest.class.getName(), typesTestClassBytes, 0,
                  typesTestClassBytes.length);
         }
      }
      
      Class<?> class1 = new Ldr().getUniqueClass();
      Class<?> class2 = new Ldr().getUniqueClass();
      Class<?> class3 = new Ldr().getUniqueClass();
      // We now have three different classes, but all have the same name
      assertNotEquals(class1, class2);
      assertNotEquals(class2, class3);
      assertNotEquals(class1, class3);
      assertEquals(class1.getName(), class2.getName());
      assertEquals(class2.getName(), class3.getName());
      // Make sure this doesn't trip up Types.getArrayType()
      Class<?> arrayClass1 = Types.getArrayType(class1);
      Class<?> arrayClass2 = Types.getArrayType(class2);
      Class<?> arrayClass3 = Types.getArrayType(class3);
      assertNotEquals(arrayClass1, arrayClass2);
      assertNotEquals(arrayClass2, arrayClass3);
      assertNotEquals(arrayClass1, arrayClass3);
      assertSame(class1, arrayClass1.getComponentType());
      assertSame(class2, arrayClass2.getComponentType());
      assertSame(class3, arrayClass3.getComponentType());
   }
}
