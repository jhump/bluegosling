package com.bluegosling.collections;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;

public class MoreStreamsTest {
   
   @Test public void zip_twoStreams() {
      // test with a variety of backing iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Iterable<Integer> c3 = () -> Arrays.asList(1, 2, 3, 4).iterator();

      // first iterable is longer
      List<String> result = new ArrayList<>();
      MoreStreams.zip(c1.stream(), c2.stream(), String::concat).forEach(result::add);
      assertEquals(Arrays.asList("au", "bv", "cx", "dy", "ez"), result);

      // first iterable is shorter
      result.clear();
      MoreStreams.zip(asStream(c3), c2.stream(), (i, s) -> i + s).forEach(result::add);
      assertEquals(Arrays.asList("1u", "2v", "3x", "4y"), result);

      // iterables are same length
      result.clear();
      MoreStreams.zip(c1.stream(), c1.stream(), String::concat).forEach(result::add);
      assertEquals(Arrays.asList("aa", "bb", "cc", "dd", "ee", "ff"), result);
   }

   @Test public void zip_threeStreams() {
      // test with a variety of backing iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Iterable<Integer> c3 = () -> Arrays.asList(1, 2, 3, 4).iterator();

      // first iterable is shortest
      List<String> result = new ArrayList<>();
      MoreStreams.zip(asStream(c3), c2.stream(), c1.stream(), (i, s1, s2) -> i + s1 + s2)
            .forEach(result::add);
      assertEquals(Arrays.asList("1ua", "2vb", "3xc", "4yd"), result);

      // second iterable is shortest
      result.clear();
      MoreStreams.zip(c1.stream(), asStream(c3), c2.stream(), (s1, i, s2) -> s1 + i + s2)
            .forEach(result::add);
      assertEquals(Arrays.asList("a1u", "b2v", "c3x", "d4y"), result);

      // third iterable is shortest
      result.clear();
      MoreStreams.zip(c1.stream(), c2.stream(), asStream(c3), (s1, s2, i) -> s1 + s2 + i)
            .forEach(result::add);
      assertEquals(Arrays.asList("au1", "bv2", "cx3", "dy4"), result);

      // iterables are same length
      result.clear();
      MoreStreams.zip(c1.stream(), c1.stream(), c1.stream(), (s1, s2, s3) -> s1 + s2 + s3)
            .forEach(result::add);
      assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd", "eee", "fff"), result);
   }

   @Test public void zip_multipleStreams() {
      // test with a variety of backing iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Iterable<Integer> c3 = () -> Arrays.asList(1, 2, 3, 4).iterator();

      // permute the order, ensuring that shortest element always determines length of result
      List<Object> result = new ArrayList<>();
      MoreStreams.zip(c1.stream(), c2.stream(), asStream(c3)).forEach(result::add);
      assertEquals(Arrays.asList(Arrays.asList("a", "u", 1), Arrays.asList("b", "v", 2),
            Arrays.asList("c", "x", 3), Arrays.asList("d", "y", 4)), result);

      result.clear();
      MoreStreams.zip(asStream(c3), c2.stream(), c1.stream()).forEach(result::add);
      assertEquals(Arrays.asList(Arrays.asList(1, "u", "a"), Arrays.asList(2, "v", "b"),
            Arrays.asList(3, "x", "c"), Arrays.asList(4, "y", "d")), result);

      // iterables are same length
      result.clear();
      MoreStreams.zip(Arrays.asList(c1.stream(), c1.stream(), c1.stream())).forEach(result::add);
      assertEquals(Arrays.asList(Arrays.asList("a", "a", "a"), Arrays.asList("b", "b", "b"),
            Arrays.asList("c", "c", "c"), Arrays.asList("d", "d", "d"),
            Arrays.asList("e", "e", "e"), Arrays.asList("f", "f", "f")), result);
   }

   @Test public void zip_zeroStreams() {
      Stream<List<Integer>> s = MoreStreams.zip(); //var-arg/array
      assertEquals(0, s.count());

      s = MoreStreams.zip(Collections.emptySet()); //iterable
      assertEquals(0, s.count());
   }

   private <T> Stream<T> asStream(Iterable<T> iterable) {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterable.iterator(), 0),
            false);
   }
}
