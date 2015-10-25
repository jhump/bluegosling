package com.apriori.reflect;

import com.apriori.vars.VariableInt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An enumeration of elements. In many ways, this is like a {@code Class<Enum>} except that it can
 * be used for enumerations defined at runtime instead of at compile-time. In most other ways, this
 * is like an immutable set of elements, but with the ability to query members by name or by ordinal
 * position and to impose an ordering over members based on their ordinal position.
 *
 * @param <T> the type of the enum values
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public abstract class EnumType<T> implements Comparator<T> {
   
   /**
    * Represents a single element in an enumeration. This is a wrapper around a single enum value
    * and provides accessor methods for the element's name and ordinal position.
    *
    * @param <T> the type of an element's value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public interface EnumElement<T> extends Supplier<T>, Comparable<EnumElement<T>> {
      /**
       * Returns the enum element's name
       *
       * @return the enum element's name
       */
      String name();
      
      /**
       * Returns the enum element's ordinal position. Zero is the position of the first element
       * defined in the enum.
       *
       * @return the enum element's ordinal position
       */
      int ordinal();
      
      /**
       * Returns the enum type to which this element belongs.
       *
       * @return the enum type to which this element belongs
       */
      EnumType<T> enumType();
      
      /**
       * Returns the actual value of the enum element.
       * 
       * @return the actual value of the enum element
       */
      @Override T get();
      
      /**
       * Compares this element to another. The two elements are <em>not</em> comparable if they
       * belong to different enum types, and an {@link IllegalArgumentException} is thrown in this
       * case. Otherwise, the ordering of elements is based on their ordinal position, so an
       * element with a lower ordinal position is "less than" an element with a higher one.
       * 
       * @return {@inheritDoc}
       * @throws IllegalArgumentException if the given object is not a member of the same enum
       *       type as this object
       */
      @Override
      default int compareTo(EnumElement<T> o) {
         if (!enumType().equals(o.enumType())) {
            throw new IllegalArgumentException("Cannot compare elements of different enum types");
         }
         return Integer.compare(ordinal(), o.ordinal());
      }
      
      // TODO: doc
      @Override boolean equals(Object o);
      @Override int hashCode();
   }
   
   EnumType() {
   }
   
   /**
    * Returns the name of the enum element with the given value.
    *
    * @param value the value of an enum element
    * @return the name of the enum element
    * @throws IllegalArgumentException if the given value is not a member of this enum type
    */
   public abstract String name(T value);
   
   /**
    * Returns the ordinal position of the enum element with the given value. Zero is the oridinal
    * position of the first element of the enum.
    *
    * @param value the value of an enum element
    * @return the ordinal position of the enum element
    * @throws IllegalArgumentException if the given value is not a member of this enum type
    */
   public abstract int ordinal(T value);
   
   /**
    * Returns the enum element with the given name.
    *
    * @param name a name
    * @return the enum element with the given name
    * @throws IllegalArgumentException if this enum type has no element with the given name
    */
   public abstract EnumElement<T> valueOf(String name);
   
   /**
    * Returns the list of enum elements in this enum type, in order.
    *
    * @return the list of enum elements
    */
   public abstract List<EnumElement<T>> values();
   
   /**
    * Returns a view of the enum elements' values as a set.
    *
    * @return a view of the elements of this enum type as a set
    */
   public abstract Set<T> asSet();
   
   /*
    * If we had asm as a dep, we could also implement this one:
    */
