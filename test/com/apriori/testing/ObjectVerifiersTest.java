package com.apriori.testing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Tests the functionality in {@link ObjectVerifiers}.
 * 
 * @author jhumphries
 */
public class ObjectVerifiersTest extends TestCase {
   
   /**
    * A verifier that always raises an assertion error.
    */
   private class FailingVerifier implements ObjectVerifier<Object> {
      
      public FailingVerifier() {}

      @Override
      public Object verify(Object test, Object reference) {
         fail();
         return null; // make compiler happy
      }
   }

   /**
    * A verifier that never raises an assertion error and always returns
    * the object specified at construction time.
    */
   private class PassingVerifier implements ObjectVerifier<Object> {
      private Object o;

      public PassingVerifier(Object o) {
         this.o = o;
      }

      @Override
      public Object verify(Object test, Object reference) {
         return o;
      }
   }

   /**
    * A mock object that allows the results of {@code equals()}, {@code hashCode()},
    * and {@code compareTo()} to be controlled by manipulating public fields.
    */
   private class MockObject implements Comparable<MockObject> {
      public boolean equals;
      public int hashCode;
      public int compareTo;
      
      public MockObject() {}
      
      @Override
      public boolean equals(Object o) {
         return equals;
      }
      
      @Override
      public int hashCode() {
         return hashCode;
      }
      
      @Override
      public int compareTo(MockObject o) {
         return compareTo;
      }
   }

   /**
    * A mock comparator that allows the results of {@code compare()} to be
    * controlled by manipulating a public field.
    */
   private class MockComparator implements Comparator<Object> {
      public int compare;
      
      public MockComparator() {}
      
      @Override
      public int compare(Object o1, Object o2) {
         return compare;
      }
   }
   
   /**
    * A simple test interface for use in testing {@code ObjectVerifiers.forTesting()}.
    */
   private static interface TestInterface {
      int getIntForString(String str);
   }
   
   // helper methods:
   
   private static <T> void assertReturnsFirst(ObjectVerifier<? super T> v, T o1, T o2) {
      assertSame(o1, v.verify(o1, o2));
   }

   private static <T> void assertReturnsNull(ObjectVerifier<? super T> v, T o1, T o2) {
      assertNull(v.verify(o1, o2));
   }

   private static <T> void assertFails(ObjectVerifier<? super T> v, T o1, T o2) {
      try {
         v.verify(o1, o2);
      } catch (AssertionFailedError e) {
         return; // expected
      }
      fail(); // should have throw exception
   }
   
   private static <T> void doNullTests(ObjectVerifier<? super T> v, T o1, T o2) {
      assertFails(v, null, o2);
      assertFails(v, o1, null);
      assertReturnsNull(v, null, null);
   }

   /**
    * Tests {@link ObjectVerifiers#NO_OP}.
    */
   public void testNoOp() {
      ObjectVerifier<Object> v = ObjectVerifiers.NO_OP;
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // verification always succeeds and returns test object
      o1.equals = o2.equals = true;
      assertReturnsFirst(v, o1, o2);

      // succeed even when objects not equal
      o1.equals = o2.equals = false;
      assertReturnsFirst(v, o1, o2);
      
      // normal null tests don't apply to this verifier since
      // it always succeeds
      assertReturnsFirst(v, o1, null);
      assertReturnsNull(v, null, o2);
      assertReturnsNull(v, null, null);
   }
   
   /**
    * Tests {@link ObjectVerifiers#NULLS}.
    */
   public void testNulls() {
      ObjectVerifier<Object> v = ObjectVerifiers.NULLS;
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // objects are equal
      o1.equals = o2.equals = true;
      assertReturnsFirst(v, o1, o2);
      
      // objects NOT equal - still passes since both are non-null
      o1.equals = o2.equals = false;
      assertReturnsFirst(v, o1, o2);
      
      doNullTests(v, o1, o2);
   }

   /**
    * Tests {@link ObjectVerifiers#EQUALS}.
    */
   public void testEquals() {
      ObjectVerifier<Object> v = ObjectVerifiers.EQUALS;
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // objects are equal
      o1.equals = o2.equals = true;
      assertReturnsFirst(v, o1, o2);
      
      // objects NOT equal
      o1.equals = o2.equals = false;
      assertFails(v, o1, o2);

      doNullTests(v, o1, o2);
   }
   
   /**
    * Tests {@link ObjectVerifiers#HASH_CODES}.
    */
   public void testHashCodes() {
      ObjectVerifier<Object> v = ObjectVerifiers.HASH_CODES;
      
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // objects match
      o1.equals = o2.equals = false; // this shouldn't matter
      o1.hashCode = o2.hashCode = -99;
      assertReturnsFirst(v, o1, o2);
      
      // objects do NOT match
      o1.hashCode = 44;
      assertFails(v, o1, o2);

      doNullTests(v, o1, o2);
   }

