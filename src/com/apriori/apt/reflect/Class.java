package com.apriori.apt.reflect;

import static com.apriori.apt.ProcessingEnvironments.elements;
import static com.apriori.apt.ProcessingEnvironments.types;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
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

/**
 * A class (or interface, enum, or annotation type). This is analogous to {@link java.lang.Class
 * java.lang.Class} except that it represents a type in source form vs. in compiled form at runtime.
 *
 * <p>Sadly, since the types here may not be available as actual {@code java.lang.Class} tokens,
 * this type cannot be parameterized the way that {@code java.lang.Class} is.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see java.lang.Class
 */
// TODO: javadoc!!
public abstract class Class implements AnnotatedElement, GenericDeclaration, Type {

   /**
    * An enumeration of the various kinds of classes.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public enum Kind {
      /**
       * A class. This represents a type that is not an interface, not an enum, not an annotation
       * type, not an array, and not a primitive.
       */
      CLASS("class "),
      
      /**
       * An interface. This represents a normal {@code interface}; an {@code @interface} is
       * represented by {@link #ANNOTATION_TYPE}.
       */
      INTERFACE("interface "),
      
      /**
       * An enum.
       */
      ENUM("enum "),
      
      /**
       * An annotation type (aka {@code @interface}).
       */
      ANNOTATION_TYPE("@interface "),
      
      /**
       * An array type.
       */
      ARRAY("class "),
      
      /**
       * A primitive type. Though {@code void} is not technically a primitive, the {@link Class}
       * that represents {@code void} will be of this kind.
       */
      PRIMITIVE("");
      
      private final String classStringPrefix;
      Kind(String classStringPrefix) {
         this.classStringPrefix = classStringPrefix;
      }
      
