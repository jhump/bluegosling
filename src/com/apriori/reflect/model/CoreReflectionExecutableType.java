package com.apriori.reflect.model;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

/**
 * An {@link ExecutableType} backed by a core reflection {@link Executable}. This represents either
 * a method or a constructor.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class CoreReflectionExecutableType extends CoreReflectionBase<Executable>
implements ExecutableType {
   
   CoreReflectionExecutableType(Executable executable) {
      super(executable);
   }
   
   @Override
   public TypeKind getKind() {
      return TypeKind.EXECUTABLE;
   }

   @Override
   public <R, P> R accept(TypeVisitor<R, P> v, P p) {
      return v.visitExecutable(this, p);
   }

   @Override
   public List<? extends TypeVariable> getTypeVariables() {
      java.lang.reflect.TypeVariable<?>[] typeVars = base().getTypeParameters();
      List<TypeVariable> result = new ArrayList<>(typeVars.length);
      for (java.lang.reflect.TypeVariable<?> typeVar : typeVars) {
         result.add(CoreReflectionTypes.INSTANCE.getTypeVariableMirror(typeVar));
      }
      return result;
   }

   @Override
   public TypeMirror getReturnType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedReturnType());
   }

   @Override
   public List<? extends TypeMirror> getParameterTypes() {
      AnnotatedType[] paramTypes = base().getAnnotatedParameterTypes();
      List<TypeMirror> result = new ArrayList<>(paramTypes.length);
      for (AnnotatedType type : paramTypes) {
         result.add(CoreReflectionTypes.INSTANCE.getTypeMirror(type));
      }
      return result;
   }

   @Override
   public TypeMirror getReceiverType() {
      return CoreReflectionTypes.INSTANCE.getTypeMirror(base().getAnnotatedReceiverType());
   }

   @Override
   public List<? extends TypeMirror> getThrownTypes() {
      AnnotatedType[] exTypes = base().getAnnotatedExceptionTypes();
      List<TypeMirror> result = new ArrayList<>(exTypes.length);
      for (AnnotatedType type : exTypes) {
         result.add(CoreReflectionTypes.INSTANCE.getTypeMirror(type));
      }
      return result;
   }
   
   @Override
   public String toString() {
      return base().toGenericString();
   }
}
