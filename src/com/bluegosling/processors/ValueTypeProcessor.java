package com.bluegosling.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.bluegosling.apt.AbstractProcessor;
import com.bluegosling.apt.SupportedAnnotationClasses;
import com.bluegosling.util.ValueType;

/**
 * Verifies, to the extent possible by an annotation processor, that the {@link ValueType}
 * annotation is used correctly. The rules that can be verified using information available to an
 * annotation processor are the following:
 * <ul>
 * <li>Value types must be final concrete classes (not enums, not interfaces, and not annotation
 * types).</li>
 * <li>Value types must be immutable (e.g. all member fields must be final).</li>
 * <li>Value types cannot have a super-class, other than {@code java.lang.Object}.</li>
 * <li>Value types may not use the default implementations for {@link Object#equals(Object)},
 * {@link Object#hashCode()}, or {@link Object#toString()} since they leak information about the
 * instance's identity.</li>
 * </ul>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@SupportedAnnotationClasses(ValueType.class)
public class ValueTypeProcessor extends AbstractProcessor {
   @Override
   public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latest();
   }

   @Override
   protected boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      Messager msg = processingEnv.getMessager();
      
      for (TypeElement annotation : annotations) {
         if (annotation.getQualifiedName().toString().equals(ValueType.class.getCanonicalName())) {
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(annotation)) {
               if (annotatedElement.getKind() != ElementKind.CLASS) {
                  msg.printMessage(Kind.ERROR,
                        "Value types must be classes (not interfaces, enums, or annotation types).",
                        annotatedElement);
                  // don't bother checking the other criteria
                  continue;
               }
               
               // Some (all?) Messager implementations only store a single message per location. So
               // if we find several issues for the annotated element, only one such message may be
               // reported to the user (the first). To report *all* messages, we put everything into
               // a single message.
               List<String> annotatedElementMessages = new ArrayList<>(5); 
               
               if (!annotatedElement.getModifiers().contains(Modifier.FINAL)) {
                  annotatedElementMessages.add("Value types must be final.");
               }

               TypeMirror superClass = ((TypeElement) annotatedElement).getSuperclass();
               TypeElement superClassElement =
                     (TypeElement) processingEnv.getTypeUtils().asElement(superClass);
               if (!Object.class.getCanonicalName().equals(
                     superClassElement.getQualifiedName().toString())) {
                  annotatedElementMessages.add(
                        "Value types may not have a superclass, other than java.lang.Object.");
               }

               boolean hasEquals = false;
               boolean hasHashCode = false;
               boolean hasToString = false;
               for (Element e : annotatedElement.getEnclosedElements()) {
                  if (e.getKind() == ElementKind.METHOD) {
                     ExecutableElement ee = (ExecutableElement) e;
                     if (isEquals(ee)) {
                        hasEquals = true;
                     } else if (isHashCode(ee)) {
                        hasHashCode = true;
                     } else if (isToString(ee)) {
                        hasToString = true;
                     }
                  } else if (e.getKind() == ElementKind.FIELD) {
                     if (!e.getModifiers().contains(Modifier.STATIC)
                           && !e.getModifiers().contains(Modifier.FINAL)) {
                        msg.printMessage(Kind.ERROR,
                              "Value types cannot have non-final member fields.", e);
                     }
                  }
               }
               
               if (!hasEquals) {
                  annotatedElementMessages.add(
                        "Value types must not use the default implementation for equals(Object).");
               }
               if (!hasHashCode) {
                  annotatedElementMessages.add(
                        "Value types must not use the default implementation for hashCode().");
               }
               if (!hasToString) {
                  annotatedElementMessages.add(
                        "Value types must not use the default implementation for toString().");
               }
               
               if (!annotatedElementMessages.isEmpty()) {
                  msg.printMessage(Kind.ERROR,
                        annotatedElementMessages.stream().collect(Collectors.joining(" ")),
                        annotatedElement);
               }
            }
         }
         // Log or throw if we received an unexpected annotation??
      }
      return false;
   }
   
   private boolean isEquals(ExecutableElement method) {
      if (!"equals".equals(method.getSimpleName().toString())) {
         return false;
      }
      if (method.getParameters().size() != 1) {
         return false;
      }
      if (method.getReturnType().getKind() != TypeKind.BOOLEAN) {
         return false;
      }
      TypeMirror paramType = method.getParameters().get(0).asType();
      if (paramType.getKind() != TypeKind.DECLARED) {
         return false;
      }
      TypeElement paramElement = (TypeElement) processingEnv.getTypeUtils().asElement(paramType);
      if (!Object.class.getCanonicalName().equals(paramElement.getQualifiedName().toString())) {
         return false;
      }
      return true;
   }

   private boolean isHashCode(ExecutableElement method) {
      if (!"hashCode".equals(method.getSimpleName().toString())) {
         return false;
      }
      if (!method.getParameters().isEmpty()) {
         return false;
      }
      if (method.getReturnType().getKind() != TypeKind.INT) {
         return false;
      }
      return true;
   }

   private boolean isToString(ExecutableElement method) {
      if (!"toString".equals(method.getSimpleName().toString())) {
         return false;
      }
      if (!method.getParameters().isEmpty()) {
         return false;
      }
      TypeMirror returnType = method.getReturnType();
      if (returnType.getKind() != TypeKind.DECLARED) {
         return false;
      }
      TypeElement returnElement = (TypeElement) processingEnv.getTypeUtils().asElement(returnType);
      if (!String.class.getCanonicalName().equals(returnElement.getQualifiedName().toString())) {
         return false;
      }
      return true;
   }
}
