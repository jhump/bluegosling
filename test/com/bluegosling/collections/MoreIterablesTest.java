package com.bluegosling.collections;

import static com.bluegosling.testing.MoreAsserts.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class MoreIterablesTest {
   
   @Test public void asCollection() {
      Collection<Integer> c = Arrays.asList(1, 2, 3, 4);
      // already is collection so nothing done
      assertSame(c, MoreIterables.asCollection(c));
      
      Collection<Integer> o = MoreIterables.asCollection(() -> c.iterator());
      // the output is distinct
      assertNotSame(c, o);
      assertNotEquals(c, o);
      // but contains same elements in same order as source collection
      List<Integer> l = new ArrayList<>(o);
      assertEquals(c, l);
   }
   
   @Test public void comparator() {
      @SuppressWarnings("unchecked")
      Collection<Integer>[] iterables = (Collection<Integer>[]) new Collection<?>[] {
         // strictly ascending
         Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3, 4), Arrays.asList(1, 2, 3, 5),
         Arrays.asList(1, 2, 4, 4), Arrays.asList(1, 3, 3, 4), Arrays.asList(2, 2, 3, 4),
         Arrays.asList(3), Arrays.asList(4, 0), Arrays.asList(4, 0, 1)
      };
      
      Comparator<Iterable<Integer>> comp = MoreIterables.comparator(Integer::compareTo);
      
      for (int i = 0; i < iterables.length; i++) {
         for (int j = 0; j < iterables.length; j++) {
            if (i < j) {
               assertEquals(-1, comp.compare(iterables[i], iterables[j]));
            } else if (i > j) {
               assertEquals(1, comp.compare(iterables[i], iterables[j]));
            } else {
               assertEquals(0, comp.compare(iterables[i], iterables[j]));
            }
         }
      }
   }

   @Test public void flatMap() {
      Set<List<String>> iterable = ImmutableSet.of(
            Arrays.asList("a", "b", "c"),
            Arrays.asList("x", "y", "z"),
            Arrays.asList("1", "2", "3"));
      List<String> flattened = new ArrayList<>();
      MoreIterables.flatMap(iterable, Iterable::iterator).forEach(flattened::add);
      assertEquals(Arrays.asList("a", "b", "c", "x", "y", "z", "1", "2", "3"), flattened);
   }
   
   @Test public void snapshot() {
      List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 6);
      List<Integer> l = new ArrayList<>(source);
      Collection<Integer> snapshot = MoreIterables.snapshot(l);
      l.add(7); l.add(8); l.add(9); // mutations not reflected in snapshot
      
      assertEquals(6, snapshot.size());
      List<Integer> list = new ArrayList<>();
      snapshot.forEach(list::add);
      assertEquals(source, list);
      
      // with a larger collection
      source = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
         source.add(i);
      }
      l.clear();
      l.addAll(source);
      snapshot = MoreIterables.snapshot(l);
      l.add(7); l.add(8); l.add(9); // mutations not reflected in snapshot
      
      assertEquals(1000, snapshot.size());
      list.clear();
      snapshot.forEach(list::add);
      assertEquals(source, list);
      
      // with an iterable that does not implement Collection
      l.clear();
      l.addAll(source);
      snapshot = MoreIterables.snapshot(() -> l.iterator());
      l.add(7); l.add(8); l.add(9); // mutations not reflected in snapshot
      
      assertEquals(1000, snapshot.size());
      list.clear();
      snapshot.forEach(list::add);
      assertEquals(source, list);
      
      // TODO more cases?
   }

   @Test public void reversed() {
      // TODO include cases for reversible collections -- List, Deque, and NavigableSet -- as well
      // as one that requires manual reversal -- Set
   }
   
   @Test public void toArray() {
      // TODO
   }
   
   @Test public void toArray_withTargetArray() {
      // TODO
   }
   
   @Test public void trySize() {
      // with a collection
      assertEquals(6, MoreIterables.trySize(Arrays.asList(1, 2, 3, 4, 5, 6)).getAsInt());
      // with an iterable that does not expose its size
      Iterable<Integer> i = () -> Arrays.asList(1, 2, 3, 4, 5, 6).iterator();
      assertFalse(MoreIterables.trySize(i).isPresent());
   }

   @Test public void unique() {
      List<Integer> list = Arrays.asList(1, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5);
      List<Integer> unique = new ArrayList<>();
      MoreIterables.unique(list).forEach(unique::add);
      assertEquals(Arrays.asList(1, 2, 3, 4, 5), unique);
      // TODO
   }
   
   @Test public void zip_twoIterables() {
     // test with a variety of iterable implementations
     List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
     Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
     Iterable<Integer> c3 = () -> Arrays.asList(1, 2, 3, 4).iterator();
     
     // first iterable is longer
     List<String> result = new ArrayList<>();
     MoreIterables.zip(c1, c2, String::concat).forEach(result::add);
     assertEquals(Arrays.asList("au", "bv", "cx", "dy", "ez"), result);

     // first iterable is shorter
     result.clear();
     MoreIterables.zip(c3, c2, (i, s) -> i + s).forEach(result::add);
     assertEquals(Arrays.asList("1u", "2v", "3x", "4y"), result);
     
     // iterables are same length
     result.clear();
     MoreIterables.zip(c1, c1, String::concat).forEach(result::add);
     assertEquals(Arrays.asList("aa", "bb", "cc", "dd", "ee", "ff"), result);
   }
   
   @Test public void zip_threeIterables() {
      // test with a variety of iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Iterable<Integer> c3 = () -> Arrays.asList(1, 2, 3, 4).iterator();
      
      // first iterable is shortest
      List<String> result = new ArrayList<>();
      MoreIterables.zip(c3, c2, c1, (i, s1, s2) -> i + s1 + s2).forEach(result::add);
      assertEquals(Arrays.asList("1ua", "2vb", "3xc", "4yd"), result);

      // second iterable is shortest
      result.clear();
      MoreIterables.zip(c1, c3, c2, (s1, i, s2) -> s1 + i + s2).forEach(result::add);
      assertEquals(Arrays.asList("a1u", "b2v", "c3x", "d4y"), result);

      // third iterable is shortest
      result.clear();
      MoreIterables.zip(c1, c2, c3, (s1, s2, i) -> s1 + s2 + i).forEach(result::add);
      assertEquals(Arrays.asList("au1", "bv2", "cx3", "dy4"), result);

      // iterables are same length
      result.clear();
      MoreIterables.zip(c1, c1, c1, (s1, s2, s3) -> s1 + s2 + s3).forEach(result::add);
      assertEquals(Arrays.asList("aaa", "bbb", "ccc", "ddd", "eee", "fff"), result);
   } 

   @Test public void zip_multipleIterables() {
      // test with a variety of iterable implementations
      List<String> c1 = Arrays.asList("a", "b", "c", "d", "e", "f");
      Set<String> c2 = new LinkedHashSet<>(Arrays.asList("u", "v", "x", "y", "z"));
      Iterable<Integer> c3 = () -> Arrays.asList(1, 2, 3, 4).iterator();
      
      // permute the order, ensuring that shortest element always determines length of result
      List<Object> result = new ArrayList<>();
      MoreIterables.zip(c1, c2, c3).forEach(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList("a", "u", 1),
                  Arrays.asList("b", "v", 2),
                  Arrays.asList("c", "x", 3),
                  Arrays.asList("d", "y", 4)),
            result);

      result.clear();
      MoreIterables.zip(c3, c2, c1).forEach(result::add);
      assertEquals(
            Arrays.asList(
                  Arrays.asList(1, "u", "a"),
                  Arrays.asList(2, "v", "b"),
                  Arrays.asList(3, "x", "c"),
                  Arrays.asList(4, "y", "d")),
            result);

      // iterables are same length
      result.clear();
      MoreIterables.zip(Arrays.asList(c1, c1, c1)).forEach(result::add);
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
      Iterable<List<Integer>> i = MoreIterables.zip(); //var-arg/array
      assertFalse(i.iterator().hasNext());

      i = MoreIterables.zip(Collections.emptySet()); //iterable
      assertFalse(i.iterator().hasNext());
   }
}
