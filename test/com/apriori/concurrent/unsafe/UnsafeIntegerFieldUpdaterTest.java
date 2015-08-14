package com.apriori.concurrent.unsafe;

import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.apriori.concurrent.test.FieldUpdaterFactory;
import com.apriori.concurrent.test.TestPublicClass;

import org.junit.Test;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;


public class UnsafeIntegerFieldUpdaterTest {
   
   static final IntUnaryOperator op1 = b -> -b;
   static final IntBinaryOperator op2 = (b1, b2) -> b1 ^ b2;

   volatile int value;
   UnsafeIntegerFieldUpdater<UnsafeIntegerFieldUpdaterTest> updater =
         new UnsafeIntegerFieldUpdater<>(UnsafeIntegerFieldUpdaterTest.class, "value");
   
   FieldUpdaterFactory callerPublic = TestPublicClass.createPublic();
   FieldUpdaterFactory callerPrivate = TestPublicClass.createPrivate();
   FieldUpdaterFactory callerNonPublic = TestPublicClass.createNonPublic();
   Class<?> publicClass = callerPublic.getClass();
   Class<?> privateClass = callerPrivate.getClass();
   Class<?> nonPublicClass = callerNonPublic.getClass();
   
   @Test public void get() {
      assertEquals(0, updater.get(this));
      value = 123;
      assertEquals(123, updater.get(this));
   }

   @Test public void set() {
      updater.set(this, 987);
      assertEquals(987, value);
      updater.set(this, -321);
      assertEquals(-321, value);
   }

   @Test public void lazySet() {
      // since this is single-threaded, same behavior as set...
      updater.lazySet(this, 987);
      assertEquals(987, value);
      updater.lazySet(this, -321);
      assertEquals(-321, value);
   }

   @Test public void compareAndSet() {
      assertFalse(updater.compareAndSet(this, 1, -99));
      assertEquals(0, value);
      assertTrue(updater.compareAndSet(this, 0, -99));
      assertEquals(-99, value);
      assertFalse(updater.compareAndSet(this, -98, 1001));
      assertEquals(-99, value);
      assertTrue(updater.compareAndSet(this, -99, 1001));
      assertEquals(1001, value);
   }

   @Test public void getAndSet() {
      assertEquals(0, updater.getAndSet(this, 123));
      assertEquals(123, value);
      assertEquals(123, updater.getAndSet(this, -321));
      assertEquals(-321, value);
      assertEquals(-321, updater.getAndSet(this, -321));
      assertEquals(-321, value);
      assertEquals(-321, updater.getAndSet(this, 987));
      assertEquals(987, value);
   }

   @Test public void failToConstruct_badField() {
      @SuppressWarnings("unused")
      class TestClass {
         int notVolatile;
         volatile boolean notInt;
      }
      assertThrows(IllegalArgumentException.class,
            () -> new UnsafeIntegerFieldUpdater<>(TestClass.class, "notVolatile"));
      assertThrows(IllegalArgumentException.class,
            () -> new UnsafeIntegerFieldUpdater<>(TestClass.class, "x"));
      assertThrows(IllegalArgumentException.class,
            () -> new UnsafeIntegerFieldUpdater<>(TestClass.class, "notInt"));
   }
   
   @Test public void failToConstruct_npe() {
      assertThrows(NullPointerException.class,
            () -> new UnsafeIntegerFieldUpdater<>(null, "x"));
      assertThrows(NullPointerException.class,
            () -> new UnsafeIntegerFieldUpdater<>(UnsafeIntegerFieldUpdaterTest.class, null));
   }
   
   @Test public void privateField_onlyAccessibleFromSameEnclosingClass() {
      assertThrows(RuntimeException.class,
            () -> new UnsafeIntegerFieldUpdater<>(publicClass, "privateInt"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(publicClass, "privateInt"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "privateInt"));
      // these succeed since caller is in same (enclosing) class as target
      assertNotNull(callerPublic.makeIntUpdater(publicClass, "privateInt"));
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "privateInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "privateInt"));
      assertNotNull(callerPrivate.makeIntUpdater(privateClass, "privateInt"));
      assertNotNull(callerNonPublic.makeIntUpdater(nonPublicClass, "privateInt"));
   }

   @Test public void privateClass_onlyAccessibleFromSameEnclosingClass() {
      assertThrows(RuntimeException.class,
            () -> new UnsafeIntegerFieldUpdater<>(privateClass, "publicInt"));
      // not in same class (even though in same package) means can't access any fields 
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "privateInt"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "protectedInt"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "nonPublicInt"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "publicInt"));
      
