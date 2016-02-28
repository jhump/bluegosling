package com.bluegosling.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bluegosling.collections.MapBuilder;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


public class AnnotationsTest {
   
   enum TestEnum {
      A, B, C, D, E;
   }
   
   @interface SimpleAnnotation {
      int someValue();
      boolean someFlag() default false;
   }
   
   @interface NestedAnnotation {
      SimpleAnnotation[] value();
   }

   @Retention(RetentionPolicy.RUNTIME)
   @interface NestedAnnotationOuter {
      NestedAnnotation xyz();
   }

   @Retention(RetentionPolicy.RUNTIME)
   @interface ExhaustiveAnnotation {
      boolean booleanField();
      boolean booleanFieldWithDefault() default true;
      byte byteField();
      byte byteFieldWithDefault() default 1;
      char charField();
      char charFieldWithDefault() default 'x';
      short shortField();
      short shortFieldWithDefault() default 2;
      int intField();
      int intFieldWithDefault() default 3;
      long longField();
      long longFieldWithDefault() default 4;
      float floatField();
      float floatFieldWithDefault() default 5.1f;
      double doubleField();
      double doubleFieldWithDefault() default 6.2;
      String stringField();
      String stringFieldWithDefault() default "abc";
      TestEnum enumField();
      TestEnum enumFieldWithDefault() default TestEnum.A;
      SimpleAnnotation annotationField();
      SimpleAnnotation annotationFieldWithDefault() default @SimpleAnnotation(someValue = 1);
   }
   
   @Retention(RetentionPolicy.RUNTIME)
   @interface ExhaustiveArraysAnnotation {
      boolean[] booleanField();
      boolean[] booleanFieldWithDefault() default { true, false, true };
      byte[] byteField();
      byte[] byteFieldWithDefault() default { 1, 2, 3 };
      char[] charField();
      char[] charFieldWithDefault() default { 'x', 'y', 'z' };
      short[] shortField();
      short[] shortFieldWithDefault() default { 2, 3, 1 };
      int[] intField();
      int[] intFieldWithDefault() default { 3, 1, 2 };
      long[] longField();
      long[] longFieldWithDefault() default { 4, 3, 2 };
      float[] floatField();
      float[] floatFieldWithDefault() default { 5.1f, 6.1f };
      double[] doubleField();
      double[] doubleFieldWithDefault() default { 6.2, 7.2 };
      String[] stringField();
      String[] stringFieldWithDefault() default { "abc", "def" };
      TestEnum[] enumField();
      TestEnum[] enumFieldWithDefault() default { TestEnum.A, TestEnum.B };
      SimpleAnnotation[] annotationField();
      SimpleAnnotation[] annotationFieldWithDefault() default
      {
         @SimpleAnnotation(someValue = 1, someFlag = true),
         @SimpleAnnotation(someValue = 2, someFlag = false)
      };
   }

   @NestedAnnotationOuter(xyz = @NestedAnnotation(@SimpleAnnotation(someValue = 1234)))
   @ExhaustiveAnnotation(
         booleanField = false, byteField = Byte.MAX_VALUE,  charField = 'A',
         shortField = Short.MAX_VALUE, intField = -1, longField = Integer.MAX_VALUE + 1L,
         floatField = 3.14159f, doubleField = -1.1, stringField = "--" , enumField = TestEnum.C,
         annotationField = @SimpleAnnotation(someValue = 0, someFlag = true))
   private class TestExhaustive {
   }
   
   // Map representation of the annotation on TestExhaustive
   private static final Map<String, Object> NESTED_ANNOTATION_OUTER_MAP =
         AnnotationsTest.<String, Object>mapBuilder()
               .put("xyz", mapBuilder()
                     .put("value", Arrays.asList(mapBuilder()
                           .put("someValue",  1234)
                           .put("someFlag", false)
                           .build()))
                     .build())
               .build();
   
   // Map representation of the annotation on TestExhaustive
   private static final Map<String, Object> EXHAUSTIVE_ANNOTATION_MAP =
         AnnotationsTest.<String, Object>mapBuilder()
               .put("booleanField", false).put("booleanFieldWithDefault", true)
               .put("byteField", Byte.MAX_VALUE).put("byteFieldWithDefault", (byte) 1)
               .put("charField", 'A').put("charFieldWithDefault", 'x')
               .put("shortField", Short.MAX_VALUE).put("shortFieldWithDefault", (short) 2)
               .put("intField", -1).put("intFieldWithDefault", 3)
               .put("longField", Integer.MAX_VALUE + 1L).put("longFieldWithDefault", 4L)
               .put("floatField", 3.14159f).put("floatFieldWithDefault", 5.1f)
               .put("doubleField", -1.1).put("doubleFieldWithDefault", 6.2)
               .put("stringField", "--").put("stringFieldWithDefault", "abc")
               .put("enumField", TestEnum.C).put("enumFieldWithDefault", TestEnum.A)
               .put("annotationField",
                     mapBuilder().put("someValue", 0).put("someFlag", true).build())
               .put("annotationFieldWithDefault",
                     mapBuilder().put("someValue", 1).put("someFlag", false).build())
               .build();