   /**
    * Tests {@link ObjectVerifiers#SAME}.
    */
   public void testSame() {
      ObjectVerifier<Object> v = ObjectVerifiers.SAME;
      
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // objects are equal but not the same
      o1.equals = o2.equals = true;
      assertFails(v, o1, o2);
      
      doNullTests(v, o1, o2);
      
      // passing non-null cases
      assertReturnsFirst(v, o1, o1);
      assertReturnsFirst(v, o2, o2);
   }
   
   /**
    * Tests {@link ObjectVerifiers#STRICT_EXCEPTIONS}.
    */
   public void testStrictExceptions() {
      ObjectVerifier<Throwable> v = ObjectVerifiers.STRICT_EXCEPTIONS;
      
      Throwable t1 = new IllegalArgumentException();
      Throwable t2 = new IOException();
      
      // objects are not of the same class, not even same hierarchy
      assertFails(v, t1, t2);

      // objects are not of the same class but are in same hierarchy
      t2 = new RuntimeException();
      assertFails(v, t1, t2);

      // objects are of the same class
      t2 = new IllegalArgumentException();
      assertReturnsFirst(v, t1, t2);
      
      // technically, these aren't the same class since one is an
      // anonymous sub-class
      t1 = new OutOfMemoryError() {
         // get rid of compiler warning about serializable class...
         private static final long serialVersionUID = 1L;
         
         @Override public String getMessage() {
            return "sweet";
         }
      };
      t2 = new OutOfMemoryError();
      assertFails(v, t1, t2);

      // same object
      assertReturnsFirst(v, t1, t1);

      doNullTests(v, t1, t2);
   }

