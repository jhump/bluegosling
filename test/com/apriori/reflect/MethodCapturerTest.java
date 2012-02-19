package com.apriori.reflect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.TestCase;

/**
 * Tests the functionality in {@link MethodCapturer}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MethodCapturerTest extends TestCase {

   // Sample interfaces whose methods will be captured

   private interface TestInterface1 {
      void testMethod();

      String testMethod(int i);

      Object[] someOtherMethod(String a, TestInterface1 b);
   }

   private interface TestInterface2 {
      void otherTestMethod();

      String otherTestMethod(int i);

      String[] someOtherMethod(String a, TestInterface1 b);

      boolean yetAnotherMethod(TestInterface2 a, long b, long c);
   }

   private interface TestInterface3 extends TestInterface1 {
      Object otherTestMethod(); // conflicts w/ method in TestInterface2
   }

   /**
    * Tests creating a {@code MethodCapturer} for one interface and tests that its proxy and
    * accessor methods work as expected.
    */
   public void testCaptureOneInterface() {
      MethodCapturer<TestInterface1> c = new MethodCapturer<TestInterface1>(TestInterface1.class);
      assertEquals(Collections.singleton(TestInterface1.class), c.getInterfaces());
      TestInterface1 i = c.capture();

      // nothing called yet
      assertNull(c.getMethod());
      assertNull(c.getSignature());

      // void method
      i.testMethod();
      Method m = c.getMethod();
      assertEquals("testMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[0], m.getParameterTypes()));
      MethodSignature s = c.getSignature();
      assertEquals("testMethod", s.getName());
      assertEquals(Collections.emptyList(), s.getParameterTypes());

      // just captures last
      i.testMethod(100);
      i.toString();
      m = c.getMethod(i.someOtherMethod(null, null));
      // getting method again should be the same
      Method m2 = c.getMethod();
      assertEquals(m, m2);
      assertEquals("someOtherMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[] { String.class, TestInterface1.class },
            m.getParameterTypes()));
      s = c.getSignature();
      // getting signature again should be the same
      MethodSignature s2 = c.getSignature(i.someOtherMethod(null, null));
      assertEquals(s, s2);
      assertEquals("someOtherMethod", s.getName());
      assertEquals(Arrays.<Class<?>> asList(String.class, TestInterface1.class),
            s.getParameterTypes());
   }

   /**
    * Tests creating a {@code MethodCapturer} for multiple interfaces by specifying a {@code Set} to
    * the constructor and tests that its proxy and accessor methods work as expected.
    */
   public void testCaptureMultipleInterfacesSet() {
      @SuppressWarnings("unchecked")
      HashSet<Class<? extends Object>> ifaces =
            new HashSet<Class<? extends Object>>(Arrays.asList(TestInterface1.class,
                  TestInterface2.class));

      MethodCapturer<Object> c = new MethodCapturer<Object>(ifaces);
      assertEquals(ifaces, c.getInterfaces());

      Object o = c.capture();
      TestInterface1 i1 = (TestInterface1) o;
      TestInterface2 i2 = (TestInterface2) o;

      // nothing called yet
      assertNull(c.getMethod());
      assertNull(c.getSignature());

      // void method
      i1.testMethod();
      Method m = c.getMethod();
      assertEquals("testMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[0], m.getParameterTypes()));
      MethodSignature s = c.getSignature();
      assertEquals("testMethod", s.getName());
      assertEquals(Collections.emptyList(), s.getParameterTypes());

      i2.otherTestMethod();
      m = c.getMethod();
      assertEquals("otherTestMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[0], m.getParameterTypes()));
      s = c.getSignature();
      assertEquals("otherTestMethod", s.getName());
      assertEquals(Collections.emptyList(), s.getParameterTypes());

      // just captures last
      i1.testMethod(100);
      i1.toString();
      m = c.getMethod(i2.yetAnotherMethod(i2, 100, 200));
      // getting method again should be the same
      Method m2 = c.getMethod();
      assertEquals(m, m2);
      assertEquals("yetAnotherMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[] { TestInterface2.class, long.class, long.class },
            m.getParameterTypes()));
      s = c.getSignature();
      // getting signature again should be the same
      MethodSignature s2 = c.getSignature(i2.yetAnotherMethod(i2, 100, 200));
      assertEquals(s, s2);
      assertEquals("yetAnotherMethod", s.getName());
      assertEquals(Arrays.<Class<?>> asList(TestInterface2.class, long.class, long.class),
            s.getParameterTypes());
   }

   /**
    * Tests creating a {@code MethodCapturer} for multiple interfaces by specifying a var-args list
    * of interfaces to the constructor and tests that its proxy and accessor methods work as
    * expected.
    */
   public void testCaptureMultipleInterfacesVarArgs() {
      @SuppressWarnings("unchecked")
      MethodCapturer<Object> c = new MethodCapturer<Object>(TestInterface1.class,
            TestInterface3.class);

      @SuppressWarnings("unchecked")
      HashSet<Class<? extends Object>> ifaces =
            new HashSet<Class<? extends Object>>(Arrays.asList(TestInterface1.class,
                  TestInterface3.class));
      assertEquals(ifaces, c.getInterfaces());

      Object o = c.capture();
      TestInterface1 i1 = (TestInterface1) o;
      TestInterface3 i3 = (TestInterface3) o;

      // nothing called yet
      assertNull(c.getMethod());
      assertNull(c.getSignature());

      // void method
      i1.testMethod();
      Method m = c.getMethod();
      assertEquals("testMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[0], m.getParameterTypes()));
      MethodSignature s = c.getSignature();
      assertEquals("testMethod", s.getName());
      assertEquals(Collections.emptyList(), s.getParameterTypes());

      i3.otherTestMethod();
      m = c.getMethod();
      assertEquals("otherTestMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[0], m.getParameterTypes()));
      s = c.getSignature();
      assertEquals("otherTestMethod", s.getName());
      assertEquals(Collections.emptyList(), s.getParameterTypes());

      // just captures last
      i1.testMethod(100);
      i1.toString();
      m = c.getMethod(i3.testMethod(0));
      // getting method again should be the same
      Method m2 = c.getMethod();
      assertEquals(m, m2);
      assertEquals("testMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[] { int.class }, m.getParameterTypes()));
      s = c.getSignature();
      // getting signature again should be the same
      MethodSignature s2 = c.getSignature(i3.testMethod(0));
      assertEquals(s, s2);
      assertEquals("testMethod", s.getName());
      assertEquals(Arrays.<Class<?>> asList(int.class), s.getParameterTypes());
   }

   /**
    * Tests that getting two proxies for a {@code MethodCapturer} results in expected behavior (last
    * call "wins", regardless of which proxy instance).
    */
   public void testCaptureMultipleProxies() {
      MethodCapturer<TestInterface1> c = new MethodCapturer<TestInterface1>(TestInterface1.class);

      TestInterface1 t1 = c.capture();
      TestInterface1 t2 = c.capture();
      assertSame(t1, t2); // should we be testing this implementation detail here? maybe not...

      // various methods, regardless of what proxy (though test above confirms that they are really
      // all refs to the same proxy) always results in capturing only the most recent method
      t1.testMethod();
      t1.testMethod();
      t2.someOtherMethod(null, null);
      t1.testMethod();
      t2.testMethod(100);
      Method m = c.getMethod();
      assertEquals("testMethod", m.getName());
      assertTrue(Arrays.equals(new Class<?>[] { int.class }, m.getParameterTypes()));
      MethodSignature s = c.getSignature();
      assertEquals("testMethod", s.getName());
      assertEquals(Arrays.<Class<?>> asList(int.class), s.getParameterTypes());
   }

   /**
    * Tests that changes to the set provided to the constructor are not reflected in the capturer.
    * Also tests that set of interfaces returned from the capturer cannot be changed.
    */
   public void testCapturerDefendsSetOfInterfaces() {
      @SuppressWarnings("unchecked")
      HashSet<Class<? extends Object>> ifaces =
            new HashSet<Class<? extends Object>>(Arrays.asList(TestInterface1.class,
                  TestInterface2.class));

      MethodCapturer<Object> c = new MethodCapturer<Object>(ifaces);

      // verify that the set cannot be modified
      boolean caught = false;
      try {
         c.getInterfaces().add(TestInterface2.class);
      }
      catch (UnsupportedOperationException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         c.getInterfaces().remove(TestInterface1.class);
      }
      catch (UnsupportedOperationException e) {
         caught = true;
      }
      assertTrue(caught);

      // verify that changing the source has no impact on capturer
      assertEquals(ifaces, c.getInterfaces());
      // copy current interfaces
      HashSet<Class<? extends Object>> copyOfInterfaces =
            new HashSet<Class<? extends Object>>(c.getInterfaces());
      // modify original and make sure it doesn't effect the capturer
      ifaces.add(TestInterface3.class);
      assertEquals(2, c.getInterfaces().size()); // still only two items
      assertTrue(ifaces.size() != c.getInterfaces().size());
      assertFalse(ifaces.equals(c.getInterfaces())); // now different
      assertEquals(copyOfInterfaces, c.getInterfaces()); // unchanged
   }

   /**
    * Tests that constructor throws exceptions under error conditions (like null or invalid class
    * tokens specified).
    */
   public void testExceptionInConstructor() {
      boolean caught = false;
      try {
         Class<Object> clazz = null;
         @SuppressWarnings("unused")
         MethodCapturer<Object> c = new MethodCapturer<Object>(clazz);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         @SuppressWarnings({ "unused", "unchecked" })
         MethodCapturer<Object> c = new MethodCapturer<Object>(TestInterface1.class,
               TestInterface2.class, null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         @SuppressWarnings({ "unused", "unchecked" })
         // incompatible interfaces
         MethodCapturer<Object> c = new MethodCapturer<Object>(TestInterface2.class,
               TestInterface3.class);
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         @SuppressWarnings({ "unused", "unchecked" })
         // not an interface
         MethodCapturer<Object> c = new MethodCapturer<Object>(TestInterface1.class, Object.class);
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         @SuppressWarnings({ "unused", "unchecked" })
         // no classes specified
         MethodCapturer<Object> c = new MethodCapturer<Object>();
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
   }
}
