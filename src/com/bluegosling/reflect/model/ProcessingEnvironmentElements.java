package com.bluegosling.reflect.model;

import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor8;

/**
 * An implementation of {@link Elements} backed by an annotation processing environment. This
 * delegates most methods to an {@link javax.lang.model.util.Elements} instance.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class ProcessingEnvironmentElements implements Elements {
   private final javax.lang.model.util.Elements base;
   private final Types typeUtils;
   
   ProcessingEnvironmentElements(ProcessingEnvironment env) {
      this.base = env.getElementUtils();
      this.typeUtils = new ProcessingEnvironmentTypes(env.getTypeUtils(), this);
   }
   
   ProcessingEnvironmentElements(javax.lang.model.util.Elements base, Types typeUtils) {
      this.base = base;
      this.typeUtils = typeUtils;
   }
   
   Types getTypeUtils() {
      return typeUtils;
   }

   @Override
   public PackageElement getPackageElement(CharSequence name) {
      return base.getPackageElement(name);
   }

   @Override
   public TypeElement getTypeElement(CharSequence name) {
      return base.getTypeElement(name);
   }

   @Override
   public Map<? extends ExecutableElement, ? extends AnnotationValue>
   getElementValuesWithDefaults(AnnotationMirror a) {
      return base.getElementValuesWithDefaults(a);
   }

   @Override
   public String getDocComment(Element e) {
      return base.getDocComment(e);
   }

   @Override
   public boolean isDeprecated(Element e) {
      return base.isDeprecated(e);
   }

   @Override
   public Name getBinaryName(TypeElement type) {
      return base.getBinaryName(type);
   }

   @Override
   public PackageElement getPackageOf(Element type) {
      return base.getPackageOf(type);
   }

   @Override
   public List<? extends Element> getAllMembers(TypeElement type) {
      return base.getAllMembers(type);
   }

   @Override
   public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
      return base.getAllAnnotationMirrors(e);
   }

   @Override
   public boolean hides(Element hider, Element hidden) {
      return base.hides(hider, hidden);
   }

   @Override
   public boolean overrides(ExecutableElement overrider, ExecutableElement overridden,
         TypeElement type) {
      return base.overrides(overrider,  overridden, type);
   }

   @Override
   public String getConstantExpression(Object value) {
      return base.getConstantExpression(value);
   }

   @Override
   public void printElements(Writer w, Element... elements) {
      base.printElements(w, elements);
   }

   @Override
   public Name getName(CharSequence cs) {
      return base.getName(cs);
   }

   @Override
   public boolean isFunctionalInterface(TypeElement type) {
      return base.isFunctionalInterface(type);
   }

   @Override
   public TypeElement getTypeElement(Class<?> clazz) {
      if (clazz.isPrimitive()) {
         throw new IllegalArgumentException(
               "cannot get type element for primitive type: " + clazz.getTypeName());
      }
      if (clazz.isArray()) {
         throw new IllegalArgumentException(
               "cannot get type element for array type: " + clazz.getTypeName());
      }
      if (clazz.isAnonymousClass()) {
         throw new IllegalArgumentException(
               "cannot get type element for anonymous type: " + clazz.getTypeName());
      }
      
      ElementKind expectedKind;
      if (clazz.isAnnotation()) {
         expectedKind = ElementKind.ANNOTATION_TYPE;
      } else if (clazz.isEnum()) {
         expectedKind = ElementKind.ENUM;
      } else if (clazz.isInterface()) {
         expectedKind = ElementKind.INTERFACE;
      } else {
         expectedKind = ElementKind.CLASS;
      }
      
      if (clazz.isLocalClass()) {
         // NB: As of Java 8, this section always throws because access to local classes isn't
         // adequately supported. Methods and constructors never have enclosed elements (even if
         // they declare local classes), and local classes don't have canonical names that can be
         // used with Elements#getTypeElement
         
         Executable owner = clazz.getEnclosingConstructor();
         if (owner == null) {
            owner = clazz.getEnclosingMethod();
         }
         assert owner != null;
         ExecutableElement ownerElement = getExecutableElement(owner);
         for (Element child : ownerElement.getEnclosedElements()) {
            if (child.getKind() == expectedKind
                  && child.getSimpleName().toString().equals(clazz.getSimpleName())) {
               return (TypeElement) child;
            }
         }
         throw new RuntimeException(
               "could not get type element for local type: " + clazz.getTypeName());
      } else {
         TypeElement ret = base.getTypeElement(clazz.getCanonicalName());
         // basic sanity checks
         if (ret == null) {
            throw new RuntimeException(
                  "could not get type element for type: " + clazz.getTypeName());
         }
         if (!ret.getSimpleName().toString().equals(clazz.getSimpleName())) {
            throw new AssertionError("element for type " + clazz.getCanonicalName()
                  + " has incorrect simple name: " + ret.getSimpleName() + " != "
                  + clazz.getSimpleName());
         }
         if (ret.getKind() != expectedKind) {
            throw new AssertionError("element for type " + clazz.getCanonicalName()
                  + " has incorrect kind: " + ret.getKind() + " != "
                  + expectedKind);
         }
         ElementKind enclosing = ret.getEnclosingElement().getKind();
         if (clazz.isMemberClass() && !enclosing.isClass() && ! enclosing.isInterface()) {
            // class token is a nested type, but element found is not
            throw new AssertionError("element for type " + clazz.getCanonicalName()
                  + " should be a member type but is not");
         }
         if (!clazz.isMemberClass() && enclosing != ElementKind.PACKAGE) {
            // class token is a top-level type, but element found is not
            throw new AssertionError("element for type " + clazz.getCanonicalName()
                  + " should be a top-level type but is not");
         }
         return ret;
      }
   }

   @Override
   public PackageElement getPackageElement(Package pkg) {
      PackageElement ret = base.getPackageElement(pkg.getName());
      if (ret == null) {
         throw new RuntimeException("could not get package element for package: " + pkg.getName());
      }
      return ret;
   }

   @Override
   public VariableElement getParameterElement(Parameter parameter) {
      Executable decl = parameter.getDeclaringExecutable();
      // retrieve parameter element via its positional index
      int ord = -1;
      Parameter[] parms = decl.getParameters();
      for (int i = 0; i < parms.length; i++) {
         if (parms[i].equals(parameter)) {
            ord = i;
            break;
         }
      }
      assert ord >= 0;
      ExecutableElement owner = getExecutableElement(decl);
      VariableElement ret = owner.getParameters().get(ord);
      // sanity check - make sure type of the element matches the parameter's type
      if (!isSameType(ret.asType(), parameter.getType())) {
         throw new AssertionError("could not get element for parameter: " + parameter);
      }
      return ret;
   }

   @Override
   public VariableElement getFieldElement(Field field) {
      TypeElement owner = getTypeElement(field.getDeclaringClass());
      ElementKind expectedKind =
            field.isEnumConstant() ? ElementKind.ENUM_CONSTANT : ElementKind.FIELD;
      for (Element child : owner.getEnclosedElements()) {
         if (child.getKind() != expectedKind 
               || !child.getSimpleName().toString().equals(field.getName())) {
            continue;
         }
         VariableElement element = (VariableElement) child;
         // sanity check - make sure type of the element matches the field's type
         if (!isSameType(element.asType(), field.getType())) {
            break;
         }
         return element;
      }
      throw new AssertionError("could not get element for field: " + field);
   }

   @Override
   public ExecutableElement getExecutableElement(Executable executable) {
      TypeElement owner = getTypeElement(executable.getDeclaringClass());
      if (executable instanceof Constructor) {
         for (Element child : owner.getEnclosedElements()) {
            if (child.getKind() != ElementKind.CONSTRUCTOR) {
               continue;
            }
            ExecutableElement element = (ExecutableElement) child;
            if (isSameSignature(element, executable)) {
               return element;
            }
         }
      } else {
         assert executable instanceof Method;
         for (Element child : owner.getEnclosedElements()) {
            if (child.getKind() != ElementKind.METHOD 
                  || !child.getSimpleName().toString().equals(executable.getName())) {
               continue;
            }
            ExecutableElement element = (ExecutableElement) child;
            if (isSameSignature(element, executable)) {
               return element;
            }
         }
      }
      throw new AssertionError("could not get element for executable: " + executable);
   }

   @Override
   public TypeParameterElement getTypeParameterElement(TypeVariable<?> typeVar) {
      GenericDeclaration decl = typeVar.getGenericDeclaration();
      // retrieve parameter element via its positional index
      int ord = -1;
      TypeVariable<?>[] vars = decl.getTypeParameters();
      for (int i = 0; i < vars.length; i++) {
         if (vars[i].equals(typeVar)) {
            ord = i;
            break;
         }
      }
      assert ord >= 0;
      Parameterizable owner;
      if (decl instanceof Class) {
         owner = getTypeElement((Class<?>) decl);
      } else if (decl instanceof Executable) {
         owner = getExecutableElement((Executable) decl);
      } else {
         throw new IllegalArgumentException(
               "type variable has unsupported declaration type: " + decl.getClass().getTypeName());
      }
      TypeParameterElement ret = owner.getTypeParameters().get(ord);
      if (!ret.getSimpleName().toString().equals(typeVar.getName())) {
         throw new AssertionError("type parameter element at index " + ord + " is named "
               + ret.getSimpleName() + ", not " + typeVar.getName());
      }
      return ret;
   }

   /**
    * Determines if the visited type mirror has the same erased type as a given class token.
    */
   private static final TypeVisitor<Boolean, Class<?>> SAME_TYPE_VISITOR =
         new SimpleTypeVisitor8<Boolean, Class<?>>(false) {
            @Override
            public Boolean visitIntersection(IntersectionType t, Class<?> p) {
               return t.getBounds().get(0).accept(this, p);
            }

            @Override
            public Boolean visitPrimitive(PrimitiveType t, Class<?> p) {
               TypeKind kind = t.getKind();
               switch (kind) {
                  case BOOLEAN:
                     return p == boolean.class;
                  case BYTE:
                     return p == byte.class;
                  case SHORT:
                     return p == short.class;
                  case CHAR:
                     return p == char.class;
                  case INT:
                     return p == int.class;
                  case LONG:
                     return p == long.class;
                  case FLOAT:
                     return p == float.class;
                  case DOUBLE:
                     return p == double.class;
                  default:
                     throw new IllegalArgumentException(
                           "kind is not a valid primitive type: " + kind);
               }
            }

            @Override
            public Boolean visitArray(ArrayType t, Class<?> p) {
               if (!p.isArray()) {
                  return false;
               }
               return t.getComponentType().accept(this, p.getComponentType());
            }

            @Override
            public Boolean visitDeclared(DeclaredType t, Class<?> p) {
               return isSameType((TypeElement) t.asElement(), p);
            }

            @Override
            public Boolean visitTypeVariable(javax.lang.model.type.TypeVariable t, Class<?> p) {
               return t.getUpperBound().accept(this, p);
            }

            @Override
            public Boolean visitWildcard(WildcardType t, Class<?> p) {
               return t.getExtendsBound().accept(this, p);
            }

            @Override
            public Boolean visitNoType(NoType t, Class<?> p) {
               return t.getKind() == TypeKind.VOID && p == void.class;
            }
         };
   
   static boolean isSameType(TypeMirror type, Class<?> erasedType) {
      return type.accept(SAME_TYPE_VISITOR, erasedType);
   }
   
   static boolean isSameType(TypeElement type, Class<?> clazz) {
      if (!type.getSimpleName().toString().equals(clazz.getSimpleName())) {
         return false;
      }
      Element e = type.getEnclosingElement();
      if (e.getKind() == ElementKind.PACKAGE) {
         // element is not an enclosed class, so class token better not be either
         // and they must be in same package
         return clazz.getEnclosingClass() == null
               && clazz.getEnclosingConstructor() == null
               && clazz.getEnclosingMethod() == null
               && ((PackageElement) e).getQualifiedName().toString()
                     .equals(clazz.getPackage().getName());
      } else if (clazz.getEnclosingClass() == null
               && clazz.getEnclosingConstructor() == null
               && clazz.getEnclosingMethod() == null) {
         // element *is* enclosed type but class token is not...
         return false;
      }
      switch (e.getKind()) {
         case CLASS: case INTERFACE: case ENUM: case ANNOTATION_TYPE:
            Class<?> enclosingClass = clazz.getEnclosingClass();
            return enclosingClass != null && isSameType((TypeElement) e, enclosingClass);
         case METHOD:
            Method enclosingMethod = clazz.getEnclosingMethod();
            return enclosingMethod != null
                  && isSameExecutable((ExecutableElement) e, enclosingMethod);
         case CONSTRUCTOR:
            Constructor<?> enclosingCtor = clazz.getEnclosingConstructor();
            return enclosingCtor != null
                  && isSameExecutable((ExecutableElement) e, enclosingCtor);
         default:
            return false;
      }
   }
   
   static boolean isSameExecutable(ExecutableElement el, Executable ex) {
      if (ex instanceof Method) {
         if (el.getKind() != ElementKind.METHOD) {
            return false;
         }
         if (!el.getSimpleName().toString().equals(ex.getName())) {
            return false;
         }
      } else if (ex instanceof Constructor) {
         if (el.getKind() != ElementKind.CONSTRUCTOR) {
            return false;
         }
      } else {
         return false; // not method or constructor? maybe throw AssertionError...
      }
      
      // check declaring type
      TypeElement declaringType = (TypeElement) el.getEnclosingElement();
      if (!isSameType(declaringType, ex.getDeclaringClass())) {
         return false;
      }
      
      // lastly, check signature (argument and return types) to see if this is the correct overload
      return isSameSignature(el, ex);
   }
   
   static boolean isSameSignature(ExecutableElement el, Executable ex) {
      List<? extends VariableElement> elParms = el.getParameters();
      Parameter[] exParms = ex.getParameters();
      if (elParms.size() != exParms.length) {
         return false;
      }
      for (int i = 0; i < exParms.length; i++) {
         if (!isSameType(elParms.get(i).asType(), exParms[i].getType())) {
            return false;
         }
      }
      
      // if it's a method, check the return type too
      return ex instanceof Constructor
            || isSameType(el.getReturnType(), ((Method) ex).getReturnType());
   }
}
