package com.apriori.graph;

import static java.util.Objects.requireNonNull;

import com.apriori.reflect.TypeRef;

import java.util.Objects;

/**
 * A strongly typed key for describing input values. Keys can consist solely of a type. But to
 * define keys for multiple values of the same type, an optional "qualifier" can be used. Two keys
 * represent the same value if both the types and the qualifiers are the equal.
 *
 * @param <T> the type of the value described by this key
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class Key<T> {
   
   /**
    * Returns an unqualified key for the given type.
    *
    * @param type a non-generic type
    * @return a key for the given type
    */
   public static <T> Key<T> of(Class<T> type) {
      return of(TypeRef.forClass(type));
   }

   /**
    * Returns an unqualified key for the given type.
    *
    * @param type a generic type
    * @return a key for the given type
    */
   public static <T> Key<T> of(TypeRef<T> type) {
      return of(type, null);
   }

   /**
    * Returns a qualified key for the given type.
    *
    * @param type a generic type
    * @param qualifier a qualifier
    * @return a key for the given type
    */
   public static <T> Key<T> of(Class<T> type, Object qualifier) {
      return of(TypeRef.forClass(type), qualifier);
   }
   
   /**
    * Returns a qualified key for the given type.
    *
    * @param type a generic type
    * @param qualifier a qualifier
    * @return a key for the given type
    */
   public static <T> Key<T> of(TypeRef<T> type, Object qualifier) {
      return new Key<T>(type, qualifier);
   }
   
   private final TypeRef<T> type;
   private final Object qualifier;
   
   /**
    * Constructs a new key.
    *
    * @param type the type
    * @param qualifier the qualifier
    */
   private Key(TypeRef<T> type, Object qualifier) {
      this.type = requireNonNull(type);
      this.qualifier = qualifier;
   }
   
   /**
    * Returns the qualifier for the key or {@code null} if this is an unqualified key.
    *
    * @return this key's qualifier
    */
   public Object qualifier() {
      return qualifier;
   }
   
   /**
    * Returns the type of the key.
    *
    * @return this key's type
    */
   public TypeRef<T> type() {
      return type;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof Key) {
         Key<?> other = (Key<?>) o;
         return Objects.equals(qualifier, other.qualifier)
               && type.equals(other.type);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return type.hashCode() ^ Objects.hashCode(qualifier);
   }
   
   @Override
   public String toString() {
      if (qualifier == null) {
         return type.toString();
      } else {
         return qualifier.toString() + " " + type.toString();
      }
   }
}
