package com.bluegosling.reflect;

import static com.bluegosling.reflect.TypeTesting.COMPLEX_TYPE;
import static com.bluegosling.reflect.TypeTesting.EMPTY;
import static com.bluegosling.reflect.TypeTesting.GENERIC_ARRAY_TYPE;
import static com.bluegosling.reflect.TypeTesting.GENERIC_ARRAY_TYPE_VARIABLE;
import static com.bluegosling.reflect.TypeTesting.GENERIC_METHOD;
import static com.bluegosling.reflect.TypeTesting.PARAM_TYPE;
import static com.bluegosling.reflect.TypeTesting.TYPE_VAR_T;
import static com.bluegosling.reflect.TypeTesting.TYPE_VAR_Z;
import static com.bluegosling.reflect.TypeTesting.WILDCARD_ARRAY;
import static com.bluegosling.reflect.TypeTesting.WILDCARD_EXTENDS;
import static com.bluegosling.reflect.TypeTesting.WILDCARD_SUPER;
import static com.bluegosling.testing.MoreAsserts.assertNotEquals;
import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bluegosling.collections.AssociativeArrayList;
import com.bluegosling.reflect.TypeTesting.Dummy;
import com.bluegosling.reflect.TypeTesting.InvalidType;

import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiPredicate;

public class TypesTest {

   @Test public void isInterface() {
      assertFalse(Types.isInterface(void.class));
      assertFalse(Types.isInterface(long.class));
      assertFalse(Types.isInterface(Class.class));
      assertFalse(Types.isInterface(Object[].class));
      assertTrue(Types.isInterface(List.class));
      assertTrue(Types.isInterface(Override.class)); // annotations are interfaces, too
      assertTrue(Types.isInterface(PARAM_TYPE));
      assertFalse(Types.isInterface(GENERIC_ARRAY_TYPE));
      assertFalse(Types.isInterface(GENERIC_ARRAY_TYPE_VARIABLE));
      assertFalse(Types.isInterface(TYPE_VAR_Z));
      assertFalse(Types.isInterface(TYPE_VAR_T));
      assertFalse(Types.isInterface(WILDCARD_EXTENDS));
      assertFalse(Types.isInterface(WILDCARD_SUPER));
      assertFalse(Types.isInterface(WILDCARD_ARRAY));
      assertFalse(Types.isInterface(COMPLEX_TYPE));
      // wildcards and type variables aren't known types, so isInterface returns false
      // regardless of the bounds
      assertFalse(Types.isInterface(Types.newExtendsWildcardType(List.class)));
      assertFalse(Types.isInterface(fabricateTypeVarThatExtends(List.class)));
      
      assertThrows(NullPointerException.class, () -> Types.isInterface(null));
      assertThrows(UnknownTypeException.class, () -> Types.isInterface(InvalidType.INSTANCE));      
   }

   @Test public void isAnnotation() {
      assertTrue(Types.isAnnotation(Override.class));
      
      assertFalse(Types.isAnnotation(void.class));
      assertFalse(Types.isAnnotation(long.class));
      assertFalse(Types.isAnnotation(Class.class));
      assertFalse(Types.isAnnotation(Object[].class));
      assertFalse(Types.isAnnotation(List.class));
      assertFalse(Types.isAnnotation(PARAM_TYPE));
      assertFalse(Types.isAnnotation(GENERIC_ARRAY_TYPE));
      assertFalse(Types.isAnnotation(GENERIC_ARRAY_TYPE_VARIABLE));
      assertFalse(Types.isAnnotation(TYPE_VAR_Z));
      assertFalse(Types.isAnnotation(TYPE_VAR_T));
      assertFalse(Types.isAnnotation(WILDCARD_EXTENDS));
      assertFalse(Types.isAnnotation(WILDCARD_SUPER));
      assertFalse(Types.isAnnotation(WILDCARD_ARRAY));
      assertFalse(Types.isAnnotation(COMPLEX_TYPE));
      // wildcards and type variables aren't known types, so isAnnotation returns false
      // regardless of the bounds
      assertFalse(Types.isAnnotation(Types.newExtendsWildcardType(List.class)));
      assertFalse(Types.isAnnotation(fabricateTypeVarThatExtends(List.class)));
      
      assertThrows(NullPointerException.class, () -> Types.isAnnotation(null));
      assertThrows(UnknownTypeException.class, () -> Types.isAnnotation(InvalidType.INSTANCE));      
   }
   