      // these succeed since caller is in same (enclosing) class as target
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "privateInt"));
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "protectedInt"));
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "nonPublicInt"));
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "publicInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "privateInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "protectedInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "nonPublicInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "publicInt"));
   }

   @Test public void nonPublicField_onlyAccessibleFromSamePackage() {
      // wrong package
      assertThrows(RuntimeException.class,
            () -> new UnsafeIntegerFieldUpdater<>(publicClass, "nonPublicInt"));
      // right package, but can't access field of private class
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "nonPublicInt"));
      
      // these succeed since caller is in same package
      assertNotNull(callerNonPublic.makeIntUpdater(publicClass, "nonPublicInt"));
      assertNotNull(callerPublic.makeIntUpdater(publicClass, "nonPublicInt"));
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "nonPublicInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "nonPublicInt"));
      assertNotNull(callerPrivate.makeIntUpdater(privateClass, "nonPublicInt"));
      assertNotNull(callerNonPublic.makeIntUpdater(nonPublicClass, "nonPublicInt"));
   }
   
   @Test public void nonPublicClass_onlyAccessibleFromSamePackage() {
      assertThrows(RuntimeException.class,
            () -> new UnsafeIntegerFieldUpdater<>(nonPublicClass, "publicInt"));
      
      // right package, but can't access private fields
      assertThrows(RuntimeException.class,
            () -> callerPublic.makeIntUpdater(nonPublicClass, "privateInt"));
      assertThrows(RuntimeException.class,
            () -> callerPrivate.makeIntUpdater(nonPublicClass, "privateInt"));
      
      // these succeed since caller is in same package
      assertNotNull(callerPublic.makeIntUpdater(nonPublicClass, "protectedInt"));
      assertNotNull(callerPublic.makeIntUpdater(nonPublicClass, "nonPublicInt"));
      assertNotNull(callerPublic.makeIntUpdater(nonPublicClass, "publicInt"));
      assertNotNull(callerPrivate.makeIntUpdater(nonPublicClass, "protectedInt"));
      assertNotNull(callerPrivate.makeIntUpdater(nonPublicClass, "nonPublicInt"));
      assertNotNull(callerPrivate.makeIntUpdater(nonPublicClass, "publicInt"));
   }

   @Test public void protectedField_onlyAccessibleFromSamePackageOrSubclass() {
      // wrong package
      assertThrows(RuntimeException.class,
            () -> new UnsafeIntegerFieldUpdater<>(publicClass, "protectedInt"));
      // right package, but can't access field of private class
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.makeIntUpdater(privateClass, "protectedInt"));

      // still wrong package, but this is a sub-class, so it will work
      class TestSubClass extends TestPublicClass {
         @Override
         public <T> UnsafeIntegerFieldUpdater<T> makeIntUpdater(Class<T> clazz, String fieldName) {
            return new UnsafeIntegerFieldUpdater<>(clazz, fieldName);
         }
      }
      assertNotNull(new TestSubClass().makeIntUpdater(publicClass, "protectedInt"));
      
      // these succeed since caller is in same package
      assertNotNull(callerNonPublic.makeIntUpdater(publicClass, "protectedInt"));
      assertNotNull(callerPublic.makeIntUpdater(publicClass, "protectedInt"));
      assertNotNull(callerPublic.makeIntUpdater(privateClass, "protectedInt"));
      assertNotNull(callerPrivate.makeIntUpdater(publicClass, "protectedInt"));
      assertNotNull(callerPrivate.makeIntUpdater(privateClass, "protectedInt"));
      assertNotNull(callerNonPublic.makeIntUpdater(nonPublicClass, "protectedInt"));
   }

   @Test public void publicField_publicClass_alwaysAccessible() {
      // not same package, not same enclosing class, not a sub-class - but still works
      assertNotNull(new UnsafeIntegerFieldUpdater<>(publicClass, "publicInt"));
   }
}
