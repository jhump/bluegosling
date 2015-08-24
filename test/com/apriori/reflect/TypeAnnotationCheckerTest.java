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

      assertTrue(checker.isAssignable(emptyList(), emptyList()));
      
      Nullable nullable = create(Nullable.class, emptyMap());
      Nullable otherNullable = create(Nullable.class, emptyMap());
      
      assertTrue(checker.isAssignable(emptyList(), singleton(nullable)));
      assertTrue(checker.isAssignable(singleton(nullable), emptyList()));
      assertTrue(checker.isAssignable(singleton(nullable), singleton(otherNullable)));
      assertTrue(checker.isAssignable(singleton(otherNullable), singleton(nullable)));

      NotNull notNull = create(NotNull.class, emptyMap());
      NotNull otherNotNull = create(NotNull.class, emptyMap());
      
      assertTrue(checker.isAssignable(singleton(nullable), singleton(notNull)));
      assertFalse(checker.isAssignable(singleton(notNull), singleton(nullable)));
      assertTrue(checker.isAssignable(emptyList(), singleton(notNull)));
      assertFalse(checker.isAssignable(singleton(notNull), emptyList()));
      assertTrue(checker.isAssignable(singleton(notNull), singleton(otherNotNull)));
      assertTrue(checker.isAssignable(singleton(otherNotNull), singleton(notNull)));
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
      LevelA_0_0 levelA00_some = create(LevelA_0_0.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("str", "def").build());
      LevelA_0_0 levelA00_other = create(LevelA_0_0.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "xyz").put("str", "foo").build());
      LevelA_0_1 levelA01 = create(LevelA_0_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("nums", asList(1, 2, 3, 4, 5)).build());
      LevelA_0_1 levelA01_some = create(LevelA_0_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "abc").put("nums", asList(1, 2, 3, 4, 5)).build());
      LevelA_0_1 levelA01_other = create(LevelA_0_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", "bar").put("nums", asList(1, 2, 3)).build());
      // add _same and _other
      LevelA_1_0 levelA10 = create(LevelA_1_0.class, singletonMap("value", asList(Number.class)));
      LevelA_1_1 levelA11 = create(LevelA_1_1.class, MapBuilder.<String, Object>forHashMap()
            .put("value", asList(Boolean.class, Void.class))
            .put("i", 100)
            .put("d", 3.14)
            .put("f", 5.01f)
            .put("bits", asList(true, false, true, false))
            .put("a01s", asList(levelA01))
            .build());
      LevelB_0_0 levelB00 = create(LevelB_0_0.class, singletonMap("value", 10101));
      
      // TODO!!
   }
}
