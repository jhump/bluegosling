package com.bluegosling.reflect;

import static com.bluegosling.reflect.TypeTesting.GENERIC_ARRAY_TYPE;
import static com.bluegosling.reflect.TypeTesting.PARAM_TYPE;
import static com.bluegosling.reflect.TypeTesting.TYPE_VAR_T;
import static com.bluegosling.reflect.TypeTesting.WILDCARD_EXTENDS;
import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.bluegosling.reflect.TypeTesting.InvalidType;
import com.bluegosling.vars.VariableInt;

import org.junit.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;


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
         VariableInt count = new VariableInt();
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
         VariableInt count = new VariableInt();
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
      assertEquals(1, visitor.classCount);
      assertSame(Class.class, visitor.lastClass);
      assertEquals(0, visitor.paramTypeCount);
      assertNull(visitor.lastParamType);
      assertEquals(0, visitor.arrayTypeCount);
      assertNull(visitor.lastArrayType);
      assertEquals(0, visitor.wildcardTypeCount);
      assertNull(visitor.lastWildcardType);
      assertEquals(0, visitor.typeVariableCount);
      assertNull(visitor.lastTypeVariable);
      assertEquals(0, visitor.unknownTypeCount);
      assertNull(visitor.lastUnknownType);
      
      visitor = new TestVisitor(param);
      assertSame(PARAM_TYPE, visitor.visit(PARAM_TYPE, param));
      assertEquals(0, visitor.classCount);
      assertNull(visitor.lastClass);
      assertEquals(1, visitor.paramTypeCount);
      assertSame(PARAM_TYPE, visitor.lastParamType);
      assertEquals(0, visitor.arrayTypeCount);
      assertNull(visitor.lastArrayType);
      assertEquals(0, visitor.wildcardTypeCount);
      assertNull(visitor.lastWildcardType);
      assertEquals(0, visitor.typeVariableCount);
      assertNull(visitor.lastTypeVariable);
      assertEquals(0, visitor.unknownTypeCount);
      assertNull(visitor.lastUnknownType);
      
      visitor = new TestVisitor(param);
      assertSame(GENERIC_ARRAY_TYPE, visitor.visit(GENERIC_ARRAY_TYPE, param));
      assertEquals(0, visitor.classCount);
      assertNull(visitor.lastClass);
      assertEquals(0, visitor.paramTypeCount);
      assertNull(visitor.lastParamType);
      assertEquals(1, visitor.arrayTypeCount);
      assertSame(GENERIC_ARRAY_TYPE, visitor.lastArrayType);
      assertEquals(0, visitor.wildcardTypeCount);
      assertNull(visitor.lastWildcardType);
      assertEquals(0, visitor.typeVariableCount);
      assertNull(visitor.lastTypeVariable);
      assertEquals(0, visitor.unknownTypeCount);
      assertNull(visitor.lastUnknownType);

      visitor = new TestVisitor(param);
      assertSame(WILDCARD_EXTENDS, visitor.visit(WILDCARD_EXTENDS, param));
      assertEquals(0, visitor.classCount);
      assertNull(visitor.lastClass);
      assertEquals(0, visitor.paramTypeCount);
      assertNull(visitor.lastParamType);
      assertEquals(0, visitor.arrayTypeCount);
      assertNull(visitor.lastArrayType);
      assertEquals(1, visitor.wildcardTypeCount);
      assertSame(WILDCARD_EXTENDS, visitor.lastWildcardType);
      assertEquals(0, visitor.typeVariableCount);
      assertNull(visitor.lastTypeVariable);
      assertEquals(0, visitor.unknownTypeCount);
      assertNull(visitor.lastUnknownType);

      visitor = new TestVisitor(param);
      assertSame(TYPE_VAR_T, visitor.visit(TYPE_VAR_T, param));
      assertEquals(0, visitor.classCount);
      assertNull(visitor.lastClass);
      assertEquals(0, visitor.paramTypeCount);
      assertNull(visitor.lastParamType);
      assertEquals(0, visitor.arrayTypeCount);
      assertNull(visitor.lastArrayType);
      assertEquals(0, visitor.wildcardTypeCount);
      assertNull(visitor.lastWildcardType);
      assertEquals(1, visitor.typeVariableCount);
      assertSame(TYPE_VAR_T, visitor.lastTypeVariable);
      assertEquals(0, visitor.unknownTypeCount);
      assertNull(visitor.lastUnknownType);

      visitor = new TestVisitor(param);
      assertSame(InvalidType.INSTANCE, visitor.visit(InvalidType.INSTANCE, param));
      assertEquals(0, visitor.classCount);
      assertNull(visitor.lastClass);
      assertEquals(0, visitor.paramTypeCount);
      assertNull(visitor.lastParamType);
      assertEquals(0, visitor.arrayTypeCount);
      assertNull(visitor.lastArrayType);
      assertEquals(0, visitor.wildcardTypeCount);
      assertNull(visitor.lastWildcardType);
      assertEquals(0, visitor.typeVariableCount);
      assertNull(visitor.lastTypeVariable);
      assertEquals(1, visitor.unknownTypeCount);
      assertSame(InvalidType.INSTANCE, visitor.lastUnknownType);
   }
   
   private static class TestVisitor implements TypeVisitor<Type, Object> {
      int classCount;
      Class<?> lastClass;
      int paramTypeCount;
      ParameterizedType lastParamType;
      int arrayTypeCount;
      GenericArrayType lastArrayType;
      int wildcardTypeCount;
      WildcardType lastWildcardType;
      int typeVariableCount;
      TypeVariable<?> lastTypeVariable;
      int unknownTypeCount;
      Type lastUnknownType;
      Object expectedParam;
      
      TestVisitor(Object expectedParam) {
         this.expectedParam = expectedParam;
      }

      @Override
      public Type visitClass(Class<?> clazz, Object param) {
         assertSame(expectedParam, param);
         classCount++;
         lastClass = clazz;
         return clazz;
      }

      @Override
      public Type visitParameterizedType(ParameterizedType parameterizedType, Object param) {
         assertSame(expectedParam, param);
         paramTypeCount++;
         lastParamType = parameterizedType;
         return parameterizedType;
      }

      @Override
      public Type visitGenericArrayType(GenericArrayType arrayType, Object param) {
         assertSame(expectedParam, param);
         arrayTypeCount++;
         lastArrayType = arrayType;
         return arrayType;
      }

      @Override
      public Type visitWildcardType(WildcardType wildcardType, Object param) {
         assertSame(expectedParam, param);
         wildcardTypeCount++;
         lastWildcardType = wildcardType;
         return wildcardType;
      }

      @Override
      public Type visitTypeVariable(TypeVariable<?> typeVariable, Object param) {
         assertSame(expectedParam, param);
         typeVariableCount++;
         lastTypeVariable = typeVariable;
         return typeVariable;
      }

      @Override
      public Type visitUnknownType(Type type, Object param) {
         assertSame(expectedParam, param);
         unknownTypeCount++;
         lastUnknownType = type;
         return type;
      }
   }
}
