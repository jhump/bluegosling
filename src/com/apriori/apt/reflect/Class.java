package com.apriori.apt.reflect;

import com.apriori.apt.ElementUtils;
import com.apriori.apt.TypeUtils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

// TODO: javadoc!!
public abstract class Class implements AnnotatedElement, GenericDeclaration, Type {

   public enum Kind {
      CLASS("class "),
      INTERFACE("interface "),
      ENUM("enum "),
      ANNOTATION_TYPE("@interface "),
      ARRAY("class "),
      PRIMITIVE("");
      
      private final String classStringPrefix;
      Kind(String classStringPrefix) {
         this.classStringPrefix = classStringPrefix;
      }
      
      public String getClassStringPrefix() {
         return classStringPrefix;
      }
   }
   
   private static final String CONSTRUCTOR_NAME = "<init>";
   
   Class() {}
   
   public static Class forName(String name) throws ClassNotFoundException {
      Class ret = Class.forElement(ElementUtils.get().getTypeElement(name));
      if (ret == null) {
         throw new ClassNotFoundException(name);
      }
      return ret;
   }
   
   public static Class forType(Type t) {
      return t.accept(new Visitor<Class, Void>() {
         @Override
         public Class visitClass(Class clazz, Void v) {
            return clazz;
         }
         @Override
         public Class visitGenericArrayType(GenericArrayType arrayType, Void v) {
            Class component = forType(arrayType.getGenericComponentType());
            return new ArrayClass(component);
         }
         @Override
         public Class visitParameterizedType(ParameterizedType parameterizedType, Void v) {
            return parameterizedType.getRawType();
         }
         @Override
         public Class visitTypeVariable(TypeVariable<?> typeVariable, Void v) {
            List<? extends Type> bounds = typeVariable.getBounds();
            if (bounds.isEmpty()) {
               return forJavaLangObject();
            } else {
               return forType(bounds.get(0));
            }
         }
         @Override
         public Class visitWildcardType(WildcardType wildcardType, Void v) {
            Type bound = wildcardType.getExtendsBound();
            if (bound == null) {
               return forJavaLangObject();
            } else {
               return forType(bound);
            }
         }
      }, null);
   }
   
   public static Class forJavaLangClass(java.lang.Class<?> clazz) throws ClassNotFoundException {
      if (clazz.isPrimitive()) {
         return new PrimitiveClass(clazz);
      } else if (clazz.isArray()) {
         return new ArrayClass(forJavaLangClass(clazz.getComponentType()));
      } else {
         return forName(clazz.getName());
      }
   }
   
   public static Class forElement(TypeElement element) {
      return new DeclaredClass(element);
   }
   
   public static Class forTypeMirror(TypeMirror mirror) {
      return forType(Types.forTypeMirror(mirror));
   }
   
   public static Class forArray(Class componentType) {
      return new ArrayClass(componentType);
   }
   
   public static Class forPrimitive(java.lang.Class<?> primitiveClass) {
      if (!primitiveClass.isPrimitive()) {
         throw new IllegalArgumentException("Class " + primitiveClass + " is not a primitive");
      }
      return new PrimitiveClass(primitiveClass);
   }
   
   static Class forJavaLangObject() {
      try {
         return forName("java.lang.Object");
      } catch (ClassNotFoundException e) {
         // really should never happen
         throw new AssertionError("Failed to create class for java.lang.Object");
      }
   }
   
   private volatile Map<String, Field> declaredFields;
   private volatile Map<String, Field> allPublicFields;
   private volatile Map<MethodSignature, Method> declaredMethods;
   private volatile Map<MethodSignature, Method> allPublicMethods;
   private volatile Map<MethodSignature, Constructor> constructors;
   private volatile Map<MethodSignature, Constructor> publicConstructors;
   
   private Map<String, Field> getMapOfDeclaredFields() {
      if (declaredFields == null) {
         synchronized (this) {
            if (declaredFields == null) {
               Map<String, Field> map = new LinkedHashMap<String, Field>();
               for (Field field : getDeclaredFieldsInternal()) {
                  map.put(field.getName(), field);
               }
               declaredFields = map;
            }
         }
      }
      return declaredFields;
   }

