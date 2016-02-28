package com.bluegosling.reflect;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

   public void testNoOptionsNeeded_autoboxIsLowerPriorityThanNoAutobox() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class1 {
         public String stringMethod() {
            return "abc";
         }
         public void voidMethodWithArgs(Object o, Integer i, String s) {
         }
         // autobox form will not be used:
         public Class<?> getSomeClass(Integer i, Number n) {
            return Class.class;
         }
         // this signature w/ primitive will be preferred:
         public Class<?> getSomeClass(int i, Number n) {
            return Object.class;
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
      assertSame(Object.class, i.getSomeClass(0, Double.valueOf(101010)));
      StringBuilder sb = new StringBuilder();
      assertSame(sb, i.getAnObject(sb));
      assertEquals(42, i.getLong());
   }
   
   public void testNoOptionsNeeded_dispatchToCompatibleVarArgs() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class1 {
         public String stringMethod() {
            return "abc";
         }
         public void voidMethodWithArgs(Object o, int i, Object... etc) {
            assertEquals(100, i);
            assertEquals(1, etc.length);
            assertEquals("xyz", etc[0]);
         }
         public Class<?> getSomeClass(int i, Number n, Object... objects) {
            return Object.class;
         }
         public Object getAnObject(StringBuilder... sb) {
            return sb;
         }
         public Long getLong(Object... objects) {
            return (long) objects.length;
         }
      }
      
      Interface1 i = Caster.builder(Interface1.class).build().cast(new Class1());
      assertTrue(Proxy.isProxyClass(i.getClass()));
      assertEquals("abc", i.stringMethod());
      i.voidMethodWithArgs(new Object(), 100, "xyz");
      assertSame(Object.class, i.getSomeClass(0, Double.valueOf(101010)));
      StringBuilder sb = new StringBuilder();
      assertTrue(Arrays.equals(new StringBuilder[] { sb }, (Object[]) i.getAnObject(sb)));
      assertEquals(0, i.getLong());
   }
   
   interface Interface2 {
      Long varArgsMethod(String s1, String s2, String... rest);
      String stringMethod(Integer i, Short s, Double d, Object... o);
      @SuppressWarnings("rawtypes")
      void listMethod(List l1, List l2);
   }
   
   public void testNoOptionsNeeded_repackageArgsForVarArgs() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         public Long varArgsMethod(String s, String... objects) {
            assertEquals(4, objects.length);
            assertEquals("def", objects[0]);
            assertEquals("mno", objects[1]);
            assertEquals("xyz", objects[2]);
            assertEquals("123", objects[3]);
            return 1L;
         }
         public String stringMethod(Integer i, Short s, Double d, Object objs[]) {
            return i.toString() + "," + s.toString() + "," + d.toString() + "," + objs.length;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      Interface2 i = Caster.builder(Interface2.class).build().cast(new Class2());
      assertTrue(Proxy.isProxyClass(i.getClass()));
      assertEquals(Long.valueOf(1), i.varArgsMethod("abc",  "def",  "mno", "xyz", "123"));
      assertEquals("1,2,3.0,3", i.stringMethod(1, (short) 2,  3.0, "x", "y", "z"));
   }

   public void testNoOptionsNeeded_missingMethod() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         // no varArgsMethod
         public String stringMethod(Integer i, Short s, Double d, Object objs[]) {
            return null;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      try {
         Caster.builder(Interface2.class).build().cast(new Class2());
         fail("Expected ClassCastException");
      } catch (ClassCastException expected) {}
   }

   public void testNoOptionsNeeded_methodHasTooFewArgs() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         public Long varArgsMethod(String s /* too few args */) {
            return null;
         }
         public String stringMethod(Integer i, Short s, Double d, Object objs[]) {
            return null;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      try {
         Caster.builder(Interface2.class).build().cast(new Class2());
         fail("Expected ClassCastException");
      } catch (ClassCastException expected) {}
   }

   public void testNoOptionsNeeded_methodHasBadArgType() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         public Long varArgsMethod(String s, String... objects) {
            return null;
         }
         public String stringMethod(Integer i, Short s, List<Double> d /* shouldn't be list */,
               Object objs[]) {
            return null;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      try {
         Caster.builder(Interface2.class).build().cast(new Class2());
         fail("Expected ClassCastException");
      } catch (ClassCastException expected) {}
   }
   
   public void testNoOptionsNeeded_methodHasBadReturnType() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         public Long varArgsMethod(String s, String... objects) {
            return null;
         }
         public Integer /* should be String */ stringMethod(Integer i, Short s, Double d,
               Object objs[]) {
            return null;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      try {
         Caster.builder(Interface2.class).build().cast(new Class2());
         fail("Expected ClassCastException");
      } catch (ClassCastException expected) {}
   }

   public void testNoOptionsNeeded_willNotCast() {
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         public Long varArgsMethod(String s, String... objects) {
            return null;
         }
         public String stringMethod(Integer i, Short s, Double d, Interface2 objs[]) {
            return null;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      try {
         Caster.builder(Interface2.class).build().cast(new Class2());
         fail("Expected ClassCastException");
      } catch (ClassCastException expected) {}
   }

   public void testCastingArguments_castArgSuccessful() {
      final AtomicReference<Object> o1 = new AtomicReference<Object>();
      final AtomicReference<Object> o2 = new AtomicReference<Object>();
      @SuppressWarnings("unused") // they're only used via reflection
      class Class2 {
         public Long varArgsMethod(String s, String... objects) {
            return null;
         }
         public String stringMethod(Integer i, Short s, Double d, Interface2... objs) {
            assertEquals(2, objs.length);
            o1.set(objs[0]);
            o2.set(objs[1]);
            return null;
         }
         public void listMethod(List<String> l1, List<String> l2) {
         }
      }
      
      Interface2 i = Caster.builder(Interface2.class).castingArguments().build().cast(new Class2());
      Class2 c = new Class2();
      i.stringMethod(1, (short) 2, 3.0, i, c);
      assertSame(i, o1.get());
      assertNotSame(c, o2.get());
      Interface2 i2 = (Interface2) o2.get();
      i2.stringMethod(1, (short) 2, 3.0, i, c);
   }
   
   // TODO: lots more tests
}
