package com.apriori.reflect.model;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * Represents an {@link ExecutableType} with all generic type information erased. This is the result
 * of {@linkplain Types#erasure(TypeMirror) erasing} a {@link CoreReflectionExecutableType}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionErasedExecutableType extends CoreReflectionExecutableType {

   CoreReflectionErasedExecutableType(Executable executable) {
      super(executable);
   }

   @Override
   public List<? extends TypeVariable> getTypeVariables() {
      return Collections.emptyList();
   }

   @Override
   public TypeMirror getReturnType() {
      return CoreReflectionTypes.INSTANCE.erasure(super.getReturnType());
   }

   @Override
   public List<? extends TypeMirror> getParameterTypes() {
      List<? extends TypeMirror> params = super.getParameterTypes();
      List<TypeMirror> erasedParams = new ArrayList<>(params.size());
      for (TypeMirror p : params) {
         erasedParams.add(CoreReflectionTypes.INSTANCE.erasure(p));
      }
      return erasedParams;
   }

   @Override
   public TypeMirror getReceiverType() {
      return CoreReflectionTypes.INSTANCE.erasure(super.getReceiverType());
   }

   @Override
   public List<? extends TypeMirror> getThrownTypes() {
      List<? extends TypeMirror> thrown = super.getThrownTypes();
      List<TypeMirror> erasedThrown = new ArrayList<>(thrown.size());
      for (TypeMirror t : thrown) {
         erasedThrown.add(CoreReflectionTypes.INSTANCE.erasure(t));
      }
      return erasedThrown;
   }
   
   @Override
   public String toString() {
      return base().toString();
   }
}
