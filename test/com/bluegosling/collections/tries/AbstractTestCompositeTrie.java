package com.bluegosling.collections.tries;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.map.AbstractTestMap;

public abstract class AbstractTestCompositeTrie extends AbstractTestMap {
   
   public AbstractTestCompositeTrie(String testName) {
      super(testName);
   }
   
   @Override public boolean isAllowNullKey() {
      return false;
   }
   
   @Override public abstract CompositeTrie<List<Object>, Object, Object> makeEmptyMap();
   
   @SuppressWarnings("unchecked")
   @Override public CompositeTrie<List<Object>, Object, Object> makeFullMap() {
      return (CompositeTrie<List<Object>, Object, Object>) super.makeFullMap();
   }
   
   @Override public Object[] getSampleKeys() {
      return new Object[] {
            Arrays.asList(),
            Arrays.asList(1, 2, 3),
            Arrays.asList(1, 2, 3, 4),
            Arrays.asList(1, 2, 3, 4, 5),
            Arrays.asList(1, 2, 3, 4, 5, 6),
            Arrays.asList(1, 2, 3, 9),
            Arrays.asList(1, 2, 3, 9, 9),
            Arrays.asList(1, 2, 3, 9, 9, 9),
            Arrays.asList(1, 2, 3, 9, 9, 8),
            Arrays.asList(5, 4, 3, 2, 1),
            Arrays.asList("a", "b", "c"),
            Arrays.asList("a", "b", "c", "d"),
            Arrays.asList("a", "b", "c", "d", "e"),
            Arrays.asList("a", "b", "c", "e"),
            Arrays.asList("a", "b", "c", "d", "e", "f"),
            Arrays.asList("a", "b", "c", "f")
      };
   }

   @Override public Object[] getSampleValues() {
      return new Object[] {
            null,
            1,
            2.0f,
            3.0,
            4L,
            "five",
            BigInteger.valueOf(6),
            BigDecimal.valueOf(7),
            (byte) 8,
            (short) 9,
            (char) 10,
            "11",
            "0xc",
            "015",
            14,
            15.0f
      };
   }

   @Override public Object[] getNewSampleValues() {
      return new Object[] {
            0,
            "one",
            2L,
            3.0F,
            (byte) 4,
            (short) 5,
            (char) 6,
            7.0,
            BigInteger.valueOf(8),
            BigDecimal.valueOf(9),
            "0xa",
            "013",
            "twelve",
            "13",
            14L,
            15.0
      };
   }
   
