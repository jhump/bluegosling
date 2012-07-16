package com.apriori.apt.reflect;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

//TODO: javadoc!
public class Parameter<M extends ExecutableMember> extends AbstractAnnotatedElement {

   private Parameter(VariableElement element) {
      super(element);
   }
   
   public static Parameter<?> forElement(VariableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.PARAMETER) {
         throw new IllegalArgumentException("Invalid element kind. Expected PARAMETER; got " + element.getKind().name());
      }
      return new Parameter<ExecutableMember>(element);
   }

   @Override
   public VariableElement asElement() {
      return (VariableElement) super.asElement();
   }
   
   public String getName() {
      return asElement().getSimpleName().toString();
   }
   
   public Class getType() {
      return Class.forTypeMirror(asElement().asType());
   }
   
   public Type getGenericType() {
      return Types.forTypeMirror(asElement().asType());
   }
   
   public M getEnclosingMember() {
      @SuppressWarnings("unchecked")
      M ret = (M) ReflectionVisitors.EXECUTABLE_MEMBER_VISITOR.visit(asElement().getEnclosingElement());
      if (ret == null) {
         throw new AssertionError("Unable to determine enclosing member for parameter");
      }
      return ret;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof Parameter) {
         Parameter<?> other = (Parameter<?>) o;
         return getEnclosingMember().equals(other.getEnclosingMember())
               && getName().equals(other.getName())
               && getGenericType().equals(other.getGenericType());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * (29 * getEnclosingMember().hashCode() + getName().hashCode())
            ^ getGenericType().hashCode();
   }

   @Override
   public String toString() {
      return toString(false);
   }
   
   public String toGenericString() {
      return toString(true);
   }
   
   private String toString(boolean includeGenerics) {
      Type type = includeGenerics ? getGenericType() : getType();
      return type.toTypeString() + " " + getName();
   }
}
