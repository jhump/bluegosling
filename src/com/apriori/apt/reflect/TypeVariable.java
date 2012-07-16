package com.apriori.apt.reflect;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

//TODO: javadoc!
public class TypeVariable<D extends GenericDeclaration> implements Type {
   private final TypeParameterElement element;
   
   private TypeVariable(TypeParameterElement element) {
      if (element == null) {
         throw new NullPointerException();
      }
      this.element = element;
   }
   
   public static TypeVariable<?> forElement(TypeParameterElement element) {
      return new TypeVariable<GenericDeclaration>(element);
   }
   
   @Override
   public Type.Kind getTypeKind() {
      return Kind.TYPE_VARIABLE;
   }
   
   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitTypeVariable(this,  p);
   }
   
   @Override
   public TypeMirror asTypeMirror() {
      return element.asType();
   }

   public List<? extends Type> getBounds() {
      List<? extends TypeMirror> elementBounds = element.getBounds();
      int size = elementBounds.size();
      List<Type> bounds = new ArrayList<Type>(size == 0 ? 1 : size);
      for (TypeMirror mirror : elementBounds) {
         bounds.add(Types.forTypeMirror(mirror));
      }
      if (bounds.isEmpty()) {
         bounds.add(Class.forJavaLangObject());
      }
      return bounds;
   }
   
   public D getGenericDeclaration() {
      @SuppressWarnings("unchecked")
      D decl = (D) ReflectionVisitors.GENERIC_DECLARATION_VISITOR.visit(element.getGenericElement());
      if (decl == null) {
         throw new AssertionError("Unable to determine generic declaration for type variable");
      }
      return decl;
   }
   
   public String getName() {
      return element.getSimpleName().toString();
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof TypeVariable) {
         TypeVariable<?> other = (TypeVariable<?>) o;
         return getName().equals(other.getName())
               && getBounds().equals(other.getBounds())
               && getGenericDeclaration().equals(other.getGenericDeclaration());
      }
      return false;
   }

   @Override
   public int hashCode() {
      return 31 * (31 * getName().hashCode() + getBounds().hashCode())
            + getGenericDeclaration().hashCode();
   }

   @Override
   public String toString() {
      return toTypeString();
   }

   @Override
   public String toTypeString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getName());
      if (!element.getBounds().isEmpty()) {
         boolean first = true;
         for (Type type : getBounds()) {
            if (first) {
               first = false;
            } else {
               sb.append("&");
            }
            sb.append(type.toTypeString());
         }
      }
      return sb.toString();
   }
   
   static void appendTypeParameters(StringBuilder sb, List<TypeVariable<?>> typeVariables) {
      if (!typeVariables.isEmpty()) {
         sb.append("<");
         boolean first = true;
         for (TypeVariable<?> typeVariable : typeVariables) {
            if (first) {
               first = false;
            } else {
               sb.append(",");
            }
            sb.append(typeVariable.toTypeString());
         }
         sb.append(">");
      }
   }
}
