package com.apriori.concurrent;

import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.apriori.concurrent.test.FieldUpdaterMaker;
import com.apriori.concurrent.test.TestPublicClass;
import com.apriori.util.BooleanBinaryOperator;
import com.apriori.util.BooleanUnaryOperator;

import org.junit.Test;

public class AtomicBooleanFieldUpdaterTest {
   
   static final BooleanUnaryOperator op1 = b -> !b;
   static final BooleanBinaryOperator op2 = (b1, b2) -> b1 ^ b2;

   volatile boolean flag;
   AtomicBooleanFieldUpdater<AtomicBooleanFieldUpdaterTest> updater =
         AtomicBooleanFieldUpdater.newUpdater(AtomicBooleanFieldUpdaterTest.class, "flag");
   
   FieldUpdaterMaker callerPublic = TestPublicClass.createPublic();
   FieldUpdaterMaker callerPrivate = TestPublicClass.createPrivate();
   FieldUpdaterMaker callerNonPublic = TestPublicClass.createNonPublic();
   Class<?> publicClass = callerPublic.getClass();
   Class<?> privateClass = callerPrivate.getClass();
   Class<?> nonPublicClass = callerNonPublic.getClass();
   
   @Test public void get() {
      assertFalse(updater.get(this));
      flag = true;
      assertTrue(updater.get(this));
   }

   @Test public void set() {
      updater.set(this, true);
      assertTrue(flag);
      updater.set(this, false);
      assertFalse(flag);
   }

   @Test public void lazySet() {
      // since this is single-threaded, same behavior as set...
      updater.lazySet(this, true);
      assertTrue(flag);
      updater.lazySet(this, false);
      assertFalse(flag);
   }

   @Test public void compareAndSet() {
      assertFalse(updater.compareAndSet(this, true, false));
      assertFalse(flag);
      assertTrue(updater.compareAndSet(this, false, true));
      assertTrue(flag);
      assertFalse(updater.compareAndSet(this, false, true));
      assertTrue(flag);
      assertTrue(updater.compareAndSet(this, true, false));
      assertFalse(flag);
   }

   @Test public void weakCompareAndSet() {
      // since this is single-threaded, same behavior as compareAndSet...
      assertFalse(updater.weakCompareAndSet(this, true, false));
      assertFalse(flag);
      assertTrue(updater.weakCompareAndSet(this, false, true));
      assertTrue(flag);
      assertFalse(updater.weakCompareAndSet(this, false, true));
      assertTrue(flag);
      assertTrue(updater.weakCompareAndSet(this, true, false));
      assertFalse(flag);
   }

   @Test public void getAndSet() {
      assertFalse(updater.getAndSet(this, false));
      assertFalse(flag);
      assertFalse(updater.getAndSet(this, true));
      assertTrue(flag);
      assertTrue(updater.getAndSet(this, true));
      assertTrue(flag);
      assertTrue(updater.getAndSet(this, false));
      assertFalse(flag);
   }

   @Test public void getAndUpdate() {
      assertFalse(updater.getAndUpdate(this, op1));
      assertTrue(flag);
      assertTrue(updater.getAndUpdate(this, op1));
      assertFalse(flag);
      assertFalse(updater.getAndUpdate(this, op1));
      assertTrue(flag);
   }

   @Test public void updateAndGet() {
      assertTrue(updater.updateAndGet(this, op1));
      assertTrue(flag);
      assertFalse(updater.updateAndGet(this, op1));
      assertFalse(flag);
      assertTrue(updater.updateAndGet(this, op1));
      assertTrue(flag);
   }
   
   @Test public void getAndAccumulate() {
      assertFalse(updater.getAndAccumulate(this, false, op2));
      assertFalse(flag);
      assertFalse(updater.getAndAccumulate(this, true, op2));
      assertTrue(flag);
      assertTrue(updater.getAndAccumulate(this, false, op2));
      assertTrue(flag);
      assertTrue(updater.getAndAccumulate(this, true, op2));
      assertFalse(flag);
   }

   @Test public void accumulateAndGet() {
      assertFalse(updater.accumulateAndGet(this, false, op2));
      assertFalse(flag);
      assertTrue(updater.accumulateAndGet(this, true, op2));
      assertTrue(flag);
      assertTrue(updater.accumulateAndGet(this, false, op2));
      assertTrue(flag);
      assertFalse(updater.accumulateAndGet(this, true, op2));
      assertFalse(flag);
   }

   @Test public void failToConstruct_badField() {
      @SuppressWarnings("unused")
      class TestClass {
         boolean notVolatile;
         volatile int notBoolean;
      }
      assertThrows(IllegalArgumentException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(TestClass.class, "notVolatile"));
      assertThrows(IllegalArgumentException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(TestClass.class, "x"));
      assertThrows(IllegalArgumentException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(TestClass.class, "notBoolean"));
   }
   
   @Test public void failToConstruct_npe() {
      assertThrows(NullPointerException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(null, "x"));
      assertThrows(NullPointerException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(AtomicBooleanFieldUpdaterTest.class, null));
   }
   
   @Test public void failIfUsingBadInstance() {
      AtomicBooleanFieldUpdater<?> u = updater;
      @SuppressWarnings("unchecked")
      AtomicBooleanFieldUpdater<Object> uBad = (AtomicBooleanFieldUpdater<Object>) u;
      
      assertThrows(ClassCastException.class, () -> uBad.get(new Object()));
      assertThrows(ClassCastException.class, () -> uBad.set(new Object(), false));
      assertThrows(ClassCastException.class, () -> uBad.lazySet(new Object(), false));
      assertThrows(ClassCastException.class, () -> uBad.getAndSet(new Object(), false));
      assertThrows(ClassCastException.class, () -> uBad.compareAndSet(new Object(), false, true));
      assertThrows(ClassCastException.class,
            () -> uBad.weakCompareAndSet(new Object(), false, true));
      assertThrows(ClassCastException.class, () -> uBad.getAndUpdate(new Object(), op1));
      assertThrows(ClassCastException.class, () -> uBad.updateAndGet(new Object(), op1));
      assertThrows(ClassCastException.class, () -> uBad.getAndAccumulate(new Object(), false, op2));
      assertThrows(ClassCastException.class, () -> uBad.accumulateAndGet(new Object(), false, op2));
   }
   
