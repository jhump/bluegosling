package com.apriori.apt.reflect;

import java.util.List;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;

//TODO: javadoc!!
public class Method extends AbstractExecutableMember {

   private Method(ExecutableElement element) {
      super(element);
   }
   
   public static Method forElement(ExecutableElement element) {
      if (element == null) {
         throw new NullPointerException();
      } else if (element.getKind() != ElementKind.METHOD) {
         throw new IllegalArgumentException("Invalid element kind. Expected METHOD; got " + element.getKind().name());
      }
      return new Method(element);
   }

   /**
    * Returns the method's list of type variables. This is the same as
    * {@link #getTypeVariables()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of type variables
    */
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<TypeVariable<Method>> getMethodTypeVariables() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<TypeVariable<Method>>
      return (List<TypeVariable<Method>>) ((List) getTypeVariables());
   }
   
   /**
    * Returns the method's list of parameters. This is the same as
    * {@link #getParameters()}, except that the return value has more
    * generic type information.
    * 
    * <p>(Sadly, though Java allows co-variant return types in overridden
    * methods, it does not allow co-variance where the return types'
    * erasures are the same and are co-variant only based on generic
    * parameters.)
    * 
    * @return the list of parameters
    */
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<Parameter<Method>> getMethodParameters() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<Parameter<Method>>
      return (List<Parameter<Method>>) ((List) getParameters());
   }
   
   public Class getReturnType() {
      return Class.forTypeMirror(asElement().getReturnType());
   }

   public Type getGenericReturnType() {
      return Types.forTypeMirror(asElement().getReturnType());
   }
   
   @Override 
   void appendReturnType(StringBuilder sb, boolean includeGenerics) {
      Type returnType = includeGenerics ? getGenericReturnType() : getReturnType();
      sb.append(returnType.toTypeString());
      sb.append(" ");
   }
}
