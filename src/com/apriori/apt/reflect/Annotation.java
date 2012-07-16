package com.apriori.apt.reflect;

import com.apriori.apt.ElementUtils;

import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

/**
 * An annotation in Java source code. It is sort of analogous to the actual
 * {@link java.lang.annotation.Annotation java.lang.annotation.Annotation}
 * interface, except that it represents source-level annotations during
 * annotation processing vs. annotations on actual runtime types. Unlike
 * {@code java.lang.annotation.Annotation}, this representation can be used
 * to inspect annotations whose retention policy is {@link RetentionPolicy#CLASS}
 * or {@link RetentionPolicy#SOURCE}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see java.lang.annotation.Annotation
 */
public class Annotation {
   
   private final AnnotationMirror mirror;
   
   private Annotation(AnnotationMirror mirror) {
      if (mirror == null) {
         throw new NullPointerException();
      }
      this.mirror = mirror;
   }
   
   /**
    * Constructs a new annotation based on the specified mirror.
    * 
    * @param mirror the annotation mirror
    * @return a new annotation
    */
   public static Annotation forAnnotationMirror(AnnotationMirror mirror) {
      return new Annotation(mirror);
   }

   /**
    * Returns the annotation's type.
    * 
    * @return the annotation type
    * 
    * @see java.lang.annotation.Annotation#annotationType()
    */
   public Class annotationType() {
      return Class.forTypeMirror(mirror.getAnnotationType());
   }
   
   private Map<? extends Method, ?> convertAttributes(
         Map<? extends ExecutableElement, ? extends AnnotationValue> values) {
      Map<Method, Object> ret = new HashMap<Method, Object>(values.size());
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
            : values.entrySet()) {
         Method method = Method.forElement(entry.getKey());
         Object value = ReflectionVisitors.ANNOTATION_VALUE_VISITOR.visit(entry.getValue());
         ret.put(method, value == null ? entry.getValue().getValue() : value);
      }
      return ret;
   }

   /**
    * Returns the annotation's attributes and values, include undeclared
    * attributes with the corresponding default value. The keys represent
    * the annotation's methods; the values represent the actual values.
    * The values will be instances of one of the following:
    * <ul>
    * <li>Strings and primitives.</li>
    * <li>{@code List<?>} for methods whose return type is an array. The values
    * in the list will be one the types in this list.</li>
    * <li>Other {@link Annotation}s for methods that return other annotation.</li>
    * </ul>
    * 
    * @return the annotation's attributes and values
    * 
    * @see #getDeclaredAnnotationAttributes()
    */
   public Map<? extends Method, ?> getAnnotationAttributes() {
      return convertAttributes(ElementUtils.get().getElementValuesWithDefaults(mirror));
   }
   
   /**
    * Returns the annotation's attributes and values. Only values explicitly
    * declared in the annotation are included. The keys represent
    * the annotation's methods; the values represent the actual values. See
    * {@link #getAnnotationAttributes()} for more details.
    * 
    * @return the annotation's declared attributes and values
    * 
    * @see #getAnnotationAttributes()
    */
   public Map<? extends Method, ?> getDeclaredAnnotationAttributes() {
      return convertAttributes(mirror.getElementValues());
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof Annotation) {
         Annotation other = (Annotation) o;
         return annotationType().equals(other.annotationType())
               && getAnnotationAttributes().equals(other.getAnnotationAttributes());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * annotationType().hashCode() + getAnnotationAttributes().hashCode();
   }

   private void attributeValueToString(StringBuilder sb, Object attributeValue) {
      if (attributeValue instanceof Class) {
         
      } else if (attributeValue instanceof String) {
         String s = (String) attributeValue;
         // wrap literal value with quotes
         sb.append("\"");
         // and escape embedded quotes and line-endings
         for (char ch : s.toCharArray()) {
            if (ch == '"') {
               sb.append("\\\"");
            } else if (ch == '\\') {
               sb.append("\\\\");
            } else if (ch == '\r') {
               sb.append("\\r");
            } else if (ch == '\n') {
               sb.append("\\n");
            } else if (ch == '\u0085') {
               sb.append("\\u0085");
            } else if (ch == '\u2028') {
               sb.append("\\u2028");
            } else if (ch == '\u2029') {
               sb.append("\\u2029");
            } else {
               sb.append(ch);
            }
         }
         sb.append("\"");
      } else if (attributeValue instanceof List) {
         sb.append("{");
         List<?> vals = (List<?>) attributeValue;
         boolean first = true;
         for (Object val : vals) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            attributeValueToString(sb,  val);
         }
         sb.append("}");
      } else if (attributeValue instanceof Field) {
         Field field = (Field) attributeValue;
         sb.append(field.getDeclaringClass().getSimpleName());
         sb.append(".");
         sb.append(field.getName());
      } else if (attributeValue instanceof Annotation) {
         Annotation a = (Annotation) attributeValue;
         a.toString(sb);
      } else {
         sb.append(attributeValue);
      }
   }
   
   private void toString(StringBuilder sb) {
      sb.append("@");
      sb.append(annotationType().getSimpleName());
      sb.append("(");
      boolean first = true;
      for (Map.Entry<? extends Method, ?> entry : getAnnotationAttributes().entrySet()) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(entry.getKey().getName());
         sb.append("=");
         attributeValueToString(sb, entry.getValue());
      }
      sb.append(")");
   }
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      toString(sb);
      return sb.toString();
   }
}
