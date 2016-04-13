package com.bluegosling.reflect;

import static com.bluegosling.reflect.Annotations.create;
import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import com.bluegosling.collections.MapBuilder;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TypeAnnotationHierarchyTest {
   
   @interface Nullable {
   }

   @interface NotNull {
   }

   @Test public void simpleHierarchy() {
      TypeAnnotationChecker checker = new TypeAnnotationHierarchy.Builder()
            .equivalent(null, Nullable.class)
            .assignable(NotNull.class, Nullable.class)
            .build();

      assertEquivalent(checker, emptyList(), emptyList());

      Nullable nullable = create(Nullable.class, emptyMap());
      Nullable otherNullable = create(Nullable.class, emptyMap());
      
      assertEquivalent(checker, emptyList(), singleton(nullable));
      assertEquivalent(checker, singleton(nullable), singleton(otherNullable));

      NotNull notNull = create(NotNull.class, emptyMap());
      NotNull otherNotNull = create(NotNull.class, emptyMap());
      
      assertAssignable(checker, singleton(notNull), singleton(nullable));
      assertAssignable(checker, singleton(notNull), emptyList());
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
      TypeAnnotationChecker checker = new TypeAnnotationHierarchy.Builder()
            .equivalent(null, LevelA.class)
            .assignable(LevelA_0.class, LevelA.class)
            .assignable(LevelA_0_0.class, LevelA_0.class)
            .assignable(LevelA_0_1.class, LevelA_0.class)
            .assignable(LevelA_1.class, LevelA.class)
            .assignable(LevelA_1_0.class, LevelA_1.class)
            .assignable(LevelA_1_1.class, LevelA_1.class)
            .assignable(LevelB.class, null)
            .assignable(LevelB_0.class, LevelB.class)
            .assignable(LevelB_0_0.class, LevelB_0.class)
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
      assertAssignable(checker, singleton(levelA0), singleton(levelA));
      assertAssignable(checker, singleton(levelA0), emptyList());
      assertAssignable(checker, singleton(levelA0_same), singleton(levelA));
      assertAssignable(checker, singleton(levelA0_other), singleton(levelA));
      assertEquivalent(checker, singleton(levelA0), singleton(levelA0_same));
      assertNotAssignable(checker, singleton(levelA0_other), singleton(levelA0));

      // LevelA_0 <-- LevelA_0_0
      assertAssignable(checker, singleton(levelA00), singleton(levelA0));
      assertAssignable(checker, singleton(levelA00_same), singleton(levelA0));
      assertNotAssignable(checker, singleton(levelA00_other), singleton(levelA0));
      assertEquivalent(checker, singleton(levelA00), singleton(levelA00_same));
      assertNotAssignable(checker, singleton(levelA00_other), singleton(levelA00));
      // LevelA <-- LevelA_0_0
      assertAssignable(checker, singleton(levelA00), singleton(levelA));
      assertAssignable(checker, singleton(levelA00), emptyList());
      assertAssignable(checker, singleton(levelA00_same), singleton(levelA));
      assertAssignable(checker, singleton(levelA00_other), singleton(levelA));

      // LevelA_0 <-- LevelA_0_1
      assertAssignable(checker, singleton(levelA01), singleton(levelA0));
      assertAssignable(checker, singleton(levelA01_same), singleton(levelA0));
      assertNotAssignable(checker, singleton(levelA01_other), singleton(levelA0));
      assertEquivalent(checker, singleton(levelA01), singleton(levelA01_same));
      assertNotAssignable(checker, singleton(levelA01_other), singleton(levelA01));
      // LevelA <-- LevelA_0_1
      assertAssignable(checker, singleton(levelA01), singleton(levelA));
      assertAssignable(checker, singleton(levelA01), emptyList());
      assertAssignable(checker, singleton(levelA01_same), singleton(levelA));
      assertAssignable(checker, singleton(levelA01_other), singleton(levelA));

      // LevelA <-- LevelA_1
      assertAssignable(checker, singleton(levelA1), singleton(levelA));
      assertAssignable(checker, singleton(levelA1), emptyList());
      assertAssignable(checker, singleton(levelA1_same), singleton(levelA));
      assertAssignable(checker, singleton(levelA1_other), singleton(levelA));
      assertEquivalent(checker, singleton(levelA1), singleton(levelA1_same));
      assertNotAssignable(checker, singleton(levelA1_other), singleton(levelA1));
      
      // LevelA_1 <-- LevelA_1_0
      assertAssignable(checker, singleton(levelA10), singleton(levelA1));
      assertAssignable(checker, singleton(levelA10_same), singleton(levelA1));
      assertNotAssignable(checker, singleton(levelA10_other), singleton(levelA1));
      assertEquivalent(checker, singleton(levelA10), singleton(levelA10_same));
      assertNotAssignable(checker, singleton(levelA10_other), singleton(levelA10));
      // LevelA <-- LevelA_1_0
      assertAssignable(checker, singleton(levelA10), singleton(levelA));
      assertAssignable(checker, singleton(levelA10), emptyList());
      assertAssignable(checker, singleton(levelA10_same), singleton(levelA));
      assertAssignable(checker, singleton(levelA10_other), singleton(levelA));

      // LevelA_1 <-- LevelA_1_1
      assertAssignable(checker, singleton(levelA11), singleton(levelA1));
      assertAssignable(checker, singleton(levelA11_same), singleton(levelA1));
      assertAssignable(checker, singleton(levelA11_other1), singleton(levelA1));
      assertNotAssignable(checker, singleton(levelA11_other2), singleton(levelA1));
      assertEquivalent(checker, singleton(levelA11), singleton(levelA11_same));
      assertNotAssignable(checker, singleton(levelA11_other1), singleton(levelA11));
      assertNotAssignable(checker, singleton(levelA11_other2), singleton(levelA11));
      // LevelA <-- LevelA_1_1
      assertAssignable(checker, singleton(levelA11), singleton(levelA));
      assertAssignable(checker, singleton(levelA11), emptyList());
      assertAssignable(checker, singleton(levelA11_same), singleton(levelA));
      assertAssignable(checker, singleton(levelA11_other1), singleton(levelA));
      assertAssignable(checker, singleton(levelA11_other2), singleton(levelA));

      // LevelA_0 -!- LevelA_1
      assertNotAssignable(checker, singleton(levelA1), singleton(levelA0));
      assertNotAssignable(checker, singleton(levelA1_other), singleton(levelA0_other));
      // LevelA_0_0 -!- LevelA_0_1
      assertNotAssignable(checker, singleton(levelA01), singleton(levelA00));
      assertNotAssignable(checker, singleton(levelA01_other), singleton(levelA00_other));
      // LevelA_1_0 -!- LevelA_1_1
      assertNotAssignable(checker, singleton(levelA11), singleton(levelA10));
      assertNotAssignable(checker, singleton(levelA11_other2), singleton(levelA10_other));

      // null <-- LevelB
      assertAssignable(checker, singleton(levelB), emptyList());
      // LevelB <-- LevelB_0
      assertAssignable(checker, singleton(levelB0), singleton(levelB));
      assertAssignable(checker, singleton(levelB0), emptyList());
      // LevelB_0 <-- LevelB_0_0
      assertAssignable(checker, singleton(levelB00), singleton(levelB0));
      assertAssignable(checker, singleton(levelB00), emptyList());
      assertAssignable(checker, singleton(levelB00_same), singleton(levelB0));
      assertAssignable(checker, singleton(levelB00_other), singleton(levelB0));
      assertEquivalent(checker, singleton(levelB00), singleton(levelB00_same));
      assertNotAssignable(checker, singleton(levelB00_other), singleton(levelB00));
   }
   
   @interface FromA {
      String a();
      int b();
      double c();
      boolean d();
   }

   @interface FromB {
      String z();
      int y();
      double x();
      boolean w();
      Class<?>[] u();
   }

   @interface To {
      String one();
      int two();
      double three();
      boolean four();
   }
   
   @Test public void attributeMaps() {
      TypeAnnotationChecker checker = new TypeAnnotationHierarchy.Builder()
         .equivalent(FromA.class, To.class, MapBuilder.<String, String>forHashMap()
               .put("a", "one")
               .put("b", "two")
               .put("c", "three")
               .put("d", "four")
               .build())
         .assignable(FromB.class, To.class, MapBuilder.<String, String>forHashMap()
               .put("z", "one")
               .put("y", "two")
               .put("x", "three")
               .put("w", "four")
               .build())
         .build();
      
      List<FromA> fromAs = Arrays.asList(
            create(FromA.class, MapBuilder.<String, Object>forHashMap()
                  .put("a", "test")
                  .put("b", 123)
                  .put("c", 456789.0F)
                  .put("d", true)
                  .build()),
            create(FromA.class, MapBuilder.<String, Object>forHashMap()
                  .put("a", "test")
                  .put("b", 123)
                  .put("c", 456789.0F)
                  .put("d", false)
                  .build()),
            create(FromA.class, MapBuilder.<String, Object>forHashMap()
                  .put("a", "foobar")
                  .put("b", 1)
                  .put("c", Math.PI)
                  .put("d", true)
                  .build()));
      List<FromB> fromBs = Arrays.asList(
            create(FromB.class, MapBuilder.<String, Object>forHashMap()
                  .put("z", "test")
                  .put("y", 123)
                  .put("x", 456789.0F)
                  .put("w", true)
                  .put("u", Arrays.asList(Object.class, String.class))
                  .build()),
            create(FromB.class, MapBuilder.<String, Object>forHashMap()
                  .put("z", "test")
                  .put("y", 123)
                  .put("x", 456789.0F)
                  .put("w", false)
                  .put("u", Arrays.asList(Object.class, String.class))
                  .build()),
            create(FromB.class, MapBuilder.<String, Object>forHashMap()
                  .put("z", "foobar")
                  .put("y", 1)
                  .put("x", Math.PI)
                  .put("w", true)
                  .put("u", Arrays.asList(Integer.class))
                  .build()));
      List<To> tos = Arrays.asList(
            create(To.class, MapBuilder.<String, Object>forHashMap()
                  .put("one", "test")
                  .put("two", 123)
                  .put("three", 456789.0F)
                  .put("four", true)
                  .build()),
            create(To.class, MapBuilder.<String, Object>forHashMap()
                  .put("one", "test")
                  .put("two", 123)
                  .put("three", 456789.0F)
                  .put("four", false)
                  .build()),
            create(To.class, MapBuilder.<String, Object>forHashMap()
                  .put("one", "foobar")
                  .put("two", 1)
                  .put("three", Math.PI)
                  .put("four", true)
                  .build()));      
      
      for (int i = 0; i < fromAs.size(); i++) {
         for (int j = 0; j < tos.size(); j++) {
            if (i == j) {
               assertEquivalent(checker, singleton(fromAs.get(i)), singleton(tos.get(j)));
            } else {
               assertNotAssignable(checker, singleton(fromAs.get(i)), singleton(tos.get(j)));
            }
         }
      }
      for (int i = 0; i < fromBs.size(); i++) {
         for (int j = 0; j < tos.size(); j++) {
            if (i == j) {
               assertAssignable(checker, singleton(fromBs.get(i)), singleton(tos.get(j)));
            } else {
               assertNotAssignable(checker, singleton(fromBs.get(i)), singleton(tos.get(j)));
            }
         }
      }
      // transitive assignability
      for (int i = 0; i < fromBs.size(); i++) {
         for (int j = 0; j < fromAs.size(); j++) {
            if (i == j) {
               assertAssignable(checker, singleton(fromBs.get(i)), singleton(fromAs.get(j)));
            } else {
               assertNotAssignable(checker, singleton(fromBs.get(i)), singleton(fromAs.get(j)));
            }
         }
      }
   }

   private void assertAssignable(TypeAnnotationChecker checker,
         Collection<? extends Annotation> from, Collection<? extends Annotation> to) {
      assertTrue(checker.isAssignable(from, to));
      assertFalse(checker.isAssignable(to, from));
   }

   private void assertNotAssignable(TypeAnnotationChecker checker,
         Collection<? extends Annotation> from, Collection<? extends Annotation> to) {
      assertFalse(checker.isAssignable(from, to));
      assertFalse(checker.isAssignable(to, from));
   }
   
   private void assertEquivalent(TypeAnnotationChecker checker,
         Collection<? extends Annotation> from, Collection<? extends Annotation> to) {
      assertTrue(checker.isAssignable(from, to));
      assertTrue(checker.isAssignable(to, from));
   }
   
   @Test public void builder_preventCycles() {
      TypeAnnotationHierarchy.Builder builder = new TypeAnnotationHierarchy.Builder();
      // cannot assign a type to itself, even in equivalence
      assertDisallowed(IllegalArgumentException.class, builder, null, null);
      assertDisallowed(IllegalArgumentException.class, builder, Override.class, Override.class);
      
      // no duplicate edges
      builder.equivalent(null, Nullable.class);
      assertDisallowed(IllegalStateException.class, builder, null, Nullable.class);
      assertDisallowed(IllegalStateException.class, builder, Nullable.class, null);
      builder = new TypeAnnotationHierarchy.Builder();
      builder.assignable(null, Nullable.class);
      assertDisallowed(IllegalStateException.class, builder, null, Nullable.class);
      assertDisallowed(IllegalStateException.class, builder, Nullable.class, null);
      
      // classic cycle with equivalences
      builder = new TypeAnnotationHierarchy.Builder();
      builder.equivalent(null, Nullable.class);
      builder.equivalent(Nullable.class, NotNull.class);
      // null -> Nullable -> NotNull -> null
      assertDisallowed(IllegalStateException.class, builder, null, NotNull.class);
      assertDisallowed(IllegalStateException.class, builder, NotNull.class, null);

      // classic cycle with assignability
      builder = new TypeAnnotationHierarchy.Builder();
      builder.assignable(null, Nullable.class);
      builder.assignable(Nullable.class, NotNull.class);
      // null -> Nullable -> NotNull -> null
      assertDisallowed(IllegalStateException.class, builder, NotNull.class, null);
      // redundant edge, but not a cycle, so permitted
      builder.assignable(null, NotNull.class);
      
      // shortest possible cycle (caller should use equivalent instead!)
      builder = new TypeAnnotationHierarchy.Builder();
      builder.assignable(null, Nullable.class);
      assertDisallowed(IllegalStateException.class, builder, Nullable.class, null);
   }
   
   private void assertDisallowed(Class<? extends Throwable> expectThrown,
         TypeAnnotationHierarchy.Builder builder, Class<? extends Annotation> a1,
         Class<? extends Annotation> a2) {
      assertThrows(expectThrown, () -> builder.equivalent(a1, a2));
      assertThrows(expectThrown, () -> builder.assignable(a1, a2));
      // try out the forms that take attribute maps, too, for simple (no attribute) pairs
      if (noAttributes(a1) && noAttributes(a2)) {
         assertThrows(expectThrown, () -> builder.equivalent(a1, a2, Collections.emptyMap()));
         assertThrows(expectThrown, () -> builder.assignable(a1, a2, Collections.emptyMap()));
      }
   }
   
   private boolean noAttributes(Class<? extends Annotation> annotationType) {
      return annotationType == null || annotationType.getDeclaredMethods().length == 0;
   }

   @Test public void builder_disallowIncompatibilities() {
      TypeAnnotationHierarchy.Builder builder = new TypeAnnotationHierarchy.Builder();
      assertThrows(IllegalArgumentException.class,
            () -> builder.equivalent(LevelA_1.class, LevelB.class));
      
      // this direction works
      builder.assignable(LevelA_1.class, LevelB.class);
      // but reverse does not: LevelB (no fields) can't be assigned to LevelA_1 (one field)
      TypeAnnotationHierarchy.Builder builder2 = new TypeAnnotationHierarchy.Builder();
      assertThrows(IllegalArgumentException.class,
            () -> builder2.assignable(LevelB.class, LevelA_1.class));
   }

   @Test public void builder_disallowIncorrectAttributeMaps() {
      // empty map is useless, but permitted if annotations have no attributes
      new TypeAnnotationHierarchy.Builder()
            .equivalent(LevelA.class, LevelB.class, Collections.emptyMap())
            .assignable(null, Nullable.class, Collections.emptyMap());
      
      TypeAnnotationHierarchy.Builder builder = new TypeAnnotationHierarchy.Builder();
      // map not 1-to-1
      assertDisallowed(builder, LevelA_0_0.class, LevelA_0.class, MapBuilder.<String, String>forHashMap()
            .put("value", "value")
            .put("str", "value")
            .build());
      // map doesn't have all target attributes
      assertDisallowed(builder, FromA.class, To.class, MapBuilder.<String, String>forHashMap()
            .put("a", "one")
            .put("b", "two")
            .put("c", "three")
            .build());
      // map has invalid source attribute
      assertDisallowed(builder, FromA.class, To.class, MapBuilder.<String, String>forHashMap()
            .put("a", "one")
            .put("b", "two")
            .put("c", "three")
            .put("xyz", "four")
            .build());
      // map has invalid target attribute
      assertDisallowed(builder, FromA.class, To.class, MapBuilder.<String, String>forHashMap()
            .put("a", "one")
            .put("b", "two")
            .put("c", "three")
            .put("d", "xyz")
            .build());
      // maps fields with incompatible types
      assertDisallowed(builder, FromA.class, To.class, MapBuilder.<String, String>forHashMap()
            .put("a", "one")
            .put("b", "two")
            .put("d", "three")
            .put("c", "four")
            .build());
      // map doesn't have all *source* attributes (only required for equivalent, not assignable)
      assertThrows(IllegalArgumentException.class,
            () -> builder.equivalent(FromB.class, To.class, MapBuilder.<String, String>forHashMap()
                  .put("z", "one")
                  .put("y", "two")
                  .put("x", "three")
                  .put("w", "four")
                  .build()));
      // sanity check: ensure that, w/ correct mapping, no exceptions are thrown
      builder.assignable(FromB.class, To.class, MapBuilder.<String, String>forHashMap()
            .put("z", "one")
            .put("y", "two")
            .put("x", "three")
            .put("w", "four")
            .build());
      builder.equivalent(FromA.class, To.class, MapBuilder.<String, String>forHashMap()
            .put("a", "one")
            .put("b", "two")
            .put("c", "three")
            .put("d", "four")
            .build());
   }
   
   private void assertDisallowed(TypeAnnotationHierarchy.Builder builder,
         Class<? extends Annotation> a1, Class<? extends Annotation> a2,
         Map<String, String> attributeMap) {
      assertThrows(IllegalArgumentException.class, () -> builder.equivalent(a1, a2, attributeMap));
      assertThrows(IllegalArgumentException.class, () -> builder.assignable(a1, a2, attributeMap));
   }
}
