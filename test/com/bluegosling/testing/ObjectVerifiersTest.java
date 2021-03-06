package com.bluegosling.testing;

import static com.bluegosling.testing.MoreAsserts.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Tests the functionality in {@link ObjectVerifiers}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ObjectVerifiersTest {

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
    * A verifier that never raises an assertion error and always returns the object specified at
    * construction time.
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
    * A mock object that allows the results of {@code equals()}, {@code hashCode()}, and
    * {@code compareTo()} to be controlled by manipulating public fields.
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
    * A mock comparator that allows the results of {@code compare()} to be controlled by
    * manipulating a public field.
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
      assertThrows(AssertionError.class, () -> v.verify(o1, o2));
   }

   private static <T> void doNullTests(ObjectVerifier<? super T> v, T o1, T o2) {
      assertFails(v, null, o2);
      assertFails(v, o1, null);
      assertReturnsNull(v, null, null);
   }

   /**
    * Tests {@link ObjectVerifiers#NO_OP}.
    */
   @Test public void testNoOp() {
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
   @Test public void testNulls() {
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
   @Test public void testEquals() {
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

      // test array behavior
      Object[] a1 = new Object[] { 1, 2, "a string", new String[] { "abc", "def" },
            new Number[] { 3, 4, 5 }, null, 6 };
      Object[] a2 = new Object[] { 1, 2, "a string", new Object[] { "abc", "def" },
            new Object[] { 3, 4, 5 }, null, 6 };
      Object[] a3 = new Object[] { 1, 2, 3 };
      Object[] a4 = new Object[] { 1, 2, "a string", new Object[] { "abc", "def" },
            new Object[] { 3, 4, 5 }, 6 };
      Object[] a5 = new Object[] { 1, 2, "a string", new Object[] { "abc", "Def" },
            new Object[] { 3, 4, 5 }, null, 6 };

      assertReturnsFirst(v, a1, a2);
      assertReturnsFirst(v, a2, a1);
      assertFails(v, a1, a3);
      assertFails(v, a2, a3);
      assertFails(v, a1, a4);
      assertFails(v, a2, a5);
      // mixed with non-arrays
      o1.equals = true;
      assertFails(v, o1, a1);
      // if reference object is not an array, then its equals method is called
      // instead of checking array contents
      assertReturnsFirst(v, a1, o1);
      // with nulls
      doNullTests(v, a1, a2);
   }

   /**
    * Tests {@link ObjectVerifiers#HASH_CODES}.
    */
   @Test public void testHashCodes() {
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
   @Test public void testSame() {
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
   @Test public void testStrictExceptions() {
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

         @Override
         public String getMessage() {
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
   @Test public void testRelaxedExceptionsVarArgs() {
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
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link ObjectVerifiers#relaxedExceptions(java.util.Set)}.
    */
   @Test public void testRelaxedExceptionsSet() {
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

         @Override
         public String getMessage() {
            return "sweet";
         }
      };
      t2 = new OutOfMemoryError();
      assertReturnsFirst(v, t1, t2);

      doNullTests(v, t1, t2);

      // should throw NPE with null inputs
      boolean caught = false;
      try {
         v = ObjectVerifiers.relaxedExceptions((Set<Class<? extends Throwable>>) null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      set.clear();
      set.add(null);
      try {
         v = ObjectVerifiers.relaxedExceptions(set);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link ObjectVerifiers#checkInstance}.
    */
   @Test public void testCheckInstance() {
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
   @Test public void testForComparable() {
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
   @Test public void testFromComparator() {
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
   @Test public void testCompositeVerifier() {
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

      // should throw NPE with null input
      boolean caught = false;
      try {
         v = ObjectVerifiers.compositeVerifier(pv, null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         v = ObjectVerifiers.compositeVerifier((List<ObjectVerifier<Object>>) null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link ObjectVerifiers#forTesting}.
    */
   @Test public void testForTesting() {
      ObjectVerifier<TestInterface> v = ObjectVerifiers.forTesting(TestInterface.class);

      // use arrays so we can change the value below w/out reconstructing
      // either of these interface implementations
      final int i1val[] = new int[] { 0 };
      final int i2val[] = new int[] { 0 };

      TestInterface i1 = new TestInterface() {
         @Override
         public int getIntForString(String str) {
            return i1val[0];
         }
      };
      TestInterface i2 = new TestInterface() {
         @Override
         public int getIntForString(String str) {
            return i2val[0];
         }
      };

      TestInterface proxy = v.verify(i1, i2);
      assertNotNull(proxy);
      assertTrue(proxy instanceof Proxy);
      assertNotNull(InterfaceVerifier.verifierFor(proxy));

      // smoke test the resulting proxy and its invocation handler
      assertEquals(0, proxy.getIntForString("test"));

      i2val[0] = 1; // cause them to return different values
      assertThrows(AssertionError.class, () -> proxy.getIntForString("test"));

      // no class loader specified (so make sure to use an interface that the
      // bootstrap classloader can see)
      @SuppressWarnings("rawtypes")
      ObjectVerifier<Iterable> v2 = ObjectVerifiers.forTesting(Iterable.class, null);
      Iterable<?> proxy2 = v2.verify(new ArrayList<Object>(), new HashSet<Object>());
      assertNotNull(proxy2);
      assertTrue(proxy2 instanceof Proxy);
      assertNotNull(InterfaceVerifier.verifierFor(proxy2));

      doNullTests(v, i1, i2);

      // should throw NPE with null input
      assertThrows(NullPointerException.class, () -> ObjectVerifiers.forTesting(null));
      assertThrows(NullPointerException.class, 
            () -> ObjectVerifiers.forTesting(null, TestInterface.class.getClassLoader()));

      // with bad class token (not an interface)
      assertThrows(IllegalArgumentException.class, () -> ObjectVerifiers.forTesting(Object.class));
   }
}
