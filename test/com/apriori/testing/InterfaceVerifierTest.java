package com.apriori.testing;

import com.apriori.reflect.MethodCapturer;
import com.apriori.reflect.MethodSignature;
import com.apriori.testing.InterfaceVerifier.MethodConfiguration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Tests the functionality in {@link InterfaceVerifier}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class InterfaceVerifierTest extends TestCase {

   /*
    * Interfaces for testing verifier logic. These have numerous combinations of overloaded methods,
    * varying types of parameters and return values, and method overriding (one interface overriding
    * a method on its super-interface) in order to test various edge cases.
    */

   private interface TestParentInterface {
      void doSomething() throws IOException;

      int returnAnInt() throws CloneNotSupportedException;

      long getLongForString(String s);

      TestParentInterface build() throws IOException, CloneNotSupportedException;

      <E, W extends Collection<E>> W wrapObject(Class<E> cls);

      Object[] flatten(List<?>... lists);
   }

   private interface TestInterface1 extends TestParentInterface {
      @Override
      int returnAnInt();

      @Override
      TestInterface1 build() throws IOException, CloneNotSupportedException;

      void doSomething(boolean now, boolean later, boolean async);

      void doSomethingElse();

      Object doSomethingElse(Object o);
   }

   private interface TestInterface2 {
      Object doSomething(boolean build);

      Object[] doSomething(int nTimes);

      void doOtherThings();

      void doOtherThings(long l);

      int returnAnInt() throws IOException;
   }

   private interface TestInterface3 {
      // conflicting return type with TestInterface2
      <T> List<T> doSomething(int nTimes);

      void methodForInterface3();
   }

   /*
    * Simpler interface and default implementation (all no-op) to test other cases in method
    * configuration more concisely than with the bigger interfaces above.
    */

   private interface TestSimpleInterface {
      TestSimpleInterface returnsInterface();

      Object returnsObject();

      void returnsVoid();
   }

   private class TestSimpleInterfaceAdapter implements TestSimpleInterface {
      public TestSimpleInterfaceAdapter() {}

      @Override
      public TestSimpleInterface returnsInterface() {
         return this;
      }

      @Override
      public Object returnsObject() {
         return this;
      }

      @Override
      public void returnsVoid() {}
   }

   /**
    * Tests constructing an {@link InterfaceVerifier} with invalid arguments.
    */
   @SuppressWarnings("unchecked")
   public void testConstructorThrowsException() {
      // no interfaces specified - var arg
      boolean caught = false;
      try {
         new InterfaceVerifier<Object>();
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // no interfaces specified - set
      caught = false;
      try {
         new InterfaceVerifier<Object>(new HashSet<Class<?>>());
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // null class
      caught = false;
      try {
         new InterfaceVerifier<Object>((Class<Object>) null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      // null set
      caught = false;
      try {
         new InterfaceVerifier<Object>((Set<Class<? extends Object>>) null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      // not an interface
      caught = false;
      try {
         new InterfaceVerifier<Object>(HashSet.class);
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // incompatible interfaces
      caught = false;
      try {
         new InterfaceVerifier<Object>(TestInterface1.class, TestInterface2.class,
               TestInterface3.class);
      }
      catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link InterfaceVerifier#getInterfaces()}.
    */
   @SuppressWarnings("unchecked")
   public void testGetInterfaces() {
      // single class constructor
      @SuppressWarnings("rawtypes")
      InterfaceVerifier<List> ivList = new InterfaceVerifier<List>(List.class);

      Set<Object> expected = Collections.<Object> singleton(List.class);
      assertEquals(expected, ivList.getInterfaces());

      // var-arg constructor
      InterfaceVerifier<Object> ivMulti = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);

      expected = new HashSet<Object>(Arrays.asList(TestInterface1.class, TestInterface2.class));
      assertEquals(expected, ivMulti.getInterfaces());

      // set constructor
      Set<Class<?>> ifaces = new HashSet<Class<?>>(Arrays.asList(TestInterface1.class,
            TestInterface2.class));
      InterfaceVerifier<Object> ivSet = new InterfaceVerifier<Object>(ifaces);

      assertEquals(expected, ivSet.getInterfaces());
   }

   /**
    * Tests {@link InterfaceVerifier#allMethods()}.
    * 
    * @throws Exception if an exception occurs during a reflective call when determining expected
    *            return values for method under test
    */
   public void testAllMethods() throws Exception {
      // one interface
      InterfaceVerifier<TestInterface2> iv1 = new InterfaceVerifier<TestInterface2>(
            TestInterface2.class);
      Set<Method> methods = iv1.allMethods();
      Set<Method> expected = new HashSet<Method>(Arrays.asList(
            TestInterface2.class.getMethod("doSomething", boolean.class),
            TestInterface2.class.getMethod("doSomething", int.class),
            TestInterface2.class.getMethod("doOtherThings"),
            TestInterface2.class.getMethod("doOtherThings", long.class),
            TestInterface2.class.getMethod("returnAnInt")
            ));

      assertEquals(expected, methods);

      // multiple interfaces, including overridden methods
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv2 = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);
      methods = iv2.allMethods();
      // will include add'l methods from TestInterface1, including those inherited from its
      // super-interface TestParentInterface
      expected.addAll(Arrays.asList(
            TestInterface1.class.getMethod("build"), // return types aren't the same but are
            TestParentInterface.class.getMethod("build"), // co-variant, so we'll see *both*
// versions
            TestParentInterface.class.getMethod("doSomething"),
            TestInterface1.class.getMethod("doSomething", boolean.class, boolean.class,
                  boolean.class),
            TestInterface1.class.getMethod("doSomethingElse"),
            TestInterface1.class.getMethod("doSomethingElse", Object.class),
            TestParentInterface.class.getMethod("flatten", List[].class),
            TestParentInterface.class.getMethod("getLongForString", String.class),
            TestParentInterface.class.getMethod("wrapObject", Class.class),
            TestInterface1.class.getMethod("returnAnInt") // won't see other overridden version from
                                                          // TestParentInterface since they have
// same
      // return types
      ));

      assertEquals(expected, methods);
   }

   /**
    * Tests {@link InterfaceVerifier#allMethodSignatures()}.
    */
   public void testAllMethodSignatures() {
      // one interface
      InterfaceVerifier<TestInterface2> iv1 = new InterfaceVerifier<TestInterface2>(
            TestInterface2.class);
      Set<MethodSignature> methods = iv1.allMethodSignatures();
      Set<MethodSignature> expected = new HashSet<MethodSignature>(Arrays.asList(
            new MethodSignature("doSomething", boolean.class),
            new MethodSignature("doSomething", int.class),
            new MethodSignature("doOtherThings"),
            new MethodSignature("doOtherThings", long.class),
            new MethodSignature("returnAnInt")
            ));

      assertEquals(expected, methods);

      // multiple interfaces, including overridden methods
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv2 = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);
      methods = iv2.allMethodSignatures();
      // will include add'l methods from TestInterface1, including those inherited from its
      // super-interface TestParentInterface
      expected.addAll(Arrays.asList(
            new MethodSignature("build"),
            new MethodSignature("doSomething"),
            new MethodSignature("doSomething", boolean.class, boolean.class, boolean.class),
            new MethodSignature("doSomethingElse"),
            new MethodSignature("doSomethingElse", Object.class),
            new MethodSignature("flatten", List[].class),
            new MethodSignature("getLongForString", String.class),
            new MethodSignature("wrapObject", Class.class)
            // returnAnInt signature already in the set since TestInterface2 has method
            // with same signature
            ));

      assertEquals(expected, methods);
   }

   /**
    * Tests {@link InterfaceVerifier#findMethod(String)}.
    */
   public void testFindMethod() {
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);

      MethodSignature sig = new MethodSignature("build");

      // no wildcards
      assertEquals(sig, iv.findMethod("build"));

      // several wildcard variations
      assertEquals(sig, iv.findMethod("b*"));
      assertEquals(sig, iv.findMethod("b*i*"));
      assertEquals(sig, iv.findMethod("bu??d"));
      assertEquals(sig, iv.findMethod("bu?ld"));
      assertEquals(sig, iv.findMethod("*ild"));
      assertEquals(sig, iv.findMethod("?????"));

      // no such method
      assertNull(iv.findMethod("build??"));

      // too many methods
      boolean caught = false;
      try {
         iv.findMethod("do*");
      }
      catch (TooManyMatchesException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link InterfaceVerifier#findMethods(String)}.
    */
   public void testFindMethods() {
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);

      Set<MethodSignature> build = new HashSet<MethodSignature>(Arrays.asList(
            new MethodSignature("build")));

      // no wildcards
      assertEquals(build, iv.findMethods("build"));

      Set<MethodSignature> sigs = new HashSet<MethodSignature>(Arrays.asList(
            new MethodSignature("doSomething", boolean.class, boolean.class, boolean.class),
            new MethodSignature("doSomething", boolean.class),
            new MethodSignature("doSomething", int.class),
            new MethodSignature("doSomething")));
      assertEquals(sigs, iv.findMethods("doSomething"));

      // several wildcard variations
      assertEquals(build, iv.findMethods("b*"));
      assertEquals(build, iv.findMethods("b*i*"));
      assertEquals(build, iv.findMethods("bu??d"));
      assertEquals(build, iv.findMethods("bu?ld"));
      assertEquals(build, iv.findMethods("*ild"));
      assertEquals(build, iv.findMethods("?????"));

      // no such method
      assertTrue(iv.findMethods("build??").isEmpty());

      // many methods
      sigs = new HashSet<MethodSignature>(Arrays.asList(
            new MethodSignature("doSomethingElse"),
            new MethodSignature("doSomethingElse", Object.class),
            new MethodSignature("doSomething", boolean.class, boolean.class, boolean.class),
            new MethodSignature("doSomething", boolean.class),
            new MethodSignature("doSomething", int.class),
            new MethodSignature("doSomething"),
            new MethodSignature("doOtherThings"),
            new MethodSignature("doOtherThings", long.class)));
      assertEquals(sigs, iv.findMethods("do*"));

      sigs = new HashSet<MethodSignature>(Arrays.asList(
            new MethodSignature("flatten", List[].class),
            new MethodSignature("wrapObject", Class.class)));
      assertEquals(sigs, iv.findMethods("*a*"));
   }

   /**
    * Tests {@link InterfaceVerifier#getMethodNamed(String)}.
    */
   public void testGetMethodNamed() {
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);

      assertEquals(new MethodSignature("build"), iv.getMethodNamed("build"));

      // no such method
      assertNull(iv.getMethodNamed("buildIt"));

      // too many methods (overridden)
      boolean caught = false;
      try {
         iv.getMethodNamed("doSomething");
      }
      catch (TooManyMatchesException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link InterfaceVerifier#getMethodsNamed(String)}.
    */
   public void testGetMethodsNamed() {
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);

      assertEquals(Collections.singleton(new MethodSignature("build")), iv.getMethodsNamed("build"));

      // no such method
      assertTrue(iv.getMethodsNamed("buildIt").isEmpty());

      // many methods
      Set<MethodSignature> sigs = new HashSet<MethodSignature>(Arrays.asList(
            new MethodSignature("doSomething", boolean.class, boolean.class, boolean.class),
            new MethodSignature("doSomething", boolean.class),
            new MethodSignature("doSomething", int.class),
            new MethodSignature("doSomething")));
      assertEquals(sigs, iv.getMethodsNamed("doSomething"));
   }

   /**
    * Tests {@link InterfaceVerifier#getMethod(String, Class...)}.
    */
   public void testGetMethod() {
      @SuppressWarnings("unchecked")
      InterfaceVerifier<Object> iv = new InterfaceVerifier<Object>(TestInterface1.class,
            TestInterface2.class);

      assertEquals(new MethodSignature("build"), iv.getMethod("build"));

      // no such method
      assertNull(iv.getMethod("buildIt"));

      // overridden method
      assertEquals(new MethodSignature("doSomething"), iv.getMethod("doSomething"));
      assertEquals(new MethodSignature("doSomething", boolean.class, boolean.class, boolean.class),
            iv.getMethod("doSomething", boolean.class, boolean.class, boolean.class));
      // bad args for overridden method
      assertNull(iv.getMethod("doSomething", Object.class, int.class));
   }

   /**
    * Tests that the default configuration for a new {@link InterfaceVerifier} matches the
    * documentation. This covers attributes of the verifier itself as well as attributes for each
    * method configuration.
    */
   public void testDefaultConfiguration() {
      InterfaceVerifier<TestInterface1> iv = new InterfaceVerifier<TestInterface1>(
            TestInterface1.class);

      // first check verifier configuration
      assertSame(ObjectVerifiers.STRICT_EXCEPTIONS, iv.getDefaultExceptionVerifier());
      assertSame(ObjectVerifiers.EQUALS, iv.getDefaultMutatorVerifier());
      assertFalse(iv.isCheckHashCodesAfterMutation());
      assertTrue(iv.isSuppressExceptions());
      assertNull(iv.getLastException());

      // now check method configurations:
      // these props are the same across the board
      for (MethodSignature sig : iv.allMethodSignatures()) {
         MethodConfiguration<?> conf = iv.configureMethod(sig);
         // all null (i.e. no cloners and no verifiers)
         assertEquals(Collections.nCopies(sig.getParameterTypes().size(), null),
               conf.getArgumentCloners());
         assertFalse(conf.isCloningArguments());
         assertEquals(Collections.nCopies(sig.getParameterTypes().size(), null),
               conf.getArgumentVerifiers());
         assertFalse(conf.isVerifyingArguments());
         // default exception verifier
         assertSame(ObjectVerifiers.STRICT_EXCEPTIONS, conf.getExceptionVerifier());
      }
      // but we need to check return verifier for each method...
      // methods with no return type (void):
      MethodConfiguration<?> conf = iv.configureMethod(new MethodSignature("doSomething"));
      assertNull(conf.getReturnVerifier());
      assertSame(ObjectVerifiers.EQUALS, conf.getMutatorVerifier());
      assertTrue(conf.isMutator());
      conf = iv.configureMethod(new MethodSignature("doSomething", boolean.class, boolean.class,
            boolean.class));
      assertNull(conf.getReturnVerifier());
      assertSame(ObjectVerifiers.EQUALS, conf.getMutatorVerifier());
      assertTrue(conf.isMutator());
      conf = iv.configureMethod(new MethodSignature("doSomethingElse"));
      assertNull(conf.getReturnVerifier());
      assertSame(ObjectVerifiers.EQUALS, conf.getMutatorVerifier());
      assertTrue(conf.isMutator());
      // methods that return values:
      conf = iv.configureMethod(new MethodSignature("doSomethingElse", Object.class));
      assertSame(ObjectVerifiers.EQUALS, conf.getReturnVerifier());
      assertNull(conf.getMutatorVerifier());
      assertFalse(conf.isMutator());
      conf = iv.configureMethod(new MethodSignature("getLongForString", String.class));
      assertSame(ObjectVerifiers.EQUALS, conf.getReturnVerifier());
      assertNull(conf.getMutatorVerifier());
      assertFalse(conf.isMutator());
      conf = iv.configureMethod(new MethodSignature("flatten", List[].class));
      assertSame(ObjectVerifiers.EQUALS, conf.getReturnVerifier());
      assertNull(conf.getMutatorVerifier());
      assertFalse(conf.isMutator());
      conf = iv.configureMethod(new MethodSignature("returnAnInt"));
      assertSame(ObjectVerifiers.EQUALS, conf.getReturnVerifier());
      assertNull(conf.getMutatorVerifier());
      assertFalse(conf.isMutator());
      // methods that return interfaces:
      // (will use same implementation as this one)
      Class<?> ifaceVerifierClass = ObjectVerifiers.forTesting(List.class).getClass();
      conf = iv.configureMethod(new MethodSignature("build"));
      assertSame(ifaceVerifierClass, conf.getReturnVerifier().getClass());
      assertNull(conf.getMutatorVerifier());
      assertFalse(conf.isMutator());
      conf = iv.configureMethod(new MethodSignature("wrapObject", Class.class));
      assertSame(ifaceVerifierClass, conf.getReturnVerifier().getClass());
      assertNull(conf.getMutatorVerifier());
      assertFalse(conf.isMutator());
   }

   /**
    * Tests {@link InterfaceVerifier#setSuppressExceptions(boolean)} and
    * {@link InterfaceVerifier#isSuppressExceptions()}. This tests both the getter/setter methods as
    * well as the interpretation of the flag by the proxy invocation handler. Testing the invocation
    * handler also verifies the behavior of {@link InterfaceVerifier#getLastException()}.
    */
   public void testSuppressingExceptions() {
      InterfaceVerifier<Runnable> iv = new InterfaceVerifier<Runnable>(Runnable.class);

      Runnable rGood = new Runnable() {
         @Override
         public void run() {
            // no-op
         }
      };
      Runnable rBad = new Runnable() {
         @Override
         public void run() {
            throw new RuntimeException();
         }
      };

      // Suppressing exceptions = true

      iv.setSuppressExceptions(true);
      assertTrue(iv.isSuppressExceptions());
      assertNull(iv.getLastException());

      // both throw
      Runnable proxy = iv.createProxy(rBad, rBad);
      proxy.run();
      assertNotNull(iv.getLastException());
      assertSame(RuntimeException.class, iv.getLastException().getClass());

      // neither throw
      proxy = iv.createProxy(rGood, rGood);
      proxy.run();
      assertNull(iv.getLastException());

      // ref throws
      proxy = iv.createProxy(rGood, rBad);
      try {
         proxy.run();
         fail("Expected exception");
      }
      catch (AssertionFailedError expected) {}
      assertNull(iv.getLastException());

      // test throws
      proxy = iv.createProxy(rBad, rGood);
      try {
         proxy.run();
         fail("Expected exception");
      }
      catch (AssertionFailedError expected) {}
      assertNotNull(iv.getLastException());
      assertSame(RuntimeException.class, iv.getLastException().getClass());

      // Suppressing exceptions = false

      iv.setSuppressExceptions(false);
      assertFalse(iv.isSuppressExceptions());
      // last exception unchanged - same as from previous invocation
      assertNotNull(iv.getLastException());
      assertSame(RuntimeException.class, iv.getLastException().getClass());

      // both throw
      proxy = iv.createProxy(rBad, rBad);
      try {
         proxy.run();
         fail("Expected exception");
      }
      catch (RuntimeException expected) {}
      assertNotNull(iv.getLastException());
      assertSame(RuntimeException.class, iv.getLastException().getClass());

      // neither throw
      proxy = iv.createProxy(rGood, rGood);
      proxy.run();
      assertNull(iv.getLastException());

      // ref throws
      proxy = iv.createProxy(rGood, rBad);
      try {
         proxy.run();
         fail("Expected exception");
      }
      catch (AssertionFailedError expected) {}
      assertNull(iv.getLastException());

      // test throws
      proxy = iv.createProxy(rBad, rGood);
      try {
         proxy.run();
         fail("Expected exception");
      }
      catch (AssertionFailedError expected) {}
      assertNotNull(iv.getLastException());
      assertSame(RuntimeException.class, iv.getLastException().getClass());
   }

   /**
    * Tests {@link InterfaceVerifier#getDefaultMutatorVerifier()} and
    * {@link InterfaceVerifier#setDefaultMutatorVerifier(ObjectVerifier)}. Also verifies that the
    * configured verifier is also used, by default, when invoking mutator methods on a verifying
    * proxy.
    */
   public void testDefaultMutatorVerifier() {
      // mutator verifier
      final boolean used[] = { false };
      ObjectVerifier<TestSimpleInterface> verifier = new ObjectVerifier<TestSimpleInterface>() {
         @Override
         public TestSimpleInterface verify(TestSimpleInterface test, TestSimpleInterface reference) {
            used[0] = true;
            return test;
         };
      };

      InterfaceVerifier<TestSimpleInterface> iv = new InterfaceVerifier<TestSimpleInterface>(
            TestSimpleInterface.class);

      assertNotSame(verifier, iv.getDefaultMutatorVerifier());
      iv.setDefaultMutatorVerifier(verifier);
      assertSame(verifier, iv.getDefaultMutatorVerifier());

      // smoke test that it's using our verifier by default
      TestSimpleInterface proxy = iv.createProxy(new TestSimpleInterfaceAdapter(),
            new TestSimpleInterfaceAdapter());
      assertFalse(used[0]);
      proxy.returnsVoid();
      assertTrue(used[0]);

      // test for expected NPE
      try {
         iv.setDefaultMutatorVerifier(null);
         fail("Expected NPE");
      }
      catch (NullPointerException expected) {}
   }

   /**
    * Tests {@link InterfaceVerifier#isCheckHashCodesAfterMutation()} and
    * {@link InterfaceVerifier#setCheckHashCodesAfterMutation(boolean)}. Also tests that hash codes
    * are examined and used properly in verification depending on the setting of this flag.
    */
   public void testCheckingHashCodesAfterMutation() {
      // super-simple implementation to verify
      class TestHashCodeChecker extends TestSimpleInterfaceAdapter {
         private int hashCode[];
         private boolean equals[];

         TestHashCodeChecker(int hashCode[], boolean equals[]) {
            this.hashCode = hashCode;
            this.equals = equals;
         }

         @Override
         public int hashCode() {
            // just look at specified hash code and increment it so
            // there's evidence that we checked it
            return hashCode[0]++;
         }

         @Override
         public boolean equals(Object o) {
            return equals[0];
         }

         @Override
         public String toString() {
            // default toString() calls hashCode(), but we want to
            // isolate calls to hashCode better than that so we can
            // distinguish from places that verify hash code and
            // places that don't...
            return "abc";
         }
      }

      InterfaceVerifier<TestSimpleInterface> iv = new InterfaceVerifier<TestSimpleInterface>(
            TestSimpleInterface.class);
      boolean equals[] = { true };
      int hashCode1[] = { 999 }; // same hash code
      int hashCode2[] = { 999 };
      TestSimpleInterface proxy = iv.createProxy(new TestHashCodeChecker(hashCode1, equals),
            new TestHashCodeChecker(hashCode2, equals));

      iv.setCheckHashCodesAfterMutation(false);
      assertFalse(iv.isCheckHashCodesAfterMutation());
      // no checking of hash codes when this flag is false

      proxy.returnsVoid();
      // verify methods were not called
      assertEquals(999, hashCode1[0]);
      assertEquals(999, hashCode2[0]);

      iv.setCheckHashCodesAfterMutation(true);
      assertTrue(iv.isCheckHashCodesAfterMutation());
      // now flag is set so we should be checking hash codes

      proxy.returnsVoid();
      // verify methods were called
      assertEquals(1000, hashCode1[0]);
      assertEquals(1000, hashCode2[0]);

      // verify failure when hash codes don't match
      hashCode1[0] = 123;
      hashCode2[0] = 234;
      try {
         proxy.returnsVoid();
         fail("Expecting assertion failure");
      }
      catch (AssertionFailedError expected) {}
      assertEquals(124, hashCode1[0]);
      assertEquals(235, hashCode2[0]);

      // make sure we don't bother checking hash codes for objects that
      // are invalid per the mutator verifier
      equals[0] = false;
      hashCode1[0] = 999;
      hashCode2[0] = 999;
      try {
         proxy.returnsVoid();
         fail("Expecting assertion failure");
      }
      catch (AssertionFailedError expected) {}
      // verify methods were not called
      assertEquals(999, hashCode1[0]);
      assertEquals(999, hashCode2[0]);

      // make sure we don't check hash codes for non-mutator methods
      iv.configureMethod(new MethodSignature("returnsVoid")).notMutator();
      hashCode1[0] = 999;
      hashCode2[0] = 999;
      proxy.returnsVoid();
      // verify methods were not called
      assertEquals(999, hashCode1[0]);
      assertEquals(999, hashCode2[0]);
   }

   public void testDefaultExceptionVerifier() {
      // test get and set default exception verifier.
      // test that default is actually used by default.
      // test for expected NPE -- or change to allow null to mean relaxed?
      fail();
   }

   public void testSetStrictDefaultExceptionVerifier() {
      // test set strict exceptions w/ get default exception verifier.
      // verify strict exceptions used by default.
      fail();
   }

   public void testSetRelaxedDefaultExceptionVerifier() {
      // test set relaxed exceptions w/ get default exception verifier.
      // verify relaxed exceptions used by default.
      fail();
   }

   public void testCreatingProxies() {
      // test createProxy and verifierFor (don't test all invocation handler functions though).
      // also test edge cases that are expected to throw exceptions (including classloader issue?)
      fail();
   }

   /**
    * Tests {@link InterfaceVerifier#methodCapturer()}.
    */
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public void testMethodCapturer() {
      InterfaceVerifier<List> iv = new InterfaceVerifier<List>(List.class);
      MethodCapturer<List> cap = iv.methodCapturer();
      List list = cap.capture();

      // smoke test the capturer
      MethodSignature sig = cap.getSignature(list.add(null));
      assertEquals(sig, new MethodSignature("add", Object.class));
      // make sure no exception is thrown by verifier when we want
      // to configure this method
      iv.configureMethod(sig);
   }

   public void testCopyFrom() {
      // test copyFrom (this could be ugly/hard... how can we simplify this?
      // maybe implement InterfaceVerifier.equals [and method configurator impl]
      // and use that?)
      fail();
   }

   // now configuration for single methods...

   public void testConfigGetMethods() {
      // just getMethods
      fail();
   }

   public void testConfigGetReturnType() {
      // just getReturnType (include test of overloaded
      // methods w/ covariant return types)
      fail();
   }

   public void testConfigGetSignature() {
      // just getSignature
      fail();
   }

   public void testConfigGetCheckedExceptions() {
      // just getCheckedExceptions (include test of overloaded
      // methods w/ differing declared exceptions)
      fail();
   }

   public void testConfigMutatorVerifier() {
      // test both variants + notMutator + getMutatorVerifier + isMutator
      fail();
   }

   public void testConfigReturnVerifier() {
      // test both variants + getReturnVerifier
      fail();
   }

   public void testConfigExceptionVerifier() {
      // test exceptionVerifier and getExceptionVerifier
      fail();
   }

   public void testConfigStrictExceptionVerifier() {
      // test strictExceptionVerifier and getExceptionVerifier
      fail();
   }

   public void testConfigRelaxedExceptionVerifier() {
      // test relaxedExceptionVerifier and getExceptionVerifier
      fail();
   }

   public void testConfigUncheckedExceptions() {
      // also test getAllExceptions
      fail();
   }

   public void testConfigCloneAndVerifyArguments() {
      // also test noCloneAndVerifyArguments + isCloningArguments
      // + isVerifyingArguments
      fail();
   }

   public void testConfigCloningArguments() {
      // test all four variants + noCloneAndVerifyArguments
      // + getArgumentCloners + isCloningArguments
      fail();
   }

   public void testConfigVerifyingArguments() {
      // test all four variants + noVerifyArguments
      // + getArgumentVerifiers + isVerifyingArguments
      fail();
   }

   // and configuring multiple methods at once

   public void testConfigMultiMutatorVerifier() {
      // two variants + notMutator.
      // can use getMutatorVerifier() on individual
      // method configs to verify
      fail();
   }

   public void testConfigMultiReturnVerifier() {
      // two variants.
      // can use getReturnVerifier() on individual
      // method configs to verify
      fail();
   }

   public void testConfigMultiExceptionVerifier() {
      // can use getExceptionVerifier() on individual
      // method configs to verify
      fail();
   }

   public void testConfigMultiStrictExceptionVerifier() {
      // can use getExceptionVerifier() on individual
      // method configs to verify
      fail();
   }

   public void testConfigMultiRelaxedExceptionVerifier() {
      // can use getExceptionVerifier() on individual
      // method configs to verify
      fail();
   }

   public void testConfigMultiUncheckedExceptions() {
      // can use getUncheckedExceptions() and getAllExceptions()
      // on individual method configs to verify
      fail();
   }

   public void testConfigMultiCloneAndVerifyArguments() {
      // can use getUncheckedExceptions() and getAllExceptions()
      // on individual method configs to verify
      fail();
   }

   public void testConfigMultiCloningArguments() {
      // four variants + noCloneAndVerifyArguments
      // can use isCloningArguments and getArgumentCloners on
      // individual method configs to verify
      fail();
   }

   public void testConfigMultiVerifyingArguments() {
      // four variants + noVerifyArguments
      // can use isCloningArguments and getArgumentCloners on
      // individual method configs to verify
      fail();
   }
}