   @Test public void isEnum() {
      assertTrue(Types.isEnum(RetentionPolicy.class));
      // if a wildcard or type variable has an enum as upper-bound, the type must be an enum
      assertTrue(Types.isEnum(Types.newExtendsWildcardType(RetentionPolicy.class)));
      assertTrue(Types.isEnum(Types.newExtendsWildcardType(Enum.class)));
      assertTrue(Types.isEnum(Types.newExtendsWildcardType(
            Types.newParameterizedType(Enum.class, Types.extendsAnyWildcard()))));
      assertTrue(Types.isEnum(fabricateTypeVarThatExtends(RetentionPolicy.class)));
      assertTrue(Types.isEnum(fabricateTypeVarThatExtends(Enum.class)));
      assertTrue(Types.isEnum(fabricateTypeVarThatExtends(
            Types.newParameterizedType(Enum.class, Types.extendsAnyWildcard()))));
      
      assertFalse(Types.isEnum(void.class));
      assertFalse(Types.isEnum(long.class));
      assertFalse(Types.isEnum(Class.class));
      assertFalse(Types.isEnum(Object[].class));
      assertFalse(Types.isEnum(List.class));
      assertFalse(Types.isEnum(Override.class));
      assertFalse(Types.isEnum(PARAM_TYPE));
      assertFalse(Types.isEnum(GENERIC_ARRAY_TYPE));
      assertFalse(Types.isEnum(GENERIC_ARRAY_TYPE_VARIABLE));
      assertFalse(Types.isEnum(TYPE_VAR_Z));
      assertFalse(Types.isEnum(TYPE_VAR_T));
      assertFalse(Types.isEnum(WILDCARD_EXTENDS));
      assertFalse(Types.isEnum(WILDCARD_SUPER));
      assertFalse(Types.isEnum(WILDCARD_ARRAY));
      assertFalse(Types.isEnum(COMPLEX_TYPE));
      
      assertThrows(NullPointerException.class, () -> Types.isEnum(null));
      assertThrows(UnknownTypeException.class, () -> Types.isEnum(InvalidType.INSTANCE));      
   }
   
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
      assertEquals(void.class, Types.getErasure(void.class));
      assertEquals(long.class, Types.getErasure(long.class));
      assertEquals(Class.class, Types.getErasure(Class.class));
      assertEquals(Object[].class, Types.getErasure(Object[].class));
      assertEquals(List.class, Types.getErasure(PARAM_TYPE));
      assertEquals(Map[].class, Types.getErasure(GENERIC_ARRAY_TYPE));
      assertEquals(CharSequence[].class, Types.getErasure(GENERIC_ARRAY_TYPE_VARIABLE));
      assertEquals(Map.class, Types.getErasure(TYPE_VAR_Z));
      assertEquals(Object.class, Types.getErasure(TYPE_VAR_T));
      assertEquals(Number.class, Types.getErasure(WILDCARD_EXTENDS));
      assertEquals(Object.class, Types.getErasure(WILDCARD_SUPER));
      assertEquals(List[].class, Types.getErasure(WILDCARD_ARRAY));
      assertEquals(Map[].class, Types.getErasure(COMPLEX_TYPE));