   @Test public void privateField_onlyAccessibleFromSameEnclosingClass() {
      assertThrows(RuntimeException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(publicClass, "privateFlag"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(publicClass, "privateFlag"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "privateFlag"));
      // these succeed since caller is in same (enclosing) class as target
      assertNotNull(callerPublic.make(publicClass, "privateFlag"));
      assertNotNull(callerPublic.make(privateClass, "privateFlag"));
      assertNotNull(callerPrivate.make(publicClass, "privateFlag"));
      assertNotNull(callerPrivate.make(privateClass, "privateFlag"));
      assertNotNull(callerNonPublic.make(nonPublicClass, "privateFlag"));
   }

   @Test public void privateClass_onlyAccessibleFromSameEnclosingClass() {
      assertThrows(RuntimeException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(privateClass, "publicFlag"));
      // not in same class (even though in same package) means can't access any fields 
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "privateFlag"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "protectedFlag"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "nonPublicFlag"));
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "publicFlag"));
      
      // these succeed since caller is in same (enclosing) class as target
      assertNotNull(callerPublic.make(privateClass, "privateFlag"));
      assertNotNull(callerPublic.make(privateClass, "protectedFlag"));
      assertNotNull(callerPublic.make(privateClass, "nonPublicFlag"));
      assertNotNull(callerPublic.make(privateClass, "publicFlag"));
      assertNotNull(callerPrivate.make(publicClass, "privateFlag"));
      assertNotNull(callerPrivate.make(publicClass, "protectedFlag"));
      assertNotNull(callerPrivate.make(publicClass, "nonPublicFlag"));
      assertNotNull(callerPrivate.make(publicClass, "publicFlag"));
   }

   @Test public void nonPublicField_onlyAccessibleFromSamePackage() {
      // wrong package
      assertThrows(RuntimeException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(publicClass, "nonPublicFlag"));
      // right package, but can't access field of private class
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "nonPublicFlag"));
      
      // these succeed since caller is in same package
      assertNotNull(callerNonPublic.make(publicClass, "nonPublicFlag"));
      assertNotNull(callerPublic.make(publicClass, "nonPublicFlag"));
      assertNotNull(callerPublic.make(privateClass, "nonPublicFlag"));
      assertNotNull(callerPrivate.make(publicClass, "nonPublicFlag"));
      assertNotNull(callerPrivate.make(privateClass, "nonPublicFlag"));
      assertNotNull(callerNonPublic.make(nonPublicClass, "nonPublicFlag"));
   }
   
   @Test public void nonPublicClass_onlyAccessibleFromSamePackage() {
      assertThrows(RuntimeException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(nonPublicClass, "publicFlag"));
      
      // right package, but can't access private fields
      assertThrows(RuntimeException.class,
            () -> callerPublic.make(nonPublicClass, "privateFlag"));
      assertThrows(RuntimeException.class,
            () -> callerPrivate.make(nonPublicClass, "privateFlag"));
      
      // these succeed since caller is in same package
      assertNotNull(callerPublic.make(nonPublicClass, "protectedFlag"));
      assertNotNull(callerPublic.make(nonPublicClass, "nonPublicFlag"));
      assertNotNull(callerPublic.make(nonPublicClass, "publicFlag"));
      assertNotNull(callerPrivate.make(nonPublicClass, "protectedFlag"));
      assertNotNull(callerPrivate.make(nonPublicClass, "nonPublicFlag"));
      assertNotNull(callerPrivate.make(nonPublicClass, "publicFlag"));
   }

   @Test public void protectedField_onlyAccessibleFromSamePackageOrSubclass() {
      // wrong package
      assertThrows(RuntimeException.class,
            () -> AtomicBooleanFieldUpdater.newUpdater(publicClass, "protectedFlag"));
      // right package, but can't access field of private class
      assertThrows(RuntimeException.class,
            () -> callerNonPublic.make(privateClass, "protectedFlag"));

      // still wrong package, but this is a sub-class, so it will work
      class TestSubClass extends TestPublicClass {
         @Override
         public <T> AtomicBooleanFieldUpdater<T> make(Class<T> clazz, String fieldName) {
            return AtomicBooleanFieldUpdater.newUpdater(clazz, fieldName);
         }
      }
      assertNotNull(new TestSubClass().make(publicClass, "protectedFlag"));
      
      // these succeed since caller is in same package
      assertNotNull(callerNonPublic.make(publicClass, "protectedFlag"));
      assertNotNull(callerPublic.make(publicClass, "protectedFlag"));
      assertNotNull(callerPublic.make(privateClass, "protectedFlag"));
      assertNotNull(callerPrivate.make(publicClass, "protectedFlag"));
      assertNotNull(callerPrivate.make(privateClass, "protectedFlag"));
      assertNotNull(callerNonPublic.make(nonPublicClass, "protectedFlag"));
   }

   @Test public void publicField_publicClass_alwaysAccessible() {
      // not same package, not same enclosing class, not a sub-class - but still works
      assertNotNull(AtomicBooleanFieldUpdater.newUpdater(publicClass, "publicFlag"));
   }
}
