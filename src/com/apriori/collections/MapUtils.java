package com.apriori.collections;

import java.util.Map;

// TODO: javadoc
final class MapUtils {
   /** Prevents instantiation. */
   private MapUtils() {
   }

   public static boolean equals(Map<?, ?> map, Object o) {
      if (o instanceof Map) {
         Map<?, ?> other = (Map<?, ?>) o;
         return map.entrySet().equals(other.entrySet());
      }
      return false;
   }

   public static int hashCode(Map<?, ?> map) {
      int ret = 0;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
         ret += entry.hashCode();
      }
      return ret;
   }
   
   public static String toString(Map<?, ?> map) {
      return map.entrySet().toString();
   }

   public static boolean equals(Map.Entry<?, ?> entry, Object o) {
      if (o instanceof Map.Entry) {
         Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
         return (entry.getKey() == null
                     ? other.getKey() == null : entry.getKey().equals(other.getKey()))
               && (entry.getValue() == null
                     ? other.getValue() == null : entry.getValue().equals(other.getValue()));
      }
      return false;
   }

   public static int hashCode(Map.Entry<?, ?> entry) {
      return (entry.getKey() == null ? 0 : entry.getKey().hashCode())
            ^ (entry.getValue() == null ? 0 : entry.getValue().hashCode());
   }
   
   public static String toString(Map.Entry<?, ?> entry) {
      return String.valueOf(entry.getKey()) + " => " + String.valueOf(entry.getValue());
   }
}
