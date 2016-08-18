package com.bluegosling.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class MoreIteratorsTest {

   @Test public void push_singleElement() {
      // TODO
   }

   @Test public void push_singleElement_withRemove() {
      // TODO
   }

   @Test public void push() {
      // TODO
   }

   @Test public void flatMap() {
      // TODO
   }
   
   @Test public void snapshot() {
      // TODO
   }
   
   @Test public void reverseListIterator() {
      // TODO
   }

   @Test public void toArray() {
      // TODO
   }
   
   @Test public void toArray_withTargetArray() {
      // TODO
   }
   
   @Test public void unmodifiableListIterator() {
      // TODO
   }

   @Test public void unique() {
      // TODO
   }

   @Test public void zip_twoIterators() {
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      List<String> c2 = Arrays.asList("u", "v", "x", "y", "z");
      List<Integer> c3 = Arrays.asList(1, 2, 3, 4);
      
      // first iterable is longer
      List<String> result = new ArrayList<>();
      MoreIterators.zip(c1.iterator(), c2.iterator(), String::concat).forEachRemaining(result::add);
      assertEquals(Arrays.asList("au", "bv", "cx", "dy", "ez"), result);

      // first iterable is shorter
      result.clear();
      MoreIterators.zip(c3.iterator(), c2.iterator(), (i, s) -> i + s)
            .forEachRemaining(result::add);
      assertEquals(Arrays.asList("1u", "2v", "3x", "4y"), result);
      
      // iterables are same length
      result.clear();
      MoreIterators.zip(c1.iterator(), c1.iterator(), String::concat).forEachRemaining(result::add);
      assertEquals(Arrays.asList("aa", "bb", "cc", "dd", "ee", "ff"), result);
   }

   @Test public void zip_threeIterators() {
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      List<String> c2 = Arrays.asList("u", "v", "x", "y", "z");
      List<Integer> c3 = Arrays.asList(1, 2, 3, 4);
      
      // first iterable is shortest
      List<String> result = new ArrayList<>();
      MoreIterators.zip(c3.iterator(), c2.iterator(), c1.iterator(), (i, s1, s2) -> i + s1 + s2)
            .forEachRemaining(result::add);
      assertEquals(Arrays.asList("1ua", "2vb", "3xc", "4yd"), result);

      // second iterable is shortest
      result.clear();
      MoreIterators.zip(c1.iterator(), c3.iterator(), c2.iterator(), (s1, i, s2) -> s1 + i + s2)
            .forEachRemaining(result::add);
      assertEquals(Arrays.asList("a1u", "b2v", "c3x", "d4y"), result);

      // third iterable is shortest
      result.clear();
      MoreIterators.zip(c1.iterator(), c2.iterator(), c3.iterator(), (s1, s2, i) -> s1 + s2 + i)
            .forEachRemaining(result::add);
      assertEquals(Arrays.asList("au1", "bv2", "cx3", "dy4"), result);

      // iterables are same length
      result.clear();
      MoreIterators.zip(c1.iterator(), c1.iterator(), c1.iterator(), (s1, s2, s3) -> s1 + s2 + s3)
            .forEachRemaining(result::add);
      assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd", "eee", "fff"), result);
   }

   @Test public void zip_multipleIterators() {
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      List<String> c2 = Arrays.asList("u", "v", "x", "y", "z");
      List<Integer> c3 = Arrays.asList(1, 2, 3, 4);
      
      // permute the order, ensuring that shortest element always determines length of result
      List<Object> result = new ArrayList<>();
      MoreIterators.zip(c1.iterator(), c2.iterator(), c3.iterator()).forEachRemaining(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList("a", "u", 1),
                  Arrays.asList("b", "v", 2),
                  Arrays.asList("c", "x", 3),
                  Arrays.asList("d", "y", 4)),
            result);

      result.clear();
      MoreIterators.zip(c3.iterator(), c2.iterator(), c1.iterator()).forEachRemaining(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList(1, "u", "a"),
                  Arrays.asList(2, "v", "b"),
                  Arrays.asList(3, "x", "c"),
                  Arrays.asList(4, "y", "d")),
            result);

      // iterables are same length
      result.clear();
      MoreIterators.zip(Arrays.asList(c1.iterator(), c1.iterator(), c1.iterator()))
            .forEachRemaining(result::add);
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

   @Test public void zip_zeroIterables() {
      Iterator<List<Integer>> i = MoreIterators.zip(); //var-arg/array
      assertFalse(i.hasNext());

      i = MoreIterators.zip(Collections.emptySet()); //iterable
      assertFalse(i.hasNext());
   }
}
