package com.bluegosling.apt.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bluegosling.apt.ProcessingEnvironments;
import com.bluegosling.apt.testing.CompilationContext;
import com.bluegosling.apt.testing.TestEnvironment;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ArClassTest {
   // List of classes, for comparing behavior of ArClass to java.lang.Class
   private static final List<Class<?>> CLASSES_TO_TEST;
   static {
      Class<?>[] nonArrayTypes = new Class<?>[] { Object.class, Class.class,
         Map.class, Map.Entry.class, ArrayList.class, Repeatable.class, Target.class,
         ElementType.class, RetentionPolicy.class, CompilationContext.TaskBuilder.class, void.class,
         boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class,
         double.class };
      List<Class<?>> classesToTest = new ArrayList<>(nonArrayTypes.length * 5);
      for (Class<?> clazz : nonArrayTypes) {
         for (int i = 0; i < 5; i++) {
            classesToTest.add(clazz);
            if (clazz == void.class) {
               break;
            }
            clazz = Array.newInstance(clazz, 0).getClass();
         }
      }
      CLASSES_TO_TEST = Collections.unmodifiableList(classesToTest);
   }
   
   @Test public void getCanonicalName() throws Exception {
      check(Class::getCanonicalName, ArClass::getCanonicalName);
   }

   @Test public void getName() throws Exception {
      check(Class::getName, ArClass::getName);
   }

   @Test public void getSimpleName() throws Exception {
      check(Class::getSimpleName, ArClass::getSimpleName);
   }

   @Test public void isInterface() throws Exception {
      check(Class::isInterface, ArClass::isInterface);
   }

   @Test public void isEnum() throws Exception {
      check(Class::isEnum, ArClass::isEnum);
   }

   @Test public void isAnnotation() throws Exception {
      check(Class::isAnnotation, ArClass::isAnnotation);
   }

   @Test public void isArray() throws Exception {
      check(Class::isArray, ArClass::isArray);
   }

   @Test public void isPrimitive() throws Exception {
      check(Class::isPrimitive, ArClass::isPrimitive);
   }

   @Test public void isMemberClass() throws Exception {
      check(Class::isMemberClass, ArClass::isMemberClass);
   }

   @Test public void getModifiers() throws Exception {
      check(Arrays.asList(PrivateFinal.class, ProtectedFinal.class, PublicFinal.class,
            StaticAbstract.class), Class::getModifiers,
            arc -> ArModifier.toBitfield(arc.getModifiers()));
      run(env -> assertTrue(ArClass.forClass(StaticAbstract.class).getModifiers()
            .contains(ArModifier.PACKAGE_PRIVATE)));
   }

   @Test public void isAnonymousClass() throws Exception {
      check(Class::isAnonymousClass, ArClass::isAnonymousClass);
      // None of these are anonymous classes. It isn't actually possible for an annotation
      // processor to get a local or anonymous class! (So it is strange that the API surface
      // area would make one expect it's possible -- since you can get a type element's nesting
      // kind which ostensibly could be anonymous or local.)
      // See https://bugs.openjdk.java.net/browse/JDK-6587158
   }

   @Test public void isLocalClass() throws Exception {
      check(Class::isLocalClass, ArClass::isLocalClass);
      // None of these are local classes. It isn't actually possible for an annotation processor to
      // get a local or anonymous class! (So it is strange that the API surface area would make one
      // expect it's possible -- since you can get a type element's nesting kind which ostensibly
      // could be anonymous or local.)
      // See https://bugs.openjdk.java.net/browse/JDK-6587158
   }

   private <T> void check(Function<Class<?>, T> classExtract, Function<ArClass, T> arClassExtract)
         throws Exception {
      check(CLASSES_TO_TEST, classExtract, arClassExtract);
   }
   
   private <T> void check(List<Class<?>> classesToTest, Function<Class<?>, T> classExtract,
         Function<ArClass, T> arClassExtract) throws Exception {
      run(env -> {
         for (Class<?> clazz : classesToTest) {
            checkClass(clazz, classExtract, arClassExtract);
         }
      });
   }
   
   private <T> void checkClass(Class<?> clazz, Function<Class<?>, T> classExtract,
         Function<ArClass, T> arClassExtract) throws ClassNotFoundException {
      ArClass arClass = ArClass.forClass(clazz);
      T t1 = classExtract.apply(clazz);
      T t2 = arClassExtract.apply(arClass);
      assertEquals(t1, t2);
   }
   
   private interface Task {
      void run(TestEnvironment env) throws Exception;
   }
   
   private void run(Task task) {
      try {
         new CompilationContext().newTask().run(env -> {
            ProcessingEnvironments.setup(env.processingEnvironment());
            try {
               task.run(env);
            } finally {
               ProcessingEnvironments.reset();
            }
            return null;
         });
      } catch (RuntimeException | Error e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
   
   public final class PublicFinal {
   }

   protected final class ProtectedFinal {
   }

   private final class PrivateFinal {
   }
   
   static abstract class StaticAbstract {
   }
}
