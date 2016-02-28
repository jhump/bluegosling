package com.bluegosling.collections;

import java.util.Map;

// TODO: javadoc
final class MapUtils {
   /** Prevents instantiation. */
   private MapUtils() {
   }

   public static boolean equals(Map<?, ?> map, Object o) {
      if (!(o instanceof Map)) {
         return false;
      }
      if (map == o) {
         return true;
      }
      Map<?, ?> other = (Map<?, ?>) o;
      if (map.size() != other.size()) {
         return false;
      }
      boolean containsItselfAsKey = false;
      Object selfValue = null;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
         Object key = entry.getKey();
         Object value = entry.getValue();
         if (key == map || key == other) {
            containsItselfAsKey = true;
            selfValue = value;
         } else {
            Object otherValue = other.get(key);
            if (value == null && otherValue == null) {
               // annoying to have to do a double look-up, but...
               if (!other.containsKey(key)) {
                  return false;
               }
            } else if (value == null || otherValue == null) {
               return false;
            } else if (value == map || value == other) {
               if (otherValue != map && otherValue != other) {
                  return false;
               }
            } else if (!value.equals(otherValue)) {
               return false;
            }
         }
      }
      if (containsItselfAsKey) {
         // verify other map also contains itself (or this map) and has proper associated value
         for (Map.Entry<?, ?> entry : other.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key == map || key == other) {
               // check that mapped value matches
               if (value == null || selfValue == null) {
                  return value == selfValue;
               } else if (value == map || value == other) {
                  return selfValue == map || selfValue == other;
               } else {
                  return value.equals(selfValue);
               }
            }
         }
         return false;
      }
      return true;
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

   public static boolean equals(ImmutableMap<?, ?> map, Object o) {
      if (!(o instanceof ImmutableMap)) {
         return false;
      }
      if (map == o) {
         return true;
      }
      ImmutableMap<?, ?> other = (ImmutableMap<?, ?>) o;
      if (map.size() != other.size()) {
         return false;
      }
      boolean containsItselfAsKey = false;
      Object selfValue = null;
      for (ImmutableMap.Entry<?, ?> entry : map) {
         Object key = entry.key();
         Object value = entry.value();
         if (key == map || key == other) {
            containsItselfAsKey = true;
            selfValue = value;
         } else {
            Object otherValue = other.get(key);
            if (value == null && otherValue == null) {
               // annoying to have to do a double look-up, but...
               if (!other.containsKey(key)) {
                  return false;
               }
            } else if (value == null || otherValue == null) {
               return false;
            } else if (value == map || value == other) {
               if (otherValue != map && otherValue != other) {
                  return false;
               }
            } else if (!value.equals(otherValue)) {
               return false;
            }
         }
      }
      if (containsItselfAsKey) {
         // verify other map also contains itself (or this map) and has proper associated value
         for (ImmutableMap.Entry<?, ?> entry : other) {
            Object key = entry.key();
            Object value = entry.value();
            if (key == map || key == other) {
               // check that mapped value matches
               if (value == null || selfValue == null) {
                  return value == selfValue;
               } else if (value == map || value == other) {
                  return selfValue == map || selfValue == other;
               } else {
                  return value.equals(selfValue);
               }
            }
         }
         return false;
      }
      return true;
   }

   public static int hashCode(ImmutableMap<?, ?> map) {
      int ret = 0;
      for (ImmutableMap.Entry<?, ?> entry : map) {
         if (entry.key() == map || entry.value() == map) {
            ret += safeHashCode(entry.key(), entry, map)
                  ^ safeHashCode(entry.value(), entry, map);
         } else {
            ret += entry.hashCode();
         }
      }
      return ret;
   }
   
   public static <K, V> String toString(ImmutableMap<K, V> map) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      for (ImmutableMap.Entry<K, V> entry : map) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(" ");
         if (entry.key() == map || entry.value() == map) {
            // don't overflow stack if map contains itself
            safeToString(sb, entry.key(), entry, "( this entry )", map, "( this map )");
            sb.append(" => ");
            safeToString(sb, entry.value(), entry, "( this entry )", map, "( this map )");
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
      Object key = entry.getKey();
      Object otherKey = other.getKey();
      if ((key == entry || key == other) && otherKey != entry && otherKey != other) {
         return false;
      }
      Object value = entry.getValue();
      Object otherValue = other.getValue();
      if ((value == entry || value == other) && otherValue != entry && otherValue != other) {
         return false;
      }
      return (key == null ? otherKey == null : key.equals(otherKey))
            && (value == null ? otherValue == null : value.equals(otherValue));
   }

   public static int hashCode(Map.Entry<?, ?> entry) {
      return safeHashCode(entry.getKey(), entry) ^ safeHashCode(entry.getValue(), entry);
   }

   public static String toString(Map.Entry<?, ?> entry) {
      StringBuilder sb = new StringBuilder();
      safeToString(sb, entry.getKey(), entry, "( this entry )");
      sb.append(" => ");
      safeToString(sb, entry.getValue(), entry, "( this entry )");
      return sb.toString();
   }
   
   public static boolean equals(ImmutableMap.Entry<?, ?> entry, Object o) {
      if (!(o instanceof ImmutableMap.Entry)) {
         return false;
      }
      if (o == entry) {
         return true;
      }
      ImmutableMap.Entry<?, ?> other = (ImmutableMap.Entry<?, ?>) o;
      Object key = entry.key();
      Object otherKey = other.key();
      if ((key == entry || key == other) && otherKey != entry && otherKey != other) {
         return false;
      }
      Object value = entry.value();
      Object otherValue = other.value();
      if ((value == entry || value == other) && otherValue != entry && otherValue != other) {
         return false;
      }
      return (key == null ? otherKey == null : key.equals(otherKey))
            && (value == null ? otherValue == null : value.equals(otherValue));
   }

   public static int hashCode(ImmutableMap.Entry<?, ?> entry) {
      return safeHashCode(entry.key(), entry) ^ safeHashCode(entry.value(), entry);
   }

   public static String toString(ImmutableMap.Entry<?, ?> entry) {
      StringBuilder sb = new StringBuilder();
      safeToString(sb, entry.key(), entry, "( this entry )");
      sb.append(" => ");
      safeToString(sb, entry.value(), entry, "( this entry )");
      return sb.toString();
   }
   
   private static int safeHashCode(Object o, Object current1, Object current2) {
      return (o == current1 || o == current2) ? System.identityHashCode(o)
            : (o == null ? 0 : o.hashCode());
   }

   private static int safeHashCode(Object o, Object current) {
      return o == current ? System.identityHashCode(current)
            : (o == null ? 0 : o.hashCode());
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
}