   @ExhaustiveArraysAnnotation(
         booleanField = { false, true }, byteField = { Byte.MAX_VALUE, Byte.MIN_VALUE },
         charField = { 'A', 'B', 'C' }, shortField = { Short.MAX_VALUE, Short.MIN_VALUE },
         intField = { }, longField = { Integer.MAX_VALUE + 1L }, floatField = { 3.14159f },
         doubleField = { -1.1, -2.2, -3.3 }, stringField = { "", "-", "--" },
         enumField = { TestEnum.C }, annotationField = {
               @SimpleAnnotation(someValue = 0, someFlag = true),
               @SimpleAnnotation(someValue = -1, someFlag = false)
         })
   private class TestExhaustiveArrays {
   }

   // Map representation of the annotation on TestExhaustiveArrays
   private static final Map<String, Object> EXHAUSTIVE_ARRAYS_ANNOTATION_MAP =
         AnnotationsTest.<String, Object>mapBuilder()
               .put("booleanField", Arrays.asList(false, true))
               .put("booleanFieldWithDefault", Arrays.asList(true, false, true))
               .put("byteField", Arrays.asList(Byte.MAX_VALUE, Byte.MIN_VALUE))
               .put("byteFieldWithDefault", Arrays.asList((byte) 1, (byte) 2, (byte) 3))
               .put("charField", Arrays.asList('A', 'B', 'C'))
               .put("charFieldWithDefault", Arrays.asList('x', 'y', 'z'))
               .put("shortField", Arrays.asList(Short.MAX_VALUE, Short.MIN_VALUE))
               .put("shortFieldWithDefault", Arrays.asList((short) 2, (short) 3, (short) 1))
               .put("intField", Collections.emptyList())
               .put("intFieldWithDefault", Arrays.asList(3, 1, 2))
               .put("longField", Arrays.asList(Integer.MAX_VALUE + 1L))
               .put("longFieldWithDefault", Arrays.asList(4L, 3L, 2L))
               .put("floatField", Arrays.asList(3.14159f))
               .put("floatFieldWithDefault", Arrays.asList(5.1f, 6.1f))
               .put("doubleField", Arrays.asList(-1.1, -2.2, -3.3))
               .put("doubleFieldWithDefault", Arrays.asList(6.2, 7.2))
               .put("stringField", Arrays.asList("", "-", "--"))
               .put("stringFieldWithDefault", Arrays.asList("abc", "def"))
               .put("enumField", Arrays.asList(TestEnum.C))
               .put("enumFieldWithDefault", Arrays.asList(TestEnum.A, TestEnum.B))
               .put("annotationField", Arrays.asList(
                     mapBuilder().put("someValue", 0).put("someFlag", true).build(),
                     mapBuilder().put("someValue", -1).put("someFlag", false).build()))
               .put("annotationFieldWithDefault", Arrays.asList(
                     mapBuilder().put("someValue", 1).put("someFlag", true).build(),
                     mapBuilder().put("someValue", 2).put("someFlag", false).build()))
               .build();
   
   @Test public void toMap() {
      NestedAnnotationOuter a1 = TestExhaustive.class.getAnnotation(NestedAnnotationOuter.class);
      assertEquals(NESTED_ANNOTATION_OUTER_MAP, Annotations.toMap(a1));

      ExhaustiveAnnotation a2 = TestExhaustive.class.getAnnotation(ExhaustiveAnnotation.class);
      assertEquals(EXHAUSTIVE_ANNOTATION_MAP, Annotations.toMap(a2));

      ExhaustiveArraysAnnotation a3 =
            TestExhaustiveArrays.class.getAnnotation(ExhaustiveArraysAnnotation.class);
      assertEquals(EXHAUSTIVE_ARRAYS_ANNOTATION_MAP, Annotations.toMap(a3));
   }

   @Test public void create() {
      NestedAnnotationOuter a1 = TestExhaustive.class.getAnnotation(NestedAnnotationOuter.class);
      assertAnnotationsEqual(a1,
            Annotations.create(NestedAnnotationOuter.class, NESTED_ANNOTATION_OUTER_MAP));

      ExhaustiveAnnotation a2 = TestExhaustive.class.getAnnotation(ExhaustiveAnnotation.class);
      assertAnnotationsEqual(a2,
            Annotations.create(ExhaustiveAnnotation.class, EXHAUSTIVE_ANNOTATION_MAP));

      ExhaustiveArraysAnnotation a3 =
            TestExhaustiveArrays.class.getAnnotation(ExhaustiveArraysAnnotation.class);
      assertAnnotationsEqual(a3,
            Annotations.create(ExhaustiveArraysAnnotation.class, EXHAUSTIVE_ARRAYS_ANNOTATION_MAP));
   }
   
   private static void assertAnnotationsEqual(Annotation a1, Annotation a2) {
      assertTrue(a1.equals(a2) && a2.equals(a1));
   }
   
   private static <K, V> MapBuilder<K, V, ?, ?> mapBuilder() {
      // argh... why can't you infer these type variables, javac!?! 
      return MapBuilder.<K, V>forLinkedHashMap();
   }
}
