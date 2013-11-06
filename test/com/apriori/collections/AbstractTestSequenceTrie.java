package com.apriori.collections;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.map.AbstractTestMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

//TODO: add tests for sequence trie methods!
public abstract class AbstractTestSequenceTrie extends AbstractTestMap {
   
   public AbstractTestSequenceTrie(String testName) {
      super(testName);
   }
 
   @Override public abstract SequenceTrie<Object, Object> makeEmptyMap();
   
   @SuppressWarnings("unchecked")
   @Override public SequenceTrie<Object, Object> makeFullMap() {
      return (SequenceTrie<Object, Object>) super.makeFullMap();
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
            14
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
            14L
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
      return new BulkTestPrefixMap(this);
   }
   
   public void testPrefixMaps() {
      SequenceTrie<Object, Object> trie = makeFullMap();
      SequenceTrie<Object, Object> prefix1a = trie.prefixMap(1);
      SequenceTrie<Object, Object> prefix1b = trie.prefixMap(Arrays.asList(1));
      SequenceTrie<Object, Object> prefix1c = trie.prefixMap(Arrays.<Object>asList(1, 2, 3), 1);
      assertEquals(prefix1a, prefix1b);
      assertEquals(prefix1b, prefix1c);
      assertEquals(prefix1a, prefix1b);
      SequenceTrie<Object, Object> prefix2a = trie.prefixMap(Arrays.asList(1, 2, 3));
      SequenceTrie<Object, Object> prefix2b = trie.prefixMap(Arrays.<Object>asList(1, 2, 3, 4, 5, 6, 7), 3);
      assertEquals(prefix2a, prefix2b);
   }

   public static class BulkTestPrefixMap extends AbstractTestSequenceTrie {
      private final AbstractTestSequenceTrie outer;
      
      public BulkTestPrefixMap(AbstractTestSequenceTrie outer) {
         super("");
         this.outer = outer;
      }
      
      @Override public SequenceTrie<Object, Object> makeEmptyMap() {
         return outer.makeFullMap().prefixMap("!");
      }
      
      @Override public SequenceTrie<Object, Object> makeFullMap() {
         return outer.makeFullMap().prefixMap(Arrays.asList(1, 2, 3));
      }
      
      @Override public Object[] getSampleKeys() {
         return new Object[] {
               Arrays.asList(),
               Arrays.asList(4),
               Arrays.asList(4, 5),
               Arrays.asList(4, 5, 6),
               Arrays.asList(9),
               Arrays.asList(9, 9),
               Arrays.asList(9, 9, 9)
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
               BigDecimal.valueOf(7)
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
               7.0
         };
      }
      
      @Override
      public BulkTest bulkTestPrefixMap() {
         return new BulkTestRecursivePrefixMap(this);
      }
   }
   
   public static class BulkTestRecursivePrefixMap extends AbstractTestMap {
      private final BulkTestPrefixMap outer;
      
      public BulkTestRecursivePrefixMap(BulkTestPrefixMap outer) {
         super("");
         this.outer = outer;
      }
      
      @Override public SequenceTrie<Object, Object> makeEmptyMap() {
         return outer.makeFullMap().prefixMap("!");
      }
      
      @Override public SequenceTrie<Object, Object> makeFullMap() {
         return outer.makeFullMap().prefixMap(9);
      }
      
      @Override public Object[] getSampleKeys() {
         return new Object[] {
               Arrays.asList(),
               Arrays.asList(9),
               Arrays.asList(9, 9)
         };
      }

      @Override public Object[] getSampleValues() {
         return new Object[] {
               "five",
               BigInteger.valueOf(6),
               BigDecimal.valueOf(7)
         };
      }

      @Override public Object[] getNewSampleValues() {
         return new Object[] {
               (short) 5,
               (char) 6,
               7.0
         };
      }
   }      
}
