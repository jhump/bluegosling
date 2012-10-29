package com.apriori.apt.reflect;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;

/**
 * A representation of a parameter to an executable member, like a constructor or method. Unlike
 * many of the other types in this package, this one has no analog in the {@code java.lang.reflect}
 * package since parameter details aren't stored in class files and thus aren't reified for the
 * benefit of runtime reflection. The {@code java.lang.reflect} APIs do have ways to get at
 * parameter details (via {@link java.lang.reflect.Method java.lang.reflect.Method} and
 * {@link java.lang.reflect.Constructor java.lang.reflect.Constructor} classes), but they are
 * lacking one key attribute of a parameter that is available in Java source but not in runtime
 * types: the parameter {@linkplain #getName() name}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <M> the type of executable member (constructor or method) to which the parameter belongs
 */
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
   
   /**
    * Returns the parameter name.
    * 
    * @return the name of the parameter
    */
   public String getName() {
      return asElement().getSimpleName().toString();
   }
   
   /**
    * Returns the type of this parameter. This is a raw type with generic type information erased.
    * For full generic type information, use {@link #getGenericType()}.
    * 
    * @return the type of the parameter
    */
   public Class getType() {
      return Class.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns the type of this parameter. This includes all generic type information
    * that was specified in the source declaration of the type.
    * 
    * @return the type of the parameter
    */
   public Type getGenericType() {
      return Types.forTypeMirror(asElement().asType());
   }
   
   /**
    * Returns the constructor or method to which this parameter belongs.
    * 
    * @return the executable member to which the parameter belongs
    */
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
   
   /**
    * Returns a string representation of this parameter that includes generic type information. This
    * is similar to {@link #toString()} except that it includes generic type information instead of
    * just indicating erased types.
    * 
    * @return a string representation of the field that includes generic type information
    */
   public String toGenericString() {
      return toString(true);
   }
   
   private String toString(boolean includeGenerics) {
      Type type = includeGenerics ? getGenericType() : getType();
      return type.toTypeString() + " " + getName();
   }
}
