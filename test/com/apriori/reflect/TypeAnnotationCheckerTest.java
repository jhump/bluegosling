package com.apriori.reflect;

import static com.apriori.reflect.Annotations.create;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import com.apriori.collections.MapBuilder;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collection;

public class TypeAnnotationCheckerTest {
   
   @interface Nullable {
   }

   @interface NotNull {
   }

   @Test public void simpleHierarchy() {
      TypeAnnotationChecker checker = new TypeAnnotationChecker.Builder()
            .equivalent(null, Nullable.class)
            .assignable(Nullable.class, NotNull.class)
            .build();

      assertEquivalent(checker, emptyList(), emptyList());

      Nullable nullable = create(Nullable.class, emptyMap());
      Nullable otherNullable = create(Nullable.class, emptyMap());
      
      assertEquivalent(checker, emptyList(), singleton(nullable));
      assertEquivalent(checker, singleton(nullable), singleton(otherNullable));

      NotNull notNull = create(NotNull.class, emptyMap());
      NotNull otherNotNull = create(NotNull.class, emptyMap());
      
      assertAssignable(checker, singleton(nullable), singleton(notNull));
      assertAssignable(checker, emptyList(), singleton(notNull));
      assertEquivalent(checker, singleton(notNull), singleton(otherNotNull));
   }
   
   @interface LevelA {
   }
   
   @interface LevelB {
   }
   
   @interface LevelA_0 {
      String value();
   }

   @interface LevelA_1 {
      Class<?>[] value();
   }

   @interface LevelB_0 {
   }
   
   @interface LevelA_0_0 {
      String value();
      String str();
   }

   @interface LevelA_0_1 {
      String value();
      int[] nums();
   }

   @interface LevelA_1_0 {
      Class<?>[] value();
   }

   @interface LevelA_1_1 {
      Class<?>[] value();
      int i();
      double d();
      float f();
      boolean[] bits();
      LevelA_0_1[] a01s();
   }
   
   @interface LevelB_0_0 {
      int value();
   }

