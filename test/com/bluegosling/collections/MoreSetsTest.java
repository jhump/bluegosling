package com.bluegosling.collections;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class MoreSetsTest {

   @Test public void fromCollection() {
      List<Integer> l = Arrays.asList(1, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5);
      Set<Integer> s = MoreSets.fromCollection(l);
      
      assertEquals(5, s.size());
      assertEquals(ImmutableSet.of(1, 2, 3, 4, 5), s);
      // TODO: more assertions...
   }

   @Test public void newSortedSetFromMap() {
      // TODO
   }

   @Test public void newNavigableSetFromMap() {
      // TODO
   }
}