   private Map<String, Field> getMapOfAllPublicFields() {
      if (allPublicFields == null) {
         synchronized (this) {
            if (allPublicFields == null) {
               Map<String, Field> map = new HashMap<String, Field>();
               for (Class clazz = this; clazz != null; clazz = clazz.getSuperclass()) {
                  for (Field field : getDeclaredFields()) {
                     if (field.getModifiers().contains(Modifier.PUBLIC) && !map.containsKey(field.getName())) {
                        map.put(field.getName(), field);
                     }
                  }
               }
               allPublicFields = map;
            }
         }
      }
      return allPublicFields;
   }

   private Map<MethodSignature, Method> getMapOfDeclaredMethods() {
      if (declaredMethods == null) {
         synchronized (this) {
            if (declaredMethods == null) {
               Map<MethodSignature, Method> map = new LinkedHashMap<MethodSignature, Method>();
               for (Method method : getDeclaredMethodsInternal()) {
                  map.put(new MethodSignature(method), method);
               }
               declaredMethods = map;
            }
         }
      }
      return declaredMethods;
   }
   
   private Map<MethodSignature, Method> getMapOfAllPublicMethods() {
      if (allPublicMethods == null) {
         synchronized (this) {
            if (allPublicMethods == null) {
               Map<MethodSignature, Method> map = new HashMap<MethodSignature, Method>();
               for (Class clazz = this; clazz != null; clazz = clazz.getSuperclass()) {
                  for (Method method : getDeclaredMethods()) {
                     MethodSignature signature = new MethodSignature(method);
                     if (method.getModifiers().contains(Modifier.PUBLIC) && !map.containsKey(signature)) {
                        map.put(signature, method);
                     }
                  }
               }
               allPublicMethods = map;
            }
         }
      }
      return allPublicMethods;
   }

   private Map<MethodSignature, Constructor> getMapOfConstructors() {
      if (constructors == null) {
         synchronized (this) {
            if (constructors == null) {
               Map<MethodSignature, Constructor> map = new LinkedHashMap<MethodSignature, Constructor>();
               for (Constructor constructor : getDeclaredConstructorsInternal()) {
                  map.put(new MethodSignature(CONSTRUCTOR_NAME, constructor.getParameterTypes()),
                        constructor);
               }
               constructors = map;
            }
         }
      }
      return constructors;
   }

   private Map<MethodSignature, Constructor> getMapOfPublicConstructors() {
      if (publicConstructors == null) {
         synchronized (this) {
            if (publicConstructors == null) {
               Map<MethodSignature, Constructor> map = new HashMap<MethodSignature, Constructor>();
               for (Map.Entry<MethodSignature, Constructor> entry : getMapOfConstructors().entrySet()) {
                  if (entry.getValue().getModifiers().contains(Modifier.PUBLIC)) {
                     map.put(entry.getKey(), entry.getValue());
                  }
               }
               publicConstructors = map;
            }
         }
      }
      return publicConstructors;
   }
   
   @Override
   public Type.Kind getTypeKind() {
      return Type.Kind.CLASS;
   }
   
   @Override
   public <R, P> R accept(Visitor<R, P> visitor, P p) {
      return visitor.visitClass(this,  p);
   }
   
   public boolean isInterface() {
      // default
      return false;
   }

   public boolean isEnum() {
      // default
      return false;
   }
   
   public boolean isAnnotation() {
      // default
      return false;
   }

   public boolean isArray() {
      // default
      return false;
   }
   
   public boolean isPrimitive() {
      // default
      return false;
   }
   
   public boolean isAnonymousClass() {
      // default
      return false;
   }
   
   public boolean isLocalClass() {
      // default
      return false;
   }
   
   public boolean isMemberClass() {
      // default
      return false;
   }

   public abstract Kind getClassKind();
   
   public abstract String getName();

   public abstract String getSimpleName();

