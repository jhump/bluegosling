package com.apriori.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import com.apriori.util.Cloner;
import com.apriori.util.Cloners;
import com.apriori.util.CloningException;

import junit.framework.TestCase;

/**
 * Tests the functionality in {@link Cloners}.
 * 
 * @author jhumphries
 */
public class ClonersTest extends TestCase {

   /**
    * Tests {@link Cloners#checkClone(Object, Object)}.
    */
   public void testCheckClone() {
      Object o1 = new ArrayList<Object>(Arrays.asList(1, 2, 3));
      Object o2 = new ArrayList<Object>(Arrays.asList(1, 2, 3));
      Object o3 = new ArrayList<Object>(Arrays.asList(4, 5, 6));
      Object o4 = new LinkedList<Object>(Arrays.asList(1, 2, 3));
      
      // shouldn't throw an exception
      Cloners.checkClone(o1, o2);
      // even if they aren't equal
      Cloners.checkClone(o1, o3);

      // same instance
      boolean caught = false;
      try {
         Cloners.checkClone(o1, o1);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // not same class
      caught = false;
      try {
         Cloners.checkClone(o1, o4);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);

      // nulls
      caught = false;
      try {
         Cloners.checkClone(o1, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.checkClone(null, o1);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   // Cloneable classes for method below
   
   private class SimpleCloneable implements Cloneable {
      public int int1, int2;
      public String string1, string2;
      
      public SimpleCloneable() { }
      public Object getOuter() { return ClonersTest.this; };
      
      @Override
      protected Cloneable clone() {
         try {
            return (Cloneable) super.clone();
         } catch (CloneNotSupportedException e) {
            // should never happen
            throw new RuntimeException(e);
         }
      }
   }

   private static class SimplerCloneable implements Cloneable {
      // doesn't even override clone()
      public String name;
      public SimplerCloneable() { }
   }

   /**
    * Tests {@link Cloners#forCloneable()}.
    */
   public void testForCloneable() {
      SimpleCloneable obj = new SimpleCloneable();
      obj.int1 = 100; obj.int2 = 200;
      obj.string1 = "abc"; obj.string2 = "def";
      Cloner<SimpleCloneable> objCloner = Cloners.forCloneable();
      SimpleCloneable clone1 = objCloner.clone(obj);
      SimpleCloneable clone2 = objCloner.clone(obj);

      assertNotSame(obj, clone1);
      assertNotSame(obj, clone2);
      assertNotSame(clone1, clone2);
      assertSame(SimpleCloneable.class, clone1.getClass());
      assertSame(SimpleCloneable.class, clone2.getClass());
      assertEquals(obj.int1, clone1.int1);
      assertEquals(obj.int2, clone1.int2);
      assertSame(obj.string1, clone1.string1);
      assertSame(obj.string2, clone1.string2);
      assertSame(this, clone1.getOuter());
      assertEquals(obj.int1, clone2.int1);
      assertEquals(obj.int2, clone2.int2);
      assertSame(obj.string1, clone2.string1);
      assertSame(obj.string2, clone2.string2);
      assertSame(this, clone2.getOuter());

      // another cloneable that doesn't actually declare/override clone()
      SimplerCloneable s = new SimplerCloneable();
      s.name = "foobar";
      Cloner<SimplerCloneable> sCloner = Cloners.forCloneable();
      SimplerCloneable sClone1 = sCloner.clone(s);
      SimplerCloneable sClone2 = sCloner.clone(s);

      assertNotSame(s, sClone1);
      assertNotSame(s, sClone2);
      assertNotSame(sClone1, sClone2);
      assertSame(SimplerCloneable.class, sClone1.getClass());
      assertSame(SimplerCloneable.class, sClone2.getClass());
      assertEquals(s.name, sClone1.name);
      assertEquals(s.name, sClone2.name);

      // arrays are cloneables, too
      Object array[] = new Object[] { new SimplerSerializable(), "abc", "def", new Object[] { 1, 2, 3 } };
      Cloner<Object[]> arrayCloner = Cloners.forCloneable();
      Object clonedArray1[] = arrayCloner.clone(array);
      Object clonedArray2[] = arrayCloner.clone(array);
      
      assertNotSame(array, clonedArray1);
      assertNotSame(array, clonedArray2);
      assertNotSame(clonedArray1, clonedArray2);
      assertSame(Object[].class, clonedArray1.getClass());
      assertSame(Object[].class, clonedArray2.getClass());
      assertEquals(array.length, clonedArray1.length);
      assertEquals(array.length, clonedArray2.length);
      // assert that they are not "deep" copies (because that's not how arrays are cloned)
      for (int i = 0; i < array.length; i++) {
         assertSame(array[i], clonedArray1[i]);
         assertSame(array[i], clonedArray2[i]);
      }
   }
   
   // Serializable classes for method below
   
   @SuppressWarnings("serial")
   private class SimpleSerializable implements Serializable {
      public String name;
      public int val;
      
      public SimpleSerializable() { }
      public Object getOuter() { return ClonersTest.this; };
      
      private void writeObject(ObjectOutputStream out) throws IOException {
         out.writeObject("boogers!");
         out.write(name.getBytes());
         out.write(new byte[] { 0 });
         out.writeObject(val);
         out.writeObject(null);
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         name = (String) in.readObject(); // intentionally out of order
         // consume previously written name
         while (in.readByte() != 0);
         
         in.readObject();
         Object newVal = in.readObject();
         val = newVal == null ? -1 : (Integer) newVal;
      }
   }

   @SuppressWarnings("serial")
   private static class SimplerSerializable implements Serializable {
      public int int1;
      transient public int int2;
      
      public SimplerSerializable() { }

      @Override public boolean equals(Object o) {
         if (o instanceof SimplerSerializable) {
            return int1 == ((SimplerSerializable) o).int1;
         }
         return false;
      }
      @Override public int hashCode() {
         return int1;
      }
   }

   // extracted out part of testForSerializable so it can be re-used to test
   // GENERIC_CLONER and cloner returned from defaultClonerFor().
   private void doSerializableClonerTest(Cloner<SimpleSerializable> cloner) {
      SimpleSerializable obj = new SimpleSerializable();
      obj.name = "sweet!";
      obj.val = 101;
      SimpleSerializable clone1 = cloner.clone(obj);
      SimpleSerializable clone2 = cloner.clone(obj);

      assertNotSame(obj, clone1);
      assertNotSame(obj, clone2);
      assertNotSame(clone1, clone2);
      assertSame(SimpleSerializable.class, clone1.getClass());
      assertSame(SimpleSerializable.class, clone2.getClass());
      // this has custom serialization so objects won't actually match...
      assertEquals("boogers!", clone1.name);
      assertEquals("boogers!", clone2.name);
      assertEquals(-1, clone1.val);
      assertEquals(-1, clone2.val);
      assertNull(clone1.getOuter());
      assertNull(clone2.getOuter());
   }

   /**
    * Tests {@link Cloners#forSerializable()}.
    */
   public void testForSerializable() {
      Cloner<SimpleSerializable> objCloner = Cloners.forSerializable();
      doSerializableClonerTest(objCloner);

      // another serializable w/out custom serialization
      SimplerSerializable s = new SimplerSerializable();
      s.int1 = 1001; s.int2 = 2002;
      Cloner<SimplerSerializable> sCloner = Cloners.forSerializable();
      SimplerSerializable sClone1 = sCloner.clone(s);
      SimplerSerializable sClone2 = sCloner.clone(s);

      assertNotSame(s, sClone1);
      assertNotSame(s, sClone2);
      assertNotSame(sClone1, sClone2);
      assertSame(SimplerSerializable.class, sClone1.getClass());
      assertSame(SimplerSerializable.class, sClone2.getClass());
      assertEquals(s.int1, sClone1.int1);
      assertEquals(0, sClone1.int2); // this field was marked transient
      assertEquals(s.int1, sClone2.int1);
      assertEquals(0, sClone2.int2);

      // arrays are serializables, too
      Object array[] = new Object[] { new SimplerSerializable(), "abc", "def", new Object[] { 1, 2, 3 } };
      Cloner<Object[]> arrayCloner = Cloners.forSerializable();
      Object clonedArray1[] = arrayCloner.clone(array);
      Object clonedArray2[] = arrayCloner.clone(array);
      
      assertNotSame(array, clonedArray1);
      assertNotSame(array, clonedArray2);
      assertNotSame(clonedArray1, clonedArray2);
      assertSame(Object[].class, clonedArray1.getClass());
      assertSame(Object[].class, clonedArray2.getClass());
      assertEquals(array.length, clonedArray1.length);
      assertEquals(array.length, clonedArray2.length);
      // assert that they are "deep" copies since this cloner will serialize and
      // then de-serialize entire object graph
      for (int i = 0; i < array.length; i++) {
         assertNotSame(array[i], clonedArray1[i]);
         assertNotSame(array[i], clonedArray2[i]);
         assertSame(array[i].getClass(), clonedArray1[i].getClass());
         assertSame(array[i].getClass(), clonedArray2[i].getClass());
      }
      assertTrue(Arrays.deepEquals(array, clonedArray1));
      assertTrue(Arrays.deepEquals(array, clonedArray2));
   }
   
   // Interfaces and classes for testing argument specificity when looking up
   // copy methods and copy constructors. These mirror the example given in the
   // javadoc for Cloners.findCopyConstructor().
   interface Interface1A1 { }
   interface Interface1A extends Interface1A1 { }
   interface Interface1B { }
   interface Interface1 extends Interface1A, Interface1B { }
   interface Interface2A { }
   interface Interface2B { }
   interface Interface2 extends Interface2A, Interface2B { }
   interface Interface3 { }
   
   static class Class1 { // implicitly extends Object
      // for testing lookup of instance copy method in ancestor classes
      public Class1 doClone() { return new MyClass_MyClass(this); }
   }
   static class Class2 extends Class1 implements Interface2, Interface3 { }
   static class Class3 extends Class2 {
      public Class<?> copySource;
      public Class3(Class<?> copySource) { this.copySource = copySource; }
      // for testing lookup of instance copy method in ancestor classes
      public Class3 makeClone() { return new MyClass_MyClass(this); }
      // for testing to make sure lookup of static method does not look at super-classes
      public static MyClass_MyClass otherStaticCopy(MyClass_MyClass other) { return new MyClass_MyClass(other); }
   }
   
   // Multiple leaf classes w/ different mixes of constructors
   
   @SuppressWarnings("unused")
   static class MyClass_MyClass extends Class3 implements Interface1 {
      // testing constructors
      MyClass_MyClass(MyClass_MyClass other, String str) { super(null); fail("wrong constructor"); }
      MyClass_MyClass(MyClass_MyClass other) { super(MyClass_MyClass.class); }
      MyClass_MyClass(MyClass_MyClass other, int i) { super(null); fail("wrong constructor"); }
      MyClass_MyClass(Class3 other) { super(Class3.class); }
      MyClass_MyClass(Class2 other) { super(Class2.class); }
      MyClass_MyClass(Class1 other) { super(Class1.class); }
      // testing static copy methods
      static MyClass_MyClass staticCopy(MyClass_MyClass other) { return new MyClass_MyClass(other); }
      static MyClass_MyClass staticCopyBadSignature(MyClass_MyClass other, int i) { return new MyClass_MyClass(other); }
      static Object staticCopyBadReturn(MyClass_MyClass other) { return new MyClass_Class3(other); }
      static MyClass_MyClass staticCopy(Class3 other) { return new MyClass_MyClass(other); }
      static MyClass_MyClass staticCopy(Class2 other) { return new MyClass_MyClass(other); }
      static MyClass_MyClass staticCopy(Class1 other) { return new MyClass_MyClass(other); }
      // instance copy methods
      MyClass_MyClass instanceCopyBadSignature(MyClass_MyClass other) { return new MyClass_MyClass(other); }
      MyClass_MyClass instanceCopy() { return new MyClass_MyClass(this); }
   }

   @SuppressWarnings("unused")
   static class MyClass_Class3 extends Class3 implements Interface1 {
      // testing constructors: no constructor that takes current class falls back to super-class
      MyClass_Class3(Class3 other) { super(Class3.class); }
      MyClass_Class3(Class2 other) { super(Class2.class); }
      MyClass_Class3(Class1 other) { super(Class1.class); }
      // testing static copy methods
      static MyClass_Class3 staticCopy(Class3 other) { return new MyClass_Class3(other); }
      static MyClass_Class3 staticCopy(Class2 other) { return new MyClass_Class3(other); }
      static MyClass_Class3 staticCopy(Class1 other) { return new MyClass_Class3(other); }
   }

   @SuppressWarnings("unused")
   static class MyClass_Class1 extends Class3 implements Interface1 {
      // testing constructors: super-classes over interfaces
      MyClass_Class1(Class1 other) { super(Class1.class); }
      MyClass_Class1(Interface1 other) { super(Interface1.class); }
      MyClass_Class1(Interface2 other) { super(Interface2.class); }
      // testing static copy methods
      static MyClass_Class1 staticCopy(Class1 other) { return new MyClass_Class1(other); }
      static MyClass_Class1 staticCopy(Interface1 other) { return new MyClass_Class1(other); }
      static MyClass_Class1 staticCopy(Interface2 other) { return new MyClass_Class1(other); }
   }
   
   @SuppressWarnings("unused")
   static class MyClass_Interface1A1 extends Class3 implements Interface1 {
      // testing constructors: interfaces over Object
      MyClass_Interface1A1(Interface1A1 other) { super(Interface1A1.class); }
      MyClass_Interface1A1(Interface2A other) { super(Interface2A.class); }
      MyClass_Interface1A1(Object other) { super(Object.class); }
      // testing static copy methods
      static MyClass_Interface1A1 staticCopy(Interface1A1 other) { return new MyClass_Interface1A1(other); }
      static MyClass_Interface1A1 staticCopy(Interface2A other) { return new MyClass_Interface1A1(other); }
      static MyClass_Interface1A1 staticCopy(Object other) { return new MyClass_Interface1A1(other); }
   }

   @SuppressWarnings("unused")
   static class MyClass_Interface1 extends Class3 implements Interface1 {
      // testing constructors: ambiguous specificity (between Interface1A
      // and Interface1B) but shouldn't matter because Interface1 is more
      // specific and should be used
      MyClass_Interface1(Interface1 other) { super(Interface1.class); }
      MyClass_Interface1(Interface1A other) { super(Interface1A.class); }
      MyClass_Interface1(Interface1B other) { super(Interface1B.class); }
      // testing static copy methods
      static MyClass_Interface1 staticCopy(Interface1 other) { return new MyClass_Interface1(other); }
      static MyClass_Interface1 staticCopy(Interface1A other) { return new MyClass_Interface1(other); }
      static MyClass_Interface1 staticCopy(Interface1B other) { return new MyClass_Interface1(other); }
   }

   @SuppressWarnings("unused")
   static class MyClass_Interface1A_Interface1B extends Class3 implements Interface1 {
      // testing constructors: ambiguous specificity due to these two
      MyClass_Interface1A_Interface1B(Interface1A other) { super(Interface1A.class); }
      MyClass_Interface1A_Interface1B(Interface1B other) { super(Interface1B.class); }
      MyClass_Interface1A_Interface1B(Interface1A1 other) { super(Interface1A1.class); }
      // testing static copy methods
      static MyClass_Interface1A_Interface1B staticCopy(Interface1A other) { return new MyClass_Interface1A_Interface1B(other); }
      static MyClass_Interface1A_Interface1B staticCopy(Interface1B other) { return new MyClass_Interface1A_Interface1B(other); }
      static MyClass_Interface1A_Interface1B staticCopy(Interface1A1 other) { return new MyClass_Interface1A_Interface1B(other); }
   }
   
   @SuppressWarnings("unused")
   static class MyClass_Object extends Class3 implements Interface1 {
      // testing constructors: last resort is Object
      MyClass_Object(Object other) { super(Object.class); }
      MyClass_Object() { super(null); }
      // testing static copy methods
      static MyClass_Object staticCopy(Object other) { return new MyClass_Object(other); }
      static MyClass_Object staticCopy() { return new MyClass_Object(null); }
   }

   @SuppressWarnings("unused")
   static class MyClass_None extends Class3 implements Interface1 {
      // testing constructors: no valid constructors
      MyClass_None() { super(null); }
      MyClass_None(int i) { super(null); fail("wrong constructor"); }
      MyClass_None(List<?> l) { super(null); fail("wrong constructor"); }
      // testing static copy methods
      static MyClass_None staticCopy() { return new MyClass_None(); }
      static MyClass_None staticCopy(int i) { return new MyClass_None(); }
      static MyClass_None staticCopy(List<?> l) { return new MyClass_None(); }
   }

   // helper method for invoking findCopyConstructor and checking its result
   private void doTestFindCopyConstructor(Class<?> clazz, Class<?> argType) {
      Constructor<?> c = Cloners.findCopyConstructor(clazz);
      assertSame(clazz, c.getDeclaringClass());
      assertEquals(1, c.getParameterTypes().length);
      assertEquals(argType, c.getParameterTypes()[0]);
   }
   
   /**
    * Tests {@link Cloners#findCopyConstructor(Class)}.
    */
   public void testFindCopyConstructor() {
      // make sure it finds the correct constructor for each one
      doTestFindCopyConstructor(MyClass_MyClass.class, MyClass_MyClass.class);
      doTestFindCopyConstructor(MyClass_Class3.class, Class3.class);
      doTestFindCopyConstructor(MyClass_Class1.class, Class1.class);
      doTestFindCopyConstructor(MyClass_Interface1.class, Interface1.class);
      doTestFindCopyConstructor(MyClass_Interface1A1.class, Interface1A1.class);
      doTestFindCopyConstructor(MyClass_Object.class, Object.class);

      // no such constructor
      assertNull(Cloners.findCopyConstructor(MyClass_None.class));
      
      // ambiguous
      boolean caught = false;
      try {
         Cloners.findCopyConstructor(MyClass_Interface1A_Interface1B.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // npe
      caught = false;
      try {
         Cloners.findCopyConstructor(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link Cloners#withCopyConstructor(Constructor)}.
    * 
    * @throws Exception on error
    */
   public void testWithCopyConstructor() throws Exception {
      // too many parameters
      boolean caught = false;
      Constructor<?> cons = MyClass_MyClass.class.getDeclaredConstructor(MyClass_MyClass.class, String.class);
      try {
         Cloners.withCopyConstructor(cons);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // primitive parameter
      caught = false;
      cons = MyClass_None.class.getDeclaredConstructor(int.class);
      try {
         Cloners.withCopyConstructor(cons);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // incompatible parameter
      caught = false;
      cons = MyClass_None.class.getDeclaredConstructor(List.class);
      try {
         Cloners.withCopyConstructor(cons);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // npe
      caught = false;
      try {
         Cloners.withCopyConstructor((Constructor<Class3>) null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // 1 arg
      Cloner<MyClass_MyClass> cloner1 =
            Cloners.withCopyConstructor(MyClass_MyClass.class.getDeclaredConstructor(
                  MyClass_MyClass.class));
      MyClass_MyClass obj1 = new MyClass_MyClass(null);
      // make the clone
      Class3 clone = cloner1.clone(obj1);
      
      assertNotSame(obj1, clone);
      assertSame(obj1.getClass(), clone.getClass());
      assertSame(MyClass_MyClass.class, clone.copySource);
      
      // no arg
      Cloner<MyClass_None> cloner2 =
            Cloners.withCopyConstructor(MyClass_None.class.getDeclaredConstructor());
      MyClass_None obj2 = new MyClass_None();
      // make the clone
      clone = cloner2.clone(obj2);
      
      assertNotSame(obj2, clone);
      assertSame(obj2.getClass(), clone.getClass());
      assertNull(clone.copySource);
   }
   
   // helper method for invoking withCopyConstructor and checking its result
   private <T extends Class3> void doTestWithCopyConstructor(T obj, Class<T> clazz, Class<?> argType) {
      Cloner<T> c = Cloners.withCopyConstructor(clazz);
      T clone = c.clone(obj);
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      assertSame(argType, clone.copySource);
   }

   /**
    * Tests {@link Cloners#withCopyConstructor(Class)}.
    */
   public void testWithCopyConstructorFind() {
      // make sure it finds the correct constructor for each one
      doTestWithCopyConstructor(new MyClass_MyClass(null), MyClass_MyClass.class, MyClass_MyClass.class);
      doTestWithCopyConstructor(new MyClass_Class3(null), MyClass_Class3.class, Class3.class);
      doTestWithCopyConstructor(new MyClass_Class1((Class1) null), MyClass_Class1.class, Class1.class);
      doTestWithCopyConstructor(new MyClass_Interface1(null), MyClass_Interface1.class, Interface1.class);
      doTestWithCopyConstructor(new MyClass_Interface1A1((Interface1A1) null), MyClass_Interface1A1.class, Interface1A1.class);
      doTestWithCopyConstructor(new MyClass_Object(null), MyClass_Object.class, Object.class);

      // no such constructor
      boolean caught = false;
      try {
         Cloners.withCopyConstructor(MyClass_None.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // ambiguous
      caught = false;
      try {
         Cloners.withCopyConstructor(MyClass_Interface1A_Interface1B.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // npe
      caught = false;
      try {
         Cloners.withCopyConstructor((Class<?>) null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   /**
    * Tests {@link Cloners#withCopyConstructor(Class, Class)}.
    */
   public void testWithCopyConstructorSpecific() {
      // npe
      boolean caught = false;
      try {
         Cloners.withCopyConstructor(Class3.class, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.withCopyConstructor(null, Class3.class);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // no such constructor
      caught = false;
      try {
         Cloners.withCopyConstructor(MyClass_MyClass.class, Interface1.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // a valid one
      Cloner<MyClass_MyClass> cloner =
            Cloners.withCopyConstructor(MyClass_MyClass.class, Class3.class);
      MyClass_MyClass obj = new MyClass_MyClass(null);
      // make the clone
      MyClass_MyClass clone = cloner.clone(obj);
      
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      assertSame(Class3.class, clone.copySource);
   }
   
   // helper method for invoking findStaticCopyMethod and checking its result
   private void doTestFindStaticCopyMethod(Class<?> clazz, Class<?> argType) {
      Method m = Cloners.findStaticCopyMethod(clazz, "staticCopy");
      assertSame(clazz, m.getDeclaringClass());
      assertEquals(1, m.getParameterTypes().length);
      assertEquals(argType, m.getParameterTypes()[0]);
   }

   /**
    * Tests {@link Cloners#findStaticCopyMethod(Class, String)}.
    */
   public void testFindStaticCopyMethod() {
      // make sure it finds the correct method for each one
      doTestFindStaticCopyMethod(MyClass_MyClass.class, MyClass_MyClass.class);
      doTestFindStaticCopyMethod(MyClass_Class3.class, Class3.class);
      doTestFindStaticCopyMethod(MyClass_Class1.class, Class1.class);
      doTestFindStaticCopyMethod(MyClass_Interface1.class, Interface1.class);
      doTestFindStaticCopyMethod(MyClass_Interface1A1.class, Interface1A1.class);
      doTestFindStaticCopyMethod(MyClass_Object.class, Object.class);

      // no such method
      assertNull(Cloners.findStaticCopyMethod(MyClass_None.class, "staticCopy"));
      
      // won't find method on super-class
      assertNull(Cloners.findStaticCopyMethod(MyClass_MyClass.class, "otherStaticCopy"));
      
      // method exists but is not static
      assertNull(Cloners.findStaticCopyMethod(MyClass_MyClass.class, "instanceCopyBadSignature"));
      
      // ambiguous
      boolean caught = false;
      try {
         Cloners.findStaticCopyMethod(MyClass_Interface1A_Interface1B.class, "staticCopy");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // npe
      caught = false;
      try {
         Cloners.findStaticCopyMethod(null, "staticCopy");
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      caught = false;
      try {
         Cloners.findStaticCopyMethod(null, "staticCopy");
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.findStaticCopyMethod(null, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link Cloners#withCopyMethod(Method)}.
    * 
    * @throws Exception on error
    */
   public void testWithCopyMethod() throws Exception {
      // too many parameters static method
      boolean caught = false;
      Method method = MyClass_MyClass.class.getDeclaredMethod("staticCopyBadSignature",
            MyClass_MyClass.class, int.class);
      try {
         Cloners.withCopyMethod(method);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // too many parameters instance method
      caught = false;
      method = MyClass_MyClass.class.getDeclaredMethod("instanceCopyBadSignature",
            MyClass_MyClass.class);
      try {
         Cloners.withCopyMethod(method);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // primitive parameter
      caught = false;
      method = MyClass_None.class.getDeclaredMethod("staticCopy", int.class);
      try {
         Cloners.withCopyMethod(method);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // incompatible parameter
      caught = false;
      method = MyClass_None.class.getDeclaredMethod("staticCopy", List.class);
      try {
         Cloners.withCopyMethod(method);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // npe
      caught = false;
      try {
         Cloners.withCopyMethod(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // 1 arg static method
      Cloner<MyClass_MyClass> cloner1 =
            Cloners.withCopyMethod(MyClass_MyClass.class.getDeclaredMethod("staticCopy",
                  MyClass_MyClass.class));
      MyClass_MyClass obj1 = new MyClass_MyClass(null);
      // make the clone
      Class3 clone = cloner1.clone(obj1);
      
      assertNotSame(obj1, clone);
      assertSame(obj1.getClass(), clone.getClass());
      assertSame(MyClass_MyClass.class, clone.copySource);
      
      // no arg static method
      Cloner<MyClass_None> cloner2 =
            Cloners.withCopyMethod(MyClass_None.class.getDeclaredMethod("staticCopy"));
      MyClass_None obj2 = new MyClass_None();
      // make the clone
      clone = cloner2.clone(obj2);
      
      assertNotSame(obj2, clone);
      assertSame(obj2.getClass(), clone.getClass());
      assertNull(clone.copySource);

      // no arg instance method
      cloner1 = Cloners.withCopyMethod(MyClass_MyClass.class.getDeclaredMethod("instanceCopy"));
      obj1 = new MyClass_MyClass(null);
      // make the clone
      clone = cloner1.clone(obj1);
      
      assertNotSame(obj1, clone);
      assertSame(obj1.getClass(), clone.getClass());
      assertSame(MyClass_MyClass.class, clone.copySource);
   }
   
   /**
    * Tests {@link Cloners#withInstanceCopyMethod(Class, String)}.
    */
   public void testWithInstanceCopyMethod() {
      // no such method
      boolean caught = false;
      try {
         Cloners.withInstanceCopyMethod(MyClass_MyClass.class, "noSuchMethod");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // incorrect signature (should have no arguments)
      caught = false;
      try {
         Cloners.withInstanceCopyMethod(MyClass_MyClass.class, "instanceCopyBadSignature");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // static method
      caught = false;
      try {
         Cloners.withInstanceCopyMethod(MyClass_None.class, "staticCopy");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // npe
      caught = false;
      try {
         Cloners.withInstanceCopyMethod(null, "copyMethod");
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      caught = false;
      try {
         Cloners.withInstanceCopyMethod(MyClass_MyClass.class, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.withInstanceCopyMethod(null, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      // Instance method on class
      Cloner<MyClass_MyClass> cloner1 = Cloners.withInstanceCopyMethod(
            MyClass_MyClass.class, "instanceCopy");
      MyClass_MyClass obj1 = new MyClass_MyClass(null);
      // make the clone
      Class3 clone = cloner1.clone(obj1);
      
      assertNotSame(obj1, clone);
      assertSame(obj1.getClass(), clone.getClass());
      assertSame(MyClass_MyClass.class, clone.copySource);

      // Instance method on super-class
      cloner1 = Cloners.withInstanceCopyMethod(
            MyClass_MyClass.class, "makeClone");
      obj1 = new MyClass_MyClass(null);
      // make the clone
      clone = cloner1.clone(obj1);
      
      assertNotSame(obj1, clone);
      assertSame(obj1.getClass(), clone.getClass());
      assertSame(Class3.class, clone.copySource);

      // Instance method on ancestor class
      cloner1 = Cloners.withInstanceCopyMethod(
            MyClass_MyClass.class, "doClone");
      obj1 = new MyClass_MyClass(null);
      // make the clone
      clone = cloner1.clone(obj1);
      
      assertNotSame(obj1, clone);
      assertSame(obj1.getClass(), clone.getClass());
      assertSame(Class1.class, clone.copySource);
   }

   // helper method for invoking withCopyConstructor and checking its result
   private <T extends Class3> void doTestWithStaticCopyMethod(T obj, Class<T> clazz, Class<?> argType) {
      Cloner<T> c = Cloners.withStaticCopyMethod(clazz, "staticCopy");
      T clone = c.clone(obj);
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      assertSame(argType, clone.copySource);
   }

   /**
    * Tests {@link Cloners#withStaticCopyMethod(Class, String)}.
    */
   public void testWithStaticCopyMethodFind() {
      // make sure it finds the correct constructor for each one
      doTestWithStaticCopyMethod(new MyClass_MyClass(null), MyClass_MyClass.class, MyClass_MyClass.class);
      doTestWithStaticCopyMethod(new MyClass_Class3(null), MyClass_Class3.class, Class3.class);
      doTestWithStaticCopyMethod(new MyClass_Class1((Class1) null), MyClass_Class1.class, Class1.class);
      doTestWithStaticCopyMethod(new MyClass_Interface1(null), MyClass_Interface1.class, Interface1.class);
      doTestWithStaticCopyMethod(new MyClass_Interface1A1((Interface1A1) null), MyClass_Interface1A1.class, Interface1A1.class);
      doTestWithStaticCopyMethod(new MyClass_Object(null), MyClass_Object.class, Object.class);

      // no such method
      boolean caught = false;
      try {
         Cloners.withStaticCopyMethod(MyClass_None.class, "staticCopy");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // won't find static method on super-class
      caught = false;
      try {
         Cloners.withStaticCopyMethod(MyClass_MyClass.class, "otherStaticCopy");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // ambiguous
      caught = false;
      try {
         Cloners.withStaticCopyMethod(MyClass_Interface1A_Interface1B.class, "staticCopy");
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // npe
      caught = false;
      try {
         Cloners.withStaticCopyMethod(null, "staticCopy");
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.withStaticCopyMethod(MyClass_MyClass.class, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.withStaticCopyMethod(null, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   /**
    * Tests {@link Cloners#withStaticCopyMethod(Class, String, Class)}.
    */
   public void testWithStaticCopyMethodSpecific() {
      // npe
      boolean caught = false;
      try {
         Cloners.withStaticCopyMethod(Class3.class, "staticCopy", null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.withStaticCopyMethod(null, "staticCopy", Class3.class);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      caught = false;
      try {
         Cloners.withStaticCopyMethod(Class3.class, null, Class3.class);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // no such method
      caught = false;
      try {
         Cloners.withStaticCopyMethod(MyClass_MyClass.class, "staticCopy", Interface1.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // won't find static method on super-class
      caught = false;
      try {
         Cloners.withStaticCopyMethod(MyClass_MyClass.class, "otherStaticCopy", MyClass_MyClass.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // a valid one
      Cloner<MyClass_MyClass> cloner =
            Cloners.withStaticCopyMethod(MyClass_MyClass.class, "staticCopy", Class3.class);
      MyClass_MyClass obj = new MyClass_MyClass(null);
      // make the clone
      MyClass_MyClass clone = cloner.clone(obj);
      
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      assertSame(Class3.class, clone.copySource);
   }

   /**
    * Tests {@link Cloners#fromInstance(Object)},
    */
   public void testFromInstance() {
      Class3 obj1 = new MyClass_MyClass(null);
      Class3 obj2 = new MyClass_MyClass(null);
      Class3 obj3 = new MyClass_Class1((Class1) null);
      
      Cloner<Class3> cloner = Cloners.fromInstance(obj1);
      
      // same object - no good
      boolean caught = false;
      try {
         cloner.clone(obj1);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // good clone
      assertSame(obj1, cloner.clone(obj2));
      
      // not same class - no good
      caught = false;
      try {
         cloner.clone(obj3);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // npe
      caught = false;
      try {
         Cloners.fromInstance(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   // helper method for invoking withCopyConstructor and checking its result
   private void doTestFromInstances(List<Cloner<?>> cloners, Object obj1, Object obj2,
         String str1, String str2, Integer int1, Integer int2) {
      
      assertEquals(5, cloners.size());
      
      @SuppressWarnings("unchecked")
      Cloner<Object> cloner1 = (Cloner<Object>) cloners.get(0);
      @SuppressWarnings("unchecked")
      Cloner<Object> cloner2 = (Cloner<Object>) cloners.get(2);
      @SuppressWarnings("unchecked")
      Cloner<Object> cloner3 = (Cloner<Object>) cloners.get(4);
      
      assertSame(obj1, cloner1.clone(obj2));
      assertNull(cloners.get(1));
      assertSame(str1, cloner2.clone(str2));
      assertNull(cloners.get(3));
      assertSame(int1, cloner3.clone(int2));
   }
   
   /**
    * Tests {@link Cloners#fromInstances}.
    */
   public void testFromInstances() {
      Class3 obj1 = new MyClass_MyClass(null);
      Class3 obj2 = new MyClass_MyClass(null);
      Class3 obj3 = new MyClass_Class1((Class1) null);
      
      String str1 = "abc";
      String str2 = "def";
      
      Integer int1 = 1;
      Integer int2 = 2;
      
      List<Cloner<?>> cloners = Cloners.fromInstances(obj1, null, str1, null, int1);
      // check the whole list:
      doTestFromInstances(cloners, obj1, obj2, str1, str2, int1, int2);
      // and again with one created using list instead of var-args
      cloners = Cloners.fromInstances(Arrays.asList(obj1, null, str1, null, int1));
      doTestFromInstances(cloners, obj1, obj2, str1, str2, int1, int2);
      
      // now test one of the cloners with a few more edge cases:
      
      @SuppressWarnings("unchecked")
      Cloner<Object> cloner = (Cloner<Object>) cloners.get(0);

      // same object - no good
      boolean caught = false;
      try {
         cloner.clone(obj1);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // not same class - no good
      caught = false;
      try {
         cloner.clone(obj3);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // now test for npe with null list
      caught = false;
      try {
         Cloners.fromInstances((List<?>) null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link Cloners#fromCallable(java.util.concurrent.Callable)}.
    */
   public void testFromCallable() {
      final Class3 obj1 = new MyClass_MyClass(null);
      Class3 obj2 = new MyClass_MyClass(null);
      Class3 obj3 = new MyClass_Class1((Class1) null);
      
      // callable just returns obj1
      Cloner<Class3> cloner = Cloners.fromCallable(new Callable<Class3>() {
         @Override
         public Class3 call() {
            return obj1;
         }
      });
      
      // same object - no good
      boolean caught = false;
      try {
         cloner.clone(obj1);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // good clone
      assertSame(obj1, cloner.clone(obj2));
      
      // not same class - no good
      caught = false;
      try {
         cloner.clone(obj3);
      } catch (CloningException e) {
         caught = true;
      }
      assertTrue(caught);
      
      // if callable throws exception, should be translated to
      // CloningException
      cloner = Cloners.fromCallable(new Callable<Class3>() {
         @Override
         public Class3 call() throws Exception {
            throw new ArrayStoreException();
         }
      });
      caught = false;
      try {
         cloner.clone(obj1);
      } catch (CloningException e) {
         caught = true;
         assertSame(ArrayStoreException.class, e.getCause().getClass());
      }
      assertTrue(caught);
      
      // npe
      caught = false;
      try {
         Cloners.fromCallable(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link Cloners#forArray(Cloner)}.
    */
   public void testForArray() {
      Cloner<String> cloner = new Cloner<String>() {
         @Override
         public String clone(String o) {
            return new String(o);
         }
      };
      
      // test 1-D array
      Cloner<String[]> arrayCloner = Cloners.forArray(cloner);
      String obj[] = new String[] { "abc", "def", "ghi", null, "jkl", "mno", null };
      String clone[] = arrayCloner.clone(obj);
      
      assertEquals(obj.length, clone.length);
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      // verify it's a "deep" copy
      for (int i = 0; i < obj.length; i++) {
         if (obj[i] == null) {
            assertNull(clone[i]);
         } else {
            assertNotSame(obj[i], clone[i]);
            assertSame(obj[i].getClass(), clone[i].getClass());
            assertEquals(obj[i], clone[i]);
         }
      }
      
      // and 2-D array
      Cloner<String[][]> array2dCloner = Cloners.forArray(arrayCloner);
      String obj2d[][] = new String[][] { { "abc", "def", "ghi" }, null, { "jkl", "mno" }, { "pqr", null } };
      String clone2d[][] = array2dCloner.clone(obj2d);
      
      assertEquals(obj2d.length, clone2d.length);
      assertNotSame(obj2d, clone2d);
      assertSame(obj2d.getClass(), clone2d.getClass());
      // verify it's a "deep" copy
      for (int i = 0; i < obj2d.length; i++) {
         if (obj2d[i] == null) {
            assertNull(clone2d[i]);
         } else {
            for (int j = 0; j < obj2d[i].length; j++) {
               if (obj2d[i][j] == null) {
                  assertNull(clone2d[i][j]);
               } else {
                  assertNotSame(obj2d[i][j], clone2d[i][j]);
                  assertSame(obj2d[i][j].getClass(), clone2d[i][j].getClass());
                  assertEquals(obj2d[i][j], clone2d[i][j]);
               }
            }
         }
      }
      
      // npe
      boolean caught = false;
      try {
         Cloners.forArray(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   /**
    * Tests {@link Cloners#forNestedArray(Class, Cloner)}.
    */
   public void testForNestedArray() {
      Cloner<String> cloner = new Cloner<String>() {
         @Override
         public String clone(String o) {
            return new String(o);
         }
      };
      
      // test 1-D array
      String obj[] = new String[] { "abc", "def", "ghi", null, "jkl", "mno", null };
      Cloner<String[]> arrayCloner = Cloners.forNestedArray(String[].class, cloner);
      String clone[] = arrayCloner.clone(obj);
      
      assertEquals(obj.length, clone.length);
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      // verify it's a "deep" copy
      for (int i = 0; i < obj.length; i++) {
         if (obj[i] == null) {
            assertNull(clone[i]);
         } else {
            assertNotSame(obj[i], clone[i]);
            assertSame(obj[i].getClass(), clone[i].getClass());
            assertEquals(obj[i], clone[i]);
         }
      }
      
      // and 2-D array
      String obj2d[][] = new String[][] { { "abc", "def", "ghi" }, null, { "jkl", "mno" }, { "pqr", null } };
      Cloner<String[][]> array2dCloner = Cloners.forNestedArray(String[][].class, cloner);
      String clone2d[][] = array2dCloner.clone(obj2d);
      
      assertEquals(obj2d.length, clone2d.length);
      assertNotSame(obj2d, clone2d);
      assertSame(obj2d.getClass(), clone2d.getClass());
      // verify it's a "deep" copy
      for (int i = 0; i < obj2d.length; i++) {
         if (obj2d[i] == null) {
            assertNull(clone2d[i]);
         } else {
            for (int j = 0; j < obj2d[i].length; j++) {
               if (obj2d[i][j] == null) {
                  assertNull(clone2d[i][j]);
               } else {
                  assertNotSame(obj2d[i][j], clone2d[i][j]);
                  assertSame(obj2d[i][j].getClass(), clone2d[i][j].getClass());
                  assertEquals(obj2d[i][j], clone2d[i][j]);
               }
            }
         }
      }
      
      // npe
      boolean caught = false;
      try {
         Cloners.forNestedArray(null, cloner);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.forNestedArray(String[].class, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);

      caught = false;
      try {
         Cloners.forNestedArray(null, null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }

   // Helper class for test below
   @SuppressWarnings("serial")
   static class CloneableAndSerializable implements Cloneable, Serializable {
      String value;
      boolean wasCloned;
      
      @Override
      protected Object clone() throws CloneNotSupportedException {
         assertFalse(wasCloned); // make sure this is false to begin with
         Object ret = super.clone();
         wasCloned = true;
         return ret;
      }
   }

   // helper methods for testing the following cloners
   private void doCloneableClonerTest(Cloner<CloneableAndSerializable> cloner) {
      CloneableAndSerializable obj = new CloneableAndSerializable();
      obj.value = "monster!";
      CloneableAndSerializable clone1 = cloner.clone(obj);
      assertTrue(obj.wasCloned);
      obj.wasCloned = false;
      CloneableAndSerializable clone2 = cloner.clone(obj);
      assertTrue(obj.wasCloned);

      assertNotSame(obj, clone1);
      assertNotSame(obj, clone2);
      assertNotSame(clone1, clone2);
      assertSame(CloneableAndSerializable.class, clone1.getClass());
      assertSame(CloneableAndSerializable.class, clone2.getClass());
      assertEquals(obj.value, clone1.value);
      assertEquals(obj.value, clone2.value);
   }

   private void doClonerFromCopyConstructorTest(Cloner<MyClass_Class3> cloner) {
      // basically the same as doTestWithCopyConstructor(), but the cloner is
      // passed in, not the source object, and the we're only looking at MyClass_Class3
      MyClass_Class3 obj = new MyClass_Class3(null);
      MyClass_Class3 clone = cloner.clone(obj);
      assertNotSame(obj, clone);
      assertSame(MyClass_Class3.class, clone.getClass());
      assertSame(Class3.class, clone.copySource);
   }
   
   private void doArrayClonerTest(Cloner<SimpleCloneable[]> cloner) {
      SimpleCloneable obj1 = new SimpleCloneable();
      obj1.int1 = 101;
      obj1.int2 = 102;
      obj1.string1 = "abc1";
      obj1.string2 = "abc2";
      SimpleCloneable obj2 = new SimpleCloneable();
      obj2.int1 = 201;
      obj2.int2 = 202;
      obj2.string1 = "def1";
      obj2.string2 = "def2";
      SimpleCloneable obj3 = new SimpleCloneable();
      obj3.int1 = 301;
      obj3.int2 = 302;
      obj3.string1 = "xyz1";
      obj3.string2 = "xyz2";
      SimpleCloneable obj4 = new SimpleCloneable();
      obj4.int1 = 401;
      obj4.int2 = 402;
      obj4.string1 = "mno1";
      obj4.string2 = "mno2";

      SimpleCloneable obj[] = new SimpleCloneable[] { obj1, obj2, null, obj3, null, obj4 };
      SimpleCloneable clone[] = cloner.clone(obj);
      
      assertEquals(obj.length, clone.length);
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      // verify it's a "deep" copy
      for (int i = 0; i < obj.length; i++) {
         if (obj[i] == null) {
            assertNull(clone[i]);
         } else {
            assertNotSame(obj[i], clone[i]);
            assertSame(obj[i].getClass(), clone[i].getClass());
            assertEquals(obj[i].int1, clone[i].int1);
            assertEquals(obj[i].int2, clone[i].int2);
            assertEquals(obj[i].string1, clone[i].string1);
            assertEquals(obj[i].string2, clone[i].string2);
         }
      }
   }

   private void doArray2dClonerTest(Cloner<SimpleCloneable[][]> cloner) {
      SimpleCloneable obj1 = new SimpleCloneable();
      obj1.int1 = 101;
      obj1.int2 = 102;
      obj1.string1 = "abc1";
      obj1.string2 = "abc2";
      SimpleCloneable obj2 = new SimpleCloneable();
      obj2.int1 = 201;
      obj2.int2 = 202;
      obj2.string1 = "def1";
      obj2.string2 = "def2";
      SimpleCloneable obj3 = new SimpleCloneable();
      obj3.int1 = 301;
      obj3.int2 = 302;
      obj3.string1 = "xyz1";
      obj3.string2 = "xyz2";
      SimpleCloneable obj4 = new SimpleCloneable();
      obj4.int1 = 401;
      obj4.int2 = 402;
      obj4.string1 = "mno1";
      obj4.string2 = "mno2";

      SimpleCloneable obj[][] = new SimpleCloneable[][] { null, new SimpleCloneable[] { obj2 },
            new SimpleCloneable[] { null, null }, new SimpleCloneable[] { obj3, null, obj4 } };
      SimpleCloneable clone[][] = cloner.clone(obj);
      
      assertEquals(obj.length, clone.length);
      assertNotSame(obj, clone);
      assertSame(obj.getClass(), clone.getClass());
      
      // verify it's a "deep" copy
      for (int i = 0; i < obj.length; i++) {
         if (obj[i] == null) {
            assertNull(clone[i]);
         } else {
            assertNotSame(obj[i], clone[i]);
            assertSame(obj[i].getClass(), clone[i].getClass());
            for (int j = 0; j < obj[i].length; j++) {
               if (obj[i][j] == null) {
                  assertNull(clone[i][j]);
               } else {
                  assertNotSame(obj[i][j], clone[i][j]);
                  assertSame(obj[i][j].getClass(), clone[i][j].getClass());
                  assertEquals(obj[i][j].int1, clone[i][j].int1);
                  assertEquals(obj[i][j].int2, clone[i][j].int2);
                  assertEquals(obj[i][j].string1, clone[i][j].string1);
                  assertEquals(obj[i][j].string2, clone[i][j].string2);
               }
            }
         }
      }
   }
   
   /**
    * Tests {@link Cloners#defaultClonerFor(Class)}.
    */
   public void testDefaultClonerFor() {
      // get "canonical" cloners for these scenarios
      Cloner<?> clnblClonerCanon = Cloners.forCloneable();
      Cloner<?> srlzblClonerCanon = Cloners.forSerializable();
      Cloner<?> consClonerCanon = Cloners.withCopyConstructor(MyClass_Class3.class);
      Cloner<?> arrayClonerCanon = Cloners.forArray(Cloners.forCloneable());
      Cloner<?> array2dClonerCanon = Cloners.forNestedArray(SimpleCloneable[][].class, Cloners.forCloneable());
      // make sure they use different implementation classes. this way, if implementation
      // changes in a way that would require tests below to be re-written, we can hopefully
      // catch that here.
      assertNotSame(clnblClonerCanon.getClass(), srlzblClonerCanon.getClass());
      assertNotSame(clnblClonerCanon.getClass(), consClonerCanon.getClass());
      assertNotSame(clnblClonerCanon.getClass(), arrayClonerCanon.getClass());
      assertNotSame(clnblClonerCanon.getClass(), array2dClonerCanon.getClass());
      assertNotSame(srlzblClonerCanon.getClass(), consClonerCanon.getClass());
      assertNotSame(srlzblClonerCanon.getClass(), arrayClonerCanon.getClass());
      assertNotSame(srlzblClonerCanon.getClass(), array2dClonerCanon.getClass());
      assertNotSame(consClonerCanon.getClass(), arrayClonerCanon.getClass());
      assertNotSame(consClonerCanon.getClass(), array2dClonerCanon.getClass());
      // the array cloners should be the same class...
      assertSame(arrayClonerCanon.getClass(), array2dClonerCanon.getClass());

      // cloneable (that is also serializable to make sure
      // cloneable is preferred)
      Cloner<CloneableAndSerializable> clnblCloner = Cloners.defaultClonerFor(CloneableAndSerializable.class);
      assertSame(clnblCloner.getClass(), clnblClonerCanon.getClass());
      doCloneableClonerTest(clnblCloner);
      
      // serializable
      Cloner<SimpleSerializable> srlzblCloner = Cloners.defaultClonerFor(SimpleSerializable.class);
      assertSame(srlzblCloner.getClass(), srlzblClonerCanon.getClass());
      doSerializableClonerTest(srlzblCloner);

      // using copy constructor
      Cloner<MyClass_Class3> consCloner = Cloners.defaultClonerFor(MyClass_Class3.class);
      assertSame(consCloner.getClass(), consClonerCanon.getClass());
      doClonerFromCopyConstructorTest(consCloner);

      // array
      Cloner<SimpleCloneable[]> arrayCloner = Cloners.defaultClonerFor(SimpleCloneable[].class);
      assertSame(arrayCloner.getClass(), arrayClonerCanon.getClass());
      doArrayClonerTest(arrayCloner);
      
      // multi-dimensional array
      Cloner<SimpleCloneable[][]> array2dCloner = Cloners.defaultClonerFor(SimpleCloneable[][].class);
      assertSame(array2dCloner.getClass(), array2dClonerCanon.getClass());
      doArray2dClonerTest(array2dCloner);

      // no good default
      boolean caught = false;
      try {
         Cloners.defaultClonerFor(MyClass_None.class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);
      
      caught = false;
      try { // array whose elements have no good default cloner
         Cloners.defaultClonerFor(Object[].class);
      } catch (IllegalArgumentException e) {
         caught = true;
      }
      assertTrue(caught);

      // npe
      caught = false;
      try {
         Cloners.defaultClonerFor(null);
      } catch (NullPointerException e) {
         caught = true;
      }
      assertTrue(caught);
   }
   
   /**
    * Tests {@link Cloners#GENERIC_CLONER}.
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void testGenericCloner() {
      Cloner cloner = Cloners.GENERIC_CLONER;
      // run same tests as above, but all using same cloner instance
      doCloneableClonerTest(cloner);
      doSerializableClonerTest(cloner);
      doClonerFromCopyConstructorTest(cloner);
      doArrayClonerTest(cloner);
      doArray2dClonerTest(cloner);
   }
}
