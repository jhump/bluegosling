package com.bluegosling.reflect.model;

import static java.util.Objects.requireNonNull;

import com.bluegosling.reflect.AnnotatedTypes;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a method or constructor signature, including annotations. Technically, a signature
 * just includes the method's (or constructor's) name, type arguments, and parameters. However, this
 * class also includes details for the return type, optional receiver type (for non-static methods
 * and constructors of non-static nested classes), and thrown exception types.
 * 
 * <p>This class implements {@link AnnotatedElement} so usages can query for annotations that are
 * present on the method or constructor from which this signature was created.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
class ExecutableSignature implements AnnotatedElement {
   private static final TypeVariable<?>[] EMPTY_VARS = new TypeVariable<?>[0];
   
   /**
    * Returns a signature for the given {@link Executable}.
    *
    * @param ex an executable
    * @return an executable signature that matche the given executable
    */
   public static ExecutableSignature of(Executable ex) {
      return new ExecutableSignature(ex.getName(), ex, ex.getAnnotatedReceiverType(),
            ex.getAnnotatedReturnType(), ex.getAnnotatedParameterTypes(),
            ex.getAnnotatedExceptionTypes(), ex.getTypeParameters());
   }

   private final String name;
   private final AnnotatedElement annotationSource;
   private final AnnotatedType receiverType;
   private final AnnotatedType returnType;
   private final AnnotatedType[] paramTypes;
   private final AnnotatedType[] thrownTypes;
   private final TypeVariable<?>[] typeParameters;
   
   private ExecutableSignature(String name, AnnotatedElement annotationSource,
         AnnotatedType receiverType, AnnotatedType returnType, AnnotatedType[] paramTypes,
         AnnotatedType[] thrownTypes, TypeVariable<?>[] typeParameters) {
      this.name = requireNonNull(name);
      this.annotationSource = requireNonNull(annotationSource);
      this.receiverType = receiverType; // nullable
      this.returnType = requireNonNull(returnType);
      this.paramTypes = requireNonNull(paramTypes);
      this.thrownTypes = requireNonNull(thrownTypes);
      this.typeParameters = requireNonNull(typeParameters);
   }
   
   /**
    * Gets the name of the executable.
    *
    * @return the name
    */
   public String getName() {
      return name;
   }
   
   @Override
   public Annotation[] getDeclaredAnnotations() {
      return annotationSource.getDeclaredAnnotations();
   }
   
   @Override
   public Annotation[] getAnnotations() {
      return annotationSource.getAnnotations();
   }
   
