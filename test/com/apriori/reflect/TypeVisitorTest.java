package com.apriori.reflect;

import static com.apriori.reflect.TypeTesting.GENERIC_ARRAY_TYPE;
import static com.apriori.reflect.TypeTesting.PARAM_TYPE;
import static com.apriori.reflect.TypeTesting.TYPE_VAR_T;
import static com.apriori.reflect.TypeTesting.WILDCARD_EXTENDS;
import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.apriori.reflect.TypeTesting.InvalidType;

import org.junit.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class TypeVisitorTest {
   
   @Test public void defaults() {
      {
         TypeVisitor<Object, Void> visitor = new TypeVisitor<Object, Void>() { };
         // defaultAction returns null unless overridden
         assertNull(visitor.visit(Class.class, null));
         assertNull(visitor.visit(PARAM_TYPE, null));
         assertNull(visitor.visit(GENERIC_ARRAY_TYPE, null));
         assertNull(visitor.visit(WILDCARD_EXTENDS, null));
         assertNull(visitor.visit(TYPE_VAR_T, null));
         // unknown type throws
         assertThrows(UnknownTypeException.class, () -> visitor.visit(InvalidType.INSTANCE, null));
      }
      {
         // double-check that all other methods call defaultAction
         AtomicInteger count = new AtomicInteger();
         Object r = new Object();
         TypeVisitor<Object, Void> visitor = new TypeVisitor<Object, Void>() {
            @Override public Object defaultAction(Type type, Void v) {
               count.incrementAndGet();
               return r;
            }
         };
         assertSame(r, visitor.visit(Class.class, null));
         assertEquals(1, count.get());
         assertSame(r, visitor.visit(PARAM_TYPE, null));
         assertEquals(2, count.get());
         assertSame(r, visitor.visit(GENERIC_ARRAY_TYPE, null));
         assertEquals(3, count.get());
         assertSame(r, visitor.visit(WILDCARD_EXTENDS, null));
         assertEquals(4, count.get());
         assertSame(r, visitor.visit(TYPE_VAR_T, null));
         assertEquals(5, count.get());
         // unknown type throws
         assertThrows(UnknownTypeException.class, () -> visitor.visit(InvalidType.INSTANCE, null));
      }
   }

   @Test public void builderDefaults() {
      // Like above, but using result of a Builder to make sure it behaves the same way
      {
         TypeVisitor<Object, Void> visitor = new TypeVisitor.Builder<Object, Void>().build();
         // defaultAction returns null unless overridden
         assertNull(visitor.visit(Class.class, null));
         assertNull(visitor.visit(PARAM_TYPE, null));
         assertNull(visitor.visit(GENERIC_ARRAY_TYPE, null));
         assertNull(visitor.visit(WILDCARD_EXTENDS, null));
         assertNull(visitor.visit(TYPE_VAR_T, null));
         assertThrows(UnknownTypeException.class, () -> visitor.visit(InvalidType.INSTANCE, null));
      }
      {
         // double-check that all other methods call defaultAction
         AtomicInteger count = new AtomicInteger();
         Object r = new Object();
         TypeVisitor<Object, Void> visitor = new TypeVisitor.Builder<Object, Void>().defaultAction(
               (type, v) -> {
                  count.incrementAndGet();
                  return r;
               }).build();
         assertSame(r, visitor.visit(Class.class, null));
         assertEquals(1, count.get());
         assertSame(r, visitor.visit(PARAM_TYPE, null));
         assertEquals(2, count.get());
         assertSame(r, visitor.visit(GENERIC_ARRAY_TYPE, null));
         assertEquals(3, count.get());
         assertSame(r, visitor.visit(WILDCARD_EXTENDS, null));
         assertEquals(4, count.get());
         assertSame(r, visitor.visit(TYPE_VAR_T, null));
         assertEquals(5, count.get());
         // unknown type throws
         assertThrows(UnknownTypeException.class, () -> visitor.visit(InvalidType.INSTANCE, null));
      }
   }

   @Test public void visit() {
      // We make sure visit delegates to the expected method
      Object param = new Object();
      
      TestVisitor visitor = new TestVisitor(param);
      assertSame(Class.class, visitor.visit(Class.class, param));
      assertEquals(1, visitor.classCount.get());
      assertSame(Class.class, visitor.lastClass.get());
      assertEquals(0, visitor.paramTypeCount.get());
      assertNull(visitor.lastParamType.get());
      assertEquals(0, visitor.arrayTypeCount.get());
      assertNull(visitor.lastArrayType.get());
      assertEquals(0, visitor.wildcardTypeCount.get());
      assertNull(visitor.lastWildcardType.get());
      assertEquals(0, visitor.typeVariableCount.get());
      assertNull(visitor.lastTypeVariable.get());
      assertEquals(0, visitor.unknownTypeCount.get());
      assertNull(visitor.lastUnknownType.get());
      
      visitor = new TestVisitor(param);
      assertSame(PARAM_TYPE, visitor.visit(PARAM_TYPE, param));
      assertEquals(0, visitor.classCount.get());
      assertNull(visitor.lastClass.get());
      assertEquals(1, visitor.paramTypeCount.get());
      assertSame(PARAM_TYPE, visitor.lastParamType.get());
      assertEquals(0, visitor.arrayTypeCount.get());
      assertNull(visitor.lastArrayType.get());
      assertEquals(0, visitor.wildcardTypeCount.get());
      assertNull(visitor.lastWildcardType.get());
      assertEquals(0, visitor.typeVariableCount.get());
      assertNull(visitor.lastTypeVariable.get());
      assertEquals(0, visitor.unknownTypeCount.get());
      assertNull(visitor.lastUnknownType.get());
      
      visitor = new TestVisitor(param);
      assertSame(GENERIC_ARRAY_TYPE, visitor.visit(GENERIC_ARRAY_TYPE, param));
      assertEquals(0, visitor.classCount.get());
      assertNull(visitor.lastClass.get());
      assertEquals(0, visitor.paramTypeCount.get());
      assertNull(visitor.lastParamType.get());
      assertEquals(1, visitor.arrayTypeCount.get());
      assertSame(GENERIC_ARRAY_TYPE, visitor.lastArrayType.get());
      assertEquals(0, visitor.wildcardTypeCount.get());
      assertNull(visitor.lastWildcardType.get());
      assertEquals(0, visitor.typeVariableCount.get());
      assertNull(visitor.lastTypeVariable.get());
      assertEquals(0, visitor.unknownTypeCount.get());
      assertNull(visitor.lastUnknownType.get());

      visitor = new TestVisitor(param);
      assertSame(WILDCARD_EXTENDS, visitor.visit(WILDCARD_EXTENDS, param));
      assertEquals(0, visitor.classCount.get());
      assertNull(visitor.lastClass.get());
      assertEquals(0, visitor.paramTypeCount.get());
      assertNull(visitor.lastParamType.get());
      assertEquals(0, visitor.arrayTypeCount.get());
      assertNull(visitor.lastArrayType.get());
      assertEquals(1, visitor.wildcardTypeCount.get());
      assertSame(WILDCARD_EXTENDS, visitor.lastWildcardType.get());
      assertEquals(0, visitor.typeVariableCount.get());
      assertNull(visitor.lastTypeVariable.get());
      assertEquals(0, visitor.unknownTypeCount.get());
      assertNull(visitor.lastUnknownType.get());

      visitor = new TestVisitor(param);
      assertSame(TYPE_VAR_T, visitor.visit(TYPE_VAR_T, param));
      assertEquals(0, visitor.classCount.get());
      assertNull(visitor.lastClass.get());
      assertEquals(0, visitor.paramTypeCount.get());
      assertNull(visitor.lastParamType.get());
      assertEquals(0, visitor.arrayTypeCount.get());
      assertNull(visitor.lastArrayType.get());
      assertEquals(0, visitor.wildcardTypeCount.get());
      assertNull(visitor.lastWildcardType.get());
      assertEquals(1, visitor.typeVariableCount.get());
      assertSame(TYPE_VAR_T, visitor.lastTypeVariable.get());
      assertEquals(0, visitor.unknownTypeCount.get());
      assertNull(visitor.lastUnknownType.get());

      visitor = new TestVisitor(param);
      assertSame(InvalidType.INSTANCE, visitor.visit(InvalidType.INSTANCE, param));
      assertEquals(0, visitor.classCount.get());
      assertNull(visitor.lastClass.get());
      assertEquals(0, visitor.paramTypeCount.get());
      assertNull(visitor.lastParamType.get());
      assertEquals(0, visitor.arrayTypeCount.get());
      assertNull(visitor.lastArrayType.get());
      assertEquals(0, visitor.wildcardTypeCount.get());
      assertNull(visitor.lastWildcardType.get());
      assertEquals(0, visitor.typeVariableCount.get());
      assertNull(visitor.lastTypeVariable.get());
      assertEquals(1, visitor.unknownTypeCount.get());
      assertSame(InvalidType.INSTANCE, visitor.lastUnknownType.get());
   }
   
   private static class TestVisitor implements TypeVisitor<Type, Object> {
      final AtomicInteger classCount = new AtomicInteger();
      final AtomicReference<Class<?>> lastClass = new AtomicReference<>();
      final AtomicInteger paramTypeCount = new AtomicInteger();
      final AtomicReference<ParameterizedType> lastParamType = new AtomicReference<>();
      final AtomicInteger arrayTypeCount = new AtomicInteger();
      final AtomicReference<GenericArrayType> lastArrayType = new AtomicReference<>();
      final AtomicInteger wildcardTypeCount = new AtomicInteger();
      final AtomicReference<WildcardType> lastWildcardType = new AtomicReference<>();
      final AtomicInteger typeVariableCount = new AtomicInteger();
      final AtomicReference<TypeVariable<?>> lastTypeVariable = new AtomicReference<>();
      final AtomicInteger unknownTypeCount = new AtomicInteger();
      final AtomicReference<Type> lastUnknownType = new AtomicReference<>();
      final Object expectedParam;
      
      TestVisitor(Object expectedParam) {
         this.expectedParam = expectedParam;
      }

      @Override
      public Type visitClass(Class<?> clazz, Object param) {
         assertSame(expectedParam, param);
         classCount.incrementAndGet();
         lastClass.set(clazz);
         return clazz;
      }

      @Override
      public Type visitParameterizedType(ParameterizedType parameterizedType, Object param) {
         assertSame(expectedParam, param);
         paramTypeCount.incrementAndGet();
         lastParamType.set(parameterizedType);
         return parameterizedType;
      }

      @Override
      public Type visitGenericArrayType(GenericArrayType arrayType, Object param) {
         assertSame(expectedParam, param);
         arrayTypeCount.incrementAndGet();
         lastArrayType.set(arrayType);
         return arrayType;
      }

      @Override
      public Type visitWildcardType(WildcardType wildcardType, Object param) {
         assertSame(expectedParam, param);
         wildcardTypeCount.incrementAndGet();
         lastWildcardType.set(wildcardType);
         return wildcardType;
      }

      @Override
      public Type visitTypeVariable(TypeVariable<?> typeVariable, Object param) {
         assertSame(expectedParam, param);
         typeVariableCount.incrementAndGet();
         lastTypeVariable.set(typeVariable);
         return typeVariable;
      }

      @Override
      public Type visitUnknownType(Type type, Object param) {
         assertSame(expectedParam, param);
         unknownTypeCount.incrementAndGet();
         lastUnknownType.set(type);
         return type;
      }
   }
}
