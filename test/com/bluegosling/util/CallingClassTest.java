package com.bluegosling.util;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.bluegosling.util.test.CallingClassTestHelper;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;

public class CallingClassTest {
   
   @Test public void getCaller_withFrameCount() {
      assertSame(TestA1.class, TestA2.getCaller(0));
      assertSame(TestA2.class, TestA2.getCaller(1));
      assertSame(CallingClassTest.class, TestA2.getCaller(2));
      
      // too many frames back
      assertThrows(IllegalArgumentException.class, () -> TestA1.getCaller(2000));
      // negative frames
      assertThrows(IllegalArgumentException.class, () -> TestA1.getCaller(-1));
   }

   @Test public void getCaller_withFrameCountAndClassLoader() {
      assertSame(TestB1.class, TestB2.getCaller(0, CallingClass.class.getClassLoader()));
      assertSame(TestB2.class, TestB2.getCaller(1, CallingClass.class.getClassLoader()));
      assertSame(CallingClassTest.class, TestB2.getCaller(2, CallingClass.class.getClassLoader()));
      
      // too many frames back
      assertThrows(IllegalArgumentException.class,
            () -> TestB1.getCaller(2000, CallingClass.class.getClassLoader()));
      // negative frames
      assertThrows(IllegalArgumentException.class,
            () -> TestB1.getCaller(-1, CallingClass.class.getClassLoader()));
   }

   @Test public void getCaller_noArgs() {
      assertSame(TestC1.class, TestC2.getCaller());
      assertSame(TestC1.class, TestC1.getCaller());
      assertSame(CallingClassTest.class, CallingClass.getCaller());
   }

   @Test public void getCaller_withClassLoader() {
      assertSame(TestD1.class, TestD2.getCaller(CallingClass.class.getClassLoader()));
      assertSame(TestD1.class, TestD1.getCaller(CallingClass.class.getClassLoader()));
      assertSame(CallingClassTest.class,
            CallingClass.getCaller(Collections.singletonList(CallingClass.class.getClassLoader())));
   }
   
   @Test public void getCaller_fromInnerClass() {
      class TestInner {
         Class<?> getCaller() {
            return CallingClass.getCaller();
         }
      }
      assertSame(TestInner.class, new TestInner().getCaller());
      Supplier<Class<?>> t = new Supplier<Class<?>>() {
         @Override
         public Class<?> get() {
            return CallingClass.getCaller();
         }
      };
      assertSame(t.getClass(), t.get());
   }
   
   @Test public void getCaller_withDifferentClassLoader() throws Exception {
      // class loader that loads CallingClassTestHelper as different class
      ClassLoader cl = new ClassLoader(CallingClass.class.getClassLoader()) {
         @Override
         public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.equals(CallingClassTestHelper.class.getName())) {
               Class<?> ret = findLoadedClass(name);
               if (ret != null) {
                  return ret;
               }
               byte classBytes[];
               String resourceName =
                     CallingClassTestHelper.class.getName().replace('.', File.separatorChar)
                           + ".class";
               try (InputStream in = getResourceAsStream(resourceName)) {
                  classBytes = IoStreams.toByteArray(in);
               } catch (IOException e) {
                  throw new RuntimeException(e);
               }
               return defineClass(name, classBytes, 0, classBytes.length);
            } else {
               return super.loadClass(name);
            }
         }
      };
      Class<?> clazz = cl.loadClass(CallingClassTestHelper.class.getName());
      
      // check that class loader is working properly
      assert clazz != CallingClassTestHelper.class;
      
      @SuppressWarnings("unchecked")
      Supplier<Class<?>> supplier = (Supplier<Class<?>>) clazz.newInstance();
      @SuppressWarnings("unchecked")
      Function<Iterable<? extends ClassLoader>, Class<?>> function =
            (Function<Iterable<? extends ClassLoader>, Class<?>>) supplier;
      
      // class resolves incorrectly when incorrect ClassLoader provided
      assertSame(CallingClassTestHelper.class, supplier.get());
      assertSame(CallingClassTestHelper.class, function.apply(
            Collections.singletonList(CallingClassTest.class.getClassLoader())));

      assertNotSame(CallingClassTestHelper.class, function.apply(Collections.singletonList(cl)));
      assertSame(clazz, function.apply(Collections.singletonList(cl)));
   }
   
   static class TestA1 {
      static Class<?> getCaller(int frames) {
         return CallingClass.getCaller(frames);
      }
   }
   static class TestA2 {
      static Class<?> getCaller(int frames) {
         return TestA1.getCaller(frames);
      }
   }
   static class TestB1 {
      static Class<?> getCaller(int frames, ClassLoader cl) {
         return CallingClass.getCaller(frames, Collections.singletonList(cl));
      }
   }
   static class TestB2 {
      static Class<?> getCaller(int frames, ClassLoader cl) {
         return TestB1.getCaller(frames, cl);
      }
   }
   static class TestC1 {
      static Class<?> getCaller() {
         return CallingClass.getCaller();
      }
   }
   static class TestC2 {
      static Class<?> getCaller() {
         return TestC1.getCaller();
      }
   }
   static class TestD1 {
      static Class<?> getCaller(ClassLoader cl) {
         return CallingClass.getCaller(Collections.singletonList(cl));
      }
   }
   static class TestD2 {
      static Class<?> getCaller(ClassLoader cl) {
         return TestD1.getCaller(cl);
      }
   }
}
