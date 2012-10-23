package com.apriori.reflect;

import java.lang.reflect.Proxy;

import junit.framework.TestCase;

/**
 * Tests the functionality in {@link Caster}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class CasterTest extends TestCase {

   private interface Interface1 {
      String stringMethod();
      void voidMethodWithArgs(Object o, int i, String s);
      Class<?> getSomeClass(int i, Number n);
      Object getAnObject(StringBuilder sb);
      long getLong();
   }
   
   public void testNoOptionsNeeded_perfectMatch() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class1 {
         public String stringMethod() {
            return "abc";
         }
         public void voidMethodWithArgs(Object o, int i, String s) {
         }
         public Class<?> getSomeClass(int i, Number n) {
            return Class.class;
         }
         public Object getAnObject(StringBuilder sb) {
            return sb;
         }
         public long getLong() {
            return 42;
         }
      }
      
      Interface1 i = Caster.builder(Interface1.class).build().cast(new Class1());
      assertTrue(Proxy.isProxyClass(i.getClass()));
      assertEquals("abc", i.stringMethod());
      i.voidMethodWithArgs(new Object(), 100, "xyz");
      assertSame(Class.class, i.getSomeClass(0, Double.valueOf(101010)));
      StringBuilder sb = new StringBuilder();
      assertSame(sb, i.getAnObject(sb));
      assertEquals(42, i.getLong());
   }

   public void testNoOptionsNeeded_alreadyIsInstance() {
      class Class1 implements Interface1 {
         @Override public String stringMethod() {
            return "abc";
         }
         @Override public void voidMethodWithArgs(Object o, int i, String s) {
         }
         @Override public Class<?> getSomeClass(int i, Number n) {
            return Class.class;
         }
         @Override public Object getAnObject(StringBuilder sb) {
            return sb;
         }
         @Override public long getLong() {
            return 42;
         }
      }
      
      Class1 o = new Class1();
      Interface1 i = Caster.builder(Interface1.class).build().cast(o);
      assertFalse(Proxy.isProxyClass(i.getClass()));
      assertSame(o, i);
   }

   public void testNoOptionsNeeded_methodsAreCompatible() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class1 {
         // return types and arg types differ from Interface1 but are compatible
         public String stringMethod() {
            return "abc";
         }
         public void voidMethodWithArgs(Object o1, long i, Object o2) {
         }
         public Class<?> getSomeClass(double d, Number n) {
            return Class.class;
         }
         public String getAnObject(Object o) {
            return o.toString();
         }
         public byte getLong() {
            return 42;
         }
      }
      
      Interface1 i = Caster.builder(Interface1.class).build().cast(new Class1());
      assertTrue(Proxy.isProxyClass(i.getClass()));
      assertEquals("abc", i.stringMethod());
      i.voidMethodWithArgs(new Object(), 100, "xyz");
      assertSame(Class.class, i.getSomeClass(0, Double.valueOf(101010)));
      assertEquals("", i.getAnObject(new StringBuilder()));
      assertEquals(42, i.getLong());
   }
   
   public void testNoOptionsNeeded_incompatibleArgType() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class1 {
         public String stringMethod() {
            return "abc";
         }
         public void voidMethodWithArgs(String s1 /* String not assignable from Object */, int i,
               String s2) {
         }
         public Class<?> getSomeClass(int i, Number n) {
            return Class.class;
         }
         public Object getAnObject(StringBuilder sb) {
            return sb;
         }
         public long getLong() {
            return 42;
         }
      }
      
      try {
         Caster.builder(Interface1.class).build().cast(new Class1());
         fail("expected ClassCastException");
      } catch (ClassCastException expected) {
      }
   }
   
   public void testNoOptionsNeeded_autobox() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class1 {
         public String stringMethod() {
            return "abc";
         }
         public void voidMethodWithArgs(Object o, Integer i, String s) {
         }
         public Class<?> getSomeClass(Integer i, Number n) {
            return Class.class;
         }
         public Object getAnObject(StringBuilder sb) {
            return sb;
         }
         public Long getLong() {
            return 42L;
         }
      }
      
      Interface1 i = Caster.builder(Interface1.class).build().cast(new Class1());
      assertTrue(Proxy.isProxyClass(i.getClass()));
      assertEquals("abc", i.stringMethod());
      i.voidMethodWithArgs(new Object(), 100, "xyz");
      assertSame(Class.class, i.getSomeClass(0, Double.valueOf(101010)));
      StringBuilder sb = new StringBuilder();
      assertSame(sb, i.getAnObject(sb));
      assertEquals(42, i.getLong());
   }

   // TODO: lots more tests
}