   public abstract String getCanonicalName();
   
   public abstract EnumSet<Modifier> getModifiers();
   
   public abstract Package getPackage();
   
   public Class getDeclaringClass() {
      // default
      return null;
   }
   
   public Class getEnclosingClass() {
      // default
      return null;
   }
   
   public Constructor getEnclosingConstructor() {
      // default
      return null;
   }
   
   public Method getEnclosingMethod() {
      // default
      return null;
   }
   
   public List<Class> getClasses() {
      // default
      return Collections.emptyList();
   }
   
   public List<Class> getDeclaredClasses() {
      // default
      return Collections.emptyList();
   }
   
   public Class getComponentType() {
      // default
      return null;
   }
   
   NoSuchMethodException noSuchMethod(String name, Class... parameterTypes) {
      StringBuilder sb = new StringBuilder();
      sb.append(getName());
      sb.append(name);
      sb.append("(");
      boolean first = true;
      for (Class parameterType : parameterTypes) {
         if (first) {
            first = false;
         } else {
            sb.append(",");
         }
         sb.append(parameterType.getName());
      }
      sb.append(")");
      return new NoSuchMethodException(sb.toString());
   }
   
   List<Constructor> getDeclaredConstructorsInternal() {
      // default
      return Collections.emptyList();
   }

   public List<Constructor> getDeclaredConstructors() {
      return Collections.unmodifiableList(new ArrayList<Constructor>(getMapOfConstructors().values()));
   }
   