   @Override
   public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
      return annotationSource.getAnnotation(annotationClass);
   }
   
   /**
    * Gets the annotated receiver type for this executable. A static method or a constructor for a
    * top-level class or static nested class will have no receiver type and thus return
    * {@code null}.
    *
    * @return the annotated receiver type or {@code null} if this executable has no receiver
    */
   public AnnotatedType getAnnotatedReceiverType() {
      return receiverType;
   }
   
   /**
    * Gets the annotated return type for this executable.
    *
    * @return the annotated return type
    */
   public AnnotatedType getAnnotatedReturnType() {
      return returnType;
   }

   /**
    * Gets the annotated parameter types for this executable. If the executable takes no parameters
    * then this returns an empty array.
    *
    * @return the annotated parameter types
    */
   public AnnotatedType[] getAnnotatedParameterTypes() {
      return paramTypes.clone();
   }

   /**
    * Gets the annotated exception types declared as thrown by this executable. If the executable
    * declares to thrown types then this returns an empty array.
    *
    * @return the annotated exception types
    */
   public AnnotatedType[] getAnnotatedExceptionTypes() {
      return thrownTypes.clone();
   }

   /**
    * Gets the type parameters declared by this executable. If the executable declares no type
    * parameters then this returns an empty array.
    *
    * @return the type parameters
    */
   public TypeVariable<?>[] getTypeParameters() {
      return typeParameters.clone();
   }
   
   /**
    * Computes the erasure of this executable signature. This applies <em>Type Erasure</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.6">JLS
    * 4.6</a>) to the signature and returns the resulting signature.
    *
    * @return the erasure of this executable signature
    */
   public ExecutableSignature erased() {
      AnnotatedType recv = AnnotatedTypes.getErasure(getAnnotatedReceiverType());
      AnnotatedType ret = AnnotatedTypes.getErasure(getAnnotatedReturnType());
      AnnotatedType[] params = getAnnotatedParameterTypes();
      for (int i = 0; i < params.length; i++) {
         params[i] = AnnotatedTypes.getErasure(params[i]);
      }
      AnnotatedType[] exs = getAnnotatedExceptionTypes();
      for (int i = 0; i < exs.length; i++) {
         exs[i] = AnnotatedTypes.getErasure(exs[i]);
      }
      return new ExecutableSignature(name, annotationSource, recv, ret, params, exs, EMPTY_VARS);
   }
   
   /**
    * Resolves all types in this executable signature using the given context type. Each referenced
    * type (e.g. return type, receiver type, parameter type, exception type) is replaced with the
    * {@linkplain AnnotatedTypes#resolveType(AnnotatedType, AnnotatedType) resolved type} and the
    * resulting new signature is returned.
    *
    * @param context the context which may provide resolved argument values for type variables
    * @return a new signature that is the same as this signature except that all types are resolved
    *       in the context of the given type
    */
   public ExecutableSignature resolveTypes(AnnotatedType context) {
      AnnotatedType recv = AnnotatedTypes.resolveType(context, getAnnotatedReceiverType());
      AnnotatedType ret = AnnotatedTypes.resolveType(context, getAnnotatedReturnType());
      AnnotatedType[] params = getAnnotatedParameterTypes();
      for (int i = 0; i < params.length; i++) {
         params[i] = AnnotatedTypes.resolveType(context, params[i]);
      }
      AnnotatedType[] exs = getAnnotatedExceptionTypes();
      for (int i = 0; i < exs.length; i++) {
         exs[i] = AnnotatedTypes.resolveType(context, exs[i]);
      }

      return new ExecutableSignature(name, annotationSource, recv, ret, params, exs,
            typeParameters);
   }
   
   /**
    * Returns true if the given executable has the same signature as this one. This is different
    * from {@link #equals(Object)}. This applies the logic described in <em>Method Signature</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2">JLS
    * 8.4.2</a>) to determine if the two signatures are the same.
    *
    * @param other another executable signature
    * @return true if the given executable has the same signature as {@code this}
    */
   public boolean isSameSignature(ExecutableSignature other) {
      if (typeParameters.length != other.typeParameters.length
            || paramTypes.length != other.paramTypes.length
            || !name.equals(other.name)) {
         return false;
      }
      
      Type[] parameters;
      if (typeParameters.length > 0) {
         Map<TypeVariable<?>, Type> adaptedTypeVariables =
               new HashMap<>(typeParameters.length * 4 / 3);
         for (int i = 0; i < typeParameters.length; i++) {
            adaptedTypeVariables.put(typeParameters[i], other.typeParameters[i]);
         }
         
         // adapt type bounds and then verify they match
         for (int i = 0; i < typeParameters.length; i++) {
            Type[] bounds = typeParameters[i].getBounds();
            Type[] otherBounds = other.typeParameters[i].getBounds();
            if (bounds.length != otherBounds.length) {
               return false;
            }
            for (int j = 0; j < bounds.length; j++) {
               Type b = com.bluegosling.reflect.Types.replaceTypeVariables(bounds[j],
                     adaptedTypeVariables);
               if (!com.bluegosling.reflect.Types.equals(b, otherBounds[j])) {
                  return false;
               }
            }
         }
         
         // adapt parameter types
         parameters = new Type[paramTypes.length];
         for (int i = 0; i < paramTypes.length; i++) {
            parameters[i] = com.bluegosling.reflect.Types.replaceTypeVariables(
                  paramTypes[i].getType(), adaptedTypeVariables);
         }

      } else {
         // no type variables to check and thus no need to adapt parameter types
         parameters = new Type[paramTypes.length];
         for (int i = 0; i < parameters.length; i++) {
            parameters[i] = paramTypes[i].getType();
         }
      }
      
      for (int i = 0; i < parameters.length; i++) {
         if (!com.bluegosling.reflect.Types.equals(parameters[i], other.paramTypes[i].getType())) {
            return false;
         }
      }
      
      return true;
   }
   
   /**
    * Returns true if this signature is a subsignature of the given one. This method applies the
    * logic described in <em>Method Signature</em>
    * (<a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2">JLS
    * 8.4.2</a>) to determine if one is a subsignature of another.
    *
    * @param other another executable signature
    * @return true if this executable signature is a subsignature of the given one
    */
   public boolean isSubsignatureOf(ExecutableSignature other) {
      return isSameSignature(other) || isSameSignature(other.erased());
   }
   
   /**
    * Returns true if the given object is equal to this one. The object is equal to this one if it
    * is an executable signature with the same name, same return type, same receiver type (or also
    * has no receiver type if this one doesn't), same parameter types, and same exception types.
    * When comparing types for equality, type annotations are considered, so two references to the
    * same type but with different type annotations are considered not equal.
    */
   @Override
   public boolean equals(Object o) {
      if (o instanceof ExecutableSignature) {
         ExecutableSignature other = (ExecutableSignature) o;
         return paramTypes.length == other.paramTypes.length
               && thrownTypes.length == other.thrownTypes.length
               && typeParameters.length == other.typeParameters.length
               && name.equals(other.name)
               && receiverType == null
                     ? other.receiverType == null
                     : other.receiverType != null
                           && AnnotatedTypes.equals(receiverType, other.receiverType)
               && AnnotatedTypes.equals(returnType, other.returnType)
               && Arrays.equals(annotationSource.getDeclaredAnnotations(),
                     other.annotationSource.getDeclaredAnnotations())
               && allEqual(paramTypes, other.paramTypes)
               && allEqual(thrownTypes, other.thrownTypes)
               && allEqual(typeParameters, other.typeParameters);
      }
      return false;
   }
   
   private static boolean allEqual(AnnotatedType[] t1, AnnotatedType[] t2) {
      assert t1.length == t2.length;
      for (int i = 0; i < t1.length; i++) {
         if (!AnnotatedTypes.equals(t1[i], t2[i])) {
            return false;
         }
      }
      return true;
   }

   private static boolean allEqual(Type[] t1, Type[] t2) {
      assert t1.length == t2.length;
      for (int i = 0; i < t1.length; i++) {
         if (!com.bluegosling.reflect.Types.equals(t1[i], t2[i])) {
            return false;
         }
      }
      return true;
   }

   @Override
   public int hashCode() {
      int hash = name.hashCode();
      hash = 31 * hash + Arrays.hashCode(annotationSource.getDeclaredAnnotations());
      hash = 31 * hash + AnnotatedTypes.hashCode(returnType);
      hash = 31 * hash + (receiverType == null ? 0 : AnnotatedTypes.hashCode(receiverType));
      hash = 31 * hash + hashCode(typeParameters);
      hash = 31 * hash + hashCode(paramTypes);
      hash = 31 * hash + hashCode(thrownTypes);
      return hash;
   }
   
   private int hashCode(AnnotatedType[] types) {
      int result = 1;
      for (AnnotatedType type : types) {
         result = 31 * result + (type == null ? 0 : AnnotatedTypes.hashCode(type));
      }
      return result;
   }

   private int hashCode(Type[] types) {
      int result = 1;
      for (Type type : types) {
         result = 31 * result + (type == null ? 0 : com.bluegosling.reflect.Types.hashCode(type));
      }
      return result;
   }
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      // Type parameters
      if (typeParameters.length > 0) {
         sb.append('<');
         boolean first = true;
         for (TypeVariable<?> typeParam : typeParameters) {
            if (first) {
               first = false;
            } else {
               sb.append(',');
            }
            sb.append(typeParam.getName());
            AnnotatedType[] bounds = typeParam.getAnnotatedBounds();
            if (bounds.length > 0) {
               sb.append(" extends ");
               boolean innerFirst = true;
               for (AnnotatedType bound : bounds) {
                  if (innerFirst) {
                     innerFirst = false;
                  } else {
                     sb.append('&');
                  }
                  sb.append(AnnotatedTypes.toString(bound));
               }
            }
         }
         sb.append("> ");
      }
      // Return type
      sb.append(AnnotatedTypes.toString(returnType)).append(' ');
      // Name
      sb.append(name);
      // Parameters
      sb.append('(');
      boolean first = true;
      for (AnnotatedType p : paramTypes) {
         if (first) {
            first = false;
         } else {
            sb.append(',');
         }
         sb.append(AnnotatedTypes.toString(p));
      }
      sb.append(')');
      // Thrown exceptions
      if (thrownTypes.length > 0) {
         sb.append(" throws ");
         first = true;
         for (AnnotatedType th : thrownTypes) {
            if (first) {
               first = false;
            } else {
               sb.append(',');
            }
            sb.append(AnnotatedTypes.toString(th));
         }
      }
      return sb.toString();
   }
}
