package com.bluegosling.collections;

import static com.bluegosling.testing.MoreAsserts.assertThrows;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MoreMapsTest {
   
   @Test public void fromCollection() {
      // TODO
   }
   
   // TODO: test that non-distinct key sequences result in exception
   
   @Test public void zip() {
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Set<Integer> c3 = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
      
      // first iterable is longer
      Map<?, ?> result = MoreMaps.zip(c1, c2);
      assertEquals(
            ImmutableMap.of(
                  "a", "u",
                  "b", "v",
                  "c", "x",
                  "d", "y",
                  "e", "z"),
            result);

      // first iterable is shorter
      result = MoreMaps.zip(c3, c2);
      assertEquals(
            ImmutableMap.of(
                  1, "u",
                  2, "v",
                  3, "x",
                  4, "y"),
            result);
      
      // iterables are same length
      result = MoreMaps.zip(c1, c1);
      assertEquals(
            ImmutableMap.builder()
                  .put("a", "a")
                  .put("b", "b")
                  .put("c", "c")
                  .put("d", "d")
                  .put("e", "e")
                  .put("f", "f")
                  .build(),
            result);
   }

   @Test public void zipSorted() {
      List<String> c1 = Arrays.asList("f", "e", "d", "c", "b", "a");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      List<Integer> c3 = Arrays.asList(2, 4, 1, 3);
      
      // first iterable is longer
      NavigableMap<?, ?> result = MoreMaps.zipSorted(c1, c2);
      assertEquals(
            ImmutableMap.of(
                  "f", "u",
                  "e", "v",
                  "d", "x",
                  "c", "y",
                  "b", "z"),
            result);
      List<Object> keyOrder = new ArrayList<>();
      result.forEach((k, v) -> keyOrder.add(k));
      assertEquals(Arrays.asList("b", "c", "d", "e", "f"), keyOrder);

      // first iterable is shorter
      result = MoreMaps.zipSorted(c3, c2);
      assertEquals(
            ImmutableMap.of(
                  2, "u",
                  4, "v",
                  1, "x",
                  3, "y"),
            result);
      keyOrder.clear();
      result.forEach((k, v) -> keyOrder.add(k));
      assertEquals(Arrays.asList(1, 2, 3, 4), keyOrder);
      
      // iterables are same length
      result = MoreMaps.zipSorted(c1, c1, Comparator.reverseOrder());
      assertEquals(
            ImmutableMap.builder()
                  .put("a", "a")
                  .put("b", "b")
                  .put("c", "c")
                  .put("d", "d")
                  .put("e", "e")
                  .put("f", "f")
                  .build(),
            result);
      keyOrder.clear();
      result.forEach((k, v) -> keyOrder.add(k));
      assertEquals(Arrays.asList("f", "e", "d", "c", "b", "a"), keyOrder);
   }
   
   @Test public void zipExact() {
      Set<String> c = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      List<String> longer = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<Integer> shorter = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
      
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipExact(c, longer));
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipExact(longer, c));
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipExact(c, shorter));
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipExact(shorter, c));

      // iterables must be same length
      Map<String, String> result = MoreMaps.zipExact(c, c);
      assertEquals(
            ImmutableMap.builder()
                  .put("u", "u")
                  .put("v", "v")
                  .put("x", "x")
                  .put("y", "y")
                  .put("z", "z")
                  .build(),
            result);
   }

   @Test public void zipSortedExact() {
      Set<String> c = new LinkedHashSet<>(Arrays.asList("u", "x", "z", "y", "v"));
      List<String> longer = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<Integer> shorter = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
      
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipSortedExact(c, longer));
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipSortedExact(longer, c));
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipSortedExact(c, shorter));
      assertThrows(IllegalArgumentException.class, () -> MoreMaps.zipSortedExact(shorter, c));

      // iterables must be same length
      Map<String, String> result = MoreMaps.zipSortedExact(c, c);
      assertEquals(
            ImmutableMap.builder()
                  .put("u", "u")
                  .put("v", "v")
                  .put("x", "x")
                  .put("y", "y")
                  .put("z", "z")
                  .build(),
            result);
      result = MoreMaps.zipSortedExact(c, c, Comparator.reverseOrder());
      assertEquals(
            ImmutableMap.builder()
                  .put("z", "z")
                  .put("y", "y")
                  .put("x", "x")
                  .put("v", "v")
                  .put("u", "u")
                  .build(),
            result);
   }
}
