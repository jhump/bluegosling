package com.bluegosling.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Tests the functionality in {@link MethodSignature}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MethodSignatureTest {

   /**
    * Tests creating a signature using a name and argument type list. Also tests the accessor
    * methods.
    */
   @Test public void methodSignature() {
      MethodSignature sig = new MethodSignature("myMethodName", int.class, String.class,
            Object[].class);

      assertEquals("myMethodName", sig.getName());
      assertEquals(Arrays.<Object> asList(int.class, String.class, Object[].class),
            sig.getParameterTypes());

      sig = new MethodSignature("myOtherMethodName");

      assertEquals("myOtherMethodName", sig.getName());
      assertEquals(Collections.EMPTY_LIST, sig.getParameterTypes());
   }

   /**
    * Tests creating a signature using a null name.
    */
   @Test public void methodSignatureNullName() {
      boolean caught = false;
      try {
         @SuppressWarnings("unused")
         MethodSignature sig = new MethodSignature(null, int.class, String.class, Object[].class);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         @SuppressWarnings("unused")
         MethodSignature sig = new MethodSignature(null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests creating a signature using a null argument type.
    */
   @Test public void methodSignatureNullArgType() {
      boolean caught = false;
      try {
         @SuppressWarnings("unused")
         MethodSignature sig = new MethodSignature("methodName", String.class, int.class,
               double[].class, null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         @SuppressWarnings("unused")
         MethodSignature sig = new MethodSignature("methodName", null, null);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests creating a signature using a java.lang.reflect.Method. Also tests the accessor methods.
    * 
    * @throws Exception If reflective method look-up fails
    */
   @Test public void methodSignatureForMethod() throws Exception {
      Method m = Arrays.class.getMethod("binarySearch", Object[].class, Object.class,
            Comparator.class);
      MethodSignature sig = new MethodSignature(m);

      assertEquals("binarySearch", sig.getName());
      assertEquals(Arrays.<Object> asList(Object[].class, Object.class, Comparator.class),
            sig.getParameterTypes());
   }

   /**
    * Tests creating a signature using a null method.
    */
   @Test public void methodSignatureNullMethod() {
      boolean caught = false;
      try {
         Method m = null;
         @SuppressWarnings("unused")
         MethodSignature sig = new MethodSignature(m);
      }
      catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests the implementation of {@code equals(Object)}.
    * 
    * @throws Exception If reflective method look-up fails
    */
   @Test public void equals() throws Exception {
      Method m = Arrays.class.getMethod("binarySearch", Object[].class, Object.class,
            Comparator.class);
      MethodSignature sig1 = new MethodSignature(m);
      MethodSignature sig2 = new MethodSignature("binarySearch", Object[].class, Object.class,
            Comparator.class);
      MethodSignature sig3 = new MethodSignature("binarySearch", double[].class, double.class);

      assertTrue(sig1.equals(sig2));
      assertTrue(sig2.equals(sig1));
      assertFalse(sig1.equals(sig3));
      assertFalse(sig3.equals(sig1));
      assertFalse(sig2.equals(sig3));
      assertFalse(sig3.equals(sig2));

      // null
      assertFalse(sig1.equals(null));
      // wrong class
      assertFalse(sig1.equals(new Object()));
   }

   /**
    * Tests the implementation of {@code hashCode()}.
    * 
    * @throws Exception If reflective method look-up fails
    */
   @Test public void computeHashCode() throws Exception {
      Method m = Arrays.class.getMethod("binarySearch", Object[].class, Object.class,
            Comparator.class);
      MethodSignature sig1 = new MethodSignature(m);
      MethodSignature sig2 = new MethodSignature("binarySearch", Object[].class, Object.class,
            Comparator.class);
      MethodSignature sig3 = new MethodSignature("binarySearch", double[].class, double.class);

      assertTrue(sig1.hashCode() == sig2.hashCode());
      assertTrue(sig1.hashCode() != sig3.hashCode());
      assertTrue(sig2.hashCode() != sig3.hashCode());
   }

   /**
    * Tests the implementation of {@code toString()}.
    */
   @Test public void convertToString() {
      MethodSignature sig = new MethodSignature("binarySearch", Object[].class, Object.class,
            Comparator.class);
      String sigStr = sig.toString();
      // shouldn't be empty and should at least contain reference to method name
      assertFalse(sigStr.isEmpty());
      assertTrue(sigStr.contains("binarySearch"));
   }
}
