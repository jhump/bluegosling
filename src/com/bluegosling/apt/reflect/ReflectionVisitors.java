package com.bluegosling.apt.reflect;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.ElementKindVisitor6;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.TypeKindVisitor6;

/**
 * Visitor implementations useful for extracting reflection information from elements, annotation
 * mirrors, and type mirrors.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class ReflectionVisitors {
   private ReflectionVisitors() {
   }

   /**
    * A visitor that returns an {@link ArAnnotatedElement} or null if the visited element cannot
    * be represented by element types in this package.
    */
   public static final ElementVisitor<ArAnnotatedElement, Void> ANNOTATED_ELEMENT_VISITOR =
         new ElementKindVisitor6<ArAnnotatedElement, Void>() {
            @Override public ArAnnotatedElement visitType(TypeElement element, Void v) {
               return ArClass.forElement(element);
            }
            @Override public ArAnnotatedElement visitPackage(PackageElement element, Void v) {
               return ArPackage.forElement(element);
            }
            @Override public ArAnnotatedElement visitVariableAsField(VariableElement element, Void v) {
               return ArField.forElement(element);
            }
            @Override public ArAnnotatedElement visitVariableAsEnumConstant(VariableElement element, Void v) {
               return ArField.forElement(element);
            }
            @Override public ArAnnotatedElement visitVariableAsParameter(VariableElement element, Void v) {
               return ArParameter.forElement(element);
            }
            @Override public ArAnnotatedElement visitExecutableAsMethod(ExecutableElement element, Void v) {
               return ArMethod.forElement(element);
            }
            @Override public ArAnnotatedElement visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return ArConstructor.forElement(element);
            }
         };

   /**
    * A visitor that returns a {@link ArClass} or null if the visited element does not represent a
    * class, interface, annotation type, or enum.
    */
   public static final ElementVisitor<ArClass, Void> CLASS_VISITOR =
         new SimpleElementVisitor6<ArClass, Void>() {
            @Override public ArClass visitType(TypeElement element, Void v) {
               return ArClass.forElement(element);
            }
         };

   /**
    * A visitor that returns a {@link ArPackage} or null if the visited element does not represent a
    * package.
    */
   public static final ElementVisitor<ArPackage, Void> PACKAGE_VISITOR =
         new SimpleElementVisitor6<ArPackage, Void>() {
            @Override public ArPackage visitPackage(PackageElement element, Void v) {
               return ArPackage.forElement(element);
            }
         };

   /**
    * A visitor that returns a {@link ArField} or null if the visited element does not represent a
    * field or enum constant.
    */
   public static final ElementVisitor<ArField, Void> FIELD_VISITOR =
         new ElementKindVisitor6<ArField, Void>() {
            @Override public ArField visitVariableAsField(VariableElement element, Void v) {
               return ArField.forElement(element);
            }
            @Override public ArField visitVariableAsEnumConstant(VariableElement element, Void v) {
               return ArField.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link ArMethod} or null if the visited element does not represent a
    * method.
    */
   public static final ElementVisitor<ArMethod, Void> METHOD_VISITOR =
         new ElementKindVisitor6<ArMethod, Void>() {
            @Override public ArMethod visitExecutableAsMethod(ExecutableElement element, Void v) {
               return ArMethod.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link ArConstructor} or null if the visited element does not represent
    * a constructor.
    */
   public static final ElementVisitor<ArConstructor, Void> CONSTRUCTOR_VISITOR =
         new ElementKindVisitor6<ArConstructor, Void>() {
            @Override public ArConstructor visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return ArConstructor.forElement(element);
            }
         };
         
   /**
    * A visitor that returns a {@link ArGenericDeclaration} or null if the visited element does not
    * represent one. Generic declarations include classes, interfaces, annotation types, enums,
    * methods, and constructors (an element in Java source where a type variable can be declared).
    */
   public static final ElementVisitor<ArGenericDeclaration, Void> GENERIC_DECLARATION_VISITOR =
         new ElementKindVisitor6<ArGenericDeclaration, Void>() {
            @Override public ArGenericDeclaration visitType(TypeElement element, Void v) {
               return ArClass.forElement(element);
            }
            @Override public ArGenericDeclaration visitExecutableAsMethod(ExecutableElement element, Void v) {
               return ArMethod.forElement(element);
            }
            @Override public ArGenericDeclaration visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return ArConstructor.forElement(element);
            }
         };
   
   /**
    * A visitor that returns an {@link ArExecutableMember} or null if the visited element does not
    * represent a method or constructor.
    */
   public static final ElementVisitor<ArExecutableMember, Void> EXECUTABLE_MEMBER_VISITOR =
         new ElementKindVisitor6<ArExecutableMember, Void>() {
            @Override public ArExecutableMember visitExecutableAsMethod(ExecutableElement element, Void v) {
               return ArMethod.forElement(element);
            }
            @Override public ArExecutableMember visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return ArConstructor.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link ArParameter} or null if the visited element does not represent a
    * parameter.
    */
   public static final ElementVisitor<ArParameter<?>, Void> PARAMETER_VISITOR =
         new ElementKindVisitor6<ArParameter<?>, Void>() {
            @Override public ArParameter<?> visitVariableAsParameter(VariableElement element, Void v) {
               return ArParameter.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link ArTypeVariable} or null if the visited element does not
    * represent a type variable.
    */
   public static final ElementVisitor<ArTypeParameter<?>, Void> TYPE_VARIABLE_VISITOR =
         new SimpleElementVisitor6<ArTypeParameter<?>, Void>() {
            @Override public ArTypeParameter<?> visitTypeParameter(TypeParameterElement element, Void d) {
               return ArTypeParameter.forElement(element);
            }
         };

   /**
    * A visitor that returns an annotation method's value. This is similar to what is returned
    * from {@link AnnotationValue#getValue()} with a couple of key differences:
    * <ul>
    * <li>If the annotation method returns a {@link java.lang.Class java.lang.Class} then this
    * visitor returns a {@link ArClass} instead of a {@link TypeMirror}.</li>
    * <li>If the annotation method returns an annotation then this visitor returns an
    * {@link ArAnnotation} instead of an {@link AnnotationMirror}.</li>
    * <li>If the annotation method returns an enum then this visitor returns a {@link ArField} instead
    * of a {@link VariableElement}.</li>
    * <li>If the annotation method returns an array, this visitor returns a {@link List}. But its
    * contents have the same types returned by this visitor (instead of those returned by
    * {@link AnnotationValue#getValue()}).</li>
    * </ul>
    * 
    * @see ArAnnotation#getAnnotationAttributes()
    */
   public static final AnnotationValueVisitor<Object, Void> ANNOTATION_VALUE_VISITOR =
         new SimpleAnnotationValueVisitor6<Object, Void>() {
            @Override public Object visitAnnotation(AnnotationMirror mirror, Void v) {
               return ArAnnotation.forAnnotationMirror(mirror);
            }
            @Override public Object visitArray(List<? extends AnnotationValue> values, Void v) {
               List<Object> ret = new ArrayList<Object>(values.size());
               for (AnnotationValue value : values) {
                  ret.add(this.visit(value));
               }
               return ret;
            }
            @Override public Object visitEnumConstant(VariableElement element, Void v) {
               return ArField.forElement(element);
            }
            @Override public Object visitType(TypeMirror mirror, Void v) {
               return ArClass.forTypeMirror(mirror);
            }
            @Override public Object defaultAction(Object o, Void v) {
               return o;
            }
         };

   /**
    * A visitor that returns a {@link ArType} for the specified mirror. This will return {@code null}
    * for the null type (i.e. super-type of {@code java.lang.Object} and of primitive types). It
    * will throw a {@link MirroredTypeException} if there is an error inspecting and converting
    * the mirrors. Otherwise, it should return an appropriate implementation of {@link ArType} based
    * on the kind and details of the actual mirror visited.
    * 
    * @see ArTypes#forTypeMirror(TypeMirror)
    */
   public static final TypeVisitor<ArType, Void> TYPE_MIRROR_VISITOR =
         new TypeKindVisitor6<ArType, Void>() {
            @Override public ArType visitArray(ArrayType mirror, Void v) {
               return ArArrayType.forTypeMirror(mirror);
            }
            @Override public ArType visitDeclared(DeclaredType mirror, Void v) {
               return ArDeclaredType.forDeclaredTypeMirror(mirror);
            }
            @Override public ArType visitTypeVariable(javax.lang.model.type.TypeVariable mirror, Void v) {
               return ArTypeVariable.forTypeMirror(mirror);
            }
            @Override public ArType visitWildcard(javax.lang.model.type.WildcardType mirror, Void v) {
               return ArWildcardType.forTypeMirror(mirror);
            }
            @Override public ArType visitNoTypeAsNone(NoType mirror, Void v) {
               return null;
            }
            @Override public ArType visitNoTypeAsVoid(NoType mirror, Void v) {
               return ArPrimitiveType.forTypeMirror(mirror);
            }
            @Override public ArType visitPrimitive(PrimitiveType mirror, Void v) {
               return ArPrimitiveType.forTypeMirror(mirror);
            }
            @Override public ArType defaultAction(TypeMirror mirror, Void v) {
               throw new MirroredTypeException(mirror);
            }
         };
}