   public Constructor getDeclaredConstructor(Class... parameterTypes) throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(CONSTRUCTOR_NAME, parameterTypes);
      Constructor ret = getMapOfConstructors().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }
   
   public Set<Constructor> getConstructors() {
      return Collections.unmodifiableSet(new HashSet<Constructor>(getMapOfPublicConstructors().values()));
   }
   
   public Constructor getConstructor(Class... parameterTypes) throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(CONSTRUCTOR_NAME, parameterTypes);
      Constructor ret = getMapOfPublicConstructors().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }
   
   List<Field> getDeclaredFieldsInternal() {
      // default
      return Collections.emptyList();
   }

   public List<Field> getDeclaredFields() {
      return Collections.unmodifiableList(new ArrayList<Field>(getMapOfDeclaredFields().values()));
   }

   public Field getDeclaredField(String name) throws NoSuchFieldException {
      Field ret = getMapOfDeclaredFields().get(name);
      if (ret == null) {
         throw new NoSuchFieldException(name);
      }
      return ret;
   }
   
   public Set<Field> getFields() {
      return Collections.unmodifiableSet(new HashSet<Field>(getMapOfAllPublicFields().values()));
   }
   
   public Field getField(String name) throws NoSuchFieldException {
      Field ret = getMapOfAllPublicFields().get(name);
      if (ret == null) {
         throw new NoSuchFieldException(name);
      }
      return ret;
   }

   public List<Field> getEnumConstants() {
      List<Field> fields = new ArrayList<Field>();
      for (Field field : getMapOfDeclaredFields().values()) {
         if (field.isEnumConstant()) {
            fields.add(field);
         }
      }
      return Collections.unmodifiableList(fields);
   }
   
   List<Method> getDeclaredMethodsInternal() {
      // default
      return Collections.emptyList();
   }

   public List<Method> getDeclaredMethods() {
      return Collections.unmodifiableList(new ArrayList<Method>(getMapOfDeclaredMethods().values()));
   }

   public Method getDeclaredMethod(String name, Class... parameterTypes)
         throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(name, parameterTypes);
      Method ret = getMapOfDeclaredMethods().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }

   public Set<Method> getMethods() {
      return Collections.unmodifiableSet(new HashSet<Method>(getMapOfAllPublicMethods().values()));
   }
   
   public Method getMethod(String name, Class... parameterTypes) throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(name, parameterTypes);
      Method ret = getMapOfAllPublicMethods().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }

   public Class getSuperclass() {
      // default
      return null;
   }

   public Type getGenericSuperclass() {
      // default
      return null;
   }
   
   public List<Class> getInterfaces() {
      // default
      return Collections.emptyList();
   }

   public List<? extends Type> getGenericInterfaces() {
      // default
      return Collections.emptyList();
   }
   
   public abstract boolean isAssignableFrom(Class clazz);

   public boolean isSuperTypeOf(Class clazz) {
      return isAssignableFrom(clazz);
   }

   public boolean isSubTypeOf(Class clazz) {
      return clazz.isAssignableFrom(this);
   }

   @Override
   public List<TypeVariable<?>> getTypeVariables() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the class's list of type variables. This is the same as
    * {@link #getTypeVariables()}, except that the return value has more
    * generic type information.
    * 
    * @return the list of type variables
    */
   @SuppressWarnings({ "cast", "rawtypes", "unchecked" })
   public List<TypeVariable<Class>> getClassTypeVariables() {
      // have to cast to raw type List first or else compiler will disallow the
      // subsequent cast to List<Constructor<Method>>
      return (List<TypeVariable<Class>>) ((List) getTypeVariables());
   }

   @Override
   public Annotation getAnnotation(Class annotationClass) {
      // default
      return null;
   }

   @Override
   public Annotation getAnnotation(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass) {
      // default
      return null;
   }

   @Override
   public List<Annotation> getAnnotations() {
      // default
      return Collections.emptyList();
   }

   @Override
   public boolean isAnnotationPresent(Class annotationClass) {
      // default
      return false;
   }

   @Override
   public boolean isAnnotationPresent(
         java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass) {
      // default
      return false;
   }

   @Override
   public List<Annotation> getDeclaredAnnotations() {
      // default
      return Collections.emptyList();
   }
   
   public abstract java.lang.Class<?> asJavaLangClass() throws ClassNotFoundException;
   
   public abstract java.lang.Class<?> asJavaLangClass(ClassLoader classLoader)
         throws ClassNotFoundException;
   
   @Override
   public TypeElement asElement() {
      return null;
   }

   @Override
   public abstract boolean equals(Object o);

   @Override
   public abstract int hashCode();
   
   @Override
   public String toTypeString() {
      return getName();
   }

   @Override
   public String toString() {
      return toString(false);
   }
   
   @Override
   public String toGenericString() {
      return toString(true);
   }

   private String toString(boolean includeGenerics) {
      StringBuilder sb = new StringBuilder();
      sb.append(getClassKind().getClassStringPrefix());
      sb.append(getName());
      if (includeGenerics) {
         TypeVariable.appendTypeParameters(sb, getTypeVariables());
      }
      return sb.toString();
   }

   private static class PrimitiveClass extends Class {
      
      private static Map<java.lang.Class<?>, TypeMirror> typeMirrorMap;
      static {
         typeMirrorMap = new HashMap<java.lang.Class<?>, TypeMirror>(9);
         typeMirrorMap.put(void.class, TypeUtils.get().getNoType(TypeKind.VOID));
         typeMirrorMap.put(boolean.class, TypeUtils.get().getPrimitiveType(TypeKind.BOOLEAN));
         typeMirrorMap.put(byte.class, TypeUtils.get().getPrimitiveType(TypeKind.BYTE));
         typeMirrorMap.put(char.class, TypeUtils.get().getPrimitiveType(TypeKind.CHAR));
         typeMirrorMap.put(double.class, TypeUtils.get().getPrimitiveType(TypeKind.DOUBLE));
         typeMirrorMap.put(float.class, TypeUtils.get().getPrimitiveType(TypeKind.FLOAT));
         typeMirrorMap.put(int.class, TypeUtils.get().getPrimitiveType(TypeKind.INT));
         typeMirrorMap.put(long.class, TypeUtils.get().getPrimitiveType(TypeKind.LONG));
         typeMirrorMap.put(short.class, TypeUtils.get().getPrimitiveType(TypeKind.SHORT));
      }
      
      private final java.lang.Class<?> clazz;
      
      PrimitiveClass(java.lang.Class<?> clazz) {
         this.clazz = clazz;
      }
      
      @Override
      public boolean isPrimitive() {
         return true;
      }

      @Override
      public Kind getClassKind() {
         return Kind.PRIMITIVE;
      }
      
      @Override
      public TypeMirror asTypeMirror() {
         return typeMirrorMap.get(clazz);
      }

      @Override
      public String getName() {
         return clazz.getName();
      }

      @Override
      public String getSimpleName() {
         return clazz.getSimpleName();
      }

      @Override
      public String getCanonicalName() {
         return clazz.getCanonicalName();
      }

      @Override
      public EnumSet<Modifier> getModifiers() {
         return EnumSet.of(Modifier.PUBLIC, Modifier.FINAL);
      }

      @Override
      public Package getPackage() {
         return null;
      }

      @Override
      public boolean isAssignableFrom(Class otherClass) {
         return equals(otherClass);
      }

      @Override
      public java.lang.Class<?> asJavaLangClass() {
         return clazz;
      }

      @Override
      public java.lang.Class<?> asJavaLangClass(ClassLoader classLoader) {
         return clazz;
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof PrimitiveClass) {
            PrimitiveClass other = (PrimitiveClass) o;
            return clazz.equals(other.clazz);
         }
         return false;
      }

      @Override
      public int hashCode() {
         return clazz.hashCode();
      }
   }
   
   private static class ArrayClass extends Class {
      
      private static Map<java.lang.Class<?>, Character> primitiveNameMap;
      static {
         primitiveNameMap = new HashMap<java.lang.Class<?>, Character>(8);
         primitiveNameMap.put(boolean.class, 'Z');
         primitiveNameMap.put(byte.class, 'B');
         primitiveNameMap.put(char.class, 'C');
         primitiveNameMap.put(double.class, 'D');
         primitiveNameMap.put(float.class, 'F');
         primitiveNameMap.put(int.class, 'I');
         primitiveNameMap.put(long.class, 'J');
         primitiveNameMap.put(short.class, 'S');
      }

      private final Class componentClass;
      private final int dimensions;
      
      ArrayClass(Class componentClass) {
         if (componentClass instanceof ArrayClass) {
            ArrayClass other = (ArrayClass) componentClass;
            this.componentClass = other.componentClass;
            this.dimensions = other.dimensions + 1;
         } else {
            this.componentClass = componentClass;
            this.dimensions = 1;
         }
      }

      private ArrayClass(Class componentClass, int dimensions) {
         this.componentClass = componentClass;
         this.dimensions = dimensions;
      }
      
      @Override
      public Kind getClassKind() {
         return Kind.ARRAY;
      }
      
      @Override
      public boolean isArray() {
         return true;
      }

      @Override
      public Class getComponentType() {
         if (dimensions == 1) {
            return componentClass;
         } else {
            return new ArrayClass(componentClass, dimensions - 1);
         }
      }
      
      @Override
      public TypeMirror asTypeMirror() {
         TypeMirror ret = componentClass.asTypeMirror();
         for (int i = 0; i < dimensions; i++) {
            ret = TypeUtils.get().getArrayType(ret);
         }
         return ret;
      }

      @Override
      public List<Class> getInterfaces() {
         Class cloneable, serializable;
         try {
            cloneable = forJavaLangClass(Cloneable.class);
         }
         catch (ClassNotFoundException e) {
            // really should never happen
            throw new AssertionError("Failed to create class for java.lang.Cloneable");
         }
         try {
            serializable = forJavaLangClass(Serializable.class);
         }
         catch (ClassNotFoundException e) {
            // really should never happen
            throw new AssertionError("Failed to create class for java.io.Serializable");
         }
         return Arrays.asList(cloneable, serializable);
      }

      @Override
      public List<? extends Type> getGenericInterfaces() {
         return getInterfaces();
      }
      
      @Override
      public String getName() {
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < dimensions; i++) {
            sb.append("[");
         }
         if (componentClass instanceof PrimitiveClass) {
            sb.append(primitiveNameMap.get(((PrimitiveClass) componentClass).asJavaLangClass()));
         } else {
            sb.append("L");
            sb.append(componentClass.getName());
            sb.append(";");
         }
         return sb.toString();
      }

      @Override
      public String getSimpleName() {
         StringBuilder sb = new StringBuilder();
         sb.append(componentClass.getSimpleName());
         for (int i = 0; i < dimensions; i++) {
            sb.append("[]");
         }
         return sb.toString();
      }

      @Override
      public String getCanonicalName() {
         String componentName = componentClass.getCanonicalName();
         if (componentName == null) {
            return null;
         }
         StringBuilder sb = new StringBuilder();
         sb.append(componentName);
         for (int i = 0; i < dimensions; i++) {
            sb.append("[]");
         }
         return sb.toString();
      }

      @Override
      public EnumSet<Modifier> getModifiers() {
         EnumSet<Modifier> mods = EnumSet.of(Modifier.FINAL);
         EnumSet<Modifier> componentMods = componentClass.getModifiers();
         if (componentMods.contains(Modifier.PUBLIC)) {
            mods.add(Modifier.PUBLIC);
         } else if (componentMods.contains(Modifier.PROTECTED)) {
            mods.add(Modifier.PROTECTED);
         } else if (componentMods.contains(Modifier.PACKAGE_PRIVATE)) {
            mods.add(Modifier.PACKAGE_PRIVATE);
         } else if (componentMods.contains(Modifier.PRIVATE)) {
            mods.add(Modifier.PRIVATE);
         }
         return mods;
      }

      @Override
      public Package getPackage() {
         return componentClass.getPackage();
      }

      @Override
      public boolean isAssignableFrom(Class clazz) {
         if (clazz instanceof ArrayClass) {
            ArrayClass other = (ArrayClass) clazz;
            return (dimensions == other.dimensions && componentClass.isAssignableFrom(other.componentClass))
                  || (dimensions < other.dimensions && componentClass.equals(forJavaLangObject()));
         }
         return false;
      }

      @Override
      public java.lang.Class<?> asJavaLangClass() throws ClassNotFoundException {
         return Array.newInstance(getComponentType().asJavaLangClass(), 0).getClass();
      }

      @Override
      public java.lang.Class<?> asJavaLangClass(ClassLoader classLoader)
            throws ClassNotFoundException {
         return Array.newInstance(getComponentType().asJavaLangClass(classLoader), 0).getClass();
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof ArrayClass) {
            ArrayClass other = (ArrayClass) o;
            return dimensions == other.dimensions && componentClass.equals(other.componentClass);
         }
         return false;
      }

      @Override
      public int hashCode() {
         return (37 * componentClass.hashCode()) ^ (23 * dimensions);
      }
   }
   
   private static class DeclaredClass extends Class {
      private final AbstractAnnotatedElement annotatedElement;
      
      DeclaredClass(TypeElement element) {
         if (element == null) {
            throw new NullPointerException();
         }
         this.annotatedElement = new AbstractAnnotatedElement(element) {
            @Override
            public boolean equals(Object o) {
               return false;
            }

            @Override
            public int hashCode() {
               return 0;
            }

            @Override
            public String toString() {
               return null;
            }
         };
      }
      
      @Override
      public Annotation getAnnotation(Class annotationClass) {
         return annotatedElement.getAnnotation(annotationClass);
      }

      @Override
      public Annotation getAnnotation(
            java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass) {
         return annotatedElement.getAnnotation(annotationClass);
      }

      @Override
      public boolean isAnnotationPresent(Class annotationClass) {
         return annotatedElement.isAnnotationPresent(annotationClass);
      }

      @Override
      public boolean isAnnotationPresent(
            java.lang.Class<? extends java.lang.annotation.Annotation> annotationClass) {
         return annotatedElement.isAnnotationPresent(annotationClass);
      }

      @Override
      public List<Annotation> getAnnotations() {
         return annotatedElement.getAnnotations();
      }

      @Override
      public List<Annotation> getDeclaredAnnotations() {
         return annotatedElement.getDeclaredAnnotations();
      }

      @Override
      public TypeElement asElement() {
         return (TypeElement) annotatedElement.asElement();
      }
      
      @Override
      public TypeMirror asTypeMirror() {
         return asElement().asType();
      }
      
      @Override
      public Kind getClassKind() {
         if (isInterface()) {
            return Kind.ENUM;
         } else if (isAnnotation()) {
            return Kind.ANNOTATION_TYPE;
         } else if (isEnum()) {
            return Kind.ENUM;
         } else {
            return Kind.CLASS;
         }
      }
      
      @Override
      public boolean isInterface() {
         return asElement().getKind() == ElementKind.INTERFACE;
      }

      @Override
      public boolean isEnum() {
         return asElement().getKind() == ElementKind.ENUM;
      }
      
      @Override
      public boolean isAnnotation() {
         return asElement().getKind() == ElementKind.ANNOTATION_TYPE;
      }

      @Override
      public boolean isAnonymousClass() {
         return asElement().getNestingKind() == NestingKind.ANONYMOUS;
      }
      
      @Override
      public boolean isLocalClass() {
         return asElement().getNestingKind() == NestingKind.LOCAL;
      }
      
      @Override
      public boolean isMemberClass() {
         return asElement().getNestingKind() == NestingKind.MEMBER;
      }

      @Override
      protected List<Constructor> getDeclaredConstructorsInternal() {
         List<ExecutableElement> constructors = ElementFilter.constructorsIn(asElement().getEnclosedElements());
         List<Constructor> ret = new ArrayList<Constructor>(constructors.size());
         for (ExecutableElement constructor : constructors) {
            ret.add(Constructor.forElement(constructor));
         }
         return ret;
      }

      @Override
      protected List<Field> getDeclaredFieldsInternal() {
         List<VariableElement> fields = ElementFilter.fieldsIn(asElement().getEnclosedElements());
         List<Field> ret = new ArrayList<Field>(fields.size());
         for (VariableElement field : fields) {
            ret.add(Field.forElement(field));
         }
         return ret;
      }
      
      @Override
      protected List<Method> getDeclaredMethodsInternal() {
         List<ExecutableElement> methods = ElementFilter.methodsIn(asElement().getEnclosedElements());
         List<Method> ret = new ArrayList<Method>(methods.size());
         for (ExecutableElement method : methods) {
            ret.add(Method.forElement(method));
         }
         return ret;
      }

      @Override
      public Class getDeclaringClass() {
         if (isMemberClass()) {
            Class ret = ReflectionVisitors.CLASS_VISITOR.visit(asElement().getEnclosingElement());
            if (ret == null) {
               throw new AssertionError("Member class has enclosing element that is not enclosing class");
            }
            return ret;
         } else {
            return null;
         }
      }
      
      @Override
      public Class getEnclosingClass() {
         if (isMemberClass()) {
            return getDeclaringClass();
         } else if (isAnonymousClass() || isLocalClass()) {
            Element enclosingElement = asElement().getEnclosingElement();
            Class ret = ReflectionVisitors.CLASS_VISITOR.visit(enclosingElement);
            if (ret == null) {
               // enclosing element likely a method, constructor, or initializer so go one level
               // further out
               enclosingElement = enclosingElement.getEnclosingElement();
               ret = ReflectionVisitors.CLASS_VISITOR.visit(enclosingElement);
            }
            return ret;
         } else {
            return null;
         }
      }
      
      @Override
      public Constructor getEnclosingConstructor() {
         if (isAnonymousClass() || isLocalClass()) {
            return ReflectionVisitors.CONSTRUCTOR_VISITOR.visit(asElement().getEnclosingElement());
         } else {
            return null;
         }
      }
      
      @Override
      public Method getEnclosingMethod() {
         if (isAnonymousClass() || isLocalClass()) {
            return ReflectionVisitors.METHOD_VISITOR.visit(asElement().getEnclosingElement());
         } else {
            return null;
         }
      }
      
      @Override
      public List<Class> getClasses() {
         List<TypeElement> types = ElementFilter.typesIn(ElementUtils.get().getAllMembers(asElement()));
         List<Class> ret = new ArrayList<Class>(types.size());
         for (TypeElement type : types) {
            Class clazz = forElement(type);
            if (clazz.isMemberClass() && clazz.getModifiers().contains(Modifier.PUBLIC)) {
               ret.add(clazz);
            }
         }
         return ret;
      }
      
      @Override
      public List<Class> getDeclaredClasses() {
         List<TypeElement> types = ElementFilter.typesIn(asElement().getEnclosedElements());
         List<Class> ret = new ArrayList<Class>(types.size());
         for (TypeElement type : types) {
            Class clazz = forElement(type);
            if (clazz.isMemberClass()) {
               ret.add(clazz);
            }
         }
         return ret;
      }
      
      @Override
      public List<TypeVariable<?>> getTypeVariables() {
         List<? extends TypeParameterElement> parameters = asElement().getTypeParameters();
         List<TypeVariable<?>> ret = new ArrayList<TypeVariable<?>>(parameters.size());
         for (TypeParameterElement parameter : parameters) {
            ret.add(TypeVariable.forElement(parameter));
         }
         return Collections.unmodifiableList(ret);
      }
      
      @Override
      public Class getSuperclass() {
         return forTypeMirror(asElement().getSuperclass());
      }

      @Override
      public Type getGenericSuperclass() {
         return Types.forTypeMirror(asElement().getSuperclass());
      }
      
      @Override
      public List<Class> getInterfaces() {
         List<? extends TypeMirror> interfaces = asElement().getInterfaces();
         List<Class> ret = new ArrayList<Class>();
         for (TypeMirror iface : interfaces) {
            ret.add(forTypeMirror(iface));
         }
         return ret;
      }

      @Override
      public List<? extends Type> getGenericInterfaces() {
         List<? extends TypeMirror> interfaces = asElement().getInterfaces();
         List<Type> ret = new ArrayList<Type>();
         for (TypeMirror iface : interfaces) {
            ret.add(Types.forTypeMirror(iface));
         }
         return ret;
      }
      
      @Override
      public java.lang.Class<?> asJavaLangClass() throws ClassNotFoundException {
         return asJavaLangClass(getClass().getClassLoader());
      }
      
      @Override
      public java.lang.Class<?> asJavaLangClass(ClassLoader classLoader)
            throws ClassNotFoundException {
         return classLoader.loadClass(this.getName());
      }

      @Override
      public String getName() {
         if (asElement().getNestingKind() == NestingKind.TOP_LEVEL) {
            return getCanonicalName();
         } else {
            return getEnclosingClass().getName() + "$" + getSimpleName();
         }
      }

      @Override
      public String getSimpleName() {
         return asElement().getSimpleName().toString();
      }

      @Override
      public String getCanonicalName() {
         String canonicalName = asElement().getQualifiedName().toString();
         if (canonicalName.isEmpty()) {
            return null;
         }
         return canonicalName;
      }

      @Override
      public EnumSet<Modifier> getModifiers() {
         return Modifier.fromElementModifiers(asElement().getModifiers());
      }

      @Override
      public Package getPackage() {
         PackageElement pkgElement = ElementUtils.get().getPackageOf(asElement());
         if (pkgElement == null) {
            return null;
         }
         return Package.forElement(pkgElement);
      }

      @Override
      public boolean isAssignableFrom(Class clazz) {
         return TypeUtils.get().isAssignable(TypeUtils.get().erasure(clazz.asTypeMirror()),
               TypeUtils.get().erasure(asTypeMirror()));
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof DeclaredClass) {
            DeclaredClass clazz = (DeclaredClass) o;
            return TypeUtils.get().isSameType(TypeUtils.get().erasure(clazz.asTypeMirror()),
                  TypeUtils.get().erasure(asTypeMirror()));
         }
         return false;
      }

      @Override
      public int hashCode() {
         return getName().hashCode();
      }
   }
}