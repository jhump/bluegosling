package com.bluegosling.concurrent.atoms;

import static org.junit.Assert.assertEquals;

import com.bluegosling.collections.MapBuilder;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionTest {
   
   @Test public void simple() {
      // update these two atoms together in a transaction
      TransactionalAtom<List<String>> strings = new TransactionalAtom<>(Collections.emptyList());
      TransactionalAtom<Map<Character, List<String>>> stringsByFirstChar =
            new TransactionalAtom<>(Collections.emptyMap());
      
      int numFirstChars = Transaction.compute(t -> {
         strings.set(Arrays.asList("foobar", "foobaz", "bergerz", "burger", "abc", "xyz"));
         Map<Character, List<String>> index = new HashMap<>();
         for (String s : strings.get()) {
            index.compute(s.charAt(0), (ch, strs) -> {
               if (strs == null) {
                  strs = new ArrayList<>();
               }
               strs.add(s);
               return strs;
            });
         }
         stringsByFirstChar.set(index);
         return index.size();
      });
      
      assertEquals(4, numFirstChars);
      assertEquals(Arrays.asList("foobar", "foobaz", "bergerz", "burger", "abc", "xyz"),
            strings.get());
      assertEquals(
            MapBuilder.forHashMap()
                  .put('f', Arrays.asList("foobar", "foobaz"))
                  .put('b', Arrays.asList("bergerz", "burger"))
                  .put('a', Collections.singletonList("abc"))
                  .put('x', Collections.singletonList("xyz"))
                  .build(),
            stringsByFirstChar.get());
   }

   @Test public void multipleCommits() {
      //TODO: implement me!
   }

   @Test public void rollback() {
      //TODO: implement me!
   }

   @Test public void rollbackToSavepoint() {
      //TODO: implement me!
   }

   @Test public void isolationLevel_readCommitted() {
      //TODO: implement me!
   }

   @Test public void isolationLevel_snapshot() {
      //TODO: implement me!
   }

   @Test public void isolationLevel_serializable() {
      //TODO: implement me!
   }

   @Test public void deadlockIsRetried() {
      //TODO: implement me!
   }

   @Test public void isolationFailureIsRetried() {
      //TODO: implement me!
   }

   @Test public void nonIdempotentIsNotRetried() {
      //TODO: implement me!
   }
}
