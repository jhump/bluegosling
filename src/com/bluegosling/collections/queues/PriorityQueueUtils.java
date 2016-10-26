package com.bluegosling.collections.queues;

/**
 * Utility methods to help implement {@link PriorityQueueUtils}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
final class PriorityQueueUtils {
   private PriorityQueueUtils() {
   }

   /**
    * Determines if the given queue entry and other object are equal. They are equal if the other
    * object is also a queue entry and has the same element and priority.
    *
    * @param entry a priority queue entry
    * @param o another object
    * @return true if the specified objects are equal; false otherwise
    * 
    * @see PriorityQueue.Entry#equals(Object)
    */
   public static boolean equals(PriorityQueue.Entry<?, ?> entry, Object o) {
      if (!(o instanceof PriorityQueue.Entry)) {
         return false;
      }
      if (o == entry) {
         return true;
      }
      PriorityQueue.Entry<?, ?> other = (PriorityQueue.Entry<?, ?>) o;
      Object priority = entry.getPriority();
      Object otherPriority = other.getPriority();
      if ((priority == entry || priority == other) && otherPriority != entry && otherPriority != other) {
         return false;
      }
      Object element = entry.getElement();
      Object otherElement = other.getElement();
      if ((element == entry || element == other) && otherElement != entry && otherElement != other) {
         return false;
      }
      return (priority == null ? otherPriority == null : priority.equals(otherPriority))
            && (element == null ? otherElement == null : element.equals(otherElement));
   }

   /**
    * Computes the hash code for a queue entry.
    *
    * @param entry a queue entry
    * @return the hash code for the entry
    * 
    * @see PriorityQueue.Entry#hashCode()
    */
   public static int hashCode(PriorityQueue.Entry<?, ?> entry) {
      return safeHashCode(entry.getPriority(), entry) ^ safeHashCode(entry.getElement(), entry);
   }

   /**
    * Produces a string representation of the given queue entry. The string representation will be
    * in the form:<pre>
    * Element @ Priority
    * </pre>
    *
    * @param entry a queue entry
    * @return a string representation of the given entry
    */
   public static String toString(PriorityQueue.Entry<?, ?> entry) {
      StringBuilder sb = new StringBuilder();
      safeToString(sb, entry.getElement(), entry, "( this entry )");
      sb.append(" @ ");
      safeToString(sb, entry.getPriority(), entry, "( this entry )");
      return sb.toString();
   }
   
   private static int safeHashCode(Object o, Object current) {
      return o == current ? System.identityHashCode(current)
            : (o == null ? 0 : o.hashCode());
   }

   private static void safeToString(StringBuilder sb, Object o, Object current, String substitute) {
      if (o == current) {
         sb.append(substitute);
      } else {
         sb.append(o);
      }
   }
}