   /**
    * Tests {@link ObjectVerifiers#relaxedExceptions(Class...)}.
    */
   @SuppressWarnings("unchecked")
   public void testRelaxedExceptionsVarArgs() {
      ObjectVerifier<Throwable> v =
            ObjectVerifiers.relaxedExceptions(IOException.class, RuntimeException.class);
      
      Throwable t1 = new IllegalArgumentException();
      Throwable t2 = new IOException();
      
      // objects are not of the same class, not even same hierarchy
      assertFails(v, t1, t2);

      // objects are of the same class
      t1 = new IOException();
      assertReturnsFirst(v, t1, t2);

      // objects are not of the same class but are in same hierarchy
      t1 = new FileNotFoundException();
      assertReturnsFirst(v, t1, t2);

      // same object
      assertReturnsFirst(v, t1, t1);

      // objects are not of the same class but are siblings
      t1 = new IllegalArgumentException();
      t2 = new ArrayStoreException();
      assertReturnsFirst(v, t1, t2);

      // same test as above, but verifier does not allow RuntimeException
      v = ObjectVerifiers.relaxedExceptions(IOException.class);
      assertFails(v, t1, t2);

      doNullTests(v, t1, t2);

      // should throw NPE with null inputs
      boolean caught = false;
      try {
         v = ObjectVerifiers.relaxedExceptions(IOException.class, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   /**
    * Tests {@link ObjectVerifiers#relaxedExceptions(java.util.Set)}.
    */
   public void testRelaxedExceptionsSet() {
      HashSet<Class<? extends Throwable>> set = new HashSet<Class<? extends Throwable>>();
      set.add(IOException.class);
      set.add(RuntimeException.class);
      ObjectVerifier<Throwable> v = ObjectVerifiers.relaxedExceptions(set);

      // same tests as testRelaxedExceptionsVarArgs:
      
      Throwable t1 = new IllegalArgumentException();
      Throwable t2 = new IOException();
      
      // objects are not of the same class, not even same hierarchy
      assertFails(v, t1, t2);

      // objects are of the same class
      t1 = new IOException();
      assertReturnsFirst(v, t1, t2);

      // objects are not of the same class but are in same hierarchy
      t1 = new FileNotFoundException();
      assertReturnsFirst(v, t1, t2);

      // same object
      assertReturnsFirst(v, t1, t1);

      // objects are not of the same class but are in same hierarchy for
      // a RuntimeException
      t1 = new IllegalArgumentException();
      t2 = new ArrayStoreException();
      assertReturnsFirst(v, t1, t2);

      // make sure that verifier isn't affected by changing the set it
      // was constructed with
      set.clear();
      set.add(IOException.class);
      assertReturnsFirst(v, t1, t2);
      
      // result only changes when we rebuild the verifier 
      // (without RuntimeException as valid ancestor for siblings)
      v = ObjectVerifiers.relaxedExceptions(set);
      assertFails(v, t1, t2);

      // technically, these aren't the same class since one is an
      // anonymous sub-class -- but they are compatible since the
      // test object (t1) is a sub-class of the reference object
      t1 = new OutOfMemoryError() {
         // get rid of compiler warning about serializable class...
         private static final long serialVersionUID = 1L;
         
         @Override public String getMessage() {
            return "sweet";
         }
      };
      t2 = new OutOfMemoryError();
      assertReturnsFirst(v, t1, t2);

      doNullTests(v, t1, t2);

      // should throw NPE with null inputs
      boolean caught = false;
      try {
         v = ObjectVerifiers.relaxedExceptions((Set<Class<? extends Throwable>>)null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      set.clear();
      set.add(null);
      try {
         v = ObjectVerifiers.relaxedExceptions(set);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link ObjectVerifiers#checkInstance}.
    */
   public void testCheckInstance() {
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();
      
      // expecting non-null object
      ObjectVerifier<MockObject> v = ObjectVerifiers.checkInstance(o1);

      // don't really care about reference value -- just expect that the
      // test value equals the one specified to checkInstance
      assertReturnsFirst(v, o1, o2);
      assertReturnsFirst(v, o1, o1);
      assertFails(v, o2, o2);
      assertFails(v, o2, o1);
      assertFails(v, null, o1);
      assertFails(v, null, o2);
      
      // expecting null object
      v = ObjectVerifiers.checkInstance(null);
      
      assertFails(v, o1, o2);
      assertFails(v, o1, o1);
      assertFails(v, o2, o2);
      assertFails(v, o2, o1);
      assertReturnsNull(v, null, o1);
      assertReturnsNull(v, null, o2);
   }
   
   /**
    * Tests {@link ObjectVerifiers#forComparable}.
    */
   public void testForComparable() {
      ObjectVerifier<MockObject> v = ObjectVerifiers.forComparable();
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // objects are not equal but compare to zero
      o1.equals = o2.equals = false;
      o1.compareTo = o2.compareTo = 0;
      assertReturnsFirst(v, o1, o2);
      
      // objects compare to non-zero
      o1.compareTo = o2.compareTo = 1;
      assertFails(v, o1, o2);

      o1.compareTo = o2.compareTo = -1;
      assertFails(v, o1, o2);

      doNullTests(v, o1, o2);
   }

   /**
    * Tests {@link ObjectVerifiers#fromComparator}.
    */
   public void testFromComparator() {
      MockComparator c = new MockComparator();
      ObjectVerifier<Object> v = ObjectVerifiers.fromComparator(c);
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();

      // objects are not equal and compare to non-zero
      // but comparator indicates zero
      o1.equals = o2.equals = false;
      o1.compareTo = o2.compareTo = 1;
      c.compare = 0;
      assertReturnsFirst(v, o1, o2);
      
      // objects compare to non-zero
      c.compare = 1;
      assertFails(v, o1, o2);

      c.compare = -1;
      assertFails(v, o1, o2);

      doNullTests(v, o1, o2);
      
      // should throw NPE with null input
      boolean caught = false;
      try {
         v = ObjectVerifiers.fromComparator(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   /**
    * Tests {@link ObjectVerifiers#compositeVerifier(ObjectVerifier...)}.
    */
   @SuppressWarnings("unchecked")
   public void testCompositeVerifier() {
      MockObject o1 = new MockObject();
      MockObject o2 = new MockObject();
      MockObject other = new MockObject();
      FailingVerifier fv = new FailingVerifier();
      PassingVerifier pv = new PassingVerifier(other); // always returns other
      
      // make sure return value is from the last in the list
      ObjectVerifier<Object> v = ObjectVerifiers.compositeVerifier(pv, ObjectVerifiers.NO_OP);
      assertReturnsFirst(v, o1, o2);
      assertReturnsNull(v, null, null);

      v = ObjectVerifiers.compositeVerifier(ObjectVerifiers.NO_OP, pv);
      assertSame(other, v.verify(o1, o2));
      assertSame(other, v.verify(null, null));
      
      // make sure exceptions in any verifier fail the whole thing
      v = ObjectVerifiers.compositeVerifier(pv, ObjectVerifiers.NULLS, ObjectVerifiers.NO_OP);
      doNullTests(v, o1, o2);
      
      v = ObjectVerifiers.compositeVerifier(pv, fv);
      assertFails(v, o1, o2);
   }

   /**
    * Tests {@link ObjectVerifiers#forTesting}.
    */
   public void testForTesting() {
      ObjectVerifier<TestInterface> v = ObjectVerifiers.forTesting(TestInterface.class);
      
      // use arrays so we can change the value below w/out reconstructing
      // either of these interface implementations
      final int i1val[] = new int[] { 0 };
      final int i2val[] = new int[] { 0 };

      TestInterface i1 = new TestInterface() {
         @Override public int getIntForString(String str) {
            return i1val[0];
         }
      };
      TestInterface i2 = new TestInterface() {
         @Override public int getIntForString(String str) {
            return i2val[0];
         }
      };
      
      TestInterface proxy = v.verify(i1, i2);
      assertNotNull(proxy);
      assertTrue(proxy instanceof Proxy);
      assertNotNull(TestingInvocationHandler.handlerFor(proxy));
      
      // smoke test the resulting proxy and its invocation handler
      assertEquals(0, proxy.getIntForString("test"));
      
      i2val[0] = 1; // cause them to return different values
      boolean caught = false;
      try {
         proxy.getIntForString("test");
      } catch (AssertionFailedError e) {
         caught = true;
      }
      assertTrue(caught);
   }
}
