package com.apriori.reflect.model;

import static java.util.Objects.requireNonNull;

import com.apriori.reflect.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.AbstractAnnotationValueVisitor8;

/**
 * Utility methods for constructing instances of {@link AnnotationMirror} and
 * {@link AnnotationValue}.
 * 
 * @see #fromProcessingEnvironment(ProcessingEnvironment)
 * @see #fromCoreReflection()
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class AnnotationMirrors {
   
   static final AnnotationMirrors CORE_REFLECTION_INSTANCE =
         new AnnotationMirrors(CoreReflectionElements.INSTANCE, CoreReflectionTypes.INSTANCE); 

   /**
    * Returns an {@link AnnotationMirrors} utility class that is backed by an annotation processing
    * environment.
    *
    * @param env an annotation processing environment
    * @return an {@link AnnotationMirrors} utility class backed by the given environment
    */
   public static AnnotationMirrors fromProcessingEnvironment(ProcessingEnvironment env) {
      ProcessingEnvironmentElements elements;
      javax.lang.model.util.Elements base = env.getElementUtils();
      if (base instanceof ProcessingEnvironmentElements) {
         elements = (ProcessingEnvironmentElements) base;
      } else {
         elements = new ProcessingEnvironmentElements(env);
      }
      return new AnnotationMirrors(elements, elements.getTypeUtils());
   }
   
   /**
    * Returns an {@link AnnotationMirrors} utility class that is backed by core reflection.
    *
    * @return an {@link AnnotationMirrors} utility class backed by core reflection
    */
   public static AnnotationMirrors fromCoreReflection() {
      return CORE_REFLECTION_INSTANCE;
   }

   /**
    * The kind of an annotation value.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private enum ValueKind {
      BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE,
      STRING, TYPE, ENUM, ANNOTATION, ARRAY;
      
      /**
       * A visitor that returns the kind of a given annotation value.
       */
      static AnnotationValueVisitor<ValueKind, Void> VISITOR =
            new AbstractAnnotationValueVisitor8<ValueKind, Void>() {
               @Override
               public ValueKind visitBoolean(boolean b, Void p) {
                  return ValueKind.BOOLEAN;
               }

               @Override
               public ValueKind visitByte(byte b, Void p) {
                  return ValueKind.BYTE;
               }

               @Override
               public ValueKind visitChar(char c, Void p) {
                  return ValueKind.CHAR;
               }

               @Override
               public ValueKind visitDouble(double d, Void p) {
                  return ValueKind.DOUBLE;
               }

               @Override
               public ValueKind visitFloat(float f, Void p) {
                  return ValueKind.FLOAT;
               }

               @Override
               public ValueKind visitInt(int i, Void p) {
                  return ValueKind.INT;
               }

               @Override
               public ValueKind visitLong(long i, Void p) {
                  return ValueKind.LONG;
               }

               @Override
               public ValueKind visitShort(short s, Void p) {
                  return ValueKind.SHORT;
               }

               @Override
               public ValueKind visitString(String s, Void p) {
                  return ValueKind.STRING;
               }

               @Override
               public ValueKind visitType(TypeMirror t, Void p) {
                  return ValueKind.TYPE;
               }

               @Override
               public ValueKind visitEnumConstant(VariableElement c, Void p) {
                  return ValueKind.ENUM;
               }

               @Override
               public ValueKind visitAnnotation(AnnotationMirror a, Void p) {
                  return ValueKind.ANNOTATION;
               }

               @Override
               public ValueKind visitArray(List<? extends AnnotationValue> vals, Void p) {
                  return ValueKind.ARRAY;
               }
            };
   }
   
   private final Elements elementUtils;
   private final Types typeUtils;
   
   private AnnotationMirrors(Elements elementUtils, Types typeUtils) {
      this.elementUtils = elementUtils;
      this.typeUtils = typeUtils;
   }

   /**
    * Returns an annotation value for the given object. Only primitives (including boxed types),
    * strings, class tokens, enum constants, annotation instances, and arrays or lists thereof are
    * allowed. Arrays and lists cannot be heterogenous (for example, cannot have both booleans and
    * bytes) and cannot be greater than 1 dimension (e.g. no arrays of arrays). 
    *
    * @param o an object
    * @return an annotation value for the given object
    * @throws IllegalArgumentException if the given object cannot be represented as an annotation
    *       value
    */
   public AnnotationValue getAnnotationValue(Object o) {
      requireNonNull(o);
      if (o instanceof Boolean) {
         return getBooleanAnnotationValue((Boolean) o);
      } else if (o instanceof Byte) {
         return getByteAnnotationValue((Byte) o);
      } else if (o instanceof Short) {
         return getShortAnnotationValue((Short) o);
      } else if (o instanceof Character) {
         return getCharAnnotationValue((Character) o);
      } else if (o instanceof Integer) {
         return getIntAnnotationValue((Integer) o);
      } else if (o instanceof Long) {
         return getLongAnnotationValue((Long) o);
      } else if (o instanceof Float) {
         return getFloatAnnotationValue((Float) o);
      } else if (o instanceof Double) {
         return getDoubleAnnotationValue((Double) o);
      } else if (o instanceof String) {
         return getStringAnnotationValue((String) o);
      } else if (o instanceof Class) {
         return getTypeAnnotationValue((Class<?>) o);
      } else if (o instanceof Enum) {
         return getEnumAnnotationValue((Enum<?>) o);
      } else if (o instanceof Annotation) {
         return getAnnotationAsValue((Annotation) o);
      } else if (o.getClass().isArray()) {
         return getArrayAnnotationValue(o);
      } else if (o instanceof List) {
         return getArrayAnnotationValue((List<?>) o);
      } else {
         throw new IllegalArgumentException(
               String.valueOf(o) + " is not a valid value for an annotation field");
      }
   }
   
   /**
    * Returns an annotation value for the given boolean.
    *
    * @param b a boolean value
    * @return an annotation value for the given boolean
    */
   public AnnotationValue getBooleanAnnotationValue(boolean b) {
      return b ? BooleanAnnotationValue.TRUE : BooleanAnnotationValue.FALSE;
   }
   
   /**
    * Enumeration of the two possible annotation values for booleans.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private enum BooleanAnnotationValue implements AnnotationValue {
      TRUE(true), FALSE(false);
      
      private final boolean b;
      BooleanAnnotationValue(boolean b) {
         this.b = b;
      }

      @Override
      public Object getValue() {
         return b;
      }
      
      @Override
      public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
         return v.visitBoolean(b, p);
      }
   }

   /**
    * Returns an annotation value for the given byte.
    *
    * @param b a byte value
    * @return an annotation value for the given byte
    */
   public AnnotationValue getByteAnnotationValue(byte b) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return b;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitByte(b, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given short.
    *
    * @param sh a short value
    * @return an annotation value for the given short
    */
   public AnnotationValue getShortAnnotationValue(short sh) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return sh;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitShort(sh, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given character.
    *
    * @param ch a char value
    * @return an annotation value for the given character
    */
   public AnnotationValue getCharAnnotationValue(char ch) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return ch;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitChar(ch, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given integer.
    *
    * @param i an int value
    * @return an annotation value for the given integer
    */
   public AnnotationValue getIntAnnotationValue(int i) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return i;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitInt(i, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given long.
    *
    * @param l a long value
    * @return an annotation value for the given long
    */
   public AnnotationValue getLongAnnotationValue(long l) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return l;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitLong(l, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given float.
    *
    * @param f a float value
    * @return an annotation value for the given float
    */
   public AnnotationValue getFloatAnnotationValue(float f) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return f;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitFloat(f, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given double.
    *
    * @param d a double value
    * @return an annotation value for the given double
    */
   public AnnotationValue getDoubleAnnotationValue(double d) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return d;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitDouble(d, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given string.
    *
    * @param s a string value
    * @return an annotation value for the given string
    */
   public AnnotationValue getStringAnnotationValue(String s) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return s;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitString(s, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given array or list.
    *
    * @param a an array or list
    * @return an annotation value for the given object
    * @throws IllegalArgumentException if the given value is neither an array nor a list
    * @throws IllegalArgumentException if the given array or list contains other arrays or lists or
    *       if it is heterogenous (e.g. mixed types of contained values)
    */
   public AnnotationValue getArrayAnnotationValue(Object a) {
      if (a instanceof List) {
         return getArrayAnnotationValue((List<?>) a);
      }
      Class<?> aType = a.getClass();
      if (aType.isArray()) {
         throw new IllegalArgumentException("Given object " + a + " is not an array");
      }
      if (Object[].class.isAssignableFrom(aType)) {
         return getArrayAnnotationValue((Object[]) a);
      }
      Class<?> component = aType.getComponentType();
      assert component.isPrimitive() && component != void.class;
      int len = Array.getLength(a);
      List<AnnotationValue> values = new ArrayList<>(len);
      if (component == boolean.class) {
         for (int i = 0; i < len; i++) {
            values.add(getBooleanAnnotationValue(Array.getBoolean(a, i)));
         }
      } else if (component == byte.class) {
         for (int i = 0; i < len; i++) {
            values.add(getByteAnnotationValue(Array.getByte(a, i)));
         }
      } else if (component == short.class) {
         for (int i = 0; i < len; i++) {
            values.add(getShortAnnotationValue(Array.getShort(a, i)));
         }
      } else if (component == char.class) {
         for (int i = 0; i < len; i++) {
            values.add(getCharAnnotationValue(Array.getChar(a, i)));
         }
      } else if (component == int.class) {
         for (int i = 0; i < len; i++) {
            values.add(getIntAnnotationValue(Array.getInt(a, i)));
         }
      } else if (component == long.class) {
         for (int i = 0; i < len; i++) {
            values.add(getLongAnnotationValue(Array.getLong(a, i)));
         }
      } else if (component == float.class) {
         for (int i = 0; i < len; i++) {
            values.add(getFloatAnnotationValue(Array.getFloat(a, i)));
         }
      } else if (component == double.class) {
         for (int i = 0; i < len; i++) {
            values.add(getDoubleAnnotationValue(Array.getDouble(a, i)));
         }
      } else {
         throw new AssertionError(
               "Array component type is neither known primitive type nor a reference type: "
                     + component);
      }

      List<AnnotationValue> results = Collections.unmodifiableList(values);
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return results;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitArray(results, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given list.
    *
    * @param a a list
    * @return an annotation value for the given list
    * @throws IllegalArgumentException if the given list contains other arrays or lists or if it is
    *       heterogenous (e.g. mixed types of contained values)
    */
   public AnnotationValue getArrayAnnotationValue(List<?> a) {
      List<AnnotationValue> values = new ArrayList<>(a.size());
      ValueKind arrayKind = null;
      for (Object o : a) {
         AnnotationValue v = getAnnotationValue(o);
         ValueKind elementKind = ValueKind.VISITOR.visit(v);
         if (arrayKind == null) {
            if (elementKind == ValueKind.ARRAY) {
               throw new IllegalArgumentException("Array within array found. Only 1-dimensional "
                     + "arrays are supported as annotation values");
            }
            arrayKind = elementKind;
         } else if (elementKind != arrayKind) {
            throw new IllegalArgumentException("Array with multiple element kinds found: "
                  + arrayKind.name() + " and " + elementKind.name() + ". Only homogenous "
                  + "arrays are supported as annotation values");
         }
         values.add(v);
      }

      List<AnnotationValue> results = Collections.unmodifiableList(values);
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return results;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitArray(results, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given array.
    *
    * @param a an array
    * @return an annotation value for the given array
    * @throws IllegalArgumentException if the given array contains other arrays or lists or if it is
    *       heterogenous (e.g. mixed types of contained values)
    */
   public AnnotationValue getArrayAnnotationValue(Object[] a) {
      return getArrayAnnotationValue(Arrays.asList(a));
   }

   /**
    * Returns an annotation value for the given enum.
    *
    * @param en an enum
    * @return an annotation value for the given enum
    */
   public AnnotationValue getEnumAnnotationValue(Enum<?> en) {
      Field f;
      try {
         f = en.getDeclaringClass().getDeclaredField(en.name());
      } catch (NoSuchFieldException e) {
         throw new AssertionError(e);
      }
      VariableElement enumElement = elementUtils.getFieldElement(f);
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return enumElement;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitEnumConstant(enumElement, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given class token.
    *
    * @param cl a class token
    * @return an annotation value for the given type
    */
   public AnnotationValue getTypeAnnotationValue(Class<?> cl) {
      TypeMirror typeMirror = elementUtils.getTypeElement(cl).asType();
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return typeMirror;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitType(typeMirror, p);
         }
      };
   }

   /**
    * Returns an annotation value for the given annotation.
    *
    * @param a an annotation
    * @return an annotation value for the given annotation
    */
   public AnnotationValue getAnnotationAsValue(Annotation a) {
      AnnotationMirror mirror = getAnnotationAsMirror(a);
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return mirror;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitAnnotation(mirror, p);
         }
      };
   }

   /**
    * Returns an annotation mirror that represents the given annotation.
    *
    * @param a an annotation
    * @return an annotation mirror that represents the given annotation
    */
   public AnnotationMirror getAnnotationAsMirror(Annotation a) {
      DeclaredType type =
            typeUtils.getDeclaredType(elementUtils.getTypeElement(a.annotationType()));
      Map<String, Object> annotationValues = Annotations.toMap(a);
      Map<ExecutableElement, AnnotationValue> resultValues =
            new LinkedHashMap<>(annotationValues.size() * 4 / 3);
      for (Method m : a.annotationType().getDeclaredMethods()) {
         Object v = annotationValues.get(m.getName());
         assert v != null;
         resultValues.put(elementUtils.getExecutableElement(m), getAnnotationValue(v));
      }
      final Map<ExecutableElement, AnnotationValue> finalResults =
            Collections.unmodifiableMap(resultValues);
      return new AnnotationMirror() {
         @Override
         public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
            return finalResults;
         }
         
         @Override
         public DeclaredType getAnnotationType() {
            return type;
         }
      };
   }
}