//   public abstract Class<? extends Enum<?>> asEnum();

   // TODO: doc

   @Override
   public int hashCode() {
      return values().hashCode();
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof EnumType) {
         EnumType<?> other = (EnumType<?>) o;
         return this == other || values().equals(other.values());
      }
      return false;
   }
   
   @Override
   public String toString() {
      return values().stream()
            .map(Object::toString)
            .collect(Collectors.joining(", ", "{ ", " }"));
   }
   
   /**
    * Create an {@link EnumType} that represent an actual {@linkplain Enum enum class}. The elements
    * of the returned enum type correspond to enum constants for the given class. The value of an
    * element in the returned enum type is an enum constant.
    *
    * @param enumClass an enum class
    * @return an {@link EnumType} that represents all of the constants of the given enum
    */
   public static <T extends Enum<T>> EnumType<T> forEnum(Class<T> enumClass) {
      return new EnumWrapper<>(enumClass);
   }   
   
   /**
    * Returns a new builder, for defining the elements of an enum type.
    *
    * @return a new builder
    */
   public static <T> Builder<T> newBuilder() {
      return new Builder<>();
   }
   
   /**
    * A builder for constructing an {@link EnumType}.
    *
    * @param <T> the type of the enum values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Builder<T> {
      private final Map<String, T> elements = new LinkedHashMap<>();
      private final Set<T> values = new HashSet<>();
      
      Builder() {
      }
      
      /**
       * Adds an enum value with the given name. The first value added will have ordinal position
       * zero. Additional values have increasing ordinal positions.
       *
       * @param name the name of the element
       * @param value the value of the element
       * @return {@code this}, for method chaining
       * @throws IllegalStateException if an element with the given name or one with the given value
       *       has already been added
       */
      public Builder<T> add(String name, T value) {
         if (elements.containsKey(name)) {
            throw new IllegalStateException("Enum type already contains an element named " + name);
         }
         if (values.contains(value)) {
            throw new IllegalStateException("Enum type already contains the value " + value);
         }
         boolean added = values.add(value);
         assert added;
         T old = elements.put(name, value);
         assert old == null;
         return this;
      }
      
      /**
       * Builds a new enum type that consists of the elements that have been
       * {@linkplain #add(String, Object) added} to this builder.
       *
       * @return a new enum type
       * @throws IllegalStateException if no elements have been added.
       */
      public EnumType<T> build() {
         if (elements.isEmpty()) {
            throw new IllegalStateException("EnumType must have at least one element");
         }
         return new BuiltEnumType<>(elements);
      }
   }

   /**
    * An {@link EnumType} whose elements are defined using an {@link EnumType.Builder}.
    *
    * @param <T> the type of the enum values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class BuiltEnumType<T> extends EnumType<T> {

      private final List<EnumElement<T>> elements;
      private final Map<String, BuiltEnumElement<T>> elementsByName;
      private final Map<T, BuiltEnumElement<T>> elementsByValue;
      
      BuiltEnumType(Map<String, T> elements) {
         int len = elements.size();
         @SuppressWarnings("unchecked")
         BuiltEnumElement<T> array[] = new BuiltEnumElement[len];
         this.elementsByName = new LinkedHashMap<>(len * 4 / 3);
         this.elementsByValue = new LinkedHashMap<>(len * 4 / 3);
         VariableInt i = new VariableInt();
         elements.forEach((k, v) -> {
            int ordinal = i.getAndIncrement();
            BuiltEnumElement<T> element = new BuiltEnumElement<>(this, k, ordinal, v);
            array[ordinal] = element;
            elementsByName.put(element.name, element);
            elementsByValue.put(element.value, element);
         });
         this.elements = Collections.unmodifiableList(Arrays.asList(array));
      }
      
      private BuiltEnumElement<T> element(T value) {
         BuiltEnumElement<T> e = elementsByValue.get(value);
         if (e == null) {
            throw new IllegalArgumentException(
                  "Item " + value + " not a member of enum type " + this);
         }
         return e;
      }

      @Override
      public String name(T value) {
         return element(value).name;
      }
      
      @Override
      public int ordinal(T value) {
         return element(value).ordinal;
      }
      
      @Override
      public EnumElement<T> valueOf(String name) {
         EnumElement<T> e = elementsByName.get(name);
         if (e == null) {
            throw new IllegalArgumentException("No element named " + name + " in enum type " + this);
         }
         return e;
      }
      
      @Override
      public List<EnumElement<T>> values() {
         return elements;
      }
      
      @Override
      public Set<T> asSet() {
         return Collections.unmodifiableSet(elementsByValue.keySet());
      }
      
      @Override
      public int compare(T o1, T o2) {
         return Integer.compare(element(o1).ordinal, element(o2).ordinal);
      }
   }

   /**
    * An {@link EnumElement} that represents a value added to an enum type view an
    * {@link EnumType.Builder}.
    *
    * @param <T> the type of an element's value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class BuiltEnumElement<T> implements EnumElement<T> {
      final EnumType<T> enumType;
      final String name;
      final int ordinal;
      final T value;
      
      BuiltEnumElement(EnumType<T> enumType, String name, int ordinal, T value) {
         this.enumType = enumType;
         this.name = name;
         this.ordinal = ordinal;
         this.value = value;
      }
      
      @Override
      public String name() {
         return name;
      }
      
      @Override
      public int ordinal() {
         return ordinal;
      }

      @Override
      public EnumType<T> enumType() {
         return enumType;
      }
      
      @Override
      public T get() {
         return value;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof EnumElement) {
            EnumElement<?> other = (EnumElement<?>) o;
            boolean equal = enumType.equals(other.enumType())
                  && ordinal == other.ordinal();
            if (equal) {
               assert name.equals(other.name());
               assert value.equals(other.get());
            }
            return equal;
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return name.hashCode() ^ ordinal;
      }
      
      @Override
      public String toString() {
         return name + " => " + value;
      }
   }
   
   /**
    * An {@link EnumType} that is merely a wrapper around the values of an {@linkplain Enum enum}.
    *
    * @param <T> the type of the enum values
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class EnumWrapper<T extends Enum<T>> extends EnumType<T> {
      private final Class<T> enumClass;
      private final List<EnumElement<T>> elements;
      private final Set<T> asSet;
      
      EnumWrapper(Class<T> enumClass) {
         this.enumClass = enumClass;
         T vals[] = Enums.values(enumClass);
         List<EnumElement<T>> e = new ArrayList<>(vals.length);
         Set<T> s = new LinkedHashSet<>(vals.length * 4 / 3);
         for (T t : vals) {
            e.add(new EnumConstantWrapper<>(this, t));
            s.add(t);
         }
         this.elements = Collections.unmodifiableList(e);
         this.asSet = Collections.unmodifiableSet(s);
      }
      
      @Override
      public int compare(T o1, T o2) {
         return o1.compareTo(o2);
      }

      @Override
      public String name(T value) {
         return value.name();
      }

      @Override
      public int ordinal(T value) {
         return value.ordinal();
      }

      @Override
      public EnumElement<T> valueOf(String name) {
         return new EnumConstantWrapper<>(this, Enum.valueOf(enumClass, name));
      }

      @Override
      public List<EnumElement<T>> values() {
         return elements;
      }

      @Override
      public Set<T> asSet() {
         return asSet;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof EnumWrapper) {
            EnumWrapper<?> other = (EnumWrapper<?>) o;
            return enumClass.equals(other.enumClass);
         }
         return super.equals(o);
      }
   }
   
   /**
    * An {@link EnumElement} that is merely a wrapper around a single {@linkplain Enum enum
    * constant}.
    *
    * @param <T> the type of an element's value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class EnumConstantWrapper<T extends Enum<T>> implements EnumElement<T> {
      private final EnumType<T> enumType;
      private final T t;

      EnumConstantWrapper(EnumType<T> enumType, T t) {
         this.enumType = enumType;
         this.t = t;
      }
      
      @Override
      public T get() {
         return t;
      }

      @Override
      public int compareTo(EnumElement<T> o) {
         if (!enumType().equals(o.enumType())) {
            throw new IllegalArgumentException("Cannot compare elements of different enum types");
         }
         return t.compareTo(o.get());
      }

      @Override
      public String name() {
         return t.name();
      }

      @Override
      public int ordinal() {
         return t.ordinal();
      }

      @Override
      public EnumType<T> enumType() {
         return enumType;
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof EnumConstantWrapper) {
            EnumConstantWrapper<?> other = (EnumConstantWrapper<?>) o;
            return t == other.t;
         } else if (o instanceof EnumElement) {
            EnumElement<?> other = (EnumElement<?>) o;
            boolean equal = enumType.equals(other.enumType())
                  && t.ordinal() == other.ordinal();
            if (equal) {
               assert t.name().equals(other.name());
               assert t.equals(other.get());
            }
            return equal;
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return t.name().hashCode() ^ t.ordinal();
      }
      
      @Override
      public String toString() {
         return t.name();
      }
   }
}