   @Override public Object[] getOtherKeys() {
      return new Object[] {
            Arrays.asList("foo", "bar", "baz"),
            Arrays.asList("abc", "def", "xyz"),
            Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9),
            Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16),
            Arrays.asList("x", "y", "z"),
            Arrays.asList("m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w"),
            Arrays.asList(" ")
      };
   }

   @Override public Object[] getOtherValues() {
      return new Object[] {
            "fubar",
            "snafu",
            111,
            222,
            333,
            444,
            555
      };
   }

   public BulkTest bulkTestPrefixMap() {
      return new BulkTestPrefixMap(this) {};
   }
   
   public void testPrefixMaps() {
      CompositeTrie<List<Object>, Object, Object> trie = makeFullMap();
      CompositeTrie<List<Object>, Object, Object> prefix1a = trie.prefixMap(1);
      CompositeTrie<List<Object>, Object, Object> prefix1b = trie.prefixMap(Arrays.asList(1));
      CompositeTrie<List<Object>, Object, Object> prefix1c =
            trie.prefixMap(Arrays.<Object>asList(1, 2, 3), 1);
      assertEquals(prefix1a, prefix1b);
      assertEquals(prefix1b, prefix1c);
      assertEquals(prefix1a, prefix1b);
      CompositeTrie<List<Object>, Object, Object> prefix2a = trie.prefixMap(Arrays.asList(1, 2, 3));
      CompositeTrie<List<Object>, Object, Object> prefix2b =
            trie.prefixMap(Arrays.<Object>asList(1, 2, 3, 4, 5, 6, 7), 3);
      assertEquals(prefix2a, prefix2b);
   }

   // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
   // only runnable when instantiated by enclosing test.
   public abstract static class BulkTestPrefixMap extends AbstractTestCompositeTrie {
      protected final AbstractTestCompositeTrie outer;
      
      public BulkTestPrefixMap(AbstractTestCompositeTrie outer) {
         super("");
         this.outer = outer;
      }
      
      @Override public CompositeTrie<List<Object>, Object, Object> makeEmptyMap() {
         CompositeTrie<List<Object>, Object, Object> ret = makeFullMap();
         ret.clear();
         return ret;
      }
      
      @Override public CompositeTrie<List<Object>, Object, Object> makeFullMap() {
         return outer.makeFullMap().prefixMap(Arrays.asList(1, 2, 3));
      }
      
      public void testPutBadKey() {
         CompositeTrie<List<Object>, ?, Object> map = makeEmptyMap();
         assertThrows(IllegalArgumentException.class,
               () -> { map.put(Collections.singletonList("foo"), "bar"); });
      }
      
      @Override public Object[] getSampleKeys() {
         return new Object[] {
               Arrays.asList(1, 2, 3),
               Arrays.asList(1, 2, 3, 4),
               Arrays.asList(1, 2, 3, 4, 5),
               Arrays.asList(1, 2, 3, 4, 5, 6),
               Arrays.asList(1, 2, 3, 9),
               Arrays.asList(1, 2, 3, 9, 9),
               Arrays.asList(1, 2, 3, 9, 9, 9),
               Arrays.asList(1, 2, 3, 9, 9, 8)
         };
      }

      @Override public Object[] getSampleValues() {
         return new Object[] {
               1,
               2.0f,
               3.0,
               4L,
               "five",
               BigInteger.valueOf(6),
               BigDecimal.valueOf(7),
               (byte) 8
         };
      }

      @Override public Object[] getNewSampleValues() {
         return new Object[] {
               "one",
               2L,
               3.0F,
               (byte) 4,
               (short) 5,
               (char) 6,
               7.0,
               8
         };
      }
      
      @Override
      public BulkTest bulkTestPrefixMap() {
         return new BulkTestRecursivePrefixMap(this) {};
      }
   }
   
   // NB: Marked abstract to prevent JUnit test runner from thinking it can run this class. It is
   // only runnable when instantiated by enclosing test.
   public abstract static class BulkTestRecursivePrefixMap extends AbstractTestMap {
      protected final BulkTestPrefixMap outer;
      
      public BulkTestRecursivePrefixMap(BulkTestPrefixMap outer) {
         super("");
         this.outer = outer;
      }
      
      @Override public boolean isAllowNullKey() {
         return false;
      }
      
      @Override public CompositeTrie<List<Object>, Object, Object> makeEmptyMap() {
         CompositeTrie<List<Object>, Object, Object> ret = makeFullMap();
         ret.clear();
         return ret;
      }
      
      @Override public CompositeTrie<List<Object>, Object, Object> makeFullMap() {
         return outer.makeFullMap().prefixMap(9);
      }
      
      @Override public Object[] getSampleKeys() {
         return new Object[] {
               Arrays.asList(1, 2, 3, 9),
               Arrays.asList(1, 2, 3, 9, 9),
               Arrays.asList(1, 2, 3, 9, 9, 9),
               Arrays.asList(1, 2, 3, 9, 9, 8)
         };
      }

      @Override public Object[] getSampleValues() {
         return new Object[] {
               "five",
               BigInteger.valueOf(6),
               BigDecimal.valueOf(7),
               (byte) 8
         };
      }

      @Override public Object[] getNewSampleValues() {
         return new Object[] {
               (short) 5,
               (char) 6,
               7.0,
               8
         };
      }
   }      
}
