package com.bluegosling.collections;

import static com.bluegosling.testing.MoreAsserts.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class MoreCollectionsTest {
   
   @Test public void zip_twoCollections() {
      // test with a variety of backing iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Set<Integer> c3 = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
      
      // first iterable is longer
      List<String> result = new ArrayList<>();
      Collection<String> zipped = MoreCollections.zip(c1, c2, String::concat);
      assertEquals(5, zipped.size());
      zipped.forEach(result::add);
      assertEquals(Arrays.asList("au", "bv", "cx", "dy", "ez"), result);

      // first iterable is shorter
      result.clear();
      zipped = MoreCollections.zip(c3, c2, (i, s) -> i + s);
      assertEquals(4, zipped.size());
      zipped.forEach(result::add);
      assertEquals(Arrays.asList("1u", "2v", "3x", "4y"), result);
      
      // iterables are same length
      result.clear();
      MoreCollections.zip(c1, c1, String::concat).forEach(result::add);
      assertEquals(Arrays.asList("aa", "bb", "cc", "dd", "ee", "ff"), result);
   }
    
   @Test public void zip_threeCollections() {
      // test with a variety of backing iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Set<Integer> c3 = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
       
      // first iterable is shortest
      List<String> result = new ArrayList<>();
      MoreCollections.zip(c3, c2, c1, (i, s1, s2) -> i + s1 + s2)
            .forEach(result::add);
      assertEquals(Arrays.asList("1ua", "2vb", "3xc", "4yd"), result);

      // second iterable is shortest
      result.clear();
      MoreCollections.zip(c1, c3, c2, (s1, i, s2) -> s1 + i + s2)
            .forEach(result::add);
      assertEquals(Arrays.asList("a1u", "b2v", "c3x", "d4y"), result);

      // third iterable is shortest
      result.clear();
      MoreCollections.zip(c1, c2, c3, (s1, s2, i) -> s1 + s2 + i)
            .forEach(result::add);
      assertEquals(Arrays.asList("au1", "bv2", "cx3", "dy4"), result);

      // iterables are same length
      result.clear();
      MoreCollections.zip(c1, c1, c1, (s1, s2, s3) -> s1 + s2 + s3)
            .forEach(result::add);
      assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd", "eee", "fff"), result);
   } 

   @Test public void zip_multipleCollections() {
      // test with a variety of backing iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Set<Integer> c3 = new TreeSet<>(Arrays.asList(1, 2, 3, 4));
       
      // permute the order, ensuring that shortest element always determines length of result
      List<Object> result = new ArrayList<>();
      MoreCollections.zip(c1, c2, c3).forEach(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList("a", "u", 1),
                  Arrays.asList("b", "v", 2),
                  Arrays.asList("c", "x", 3),
                  Arrays.asList("d", "y", 4)),
            result);

      result.clear();
      MoreCollections.zip(c3, c2, c1).forEach(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList(1, "u", "a"),
                  Arrays.asList(2, "v", "b"),
                  Arrays.asList(3, "x", "c"),
                  Arrays.asList(4, "y", "d")),
            result);

      // iterables are same length
      result.clear();
      MoreCollections.zip(Arrays.asList(c1, c1, c1)).forEach(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList("a", "a", "a"),
                  Arrays.asList("b", "b", "b"),
                  Arrays.asList("c", "c", "c"),
                  Arrays.asList("d", "d", "d"),
                  Arrays.asList("e", "e", "e"),
                  Arrays.asList("f", "f", "f")),
            result);
   }
    
   @Test public void zip_zeroStreams() {
      Collection<List<Integer>> c = MoreCollections.zip(); //var-arg/array
      assertEquals(0, c.size());
      assertFalse(c.iterator().hasNext());

      c = MoreCollections.zip(Collections.emptySet()); //iterable
      assertEquals(0, c.size());
      assertFalse(c.iterator().hasNext());
   }

   @Test public void zipExact_twoCollections() {
      Set<String> c = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      List<String> longer = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<Integer> shorter = new TreeSet<>(Arrays.asList(1, 2, 3, 4));

      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, longer, String::concat));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(longer, c, String::concat));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, shorter, (s, i) -> s + i));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(shorter, c, (i, s) -> i + s));
      
      List<String> result = new ArrayList<>();
      Collection<String> zipped = MoreCollections.zipExact(c, c, String::concat);
      assertEquals(5, zipped.size());
      zipped.forEach(result::add);
      assertEquals(Arrays.asList("uu", "vv", "xx", "yy", "zz"), result);
   }

   @Test public void zipExact_threeCollections() {
      Set<String> c = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      List<String> longer = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<Integer> shorter = new TreeSet<>(Arrays.asList(1, 2, 3, 4));

      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, longer, longer, (s1, s2, s3) -> s1 + s2 +s3));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, shorter, shorter, (s1, s2, s3) -> s1 + s2 +s3));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, shorter, longer, (s1, s2, s3) -> s1 + s2 +s3));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(shorter, c, longer, (s1, s2, s3) -> s1 + s2 +s3));
      
      List<String> result = new ArrayList<>();
      Collection<String> zipped = MoreCollections.zipExact(c, c, c, (s1, s2, s3) -> s1 + s2 + s3);
      assertEquals(5, zipped.size());
      zipped.forEach(result::add);
      assertEquals(Arrays.asList("uuu", "vvv", "xxx", "yyy", "zzz"), result);
   }

   @Test public void zipExact_multipleCollections() {
      Set<String> c = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      List<String> longer = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<Integer> shorter = new TreeSet<>(Arrays.asList(1, 2, 3, 4));

      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, longer, longer));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, shorter, shorter));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(c, shorter, longer));
      assertThrows(IllegalArgumentException.class,
            () -> MoreCollections.zipExact(shorter, c, longer));
      
      List<List<String>> result = new ArrayList<>();
      Collection<List<String>> zipped = MoreCollections.zip(Arrays.asList(c, c, c));
      assertEquals(5, zipped.size());
      zipped.forEach(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList("u", "u", "u"),
                  Arrays.asList("v", "v", "v"),
                  Arrays.asList("x", "x", "x"),
                  Arrays.asList("y", "y", "y"),
                  Arrays.asList("z", "z", "z")),
            result);
   }
   
   @Test public void zipExact_zeroStreams() {
      Collection<List<Integer>> c = MoreCollections.zipExact(); //var-arg/array
      assertEquals(0, c.size());
      assertFalse(c.iterator().hasNext());

      c = MoreCollections.zipExact(Collections.emptySet()); //iterable
      assertEquals(0, c.size());
      assertFalse(c.iterator().hasNext());
   }
}
