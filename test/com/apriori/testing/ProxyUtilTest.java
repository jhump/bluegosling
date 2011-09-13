package com.apriori.testing;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

/**
 * Tests the functionality in {@link ProxyUtil}.
 * 
 * @author jhumphries
 */
public class ProxyUtilTest extends TestCase {

   // helper interface with methods of various return types to
   // make sure "null" values returned by getNullReturnValue()
   // are appropriate
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
   
   /**
    * Tests {@link ProxyUtil#getNullReturnValue(Class)}.
    */
   public void testGetNullReturnValue() {
      // use an actual proxy so we not only check that returned values are what we
      // expect but further verify that expected values are appropriate and correct
      // (and won't generate NullPointerExceptions or ClassCastExceptions)
      TestInterface proxy = (TestInterface) Proxy.newProxyInstance(TestInterface.class.getClassLoader(),
            new Class<?>[] { TestInterface.class }, new InvocationHandler() {
               @Override
               public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                  // method under test!
                  return ProxyUtil.getNullReturnValue(method.getReturnType());
               }
      });
      
      // this just makes sure no exceptions occurs (no return value to inspect)
      proxy.voidMethod();
      // zeroes
      assertEquals(0, proxy.intMethod());
      assertEquals(0, proxy.shortMethod());
      assertEquals(0, proxy.byteMethod());
      assertEquals(0, proxy.longMethod());
      assertEquals(0, proxy.charMethod());
      assertEquals(0.0, proxy.doubleMethod());
      assertEquals(0.0F, proxy.floatMethod());
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
}
