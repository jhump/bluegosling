package com.apriori.apt.reflect;

import javax.lang.model.type.TypeMirror;


/**
 * A interface that represents a generic type. Using implementors of this interface,
 * information on generic type variables and even reifiable type parameters can be
 * programmatically determined for processed type elements.
 * 
 * <p>This mirrors the {@link java.lang.reflect.Type} interface, but is for
 * representing types in source form for annotation processing vs. types in compiled
 * form at runtime.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Types
 */
//TODO: javadoc!
public interface Type {
   
   interface Visitor<R, P> {
      R visitClass(Class clazz, P p);
      R visitGenericArrayType(GenericArrayType arrayType, P p);
      R visitParameterizedType(ParameterizedType parameterizedType, P p);
      R visitTypeVariable(TypeVariable<?> typeVariable, P p);
      R visitWildcardType(WildcardType wildcardType, P p);
   }
   
   enum Kind {
      CLASS,
      GENERIC_ARRAY_TYPE,
      PARAMETERIZED_TYPE,
      TYPE_VARIABLE,
      WILDCARD_TYPE
   }
   
   <R, P> R accept(Visitor<R, P> visitor, P p);
   
   Kind getTypeKind();

   TypeMirror asTypeMirror();
   
   String toTypeString();
}