      assertThrows(NullPointerException.class, () -> Types.getErasure(null));
      assertThrows(UnknownTypeException.class, () -> Types.getErasure(InvalidType.INSTANCE));
   }
   
   // helper interface with methods of various return types to
   // make sure values returned by getZeroValue() are appropriate
   private interface TestInterface {
      // primitives
      void voidMethod();

      int intMethod();

      short shortMethod();

      byte byteMethod();

      long longMethod();

      char charMethod();

      double doubleMethod();

      float floatMethod();

      boolean booleanMethod();

      // objects
      String stringMethod();

      Object objectMethod();

      // objects that box primitives
      Void voidObjMethod();

      Integer intObjMethod();

      Short shortObjMethod();

      Byte byteObjMethod();

      Long longObjMethod();

      Character charObjMethod();

      Double doubleObjMethod();

      Float floatObjMethod();

      Boolean booleanObjMethod();
   }

   @Test public void getZeroValue() {
      // use an actual proxy so we not only check that returned values are what we
      // expect but further verify that expected values are appropriate and correct
      // (and won't generate NullPointerExceptions or ClassCastExceptions)
      TestInterface proxy = (TestInterface) Proxy.newProxyInstance(
            TestInterface.class.getClassLoader(),
            new Class<?>[] { TestInterface.class }, new InvocationHandler() {
               @Override
               public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                  // method under test!
                  return Types.getZeroValue(method.getReturnType());
               }
            });

      // this just makes sure no exception occurs (no return value to inspect)
      proxy.voidMethod();
      // zeroes
      assertEquals(0, proxy.intMethod());
      assertEquals(0, proxy.shortMethod());
      assertEquals(0, proxy.byteMethod());
      assertEquals(0, proxy.longMethod());
      assertEquals(0, proxy.charMethod());
      assertEquals(0.0, proxy.doubleMethod(), 0.0 /* exactly zero */);
      assertEquals(0.0F, proxy.floatMethod(), 0.0F /* exactly zero */);
      // false
      assertFalse(proxy.booleanMethod());

      // the rest are objects and can(should) be null
      assertNull(proxy.voidObjMethod());
      assertNull(proxy.intObjMethod());
      assertNull(proxy.shortObjMethod());
      assertNull(proxy.byteObjMethod());
      assertNull(proxy.longObjMethod());
      assertNull(proxy.charObjMethod());
      assertNull(proxy.doubleObjMethod());
      assertNull(proxy.floatObjMethod());
      assertNull(proxy.booleanObjMethod());

      assertNull(proxy.objectMethod());
      assertNull(proxy.stringMethod());
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
   
   @Test public void isSameType() {
      assertTrue(Types.isSameType(Class.class, Class.class));
      assertTrue(Types.isSameType(Object[].class, Types.getArrayType(Object.class)));
      assertTrue(Types.isSameType(
            Types.newGenericArrayType(
                  Types.newParameterizedType(Map.class, String.class, Number.class)),
            GENERIC_ARRAY_TYPE));
      assertTrue(Types.isSameType(Types.getTypeVariable("T", Dummy.class), TYPE_VAR_T));

      // two wildcards are not same type, even if same instance
      assertFalse(Types.isSameType(WILDCARD_EXTENDS, WILDCARD_EXTENDS));
      assertFalse(Types.isSameType(Types.newExtendsWildcardType(Number.class), WILDCARD_EXTENDS));
      assertFalse(Types.isSameType(WILDCARD_SUPER, WILDCARD_SUPER));
      assertFalse(Types.isSameType(
            Types.newSuperWildcardType(Types.newParameterizedType(List.class, TYPE_VAR_T)),
            WILDCARD_SUPER));

      assertFalse(Types.isSameType(Types.newParameterizedType(Map.class, String.class, Number.class),
            GENERIC_ARRAY_TYPE));
      assertFalse(Types.isSameType(Class.class, Class[].class));
      assertFalse(Types.isSameType(int.class, Integer.class));
      assertFalse(Types.isSameType(TYPE_VAR_T, TYPE_VAR_Z));
      assertFalse(Types.isSameType(WILDCARD_ARRAY, WILDCARD_EXTENDS));
      assertFalse(Types.isSameType(PARAM_TYPE, GENERIC_ARRAY_TYPE));
      
      assertThrows(NullPointerException.class, () -> Types.isSameType(Class.class, null));
      assertThrows(NullPointerException.class, () -> Types.isSameType(null, Class.class));
      assertThrows(NullPointerException.class, () -> Types.isSameType(null, null));
      assertFalse(Types.isSameType(InvalidType.INSTANCE, Object.class));
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
      assertEquals("com.bluegosling.reflect.TypeTesting.Dummy", Types.toString(Dummy.class));
      assertEquals("com.bluegosling.reflect.TypesTest$2", Types.toString(new Object() { }.getClass()));
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
      // primitives cannot be assigned "strictly", but non-strict allows boxing conversion
      assertIsAssignableButNotStrict(boolean.class, Object.class);
      assertIsAssignableButNotStrict(byte.class, Object.class);
      assertIsAssignableButNotStrict(char.class, Object.class);
      assertIsAssignableButNotStrict(short.class, Object.class);
      assertIsAssignableButNotStrict(int.class, Object.class);
      assertIsAssignableButNotStrict(long.class, Object.class);
      assertIsAssignableButNotStrict(float.class, Object.class);
      assertIsAssignableButNotStrict(double.class, Object.class);
      assertIsAssignableButNotStrict(void.class, Object.class);
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
      // primitives only strictly assignable from themselves
      Class<?> primitives[] = { boolean.class, byte.class, char.class, short.class, int.class,
            long.class, float.class, double.class, void.class };
      for (int i = 0; i < primitives.length; i++) {
         for (int j = 0; j < primitives.length; j++) {
            if (i == j) {
               assertIsAssignable(primitives[i], primitives[j]);
            } else if (Types.getAllSupertypes(primitives[i]).contains(primitives[j])) {
               // strict assignment does not check primitive subtyping rules
               assertIsAssignableButNotStrict(primitives[i], primitives[j], true);
            } else {
               assertIsNotAssignable(primitives[i], primitives[j]);
            }
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
      // to not "strictly" assignable (assignment conversion, however, allows such a cast)
      // (PARAM_TYPE is a List, so it can be assiged fr
      assertIsAssignableButNotStrict(List.class, PARAM_TYPE);
      assertIsAssignableButNotStrict(ArrayList.class, PARAM_TYPE);
      // but parameterized types still invariant (e.g. unchecked conversion does not allow
      // assigning List<Map> to List<Map<T, Integer>>)
      assertIsNotAssignable(Types.newParameterizedType(List.class, Map.class), PARAM_TYPE);
      assertIsNotAssignable(Collection.class, PARAM_TYPE); // narrowing conversion not allowed
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
      // would require unchecked casts
      assertIsAssignableButNotStrict(TypedStringList.class, listOfString);
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

   // NB: There is no isAssignable_toGenericArrayType because all of the other tests
   // implicitly test that

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
      doAssertAssignable(from, to, Types::isAssignable);
      doAssertAssignable(from, to, Types::isAssignableStrict);
      if (!Types.equals(from, to)) {
         doAssertAssignable(from, to, Types::isSubtype);
         doAssertAssignable(from, to, Types::isSubtypeStrict);
      } else {
         // can't do the whole doAssertNotAssignable battery, but we know if they are equal
         // then they cannot be a subtype of one another
         assertFalse(Types.isSubtype(from, to));
         assertFalse(Types.isSubtypeStrict(from, to));
         assertFalse(Types.isSubtype(to, from));
         assertFalse(Types.isSubtypeStrict(to, from));
      }
   }

   private void assertIsAssignableButNotStrict(Type from, Type to) {
      assertIsAssignableButNotStrict(from, to, false);
   }

   private void assertIsAssignableButNotStrict(Type from, Type to, boolean isPrimitiveSubtype) {
      doAssertAssignable(from, to, Types::isAssignable);
      doAssertNotAssignable(from, to, Types::isAssignableStrict);
      // sub-typing does not use assignment conversions (like boxing/unboxing, unchecked, etc)
      // if not strictly assignable, not a subtype either
      if (isPrimitiveSubtype) {
         doAssertAssignable(from, to, Types::isSubtype);
      } else {
         doAssertNotAssignable(from, to, Types::isSubtype);
      }
      doAssertNotAssignable(from, to, Types::isSubtypeStrict);
   }

   private void doAssertAssignable(Type from, Type to, BiPredicate<Type, Type> a) {
      assertTrue(a.test(from, to));
      // if we can, check that wildcard or type var that extends given type is also assignable
      // (can't have a wildcard or type var that extends a primitive or other wildcard)
      if (!Types.isPrimitive(from) && !(from instanceof WildcardType)) {
         assertTrue(a.test(Types.newExtendsWildcardType(from), to));
         assertTrue(a.test(fabricateTypeVarThatExtends(from), to));
      }
      // if we can, do similar check for arrays of the given types (cannot create arrays of
      // wildcard type, void, or unknown type impls)
      if (from != void.class && !(from instanceof WildcardType) && from != InvalidType.INSTANCE
            && to != void.class && !(to instanceof WildcardType)) {
         Type fromArray = Types.getArrayType(from);
         Type toArray = Types.getArrayType(to);
         if ((Types.getErasure(from).isPrimitive() || Types.getErasure(to).isPrimitive())
               && from != to) {
            // assignment conversion allows boxing/unboxing, but not on array components
            // also, primitive subtyping rules do not extend to array covariance
            assertFalse(a.test(fromArray, toArray));
            assertFalse(a.test(Types.newExtendsWildcardType(fromArray), toArray));
            assertFalse(a.test(fabricateTypeVarThatExtends(fromArray), toArray));
         } else {
            assertTrue(a.test(fromArray, toArray));
            assertTrue(a.test(Types.newExtendsWildcardType(fromArray), toArray));
            assertTrue(a.test(fabricateTypeVarThatExtends(fromArray), toArray));
         }
      }
   }

   /**
    * Asserts the given {@code from} type is <em>not</em> assignable to the {@code to} type. This
    * will also check that wildcards and type variables that extend the {@code from} are also not
    * assignable.
    */
   private void assertIsNotAssignable(Type from, Type to) {
      doAssertNotAssignable(from, to, Types::isAssignable);
      doAssertNotAssignable(from, to, Types::isAssignableStrict);
      doAssertNotAssignable(from, to, Types::isSubtype);
      doAssertNotAssignable(from, to, Types::isSubtypeStrict);
   }
   
   private void doAssertNotAssignable(Type from, Type to, BiPredicate<Type, Type> a) {
      assertFalse(a.test(from, to));
      // if we can, do similar check for arrays of the given types (cannot create arrays of
      // wildcard type, void, or unknown type impls)
      if (from != void.class && !(from instanceof WildcardType) && from != InvalidType.INSTANCE
            && to != void.class && !(to instanceof WildcardType)) {
         Type fromArray = Types.getArrayType(from);
         Type toArray = Types.getArrayType(to);
         assertFalse(a.test(fromArray, toArray));
         assertFalse(a.test(Types.newExtendsWildcardType(fromArray), toArray));
         assertFalse(a.test(fabricateTypeVarThatExtends(fromArray), toArray));
      }
      if (Types.isPrimitive(from) || from instanceof WildcardType) {
         // no more checks can be made (can't have a wildcard or type var that
         // extends a primitive or wildcard type)
         return;
      }
      assertFalse(a.test(Types.newExtendsWildcardType(from), to));
      assertFalse(a.test(fabricateTypeVarThatExtends(from), to));
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

   @Test public void resolveSupertype() {
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
   
   @Test public void getDirectSupertypes_primitives() {
      assertEquals(Collections.emptySet(), asSet(Types.getDirectSupertypes(boolean.class)));
      assertEquals(asSet(short.class), asSet(Types.getDirectSupertypes(byte.class)));
      assertEquals(asSet(int.class), asSet(Types.getDirectSupertypes(short.class)));
      assertEquals(asSet(int.class), asSet(Types.getDirectSupertypes(char.class)));
      assertEquals(asSet(long.class), asSet(Types.getDirectSupertypes(int.class)));
      assertEquals(asSet(float.class), asSet(Types.getDirectSupertypes(long.class)));
      assertEquals(asSet(double.class), asSet(Types.getDirectSupertypes(float.class)));
      assertEquals(Collections.emptySet(), asSet(Types.getDirectSupertypes(double.class)));
      assertEquals(Collections.emptySet(), asSet(Types.getDirectSupertypes(void.class)));
   }

   // test types for testing getDirectSupertypes
   
   interface TestSerialized extends Serializable, Cloneable {
   }
   interface GenericInterface<E> extends Collection<E> {
   }
   interface GenericInterfaceWithoutSuperinterface<E> {
   }
   
   abstract class GenericClass<E> extends AbstractCollection<E>
   implements Queue<E>, Comparable<GenericClass<? extends E>> {
   }
   abstract class GenericClassWithoutSuperclass<E> implements Iterable<E> {
   }
   class GenericClassWithoutsupertype<K, V> {
   }
   
   class ClassWithoutSupertype {
   }
   abstract class ClassWithoutSuperclass implements Cloneable, List<String> {
   }
   class ClassWithSuperclass extends ClassWithoutSupertype {
   }
   abstract class ClassWithSupertypes extends ClassWithSuperclass implements Comparator<Number> {
   }

   @Test public void getDirectSupertypes_rawTypes() {
      // Interface with super-interfaces
      assertEquals(asSet(Serializable.class, Cloneable.class),
            asSet(Types.getDirectSupertypes(TestSerialized.class)));
      // Interface without super-interfaces - direct supertype is Object
      assertEquals(Collections.singleton(Object.class),
            asSet(Types.getDirectSupertypes(Cloneable.class)));
      // Raw type use of generic interface - direct supertype will be raw, too
      assertEquals(Collections.singleton(Collection.class),
            asSet(Types.getDirectSupertypes(GenericInterface.class)));
      // Raw type use of generic class
      assertEquals(asSet(AbstractCollection.class, Queue.class, Comparable.class),
            asSet(Types.getDirectSupertypes(GenericClass.class)));
      // Non-generic classes
      assertEquals(
            asSet(ClassWithSuperclass.class,
                  Types.newParameterizedType(Comparator.class, Number.class)),
            asSet(Types.getDirectSupertypes(ClassWithSupertypes.class)));
      assertEquals(Collections.singleton(ClassWithoutSupertype.class),
            asSet(Types.getDirectSupertypes(ClassWithSuperclass.class)));
      assertEquals(Collections.singleton(Object.class),
            asSet(Types.getDirectSupertypes(ClassWithoutSupertype.class)));
      // Object has no supertypes
      assertEquals(Collections.emptySet(), asSet(Types.getDirectSupertypes(Object.class)));
   }

   @Test public void getDirectSupertypes_wildcardsAndTypeVariables() {
      assertEquals(Collections.emptySet(), asSet(Types.getDirectSupertypes(Object.class)));
      
      // TODO!!!
   }

   @Test public void getDirectSupertypes_arrays() {
      // TODO!!!
   }

   @Test public void getDirectSupertypes_parameterizedTypes() {
      // Generic interface
      assertEquals(
            asSet(GenericInterface.class,
                  Types.newParameterizedType(Collection.class, String.class)),
            asSet(Types.getDirectSupertypes(
                  Types.newParameterizedType(GenericInterface.class, String.class))));
      // Generic class
      assertEquals(
            asSet(GenericClass.class,
                  Types.newParameterizedType(AbstractCollection.class, Number.class),
                  Types.newParameterizedType(Queue.class, Number.class),
                  Types.newParameterizedType(Comparable.class,
                        Types.newParameterizedType(GenericClass.class,
                              Types.newExtendsWildcardType(Number.class)))),
            asSet(Types.getDirectSupertypes(
                  Types.newParameterizedType(GenericClass.class, Number.class))));
   }
   
   @Test public void getGenericSuperclass() {
      // TODO!!!
   }

   @Test public void getGenericInterfaces() {
      // TODO!!!
   }

   @Test public void getAllSupertypes_primitives() {
      assertEquals(Collections.emptySet(), Types.getAllSupertypes(boolean.class));
      assertEquals(asSet(short.class, int.class, long.class, float.class, double.class),
            Types.getAllSupertypes(byte.class));
      assertEquals(asSet(int.class, long.class, float.class, double.class),
            Types.getAllSupertypes(short.class));
      assertEquals(asSet(int.class, long.class, float.class, double.class),
            Types.getAllSupertypes(char.class));
      assertEquals(asSet(long.class, float.class, double.class), Types.getAllSupertypes(int.class));
      assertEquals(asSet(float.class, double.class), Types.getAllSupertypes(long.class));
      assertEquals(asSet(double.class), Types.getAllSupertypes(float.class));
      assertEquals(Collections.emptySet(), Types.getAllSupertypes(double.class));
      assertEquals(Collections.emptySet(), Types.getAllSupertypes(void.class));
   }
   
   @Test public void getAllSupertypes() {
      // ensure that getAllSupertypes agrees with getDirectSupertypes
      checkAllSupertypes(Class.class);
      checkAllSupertypes(Object[].class);
      checkAllSupertypes(List.class);
      checkAllSupertypes(Override.class);
      checkAllSupertypes(RetentionPolicy.class);
      checkAllSupertypes(PARAM_TYPE);
      checkAllSupertypes(GENERIC_ARRAY_TYPE);
      checkAllSupertypes(GENERIC_ARRAY_TYPE_VARIABLE);
      checkAllSupertypes(TYPE_VAR_Z);
      checkAllSupertypes(TYPE_VAR_T);
      checkAllSupertypes(WILDCARD_EXTENDS);
      checkAllSupertypes(WILDCARD_SUPER);
      checkAllSupertypes(WILDCARD_ARRAY);
      checkAllSupertypes(COMPLEX_TYPE);
   }
   
   private void checkAllSupertypes(Type type) {
      // check that Types.getAllSupertypes agrees with recursive calls to Types.getDirectSupertypes
      Set<Type> supertypes = new LinkedHashSet<>();
      findAllSupertypesRecursive(type, supertypes);
      
      assertEquals(supertypes, Types.getAllSupertypes(type));
   }

   private void findAllSupertypesRecursive(Type type, Set<Type> soFar) {
      Type[] directSupertypes = Types.getDirectSupertypes(type);
      for (Type s : directSupertypes) {
         soFar.add(s);
         // we always recurse, even if soFar.add(...) returned false (meaning we've seen this type
         // already), to ensure there are no cycles (e.g. we should never see stack overflow).
         findAllSupertypesRecursive(s, soFar);
      }
   }
   
   @Test public void getLeastUpperBounds() {
      // nothing in common but root type
      assertEquals(Collections.singleton(Object.class),
            asSet(Types.getLeastUpperBounds(Object.class, TYPE_VAR_T, String.class)));
      
      assertEquals(
            asSet(Types.newParameterizedType(Comparable.class, Types.extendsAnyWildcard()),
                  Serializable.class),
            asSet(Types.getLeastUpperBounds(Integer.class, String.class)));
      
      // one type is raw, so least upper bound is also raw (e.g. no type args)
      assertEquals(asSet(Collection.class),
            asSet(Types.getLeastUpperBounds(GenericClass.class,
                  Types.newParameterizedType(List.class, String.class))));

      // induce a complex wildcard in parameterized upper bound
      assertEquals(
            asSet(Types.newParameterizedType(Collection.class,
                  new Types.WildcardTypeImpl(Arrays.asList(
                        Types.newParameterizedType(Comparable.class, Types.extendsAnyWildcard()),
                        Serializable.class), Collections.emptyList()))),
            asSet(Types.getLeastUpperBounds(
                  Types.newParameterizedType(GenericClass.class, Double.class),
                  Types.newParameterizedType(List.class, String.class))));
      
      WildcardType extendsCharSequence = new Types.WildcardTypeImpl(
            Arrays.asList(Serializable.class, CharSequence.class), Collections.emptyList());
      assertEquals(
            asSet(Types.newParameterizedType(AbstractCollection.class, extendsCharSequence),
                  Types.newParameterizedType(Deque.class, extendsCharSequence),
                  Cloneable.class, Serializable.class),
            asSet(Types.getLeastUpperBounds(
                  Types.newParameterizedType(LinkedList.class, String.class),
                  Types.newParameterizedType(ArrayDeque.class, StringBuilder.class))));
      
      // primitive types have no shared bound with references types
      for (Type primitive : Arrays.asList(boolean.class, byte.class, short.class, char.class,
            int.class, long.class, float.class, double.class, void.class)) {
         assertEquals(Collections.emptySet(),
               asSet(Types.getLeastUpperBounds(primitive, Object.class)));
      }
   }

   @Test public void getLeastUpperBounds_glbProducesBadIntersection() {
      // TODO
   }
   
   @Test public void getLeastUpperBounds_recursiveType() {
      // TODO
   }
   
   @Test public void isFunctionalInterface() {
      // TODO
   }

   @Test public void isFunctionalInterface_twoMethodsThatCanEqualWithParameterizations() {
      // TODO
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
   
   static class MutuallyRecursiveComparable1 implements Comparable<MutuallyRecursiveComparable2> {
      @Override public int compareTo(MutuallyRecursiveComparable2 o) {
         return 0;
      }
   }
   static class MutuallyRecursiveComparable2 implements Comparable<MutuallyRecursiveComparable1> {
      @Override public int compareTo(MutuallyRecursiveComparable1 o) {
         return 0;
      }
   }
   
   @Test public void newParameterizedType_boundsChecksWildcardsAndNestedParameterizedTypes() {
      class Test<T extends List<String>> {
      }
      @SuppressWarnings("serial")
      class TestList extends ArrayList<String> {
      }
      
      class AnotherTest<T extends List<HashMap<String, ? extends Number>>> {
      }

      // successful bounds checks
      Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(TestList.class));
      Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(
            Types.newParameterizedType(AssociativeArrayList.class,
                  Types.extendsAnyWildcard(), Types.extendsAnyWildcard())));
      Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(List.class));
      Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(
            Types.newParameterizedType(List.class, Types.extendsAnyWildcard())));

      Types.newParameterizedType(AnotherTest.class, Types.newExtendsWildcardType(
            Types.newParameterizedType(ArrayList.class, Types.extendsAnyWildcard())));
      Types.newParameterizedType(AnotherTest.class, Types.newExtendsWildcardType(
            Types.newParameterizedType(ArrayList.class, Types.newExtendsWildcardType(
                  Types.newParameterizedType(Map.class, String.class,
                        Types.newExtendsWildcardType(Double.class))))));

      // unsuccessful checks:
      // Collection<Number> conflicts with bound List<String> 
      assertThrows(IllegalArgumentException.class, () ->
            Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(
                  Types.newParameterizedType(Collection.class, Number.class))));
      // AssociativeArrayList<Object, ?> conflicts with bound List<String> 
      assertThrows(IllegalArgumentException.class, () ->
            Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(
                  Types.newParameterizedType(AssociativeArrayList.class, Object.class,
                        Types.extendsAnyWildcard()))));
      // List<? extends Comparator> conflicts with bound List<String> 
      assertThrows(IllegalArgumentException.class, () ->
            Types.newParameterizedType(Test.class, Types.newExtendsWildcardType(
                  Types.newParameterizedType(List.class,
                        Types.newExtendsWildcardType(Comparator.class)))));
      // In Map bound, key type ? extends List<?> conflicts with bound String 
      assertThrows(IllegalArgumentException.class, () ->
            Types.newParameterizedType(AnotherTest.class, Types.newExtendsWildcardType(
                  Types.newParameterizedType(ArrayList.class, Types.newExtendsWildcardType(
                        Types.newParameterizedType(Map.class,
                              Types.newExtendsWildcardType(Types.newParameterizedType(List.class,
                                    Types.extendsAnyWildcard())),
                              Types.newExtendsWildcardType(Double.class)))))));
      // In Map bound, ? extends LinkedHashMap conflicts with (invariant) HashMap
      assertThrows(IllegalArgumentException.class, () ->
            Types.newParameterizedType(AnotherTest.class, Types.newExtendsWildcardType(
                  Types.newParameterizedType(ArrayList.class, Types.newExtendsWildcardType(
                        Types.newParameterizedType(LinkedHashMap.class, String.class,
                              Types.newExtendsWildcardType(Double.class)))))));
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
      class MutuallyRecursiveBound<T extends Comparable<S>, S extends Comparable<T>> {
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

      // type var with mutually recursive bounds (cyclic)
      paramType = Types.newParameterizedType(MutuallyRecursiveBound.class,
            String.class, String.class);
      assertNull(paramType.getOwnerType());
      assertSame(MutuallyRecursiveBound.class, paramType.getRawType());
      assertArrayEquals(new Type[] { String.class, String.class },
            paramType.getActualTypeArguments());

      paramType = Types.newParameterizedType(MutuallyRecursiveBound.class,
            MutuallyRecursiveComparable1.class, MutuallyRecursiveComparable2.class);
      assertNull(paramType.getOwnerType());
      assertSame(MutuallyRecursiveBound.class, paramType.getRawType());
      assertArrayEquals(
            new Type[] {
                  MutuallyRecursiveComparable1.class, MutuallyRecursiveComparable2.class },
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
      // mutual bounds don't hold
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(MutuallyRecursiveBound.class, String.class,
                  Integer.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(MutuallyRecursiveBound.class, BadBound.class,
                  Integer.class));
      assertThrows(IllegalArgumentException.class,
            () -> Types.newParameterizedType(MutuallyRecursiveBound.class,
                  MutuallyRecursiveComparable1.class,
                  Types.newParameterizedType(Comparable.class,
                        MutuallyRecursiveComparable2.class)));

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

      assertThrows(NullPointerException.class, () -> Types.getArrayType(null));
      assertThrows(IllegalArgumentException.class, () -> Types.getArrayType(void.class));
   }
   
   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   private static <T> Set<T> asSet(T... array) {
      return new LinkedHashSet<>(Arrays.asList(array));
   }
}
