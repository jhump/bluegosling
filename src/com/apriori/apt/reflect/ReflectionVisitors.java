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

//TODO: javadoc!
public final class ReflectionVisitors {
   
   private ReflectionVisitors() {
   }
   
   public static final ElementVisitor<Class, Void> CLASS_VISITOR =
         new SimpleElementVisitor6<Class, Void>() {
            @Override public Class visitType(TypeElement element, Void v) {
               return Class.forElement(element);
            }
         };

   public static final ElementVisitor<Package, Void> PACKAGE_VISITOR =
         new SimpleElementVisitor6<Package, Void>() {
            @Override public Package visitPackage(PackageElement element, Void v) {
               return Package.forElement(element);
            }
         };

   public static final ElementVisitor<Field, Void> FIELD_VISITOR =
         new ElementKindVisitor6<Field, Void>() {
            @Override public Field visitVariableAsField(VariableElement element, Void v) {
               return Field.forElement(element);
            }
            @Override public Field visitVariableAsEnumConstant(VariableElement element, Void v) {
               return Field.forElement(element);
            }
         };
   
   public static final ElementVisitor<Method, Void> METHOD_VISITOR =
         new ElementKindVisitor6<Method, Void>() {
            @Override public Method visitExecutableAsMethod(ExecutableElement element, Void v) {
               return Method.forElement(element);
            }
         };
   
   public static final ElementVisitor<Constructor, Void> CONSTRUCTOR_VISITOR =
         new ElementKindVisitor6<Constructor, Void>() {
            @Override public Constructor visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return Constructor.forElement(element);
            }
         };
         
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
   
   public static final ElementVisitor<ExecutableMember, Void> EXECUTABLE_MEMBER_VISITOR =
         new ElementKindVisitor6<ExecutableMember, Void>() {
            @Override public ExecutableMember visitExecutableAsMethod(ExecutableElement element, Void v) {
               return Method.forElement(element);
            }
            @Override public ExecutableMember visitExecutableAsConstructor(ExecutableElement element, Void v) {
               return Constructor.forElement(element);
            }
         };
   
   public static final ElementVisitor<Parameter<?>, Void> PARAMETER_VISITOR =
         new ElementKindVisitor6<Parameter<?>, Void>() {
            @Override public Parameter<?> visitVariableAsParameter(VariableElement element, Void v) {
               return Parameter.forElement(element);
            }
         };
   
   public static final ElementVisitor<TypeVariable<?>, Void> TYPE_VARIABLE_VISITOR =
         new SimpleElementVisitor6<TypeVariable<?>, Void>() {
            @Override public TypeVariable<?> visitTypeParameter(TypeParameterElement element, Void d) {
               return TypeVariable.forElement(element);
            }
         };

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

   public static final TypeVisitor<Type, Void> TYPE_MIRROR_VISITOR =
         new TypeKindVisitor6<Type, Void>() {
            @Override public Type visitArray(ArrayType mirror, Void v) {
               return GenericArrayType.forTypeMirror(mirror);
            }
            @Override public Type visitDeclared(DeclaredType mirror, Void v) {
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
               TypeVariable<?> ret = ReflectionVisitors.TYPE_VARIABLE_VISITOR.visit(mirror.asElement());
               if (ret == null) {
                  throw new MirroredTypeException(mirror);
               }
               return ret;
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
