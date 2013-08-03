package com.apriori.collections;

import java.util.Map;

// TODO: javadoc
final class MapUtils {
   /** Prevents instantiation. */
   private MapUtils() {
   }

   public static boolean equals(Map<?, ?> map, Object o) {
      // TODO: tighten this up so it won't overflow stack if map contains itself
      if (o instanceof Map) {
         Map<?, ?> other = (Map<?, ?>) o;
         return map.entrySet().equals(other.entrySet());
      }
      return false;
   }

   public static int hashCode(Map<?, ?> map) {
      int ret = 0;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
         if (entry.getKey() == map || entry.getValue() == map) {
            ret += safeHashCode(entry.getKey(), entry, map)
                  ^ safeHashCode(entry.getValue(), entry, map);
         } else {
            ret += entry.hashCode();
         }
      }
      return ret;
   }
   
   public static <K, V> String toString(Map<K, V> map) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      for (Map.Entry<K, V> entry : map.entrySet()) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(" ");
         if (entry.getKey() == map || entry.getValue() == map) {
            // don't overflow stack if map contains itself
            safeToString(sb, entry.getKey(), entry, "( this entry )", map, "( this map )");
            sb.append(" => ");
            safeToString(sb, entry.getValue(), entry, "( this entry )", map, "( this map )");
         } else {
            sb.append(entry);
         }
      }
      sb.append(" ]");
      return sb.toString();
   }

   public static boolean equals(Map.Entry<?, ?> entry, Object o) {
      if (!(o instanceof Map.Entry)) {
         return false;
      }
      if (o == entry) {
         return true;
      }
      Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
      return (entry.getKey() == null
                  ? other.getKey() == null : entry.getKey().equals(other.getKey()))
            && (entry.getValue() == null
                  ? other.getValue() == null : entry.getValue().equals(other.getValue()));
   }

   private static int safeHashCode(Object o, Object current1, Object current2) {
      return (o == current1 || o == current2) ? System.identityHashCode(o)
            : (o == null ? 0 : o.hashCode());
   }

   private static int safeHashCode(Object o, Object current) {
      return o == current ? System.identityHashCode(current)
            : (o == null ? 0 : o.hashCode());
   }

   public static int hashCode(Map.Entry<?, ?> entry) {
      return safeHashCode(entry.getKey(), entry) ^ safeHashCode(entry.getValue(), entry);
   }

   private static void safeToString(StringBuilder sb, Object o, Object current1, String substitute1,
         Object current2, String substitute2) {
      if (o == current1) {
         sb.append(substitute1);
      } else if (o == current2) {
         sb.append(substitute2);
      } else {
         sb.append(o);
      }
   }

   private static void safeToString(StringBuilder sb, Object o, Object current, String substitute) {
      if (o == current) {
         sb.append(substitute);
      } else {
         sb.append(o);
      }
   }
   
   public static String toString(Map.Entry<?, ?> entry) {
      StringBuilder sb = new StringBuilder();
      safeToString(sb, entry.getKey(), entry, "( this entry )");
      sb.append(" => ");
      safeToString(sb, entry.getValue(), entry, "( this entry )");
      return sb.toString();
   }
}
