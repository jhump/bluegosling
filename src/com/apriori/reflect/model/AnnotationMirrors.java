package com.apriori.reflect.model;

import static java.util.Objects.requireNonNull;

import com.apriori.reflect.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
   
   public static AnnotationMirrors fromCoreReflection() {
      return CORE_REFLECTION_INSTANCE;
   }

   private final Elements elementUtils;
   private final Types typeUtils;
   
   private AnnotationMirrors(Elements elementUtils, Types typeUtils) {
      this.elementUtils = elementUtils;
      this.typeUtils = typeUtils;
   }
   
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
      } else if (o instanceof Enum) {
         return getEnumAnnotationValue((Enum<?>) o);
      } else if (o instanceof Annotation) {
         return getAnnotationAsValue((Annotation) o);
      } else if (o.getClass().isArray()) {
         return getArrayAnnotationValue(o);
      } else {
         throw new IllegalArgumentException(
               String.valueOf(o) + " is not a valid value for an annotation field");
      }
   }
   
   public AnnotationValue getBooleanAnnotationValue(boolean b) {
      return new AnnotationValue() {
         @Override
         public Object getValue() {
            return b;
         }
         
         @Override
         public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
            return v.visitBoolean(b, p);
         }
      };
   }

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

   public AnnotationValue getArrayAnnotationValue(Object a) {
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

   public AnnotationValue getArrayAnnotationValue(Object[] a) {
      List<AnnotationValue> values = new ArrayList<>(a.length);
      for (Object o : a) {
         values.add(getAnnotationValue(o));
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
