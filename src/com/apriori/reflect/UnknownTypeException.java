package com.apriori.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * An exception that may be thrown when an unsupported sub-type of {@link Type} is encountered. The
 * "known" sub-types are {@link Class}, {@link ParameterizedType}, {@link GenericArrayType},
 * {@link TypeVariable}, and {@link WildcardType}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class UnknownTypeException extends RuntimeException {
   
   private static final long serialVersionUID = -8130625109889432666L;

   public UnknownTypeException(Type t) {
      super("Unknown type: " + t.getTypeName());
   }
}