   @Test public void moreComplicatedHierarchy() {
      TypeAnnotationChecker checker = new TypeAnnotationChecker.Builder()
            .equivalent(null, LevelA.class)
            .assignable(LevelA.class, LevelA_0.class)
            .assignable(LevelA_0.class, LevelA_0_0.class)
            .assignable(LevelA_0.class, LevelA_0_1.class)
            .assignable(LevelA.class, LevelA_1.class)
            .assignable(LevelA_1.class, LevelA_1_0.class)
            .assignable(LevelA_1.class, LevelA_1_1.class)
            .assignable(null, LevelB.class)
            .assignable(LevelB.class, LevelB_0.class)
            .assignable(LevelB_0.class, LevelB_0_0.class)
            .build();
      
      LevelA levelA = create(LevelA.class, emptyMap());
      LevelB levelB = create(LevelB.class, emptyMap());
      LevelA_0 levelA0 = create(LevelA_0.class, singletonMap("value", "abc"));
      LevelA_0 levelA0_same = create(LevelA_0.class, singletonMap("value", "abc"));
      LevelA_0 levelA0_other = create(LevelA_0.class, singletonMap("value", "def"));
      LevelA_1 levelA1 = create(LevelA_1.class,
            singletonMap("value", asList(Object.class, Integer.class)));
      LevelA_1 levelA1_same = create(LevelA_1.class,
            singletonMap("value", asList(Object.class, Integer.class)));
      LevelA_1 levelA1_other = create(LevelA_1.class, singletonMap("value", asList(Object.class)));
      LevelB_0 levelB0 = create(LevelB_0.class, emptyMap());
      LevelA_0_0 levelA00 = create(LevelA_0_0.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("str", "def").build());
      LevelA_0_0 levelA00_same = create(LevelA_0_0.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("str", "def").build());
      LevelA_0_0 levelA00_other = create(LevelA_0_0.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "xyz").put("str", "foo").build());
      LevelA_0_1 levelA01 = create(LevelA_0_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("nums", asList(1, 2, 3, 4, 5)).build());
      LevelA_0_1 levelA01_same = create(LevelA_0_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("nums", asList(1, 2, 3, 4, 5)).build());
      LevelA_0_1 levelA01_other = create(LevelA_0_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "bar").put("nums", asList(1, 2, 3)).build());
      // add _same and _other
      LevelA_1_0 levelA10 = create(LevelA_1_0.class,
            singletonMap("value", asList(Object.class, Integer.class)));
      LevelA_1_0 levelA10_same = create(LevelA_1_0.class,
            singletonMap("value", asList(Object.class, Integer.class)));
      LevelA_1_0 levelA10_other = create(LevelA_1_0.class,
            singletonMap("value", asList(String.class)));
      LevelA_1_1 levelA11 = create(LevelA_1_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", asList(Object.class, Integer.class))
            .put("i", 100)
            .put("d", 3.14)
            .put("f", 5.01f)
            .put("bits", asList(true, false, true, false))
            .put("a01s", asList(levelA01))
            .build());
      LevelA_1_1 levelA11_same = create(LevelA_1_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", asList(Object.class, Integer.class))
            .put("i", 100)
            .put("d", 3.14)
            .put("f", 5.01f)
            .put("bits", asList(true, false, true, false))
            .put("a01s", asList(levelA01_same))
            .build());
      LevelA_1_1 levelA11_other1 = create(LevelA_1_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", asList(Object.class, Integer.class))
            .put("i", 100)
            .put("d", 3.14)
            .put("f", 5.01f)
            .put("bits", asList(true, false, true, false))
            .put("a01s", asList(levelA01_other)) // only differs by this field
            .build());
      LevelA_1_1 levelA11_other2 = create(LevelA_1_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", asList(String.class)) // only differs by this field
            .put("i", 100)
            .put("d", 3.14)
            .put("f", 5.01f)
            .put("bits", asList(true, false, true, false))
            .put("a01s", asList(levelA01))
            .build());
      LevelB_0_0 levelB00 = create(LevelB_0_0.class, singletonMap("value", 10101));
      LevelB_0_0 levelB00_same = create(LevelB_0_0.class, singletonMap("value", 10101));
      LevelB_0_0 levelB00_other = create(LevelB_0_0.class, singletonMap("value", 9999));
      
      // null <-> LevelA
      assertEquivalent(checker, emptyList(), singleton(levelA));

      // LevelA <-- LevelA_0
      assertAssignable(checker, singleton(levelA), singleton(levelA0));
      assertAssignable(checker, emptyList(), singleton(levelA0));
      assertAssignable(checker, singleton(levelA), singleton(levelA0_same));
      assertAssignable(checker, singleton(levelA), singleton(levelA0_other));
      assertEquivalent(checker, singleton(levelA0), singleton(levelA0_same));
      assertNotAssignable(checker, singleton(levelA0), singleton(levelA0_other));

      // LevelA_0 <-- LevelA_0_0
      assertAssignable(checker, singleton(levelA0), singleton(levelA00));
      assertAssignable(checker, singleton(levelA0), singleton(levelA00_same));
      assertNotAssignable(checker, singleton(levelA0), singleton(levelA00_other));
      assertEquivalent(checker, singleton(levelA00), singleton(levelA00_same));
      assertNotAssignable(checker, singleton(levelA00), singleton(levelA00_other));
      // LevelA <-- LevelA_0_0
      assertAssignable(checker, singleton(levelA), singleton(levelA00));
      assertAssignable(checker, emptyList(), singleton(levelA00));
      assertAssignable(checker, singleton(levelA), singleton(levelA00_same));
      assertAssignable(checker, singleton(levelA), singleton(levelA00_other));

      // LevelA_0 <-- LevelA_0_1
      assertAssignable(checker, singleton(levelA0), singleton(levelA01));
      assertAssignable(checker, singleton(levelA0), singleton(levelA01_same));
      assertNotAssignable(checker, singleton(levelA0), singleton(levelA01_other));
      assertEquivalent(checker, singleton(levelA01), singleton(levelA01_same));
      assertNotAssignable(checker, singleton(levelA01), singleton(levelA01_other));
      // LevelA <-- LevelA_0_1
      assertAssignable(checker, singleton(levelA), singleton(levelA01));
      assertAssignable(checker, emptyList(), singleton(levelA01));
      assertAssignable(checker, singleton(levelA), singleton(levelA01_same));
      assertAssignable(checker, singleton(levelA), singleton(levelA01_other));

      // LevelA <-- LevelA_1
      assertAssignable(checker, singleton(levelA), singleton(levelA1));
      assertAssignable(checker, emptyList(), singleton(levelA1));
      assertAssignable(checker, singleton(levelA), singleton(levelA1_same));
      assertAssignable(checker, singleton(levelA), singleton(levelA1_other));
      assertEquivalent(checker, singleton(levelA1), singleton(levelA1_same));
      assertNotAssignable(checker, singleton(levelA1), singleton(levelA1_other));
      
      // LevelA_1 <-- LevelA_1_0
      assertAssignable(checker, singleton(levelA1), singleton(levelA10));
      assertAssignable(checker, singleton(levelA1), singleton(levelA10_same));
      assertNotAssignable(checker, singleton(levelA1), singleton(levelA10_other));
      assertEquivalent(checker, singleton(levelA10), singleton(levelA10_same));
      assertNotAssignable(checker, singleton(levelA10), singleton(levelA10_other));
      // LevelA <-- LevelA_1_0
      assertAssignable(checker, singleton(levelA), singleton(levelA10));
      assertAssignable(checker, emptyList(), singleton(levelA10));
      assertAssignable(checker, singleton(levelA), singleton(levelA10_same));
      assertAssignable(checker, singleton(levelA), singleton(levelA10_other));

      // LevelA_1 <-- LevelA_1_1
      assertAssignable(checker, singleton(levelA1), singleton(levelA11));
      assertAssignable(checker, singleton(levelA1), singleton(levelA11_same));
      assertAssignable(checker, singleton(levelA1), singleton(levelA11_other1));
      assertNotAssignable(checker, singleton(levelA1), singleton(levelA11_other2));
      assertEquivalent(checker, singleton(levelA11), singleton(levelA11_same));
      assertNotAssignable(checker, singleton(levelA11), singleton(levelA11_other1));
      assertNotAssignable(checker, singleton(levelA11), singleton(levelA11_other2));
      // LevelA <-- LevelA_1_1
      assertAssignable(checker, singleton(levelA), singleton(levelA11));
      assertAssignable(checker, emptyList(), singleton(levelA11));
      assertAssignable(checker, singleton(levelA), singleton(levelA11_same));
      assertAssignable(checker, singleton(levelA), singleton(levelA11_other1));
      assertAssignable(checker, singleton(levelA), singleton(levelA11_other2));

      // LevelA_0 -!- LevelA_1
      assertNotAssignable(checker, singleton(levelA0), singleton(levelA1));
      assertNotAssignable(checker, singleton(levelA0_other), singleton(levelA1_other));
      // LevelA_0_0 -!- LevelA_0_1
      assertNotAssignable(checker, singleton(levelA00), singleton(levelA01));
      assertNotAssignable(checker, singleton(levelA00_other), singleton(levelA01_other));
      // LevelA_1_0 -!- LevelA_1_1
      assertNotAssignable(checker, singleton(levelA10), singleton(levelA11));
      assertNotAssignable(checker, singleton(levelA10_other), singleton(levelA11_other2));

      // null <-- LevelB
      assertAssignable(checker, emptyList(), singleton(levelB));
      // LevelB <-- LevelB_0
      assertAssignable(checker, singleton(levelB), singleton(levelB0));
      assertAssignable(checker, emptyList(), singleton(levelB0));
      // LevelB_0 <-- LevelB_0_0
      assertAssignable(checker, singleton(levelB0), singleton(levelB00));
      assertAssignable(checker, emptyList(), singleton(levelB00));
      assertAssignable(checker, singleton(levelB0), singleton(levelB00_same));
      assertAssignable(checker, singleton(levelB0), singleton(levelB00_other));
      assertEquivalent(checker, singleton(levelB00), singleton(levelB00_same));
      assertNotAssignable(checker, singleton(levelB00), singleton(levelB00_other));
   }
   
   private void assertAssignable(TypeAnnotationChecker checker,
         Collection<? extends Annotation> target, Collection<? extends Annotation> source) {
      assertTrue(checker.isAssignable(target, source));
      assertFalse(checker.isAssignable(source, target));
   }

   private void assertNotAssignable(TypeAnnotationChecker checker,
         Collection<? extends Annotation> target, Collection<? extends Annotation> source) {
      assertFalse(checker.isAssignable(target, source));
      assertFalse(checker.isAssignable(source, target));
   }
   
   private void assertEquivalent(TypeAnnotationChecker checker,
         Collection<? extends Annotation> target, Collection<? extends Annotation> source) {
      assertTrue(checker.isAssignable(target, source));
      assertTrue(checker.isAssignable(source, target));
   }
}