      /**
       * A string that can be used to to prefix the class's name to get a declaration string, such
       * as {@code "class MyClass"}, {@code "enum MyEnum"}, or {@code "@interface MyAnnotation"}.
       * 
       * @return a prefix based on the kind of class
       */
      public String getClassStringPrefix() {
         return classStringPrefix;
      }
   }
   
   private static final String CONSTRUCTOR_NAME = "<init>";
   
   /** Restricts sub-classes to this package. */
   Class() {}
   
   /**
    * Returns the {@link Class} for the given fully-qualified class name. Unlike its analog,
    * {@link java.lang.Class#forName(String) java.lang.Class.forName(String)}, this expects a
    * canonical name for a class (so inner types are offset from their enclosing class with a dot,
    * not a dollar sign).
    * 
    * @param name the name of the class
    * @return the class
    * @throws ClassNotFoundException if no type element could be located for the specified class
    *       name
    */
   public static Class forName(String name) throws ClassNotFoundException {
      TypeElement element = elements().getTypeElement(name);
      if (element == null) {
         throw new ClassNotFoundException(name);
      }
      return Class.forElement(element);
   }
   
   /**
    * Returns a {@link Class} that corresponds to the specified {@link Type}. If the type is a
    * simple class, it is returned. If it is a parameterized type, its raw type is returned. If it
    * is a wildcard type or a type variable then its upper-bound is returned (the first upper-bound
    * in the case of a type variable that has more than one). If the type is a generic array type
    * then an array class is returned whose component is a resolved class (using the same methods
    * above to resolve a type into a class).
    * 
    * @param t the type
    * @return the corresponding class
    */
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
   
   /**
    * Returns a {@link Class} object that corresponds to the specified {@link java.lang.Class
    * java.lang.Class} token.
    * 
    * @param clazz the runtime class token
    * @return the corresponding class
    * @throws ClassNotFoundException if the specified class token is not on the compiler's main
    * class path, and thus cannot be loaded as an element by the processing environment 
    */
   public static Class forJavaLangClass(java.lang.Class<?> clazz) throws ClassNotFoundException {
      if (clazz.isPrimitive()) {
         return new PrimitiveClass(clazz);
      } else if (clazz.isArray()) {
         return new ArrayClass(forJavaLangClass(clazz.getComponentType()));
      } else {
         return forName(clazz.getCanonicalName());
      }
   }
   
   /**
    * Creates a class corresponding to the specified {@link TypeElement}.
    * 
    * @param element the element
    * @return the corresponding class
    * @throws NullPointerException if the specified element is null
    */
   public static Class forElement(TypeElement element) {
      return new DeclaredClass(element);
   }
   
   /**
    * Creates a class corresponding to the specified type mirror.
    * 
    * @param mirror the type mirror
    * @return a corresponding class
    * @throws NullPointerException if the specified type mirror is null
    */
   public static Class forTypeMirror(TypeMirror mirror) {
      return forType(Types.forTypeMirror(mirror));
   }
   
   /**
    * Returns a class that represents and array with the specified component type.
    * 
    * @param componentType the component type
    * @return the array class
    * @throws NullPointerException if the specified component type is null
    */
   public static Class forArray(Class componentType) {
      return new ArrayClass(componentType);
   }
   
   /**
    * Returns a class that represents the specified primitive type.
    * 
    * @param primitiveClass the primitive type
    * @return the corresponding class
    * @throws NullPointerException if the specified type is null
    * @throws IllegalArgumentException if the specified type is not a {@linkplain
    *       java.lang.Class#isPrimitive() primitive} type
    */
   public static Class forPrimitive(java.lang.Class<?> primitiveClass) {
      if (!primitiveClass.isPrimitive()) {
         throw new IllegalArgumentException("Class " + primitiveClass + " is not a primitive");
      }
      return new PrimitiveClass(primitiveClass);
   }
   
   /**
    * Returns the class for {@code java.lang.Object}.
    * @return the class for {@code java.lang.Object}
    */
   static Class forJavaLangObject() {
      try {
         return forName("java.lang.Object");
      } catch (ClassNotFoundException e) {
         // really should never happen
         throw new AssertionError("Failed to create class for java.lang.Object");
      }
   }
   
   // These fields are used to make accessor methods for the class's fields, methods, and
   // constructors faster than always querying for details from the underlying type element and
   // filtering every time. They are initialized lazily.
   
   private volatile Map<String, Field> declaredFields;
   private volatile Map<String, Field> allPublicFields;
   private volatile Map<MethodSignature, Method> declaredMethods;
   private volatile Map<MethodSignature, Method> allPublicMethods;
   private volatile Map<MethodSignature, Constructor> constructors;
   private volatile Map<MethodSignature, Constructor> publicConstructors;
   
   /**
    * Returns a map of the class's declared fields. This method will construct the map lazily on
    * first access.
    * 
    * @return a map of field names to field objects
    * @see #declaredFields
    */
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

   /**
    * Returns a map of the class's public fields, including those inherited from ancestors. This
    * method will construct the map lazily on first access.
    * 
    * @return a map of field names to field objects
    * @see #allPublicFields
    */
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

   /**
    * Returns a map of the class's declared methods. This method will construct the map lazily on
    * first access.
    * 
    * @return a map of method signatures to method objects
    * @see #declaredMethods
    */
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
   
   /**
    * Returns a map of the class's public methods, including those inherited from ancestors. This
    * method will construct the map lazily on first access.
    * 
    * @return a map of method signatures to method objects
    * @see #allPublicMethods
    */
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

   /**
    * Returns a map of the class's constructors. This method will construct the map lazily on
    * first access. Note that the method name used in map keys will be {@link #CONSTRUCTOR_NAME}.
    * 
    * @return a map of method signatures to constructor objects
    * @see #constructors
    */
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

   /**
    * Returns a map of the class's public constructors. This method will construct the map lazily on
    * first access. Note that the method name used in map keys will be {@link #CONSTRUCTOR_NAME}.
    * 
    * <p>This is nearly the same as {@link #getMapOfConstructors()} with the only difference being
    * that this method filters the constructors to only return public ones.
    * 
    * @return a map of method signatures to constructor objects
    * @see #publicConstructors
    */
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
   
   /**
    * Returns true if this class object represents an interface. Annotation types are considered
    * interfaces, so this returns true for annotation types, too.
    * 
    * @return true if this class is an interface; false otherwise
    * 
    * @see java.lang.Class#isInterface()
    */
   public boolean isInterface() {
      // default
      return false;
   }

   /**
    * Returns true if this class object represents an enum.
    * 
    * @return true if this class is an enum; false otherwise
    * 
    * @see java.lang.Class#isEnum()
    */
   public boolean isEnum() {
      // default
      return false;
   }
   
   /**
    * Returns true if this class object represents an annotation type.
    * 
    * @return true if this class is an annotation type; false otherwise
    * 
    * @see java.lang.Class#isAnnotation()
    */
   public boolean isAnnotation() {
      // default
      return false;
   }

   /**
    * Returns true if this class object represents an array type. If true then the array's component
    * type can be accessed using {@link #getComponentType()}.
    * 
    * @return true if this class is an array type; false otherwise
    * 
    * @see java.lang.Class#isArray()
    */
   public boolean isArray() {
      // default
      return false;
   }
   
   /**
    * Returns true if this class objects represents one of the nine non-reference types. These
    * include the eight primitive value types ({@code boolean}, {@code byte}, {@code char},
    * {@code double}, {@code float}, {@code integer}, {@code long}, and {@code short}) and
    * {@code void}.
    * 
    * @return true if this class is a primitive type; false otherwise
    * 
    * @see java.lang.Class#isPrimitive()
    */
   public boolean isPrimitive() {
      // default
      return false;
   }
   
   /**
    * Returns true if this is an anonymous inner class. If true then its enclosing class, method,
    * or constructor (depending on where it was defined) can be accessed using other methods:
    * {@link #getEnclosingClass()}, {@link #getEnclosingMethod()}, or {@link #getEnclosingConstructor()}.
    * 
    * @return true if this class is an anonymous inner class; false otherwise
    * 
    * @see java.lang.Class#isAnonymousClass()
    */
   public boolean isAnonymousClass() {
      // default
      return false;
   }
   
   /**
    * Returns true if this is a local class. If true then its enclosing method or constructor
    * (depending on where it was defined) can be accessed using other methods:
    * {@code #getEnclosingMethod()} or {@link #getEnclosingConstructor()}.
    * 
    * @return true if this class is a local class; false otherwise
    * 
    * @see java.lang.Class#isLocalClass()
    */
   public boolean isLocalClass() {
      // default
      return false;
   }
   
   /**
    * Returns true if this is a member class. If true then its enclosing class can be accessed using
    * {@link #getEnclosingClass()}.
    * 
    * @return true if this class is a member class; false otherwise
    * 
    * @see java.lang.Class#isMemberClass()
    */
   public boolean isMemberClass() {
      // default
      return false;
   }

   /**
    * Returns the kind of this class object.
    * 
    * @return the kind of this class
    */
   public abstract Kind getClassKind();
   
   /**
    * Returns the name of this class. This will be a fully-qualified name that includes
    * dot-separated package components.
    * 
    * <p>If this is not a top-level class (i.e. it is an anonymous, local, or member class) then
    * this will be the fully-qualified name of the enclosing class, followed by a dollar sign, and
    * then followed by this class's simple name (or a generated name if this is an anonymous class).
    * 
    * <p>Array types have an encoded name that is "[" followed by the encoded name of the array's
    * component type. The eight primitive value types are encoded with single letters:
    * <table>
    * <tr><td>{@code boolean}</td><td>Z</td></tr>
    * <tr><td>{@code byte}</td><td>B</td></tr>
    * <tr><td>{@code char}</td><td>C</td></tr>
    * <tr><td>{@code double}</td><td>D</td></tr>
    * <tr><td>{@code float}</td><td>F</td></tr>
    * <tr><td>{@code int}</td><td>I</td></tr>
    * <tr><td>{@code long}</td><td>J</td></tr>
    * <tr><td>{@code short}</td><td>S</td></tr>
    * </table>
    * Reference types are encoded with an "L" followed by the type's fully-qualified name and then a
    * terminating ";".
    * 
    * @return the fully-qualified name of this class
    * 
    * @see java.lang.Class#getName()
    */
   public abstract String getName();

   /**
    * Returns the simple name of this class. This is the name of the class with no package or
    * enclosing class qualifiers. For top-level classes in the default package and the nine
    * primitive types (which do not belong to a package), the simple name is the same as the
    * fully-qualified name.
    * 
    * <p>The simple name of an anonymous class is the empty string. The simple name of an array
    * type is the simple name of its component type with "[]" appended to it.
    *  
    * @return the simple name of this class
    * 
    * @see java.lang.Class#getSimpleName()
    */
   public abstract String getSimpleName();

   /**
    * Returns the canonical name of this class. The canonical name is a fully-qualified name that is
    * also the name used to reference this class in Java source code. This differs from the
    * fully-qualified name returned by {@link #getName()} only for anonymous, local, and member
    * classes -- whose canonical name will have a dot to offset its simple name from its enclosing
    * class name instead of a dollar sign.
    * 
    * <p>The canonical name of an anonymous or local class is {@code null}. The canonical name of
    * an array type is the component type's canonical name followed by "[]". If the array component
    * type's canonical name is {@code null} then the array type's canonical name is also {@code null}.
    * 
    * @return the canonical name of this class
    * 
    * @see java.lang.Class#getCanonicalName()
    */
   public abstract String getCanonicalName();
   
   /**
    * Returns all modifiers that were on the declaration of this class. If the class has no
    * visibility modifiers (and thus has "default" access, also known as "package private"), then
    * the returned set will include the pseudo-modifier {@link Modifier#PACKAGE_PRIVATE}.
    * 
    * For array types, this will always be a set containing {@link Modifier#FINAL} and another
    * modifier describing the component type's visibility (one of {@link Modifier#PUBLIC},
    * {@link Modifier#PROTECTED}, {@link Modifier#PACKAGE_PRIVATE}, or {@link Modifier#PRIVATE}).
    * 
    * <p>For primitive types, this will always be a set containing both {@link Modifier#PUBLIC} and
    * {@link Modifier#FINAL}.
    * 
    * @return the class modifiers
    * 
    * @see java.lang.Class#getModifiers()
    */
   public abstract EnumSet<Modifier> getModifiers();
   
   /**
    * Returns the package in which this class is declared. For array types, this will be the package
    * of the component type. For primitive types, this will be null.
    * 
    * @return the package in which this class is declared
    * 
    * @see java.lang.Class#getPackage()
    */
   public abstract Package getPackage();
   
   /**
    * Returns the enclosing class of which this class is a member. If this is not a member class
    * then {@code null} is returned.
    * 
    * @return the class in which this class was declared or {@code null} if this is not a member
    *       class
    * 
    * @see java.lang.Class#getDeclaringClass()
    */
   public Class getDeclaringClass() {
      // default
      return null;
   }
   
   /**
    * Returns the enclosing class that contains this class. If this is a top-level class (i.e. it
    * is not an anonymous, local, or member class) then {@code null} is returned.
    * 
    * @return the class in which this class is enclosed or {@code null} if this is a top-level
    *       class
    *       
    * @see java.lang.Class#getEnclosingClass()
    */
   public Class getEnclosingClass() {
      // default
      return null;
   }
   
   /**
    * Returns the constructor in which this class was declared. If this class is not an anonymous
    * or local class that was declared inside a constructor then {@code null} is returned.
    * 
    * @return the constructor in which this class was declared or {@code null} if this class was
    *       not declared in a constructor
    *       
    * @see java.lang.Class#getEnclosingConstructor()
    */
   public Constructor getEnclosingConstructor() {
      // default
      return null;
   }
   
   /**
    * Returns the method in which this class was declared. If this class is not an anonymous
    * or local class that was declared inside a method then {@code null} is returned.
    * 
    * @return the method in which this class was declared or {@code null} if this class was
    *       not declared in a method
    *       
    * @see java.lang.Class#getEnclosingMethod()
    */
   public Method getEnclosingMethod() {
      // default
      return null;
   }

   /**
    * Returns a list of declared member types for this class. The returned types are in the order
    * that they were declared in Java source for the class.
    * 
    * @return the list of declared member types
    * 
    * @see java.lang.Class#getDeclaredClasses()
    */
   public List<Class> getDeclaredClasses() {
      // default
      return Collections.emptyList();
   }
   
   /**
    * Returns a set of all public member types for this class, including those that may have been
    * inherited from ancestor classes. Iteration over the set will return member types in no
    * particular order.
    * 
    * @return the set of public member types
    * 
    * @see java.lang.Class#getClasses()
    */
   public Set<Class> getClasses() {
      // default
      return Collections.emptySet();
   }
   
   /**
    * Returns an array class's component type. If this class is not an array type then {@code null}
    * is returned.
    * 
    * @return the array component type or {@code null} if this is not an array type
    * 
    * @see java.lang.Class#getComponentType()
    */
   public Class getComponentType() {
      // default
      return null;
   }
   
   /**
    * Returns the declared constructors for this class. This is used internally to build the cached
    * map of constructors, for improving the performance of public methods related to querying for
    * the class's constructors.
    * 
    * @return the declared constructors for this class
    * 
    * @see #getDeclaredConstructor(Class...)
    * @see #getConstructor(Class...)
    */
   List<Constructor> getDeclaredConstructorsInternal() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the list of declared constructors for this class. The list is in order of declaration
    * in the Java source for the class.
    * 
    * @return the list of declared constructors for this class
    * 
    * @see java.lang.Class#getDeclaredConstructors()
    */
   public List<Constructor> getDeclaredConstructors() {
      return Collections.unmodifiableList(new ArrayList<Constructor>(getMapOfConstructors().values()));
   }
   
   /**
    * Returns the constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such constructor exists
    * 
    * @see java.lang.Class#getDeclaredConstructor(java.lang.Class...)
    */
   public Constructor getDeclaredConstructor(Class... parameterTypes) throws NoSuchMethodException {
      return getDeclaredConstructor(Arrays.asList(parameterTypes));
   }
   
   /**
    * Returns the constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such constructor exists
    * 
    * @see java.lang.Class#getDeclaredConstructor(java.lang.Class...)
    */
   public Constructor getDeclaredConstructor(List<Class> parameterTypes) throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(CONSTRUCTOR_NAME, parameterTypes);
      Constructor ret = getMapOfConstructors().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }
   
   /**
    * Returns the list of public constructors for this class. Iteration over the returned set will
    * return constructors in no particular order.
    * 
    * @return the set of public constructors for this class
    * 
    * @see java.lang.Class#getConstructors()
    */
   public Set<Constructor> getConstructors() {
      return Collections.unmodifiableSet(new HashSet<Constructor>(getMapOfPublicConstructors().values()));
   }
   
   /**
    * Returns the public constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the public constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such public constructor exists
    * 
    * @see java.lang.Class#getConstructor(java.lang.Class...)
    */
   public Constructor getConstructor(Class... parameterTypes) throws NoSuchMethodException {
      return getConstructor(Arrays.asList(parameterTypes));
   }
   
   /**
    * Returns the public constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the public constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such public constructor exists
    * 
    * @see java.lang.Class#getConstructor(java.lang.Class...)
    */
   public Constructor getConstructor(List<Class> parameterTypes) throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(CONSTRUCTOR_NAME, parameterTypes);
      Constructor ret = getMapOfPublicConstructors().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }
   
   /**
    * Returns the declared fields for this class. This is used internally to build the cached
    * map of fields, for improving the performance of public methods related to querying for
    * the class's fields.
    * 
    * @return the declared fields for this class
    * 
    * @see #getDeclaredField(String)
    * @see #getField(String)
    */
   List<Field> getDeclaredFieldsInternal() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the list of declared fields for this class. The list is in order of declaration
    * in the Java source for the class.
    * 
    * @return the list of declared fields for this class
    * 
    * @see java.lang.Class#getDeclaredFields()
    */
   public List<Field> getDeclaredFields() {
      return Collections.unmodifiableList(new ArrayList<Field>(getMapOfDeclaredFields().values()));
   }

   /**
    * Returns the declared field with the specified name.
    * 
    * @param name the field name
    * @return the field for this class that has the specified name
    * @throws NoSuchFieldException if no such field exists
    * 
    * @see java.lang.Class#getDeclaredField(String)
    */
   public Field getDeclaredField(String name) throws NoSuchFieldException {
      Field ret = getMapOfDeclaredFields().get(name);
      if (ret == null) {
         throw new NoSuchFieldException(name);
      }
      return ret;
   }
   
   /**
    * Returns the list of all public fields for this class, including those inherited from ancestor
    * classes. Iteration over the returned set will return fields in no particular order.
    * 
    * @return the list of all public fields for this class
    * 
    * @see java.lang.Class#getFields()
    */
   public Set<Field> getFields() {
      return Collections.unmodifiableSet(new HashSet<Field>(getMapOfAllPublicFields().values()));
   }
   
   /**
    * Returns the public field with the specified name. The field might be declared on an ancestor
    * class.
    * 
    * @param name the field name
    * @return the public field for this class that has the specified name
    * @throws NoSuchFieldException if no such field exists
    * 
    * @see java.lang.Class#getField(String)
    */
   public Field getField(String name) throws NoSuchFieldException {
      Field ret = getMapOfAllPublicFields().get(name);
      if (ret == null) {
         throw new NoSuchFieldException(name);
      }
      return ret;
   }

   /**
    * Returns the list of enum constants for this class. If this class is not an enum type then the
    * list will be empty. The items will be in the order that they are declared. Enum constants are
    * a subset of the static declared fields for a given class.
    * 
    * @return the list of enum constants for this class
    * 
    * @see java.lang.Class#getEnumConstants()
    */
   public List<Field> getEnumConstants() {
      List<Field> fields = new ArrayList<Field>();
      for (Field field : getMapOfDeclaredFields().values()) {
         if (field.isEnumConstant()) {
            fields.add(field);
         }
      }
      return Collections.unmodifiableList(fields);
   }
   
   /**
    * Returns the declared methods for this class. This is used internally to build the cached
    * map of methods, for improving the performance of public methods related to querying for
    * the class's methods.
    * 
    * @return the declared constructors for this class
    * 
    * @see #getDeclaredMethod(String, Class...)
    * @see #getMethod(String, Class...)
    */
   List<Method> getDeclaredMethodsInternal() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the list of declared methods for this class. The list is in order of declaration
    * in the Java source for the class.
    * 
    * @return the list of declared methods for this class
    * 
    * @see java.lang.Class#getDeclaredMethods()
    */
   public List<Method> getDeclaredMethods() {
      return Collections.unmodifiableList(new ArrayList<Method>(getMapOfDeclaredMethods().values()));
   }

   /**
    * Returns the declared method with the specified signature.
    * 
    * @param name the method name
    * @param parameterTypes the parameter types
    * @return the method for this class that has the specified signature
    * @throws NoSuchMethodException if no such method exists
    * 
    * @see java.lang.Class#getDeclaredMethod(String, java.lang.Class...)
    */
   public Method getDeclaredMethod(String name, Class... parameterTypes)
         throws NoSuchMethodException {
      return getDeclaredMethod(name,  Arrays.asList(parameterTypes));
   }

   /**
    * Returns the declared method with the specified signature.
    * 
    * @param name the method name
    * @param parameterTypes the parameter types
    * @return the method for this class that has the specified signature
    * @throws NoSuchMethodException if no such method exists
    * 
    * @see java.lang.Class#getDeclaredMethod(String, java.lang.Class...)
    */
   public Method getDeclaredMethod(String name, List<Class> parameterTypes)
         throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(name, parameterTypes);
      Method ret = getMapOfDeclaredMethods().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }
   
   /**
    * Returns the list of all public methods for this class, including those inherited from ancestor
    * classes. Iteration over the returned set will return methods in no particular order.
    * 
    * @return the list of all public methods for this class
    * 
    * @see java.lang.Class#getMethods()
    */
   public Set<Method> getMethods() {
      return Collections.unmodifiableSet(new HashSet<Method>(getMapOfAllPublicMethods().values()));
   }
   
   /**
    * Returns the public method with the specified signature. The method might be declared on an
    * ancestor class.
    * 
    * @param name the method name
    * @param parameterTypes the parameter types
    * @return the public method for this class that has the specified signature
    * @throws NoSuchMethodException if no such method exists
    * 
    * @see java.lang.Class#getMethod(String, java.lang.Class...)
    */
   public Method getMethod(String name, Class... parameterTypes) throws NoSuchMethodException {
      return getMethod(name, Arrays.asList(parameterTypes));
   }

   /**
    * Returns the public method with the specified signature. The method might be declared on an
    * ancestor class.
    * 
    * @param name the method name
    * @param parameterTypes the parameter types
    * @return the public method for this class that has the specified signature
    * @throws NoSuchMethodException if no such method exists
    * 
    * @see java.lang.Class#getMethod(String, java.lang.Class...)
    */
   public Method getMethod(String name, List<Class> parameterTypes) throws NoSuchMethodException {
      MethodSignature signature = new MethodSignature(name, parameterTypes);
      Method ret = getMapOfAllPublicMethods().get(signature);
      if (ret == null) {
         throw new NoSuchMethodException(signature.toString());
      }
      return ret;
   }
   
   /**
    * Returns this class's super-class. If this class is an interface, a primitive type, or
    * {@code java.lang.Object} then {@code null} is returned. If this class is an array type then
    * the class representing {@code java.lang.Object} is returned.
    * 
    * @return the super-class
    * 
    * @see java.lang.Class#getSuperclass()
    */
   public abstract Class getSuperclass();

   /**
    * Returns this class's super-class with generic type information. If this class is an interface,
    * a primitive type, or {@code java.lang.Object} then {@code null} is returned. If this class is
    * an array type then the class representing {@code java.lang.Object} is returned. If the
    * super-class is a generic type then this method can return a {@link ParameterizedType} instead
    * of a {@link Class}.
    * 
    * @return the super-class
    * 
    * @see java.lang.Class#getGenericSuperclass()
    */
   public abstract Type getGenericSuperclass();
   
   /**
    * Returns the list of interfaces directly implemented by this class. The list is returned in
    * declaration order of the {@code implements} clause on the class. If this class object
    * represents an interface then this list contains any other interface types that are extended
    * by this interface.
    * 
    * <p>If this class does not implement any interfaces (or is a primitive type) then an empty list
    * is returned. If this class is an array type then a list is returned that contains two
    * interfaces: {@code java.lang.Cloneable} and {@code java.io.Serializable}.
    * 
    * @return the interfaces implemented or extended by this class object
    * 
    * @see java.lang.Class#getInterfaces()
    */
   public abstract List<Class> getInterfaces();
   
   /**
    * Returns the list of interfaces directly implemented by this class. The list is returned in
    * declaration order of the {@code implements} clause on the class. If this class object
    * represents an interface then this list contains any other interface types that are extended
    * by this interface. If any of the implemented interfaces is a generic type then this method can
    * return instances of {@link ParameterizedType} in the list (instead of or in addition to
    * instances of {@link Class}).
    * 
    * <p>If this class does not implement any interfaces (or is a primitive type) then an empty list
    * is returned. If this class is an array type then a list is returned that contains two
    * interfaces: {@code java.lang.Cloneable} and {@code java.io.Serializable}.
    * 
    * @return the interfaces implemented or extended by this class object
    * 
    * @see java.lang.Class#getGenericInterfaces()
    */
   public abstract List<? extends Type> getGenericInterfaces();
   
   /**
    * Returns true if the specified type is assignable to this type. This will be true when this
    * type is a super-type of the specified type.
    * 
    * @param clazz another type
    * @return true if the other type is assignable to this type
    * 
    * @see java.lang.Class#isAssignableFrom(java.lang.Class)
    */
   public abstract boolean isAssignableFrom(Class clazz);

   /**
    * Returns true if this class is a super-type of the specified class. This is synonymous with
    * being assignable from the specified class, which implies the following:
    * <pre>class1.isSuperTypeOf(class2) == class1.isAssignableFrom(class2)</pre>
    * 
    * @param clazz another type
    * @return true if this type is a super-type of the other type
    */
   public boolean isSuperTypeOf(Class clazz) {
      return isAssignableFrom(clazz);
   }

   /**
    * Returns true if this class is a sub-type of the specified class. This is synonymous with
    * being assignable to the specified class, which implies the following:
    * <pre>class1.isSubTypeOf(class2) == class2.isAssignableFrom(class1)</pre>
    * 
    * @param clazz another type
    * @return true if this type is a sub-type of the other type
    */
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
   public <T extends java.lang.annotation.Annotation> T getAnnotationBridge(
         java.lang.Class<T> annotationClass) {
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
   
   /**
    * Returns a {@link java.lang.Class java.lang.Class} token for this class. The current thread's
    * context class loader is used in the attempt to load the class.
    * 
    * @return a {@code java.lang.Class}
    * @throws ClassNotFoundException if a class cannot be loaded (which means it does not exist
    *       in compiled form on the classpath or in a fashion that is available/visible to the
    *       current thread's context class loader)
    */
   public abstract java.lang.Class<?> asJavaLangClass() throws ClassNotFoundException;
   
   /**
    * Returns a {@link java.lang.Class java.lang.Class} token for this class. The specified class
    * loader is used in the attempt to load the class.
    * 
    * @param classLoader a class loader
    * @return a {@code java.lang.Class}
    * @throws ClassNotFoundException if no such class could be loaded by the specified class loader
    */
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
         int l = sb.length();
         TypeVariable.appendTypeParameters(sb, getTypeVariables());
         if (sb.length() != l) {
            sb.append(" ");
         }
      }
      return sb.toString();
   }

   /**
    * A concrete sub-class of {@link Class} for representing the nine primitive types.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class PrimitiveClass extends Class {
      
      private static Map<java.lang.Class<?>, TypeMirror> typeMirrorMap;
      static {
         typeMirrorMap = new HashMap<java.lang.Class<?>, TypeMirror>(9);
         typeMirrorMap.put(void.class, types().getNoType(TypeKind.VOID));
         typeMirrorMap.put(boolean.class, types().getPrimitiveType(TypeKind.BOOLEAN));
         typeMirrorMap.put(byte.class, types().getPrimitiveType(TypeKind.BYTE));
         typeMirrorMap.put(char.class, types().getPrimitiveType(TypeKind.CHAR));
         typeMirrorMap.put(double.class, types().getPrimitiveType(TypeKind.DOUBLE));
         typeMirrorMap.put(float.class, types().getPrimitiveType(TypeKind.FLOAT));
         typeMirrorMap.put(int.class, types().getPrimitiveType(TypeKind.INT));
         typeMirrorMap.put(long.class, types().getPrimitiveType(TypeKind.LONG));
         typeMirrorMap.put(short.class, types().getPrimitiveType(TypeKind.SHORT));
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
      public Class getSuperclass() {
         return null;
      }

      @Override
      public Type getGenericSuperclass() {
         return null;
      }

      @Override
      public List<Class> getInterfaces() {
         return Collections.emptyList();
      }
      
      @Override
      public List<? extends Type> getGenericInterfaces() {
         return Collections.emptyList();
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
   
   /**
    * A concrete sub-class of {@link Class} for representing array types.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
         if (componentClass == null) {
            throw new NullPointerException();
         }
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
            ret = types().getArrayType(ret);
         }
         return ret;
      }

      @Override
      public Class getSuperclass() {
         return forJavaLangObject();
      }

      @Override
      public Type getGenericSuperclass() {
         return forJavaLangObject();
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
   
   /**
    * A concrete sub-class of {@link Class} for representing declared reference types (basically
    * anything other than an array type or primitive type).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class DeclaredClass extends Class {
      
      private static final Map<ElementKind, Kind> kinds =
            new EnumMap<ElementKind, Class.Kind>(ElementKind.class);
      static {
         // array and primitive types not here since they aren't represented by this class
         kinds.put(ElementKind.ANNOTATION_TYPE, Kind.ANNOTATION_TYPE);
         kinds.put(ElementKind.ENUM, Kind.ENUM);
         kinds.put(ElementKind.INTERFACE, Kind.INTERFACE);
         kinds.put(ElementKind.CLASS, Kind.CLASS);
      }
      
      // since we need to extend Class, we can't also extend AbstractAnnotatedElement, so we'll pick
      // up its implementations of AnnotatedElement methods by composition
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
               return "";
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
      public <T extends java.lang.annotation.Annotation> T getAnnotationBridge(
            java.lang.Class<T> annotationClass) {
         return annotatedElement.getAnnotationBridge(annotationClass);
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
         Kind kind = kinds.get(asElement().getKind());
         return kind == null ? Kind.CLASS : kind;
      }
      
      @Override
      public boolean isInterface() {
         ElementKind kind = asElement().getKind();
         return kind == ElementKind.INTERFACE || kind == ElementKind.ANNOTATION_TYPE;
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
      public Set<Class> getClasses() {
         List<TypeElement> types = ElementFilter.typesIn(elements().getAllMembers(asElement()));
         Set<Class> ret = new HashSet<Class>();
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
         return asJavaLangClass(Thread.currentThread().getContextClassLoader());
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
         PackageElement pkgElement = elements().getPackageOf(asElement());
         if (pkgElement == null) {
            return null;
         }
         return Package.forElement(pkgElement);
      }

      @Override
      public boolean isAssignableFrom(Class clazz) {
         return types().isAssignable(types().erasure(clazz.asTypeMirror()),
               types().erasure(asTypeMirror()));
      }

      @Override
      public boolean equals(Object o) {
         if (o instanceof DeclaredClass) {
            DeclaredClass clazz = (DeclaredClass) o;
            return types().isSameType(types().erasure(clazz.asTypeMirror()),
                  types().erasure(asTypeMirror()));
         }
         return false;
      }

      @Override
      public int hashCode() {
         return getName().hashCode();
      }
   }
}