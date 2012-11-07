package com.apriori.apt.reflect;

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
    * A visitor that returns a {@link Class} or null if the visited element does not represent a
    * class, interface, annotation type, or enum.
    */
   public static final ElementVisitor<Class, Void> CLASS_VISITOR =
         new SimpleElementVisitor6<Class, Void>() {
            @Override public Class visitType(TypeElement element, Void v) {
               return Class.forElement(element);
            }
         };

   /**
    * A visitor that returns a {@link Package} or null if the visited element does not represent a
    * package.
    */
   public static final ElementVisitor<Package, Void> PACKAGE_VISITOR =
         new SimpleElementVisitor6<Package, Void>() {
            @Override public Package visitPackage(PackageElement element, Void v) {
               return Package.forElement(element);
            }
         };

   /**
    * A visitor that returns a {@link Field} or null if the visited element does not represent a
    * field or enum constant.
    */
   public static final ElementVisitor<Field, Void> FIELD_VISITOR =
         new ElementKindVisitor6<Field, Void>() {
            @Override public Field visitVariableAsField(VariableElement element, Void v) {
               return Field.forElement(element);
            }
            @Override public Field visitVariableAsEnumConstant(VariableElement element, Void v) {
               return Field.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link Method} or null if the visited element does not represent a
    * method.
    */
   public static final ElementVisitor<Method, Void> METHOD_VISITOR =
         new ElementKindVisitor6<Method, Void>() {
            @Override public Method visitExecutableAsMethod(ExecutableElement element, Void v) {
               return Method.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link Constructor} or null if the visited element does not represent
    * a constructor.
    */
   public static final ElementVisitor<Constructor, Void> CONSTRUCTOR_VISITOR =
         new ElementKindVisitor6<Constructor, Void>() {
            @Override public Constructor visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return Constructor.forElement(element);
            }
         };
         
   /**
    * A visitor that returns a {@link GenericDeclaration} or null if the visited element does not
    * represent one. Generic declarations include classes, interfaces, annotation types, enums,
    * methods, and constructors (an element in Java source where a type variable can be declared).
    */
   public static final ElementVisitor<GenericDeclaration, Void> GENERIC_DECLARATION_VISITOR =
         new ElementKindVisitor6<GenericDeclaration, Void>() {
            @Override public GenericDeclaration visitType(TypeElement element, Void v) {
               return Class.forElement(element);
            }
            @Override public GenericDeclaration visitExecutableAsMethod(ExecutableElement element, Void v) {
               return Method.forElement(element);
            }
            @Override public GenericDeclaration visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return Constructor.forElement(element);
            }
         };
   
   /**
    * A visitor that returns an {@link ExecutableMember} or null if the visited element does not
    * represent a method or constructor.
    */
   public static final ElementVisitor<ExecutableMember, Void> EXECUTABLE_MEMBER_VISITOR =
         new ElementKindVisitor6<ExecutableMember, Void>() {
            @Override public ExecutableMember visitExecutableAsMethod(ExecutableElement element, Void v) {
               return Method.forElement(element);
            }
            @Override public ExecutableMember visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return Constructor.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link Parameter} or null if the visited element does not represent a
    * parameter.
    */
   public static final ElementVisitor<Parameter<?>, Void> PARAMETER_VISITOR =
         new ElementKindVisitor6<Parameter<?>, Void>() {
            @Override public Parameter<?> visitVariableAsParameter(VariableElement element, Void v) {
               return Parameter.forElement(element);
            }
         };
   
   /**
    * A visitor that returns a {@link TypeVariable} or null if the visited element does not
    * represent a type variable.
    */
   public static final ElementVisitor<TypeVariable<?>, Void> TYPE_VARIABLE_VISITOR =
         new SimpleElementVisitor6<TypeVariable<?>, Void>() {
            @Override public TypeVariable<?> visitTypeParameter(TypeParameterElement element, Void d) {
               return TypeVariable.forElement(element);
            }
         };

   /**
    * A visitor that returns an annotation method's value. This is similar to what is returned
    * from {@link AnnotationValue#getValue()} with a couple of key differences:
    * <ul>
    * <li>If the annotation method returns a {@link java.lang.Class java.lang.Class} then this
    * visitor returns a {@link Class} instead of a {@link TypeMirror}.</li>
    * <li>If the annotation method returns an annotation then this visitor returns an
    * {@link Annotation} instead of an {@link AnnotationMirror}.</li>
    * <li>If the annotation method returns an enum then this visitor returns a {@link Field} instead
    * of a {@link VariableElement}.</li>
    * <li>If the annotation method returns an array, this visitor returns a {@link List}. But its
    * contents have the same types returned by this visitor (instead of those returned by
    * {@link AnnotationValue#getValue()}).</li>
    * </ul>
    * 
    * @see Annotation#getAnnotationAttributes()
    */
   public static final AnnotationValueVisitor<Object, Void> ANNOTATION_VALUE_VISITOR =
         new SimpleAnnotationValueVisitor6<Object, Void>() {
            @Override public Object visitAnnotation(AnnotationMirror mirror, Void v) {
               return Annotation.forAnnotationMirror(mirror);
            }
            @Override public Object visitArray(List<? extends AnnotationValue> values, Void v) {
               List<Object> ret = new ArrayList<Object>(values.size());
               for (AnnotationValue value : values) {
                  ret.add(this.visit(value));
               }
               return ret;
            }
            @Override public Object visitEnumConstant(VariableElement element, Void v) {
               return Field.forElement(element);
            }
            @Override public Object visitType(TypeMirror mirror, Void v) {
               return Class.forTypeMirror(mirror);
            }
            @Override public Object defaultAction(Object o, Void v) {
               return o;
            }
         };

   /**
    * A visitor that returns a {@link Type} for the specified mirror. This will return {@code null}
    * for the null type (i.e. super-type of {@code java.lang.Object} and of primitive types). It
    * will throw a {@link MirroredTypeException} if there is an error inspecting and converting
    * the mirrors. Otherwise, it should return an appropriate implementation of {@link Type} based
    * on the kind and details of the actual mirror visited.
    * 
    * @see Types#forTypeMirror(TypeMirror)
    */
   public static final TypeVisitor<Type, Void> TYPE_MIRROR_VISITOR =
         new TypeKindVisitor6<Type, Void>() {
            @Override public Type visitArray(ArrayType mirror, Void v) {
               Type componentType = this.visit(mirror.getComponentType(), v);
               if (componentType.getTypeKind() == Type.Kind.CLASS) {
                  return Class.forArray(Types.asClass(componentType));
               }
               return GenericArrayType.forTypeMirror(mirror);
            }
            @Override public Type visitDeclared(DeclaredType mirror, Void v) {
               // TODO: also allow types w/ no args but that are non-static member types enclosed
               // in types that have args to be represented as parameterized types
               if (mirror.getTypeArguments().isEmpty()) {
                  Class ret = ReflectionVisitors.CLASS_VISITOR.visit(mirror.asElement());
                  if (ret == null) {
                     throw new MirroredTypeException(mirror);
                  }
                  return ret;
               } else {
                  return ParameterizedType.forTypeMirror(mirror);
               }
            }
            @Override public Type visitTypeVariable(javax.lang.model.type.TypeVariable mirror, Void v) {
               return TypeVariable.forTypeMirror(mirror);
            }
            @Override public Type visitWildcard(javax.lang.model.type.WildcardType mirror, Void v) {
               return WildcardType.forTypeMirror(mirror);
            }
            @Override public Type visitNoTypeAsNone(NoType mirror, Void v) {
               return null;
            }
            @Override public Type visitNoTypeAsVoid(NoType mirror, Void v) {
               return Class.forPrimitive(void.class);
            }
            @Override public Type visitPrimitiveAsBoolean(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(boolean.class);
            }
            @Override public Type visitPrimitiveAsByte(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(byte.class);
            }
            @Override public Type visitPrimitiveAsChar(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(char.class);
            }
            @Override public Type visitPrimitiveAsDouble(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(double.class);
            }
            @Override public Type visitPrimitiveAsFloat(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(float.class);
            }
            @Override public Type visitPrimitiveAsInt(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(int.class);
            }
            @Override public Type visitPrimitiveAsLong(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(long.class);
            }
            @Override public Type visitPrimitiveAsShort(PrimitiveType mirror, Void v) {
               return Class.forPrimitive(short.class);
            }
            @Override public Type defaultAction(TypeMirror mirror, Void v) {
               throw new MirroredTypeException(mirror);
            }
         };
}
