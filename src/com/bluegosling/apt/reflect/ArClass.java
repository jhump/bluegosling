package com.bluegosling.apt.reflect;

import static com.bluegosling.apt.ProcessingEnvironments.elements;
import static com.bluegosling.apt.ProcessingEnvironments.types;

import com.bluegosling.tuples.Pair;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.TypeKindVisitor8;
import javax.lang.model.util.Types;

/**
 * A class (or interface, enum, or annotation type). This is analogous to {@link Class}
 * except that it represents a type in source form vs. in compiled form at runtime.
 *
 * <p>Sadly, since the types here may not be available as actual {@code Class} tokens,
 * this type cannot be parameterized the way that {@code Class} is.
 * 
 * <p>Unlike other {@link ArAnnotatedElement} implementations, it is possible for calls to
 * {@link ArClass#asElement()} to throw {@link UnsupportedOperationException}. This happens for
 * objects that represent primitive types and array types, which have no corresponding
 * {@link TypeElement}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @see Class
 * @see TypeElement
 */
public abstract class ArClass implements ArAnnotatedElement, ArGenericDeclaration {

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
       * A primitive type. Though {@code void} is not technically a primitive, the {@link ArClass}
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
      String getClassStringPrefix() {
         return classStringPrefix;
      }
   }
   
   private static final String CONSTRUCTOR_NAME = "<init>";
   
   /** Restricts sub-classes to this package. */
   ArClass() {}
   
   /**
    * Returns the {@link ArClass} for the given fully-qualified class name. Unlike its analog,
    * {@link Class#forName(String)}, this expects a canonical name for a class (so inner types are
    * separated from their enclosing class with a dot, not a dollar sign).
    * 
    * @param name the name of the class
    * @return the class
    * @throws ClassNotFoundException if no type element could be located for the specified class
    *       name
    */
   public static ArClass forName(String name) throws ClassNotFoundException {
      TypeElement element = elements().getTypeElement(name);
      if (element == null) {
         throw new ClassNotFoundException(name);
      }
      return ArClass.forElement(element);
   }
   
   /**
    * Returns a {@link ArClass} that corresponds to the specified {@link ArType}. If the type is a
    * simple class, it is returned. If it is a parameterized type, its raw type is returned. If it
    * is a wildcard type or a type variable then its upper-bound is returned (the first upper-bound
    * in the case of a type variable that has more than one). If the type is a generic array type
    * then an array class is returned whose component is a resolved class (using the same procedure
    * described above for resolving types into classes).
    * 
    * @param t the type
    * @return the corresponding class
    */
   public static ArClass forType(ArType t) {
      return forTypeMirror(t.asTypeMirror());
   }
   
   /**
    * Returns a {@link ArClass} object that corresponds to the specified {@link Class} token.
    * 
    * @param clazz the runtime class token
    * @return the corresponding class
    * @throws ClassNotFoundException if the specified class token is not on the compiler's main
    * class path, and thus cannot be loaded as an element by the processing environment 
    */
   public static ArClass forClass(Class<?> clazz) throws ClassNotFoundException {
      if (clazz.isPrimitive()) {
         return new PrimitiveClass(clazz);
      } else if (clazz.isArray()) {
         return new ArrayClass(forClass(clazz.getComponentType()));
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
   public static ArClass forElement(TypeElement element) {
      return new DeclaredClass(element);
   }
   
   /**
    * Creates a class corresponding to the specified type mirror.
    * 
    * @param mirror the type mirror
    * @return a corresponding class
    * @throws NullPointerException if the specified type mirror is null
    */
   public static ArClass forTypeMirror(TypeMirror mirror) {
      return mirror.accept(new TypeKindVisitor8<ArClass, Void>() {
         @Override
         public ArClass visitPrimitiveAsBoolean(PrimitiveType t, Void p) {
            return new PrimitiveClass(boolean.class);
         }

         @Override
         public ArClass visitPrimitiveAsByte(PrimitiveType t, Void p) {
            return new PrimitiveClass(byte.class);
         }

         @Override
         public ArClass visitPrimitiveAsShort(PrimitiveType t, Void p) {
            return new PrimitiveClass(short.class);
         }

         @Override
         public ArClass visitPrimitiveAsInt(PrimitiveType t, Void p) {
            return new PrimitiveClass(int.class);
         }

         @Override
         public ArClass visitPrimitiveAsLong(PrimitiveType t, Void p) {
            return new PrimitiveClass(long.class);
         }

         @Override
         public ArClass visitPrimitiveAsChar(PrimitiveType t, Void p) {
            return new PrimitiveClass(char.class);
         }

         @Override
         public ArClass visitPrimitiveAsFloat(PrimitiveType t, Void p) {
            return new PrimitiveClass(float.class);
         }

         @Override
         public ArClass visitPrimitiveAsDouble(PrimitiveType t, Void p) {
            return new PrimitiveClass(double.class);
         }

         @Override
         public ArClass visitNoTypeAsVoid(NoType t, Void p) {
            return new PrimitiveClass(void.class);
         }

         @Override
         public ArClass visitArray(ArrayType t, Void p) {
            ArClass component = forTypeMirror(t.getComponentType());
            return new ArrayClass(component);
         }

         @Override
         public ArClass visitDeclared(DeclaredType t, Void p) {
            return new DeclaredClass((TypeElement) t.asElement());
         }

         @Override
         public ArClass visitTypeVariable(TypeVariable t, Void p) {
            TypeMirror bound = t.getUpperBound();
            if (bound == null) {
               return forObject();
            } else if (bound.getKind() == TypeKind.INTERSECTION) {
               IntersectionType i = (IntersectionType) bound;
               return forTypeMirror(i.getBounds().get(0));
            } else {
               return forTypeMirror(bound);
            }
         }

         @Override
         public ArClass visitWildcard(WildcardType t, Void p) {
            TypeMirror bound = t.getExtendsBound();
            if (bound == null) {
               return forObject();
            } else {
               return forTypeMirror(bound);
            }
         }

         @Override
         protected ArClass defaultAction(TypeMirror e, Void p) {
            throw new MirroredTypeException(e);
         }
      }, null);
   }
   
   /**
    * Returns a class that represents an array with the specified component type.
    * 
    * @param componentType the component type
    * @return the array class
    * @throws NullPointerException if the specified component type is null
    */
   public static ArClass forArray(ArClass componentType) {
      return new ArrayClass(componentType);
   }
   
   /**
    * Memoized reference to the {@link ArClass} for {@code java.lang.Object} and the
    * {@link Elements} used to query and create it. We only use the memoized value when the
    * current {@link Elements} is the same (e.g. same compile context).
    */
   private static Pair<ArClass, Elements> memoizedJavaLangObject;
   
   /**
    * Returns the class for {@code Object}.
    * @return the class for {@code Object}
    */
   public static ArClass forObject() {
      // Use memoized version if we can. Not worrying about synchronization because
      // we don't require updates to be immediately visible to other threads. That
      // could improve the "hit rate" for being able to re-use existing object, but
      // it's not worth incurring a volatile access every time.
      Pair<ArClass, Elements> memoized = memoizedJavaLangObject;
      if (memoized != null && memoized.getSecond() == elements()) {
         return memoized.getFirst();
      }
      // different Elements means different compile context, so we should re-query
      try {
         ArClass javaLangObject = forName(Object.class.getCanonicalName());
         memoizedJavaLangObject = Pair.of(javaLangObject, elements());
         return javaLangObject;
      } catch (ClassNotFoundException e) {
         // really should never happen
         throw new AssertionError("Failed to create class for java.lang.Object");
      }
   }
   
   // These fields are used to make accessor methods for the class's fields, methods, and
   // constructors faster than always querying for details from the underlying type element and
   // filtering every time. They are initialized lazily. They are not volatile so that we don't
   // require volatile access on every read. If multiple threads interact with the same class, they
   // may duplicate the work to initialize the fields since writes to initialize the field in one
   // thread may not be visible to another (and that's okay).
   
   private Map<String, ArField> declaredFieldsMap;
   private List<ArField> declaredFieldsList;
   private List<ArField> enumConstantsList;
   private Map<String, ArField> allPublicFieldsMap;
   private Set<ArField> allPublicFieldsSet;
   
   private Map<ArMethodSignature, ArMethod> declaredMethodsMap;
   private List<ArMethod> declaredMethodsList;
   private Map<ArMethodSignature, ArMethod> allPublicMethods;
   private Set<ArMethod> allPublicMethodsSet;
   
   private Map<ArMethodSignature, ArConstructor> constructorsMap;
   private List<ArConstructor> constructorsList;
   private Map<ArMethodSignature, ArConstructor> publicConstructorsMap;
   private Set<ArConstructor> publicConstructorsSet;
   
   /**
    * Initializes the set of declared fields for this class.
    */
   private void initDeclaredFields() {
      declaredFieldsList = Collections.unmodifiableList(getDeclaredFieldsInternal());
      Map<String, ArField> map = new HashMap<String, ArField>(declaredFieldsList.size() * 4 / 3);
      ArrayList<ArField> enumList = new ArrayList<ArField>(declaredFieldsList.size());
      for (ArField field : declaredFieldsList) {
         map.put(field.getName(), field);
         if (field.isEnumConstant()) {
            enumList.add(field);
         }
      }
      declaredFieldsMap = Collections.unmodifiableMap(map);
      enumList.trimToSize();
      enumConstantsList = Collections.unmodifiableList(enumList);
   }
   
   /**
    * Initializes the set of public fields for this class. This crawls the type hierarchy to find
    * public fields of super-types.
    */
   private void initPublicFields() {
      ArrayDeque<ArClass> classes = new ArrayDeque<ArClass>();
      Set<ArClass> alreadySeen = new HashSet<ArClass>();
      Set<ArField> set = new LinkedHashSet<ArField>();
      Map<String, ArField> map = new HashMap<String, ArField>();
      // seed the queue with this type and its super-classes
      for (ArClass clazz = this; clazz != null; clazz = clazz.getSuperclass()) {
         classes.add(clazz);
      }
      // now crawl through the queue, adding interfaces as we go
      while (!classes.isEmpty()) {
         ArClass clazz = classes.remove();
         if (alreadySeen.add(clazz)) {
            classes.addAll(clazz.getInterfaces());
            for (ArField field : clazz.getDeclaredFields()) {
               if (field.getModifiers().contains(ArModifier.PUBLIC)
                     && !map.containsKey(field.getName())) {
                  map.put(field.getName(), field);
                  set.add(field);
               }
            }
         }
      }
      allPublicFieldsMap = Collections.unmodifiableMap(map);
      allPublicFieldsSet = Collections.unmodifiableSet(set); 
   }
   
   /**
    * Initializes the set of declared methods for this class.
    */
   private void initDeclaredMethods() {
      declaredMethodsList = Collections.unmodifiableList(getDeclaredMethodsInternal());
      Map<ArMethodSignature, ArMethod> map =
            new HashMap<ArMethodSignature, ArMethod>(declaredMethodsList.size() * 4 / 3);
      for (ArMethod method : declaredMethodsList) {
         map.put(new ArMethodSignature(method), method);
      }
      declaredMethodsMap = Collections.unmodifiableMap(map);
   }
   
   /**
    * Initializes the set of public methods for this class. This crawls the type hierarchy to find
    * public methods of super-types.
    */
   private void initPublicMethods() {
      ArrayDeque<ArClass> classes = new ArrayDeque<ArClass>();
      Set<ArClass> alreadySeen = new HashSet<ArClass>();
      Set<ArMethod> set = new LinkedHashSet<ArMethod>();
      Map<ArMethodSignature, ArMethod> map = new HashMap<ArMethodSignature, ArMethod>();
      // seed the queue with this type and its super-classes
      for (ArClass clazz = this; clazz != null; clazz = clazz.getSuperclass()) {
         classes.add(clazz);
      }
      // now crawl through the queue, adding interfaces as we go
      while (!classes.isEmpty()) {
         ArClass clazz = classes.remove();
         if (alreadySeen.add(clazz)) {
            classes.addAll(clazz.getInterfaces());
            for (ArMethod method : clazz.getDeclaredMethods()) {
               ArMethodSignature signature = new ArMethodSignature(method);
               if (method.getModifiers().contains(ArModifier.PUBLIC)
                     && !map.containsKey(signature)) {
                  map.put(signature, method);
                  set.add(method);
               }
            }
         }
      }
      allPublicMethods = Collections.unmodifiableMap(map);
      allPublicMethodsSet = Collections.unmodifiableSet(set);       
   }
   
   /**
    * Initializes the set of constructors for this class.
    */
   private void initConstructors() {
      constructorsList = Collections.unmodifiableList(getDeclaredConstructorsInternal());
      Set<ArConstructor> publicSet = new HashSet<ArConstructor>(constructorsList.size() * 4 / 3);
      Map<ArMethodSignature, ArConstructor> map =
            new HashMap<ArMethodSignature, ArConstructor>(constructorsList.size() * 4 / 3);
      for (ArConstructor constructor : constructorsList) {
         ArMethodSignature signature =
               new ArMethodSignature(CONSTRUCTOR_NAME, constructor.getParameterTypes());
         map.put(signature, constructor);
         if (constructor.getModifiers().contains(ArModifier.PUBLIC)) {
            publicSet.add(constructor);
         }
      }
      constructorsMap = Collections.unmodifiableMap(map);
      publicConstructorsSet = Collections.unmodifiableSet(publicSet);
   }
   
   /**
    * Returns a map of the class's declared fields. This method will construct the map lazily on
    * first access.
    * 
    * @return a map of field names to field objects
    * @see #declaredFieldsMap
    */
   private Map<String, ArField> getMapOfDeclaredFields() {
      if (declaredFieldsMap == null) {
         initDeclaredFields();
      }
      return declaredFieldsMap;
   }
   
   /**
    * Returns a map of the class's public fields, including those inherited from ancestors. This
    * method will construct the map lazily on first access.
    * 
    * @return a map of field names to field objects
    * @see #allPublicFieldsMap
    */
   private Map<String, ArField> getMapOfAllPublicFields() {
      if (allPublicFieldsMap == null) {
         initPublicFields();
      }
      return allPublicFieldsMap;
   }

   /**
    * Returns a map of the class's declared methods. This method will construct the map lazily on
    * first access.
    * 
    * @return a map of method signatures to method objects
    * @see #declaredMethodsMap
    */
   private Map<ArMethodSignature, ArMethod> getMapOfDeclaredMethods() {
      if (declaredMethodsMap == null) {
         initDeclaredMethods();
      }
      return declaredMethodsMap;
   }
   
   /**
    * Returns a map of the class's public methods, including those inherited from ancestors. This
    * method will construct the map lazily on first access.
    * 
    * @return a map of method signatures to method objects
    * @see #allPublicMethods
    */
   private Map<ArMethodSignature, ArMethod> getMapOfAllPublicMethods() {
      if (allPublicMethods == null) {
         initPublicMethods();
      }
      return allPublicMethods;
   }

   /**
    * Returns a map of the class's constructors. This method will construct the map lazily on
    * first access. Note that the method name used in map keys will be {@link #CONSTRUCTOR_NAME}.
    * 
    * @return a map of method signatures to constructor objects
    * @see #constructorsMap
    */
   private Map<ArMethodSignature, ArConstructor> getMapOfConstructors() {
      if (constructorsMap == null) {
         initConstructors();
      }
      return constructorsMap;
   }

   /**
    * Returns a map of the class's public constructors. This method will construct the map lazily on
    * first access. Note that the method name used in map keys will be {@link #CONSTRUCTOR_NAME}.
    * 
    * <p>This is nearly the same as {@link #getMapOfConstructors()} with the only difference being
    * that this method filters the constructors to only return public ones.
    * 
    * @return a map of method signatures to constructor objects
    * @see #publicConstructorsMap
    */
   private Map<ArMethodSignature, ArConstructor> getMapOfPublicConstructors() {
      if (publicConstructorsMap == null) {
         initConstructors();
      }
      return publicConstructorsMap;
   }
   
   /**
    * Returns true if this class object represents an interface. Annotation types are considered
    * interfaces, so this returns true for annotation types, too.
    * 
    * @return true if this class is an interface; false otherwise
    * 
    * @see Class#isInterface()
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
    * @see Class#isEnum()
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
    * @see Class#isAnnotation()
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
    * @see Class#isArray()
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
    * @see Class#isPrimitive()
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
    * @see Class#isAnonymousClass()
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
    * @see Class#isLocalClass()
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
    * @see Class#isMemberClass()
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
    * <p>Array types have an encoded name. It will begin with "[", followed by the encoded name of
    * the array's component type. The eight primitive value types are encoded with single letters:
    * <table>
    * <tr><td>{@code boolean}</td><td>Z</td></tr>
    * <tr><td>{@code byte}</td><td>B</td></tr>
    * <tr><td>{@code char}</td><td>C</td></tr>
    * <tr><td>{@code double}</td><td>D</td></tr>
    * <tr><td>{@code float}</td><td>F</td></tr>
    * <tr><td>{@code int}</td><td>I</td></tr>
    * <tr><td>{@code long}</td><td>J</td></tr>
    * <tr><td>{@code short}</td><td>S</td></tr>
    * <caption>Primitive type encoding</caption>
    * </table>
    * Reference types are encoded with an "L" followed by the type's fully-qualified name and then a
    * terminating ";".
    * 
    * @return the fully-qualified name of this class
    * 
    * @see Class#getName()
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
    * @see Class#getSimpleName()
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
    * @see Class#getCanonicalName()
    */
   public abstract String getCanonicalName();
   
   /**
    * Returns all modifiers that were on the declaration of this class. If the class has no
    * visibility modifiers (and thus has "default" access, also known as "package private"), then
    * the returned set will include the pseudo-modifier {@link ArModifier#PACKAGE_PRIVATE}.
    * 
    * For array types, this will always be a set containing {@link ArModifier#FINAL} and another
    * modifier describing the component type's visibility (one of {@link ArModifier#PUBLIC},
    * {@link ArModifier#PROTECTED}, {@link ArModifier#PACKAGE_PRIVATE}, or {@link ArModifier#PRIVATE}).
    * 
    * <p>For primitive types, this will always be a set containing both {@link ArModifier#PUBLIC} and
    * {@link ArModifier#FINAL}.
    * 
    * @return the class modifiers
    * 
    * @see Class#getModifiers()
    */
   public abstract EnumSet<ArModifier> getModifiers();
   
   /**
    * Returns the package in which this class is declared. For array types, this will be the package
    * of the component type. For primitive types, this will be null.
    * 
    * @return the package in which this class is declared
    * 
    * @see Class#getPackage()
    */
   public abstract ArPackage getPackage();
   
   /**
    * Returns the enclosing class of which this class is a member. If this is not a member class
    * then {@code null} is returned.
    * 
    * @return the class in which this class was declared or {@code null} if this is not a member
    *       class
    * 
    * @see Class#getDeclaringClass()
    */
   public ArClass getDeclaringClass() {
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
    * @see Class#getEnclosingClass()
    */
   public ArClass getEnclosingClass() {
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
    * @see Class#getEnclosingConstructor()
    */
   public ArConstructor getEnclosingConstructor() {
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
    * @see Class#getEnclosingMethod()
    */
   public ArMethod getEnclosingMethod() {
      // default
      return null;
   }

   /**
    * Returns a list of declared member types for this class. The returned types are in the order
    * that they were declared in Java source for the class.
    * 
    * @return the list of declared member types
    * 
    * @see Class#getDeclaredClasses()
    */
   public List<ArClass> getDeclaredClasses() {
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
    * @see Class#getClasses()
    */
   public Set<ArClass> getClasses() {
      // default
      return Collections.emptySet();
   }
   
   /**
    * Returns an array class's component type. If this class is not an array type then {@code null}
    * is returned.
    * 
    * @return the array component type or {@code null} if this is not an array type
    * 
    * @see Class#getComponentType()
    */
   public ArClass getComponentType() {
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
    * @see #getDeclaredConstructor(ArClass...)
    * @see #getConstructor(ArClass...)
    */
   List<ArConstructor> getDeclaredConstructorsInternal() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the list of declared constructors for this class. The list is in order of declaration
    * in the Java source for the class.
    * 
    * @return the list of declared constructors for this class
    * 
    * @see Class#getDeclaredConstructors()
    */
   public List<ArConstructor> getDeclaredConstructors() {
      if (constructorsList == null) {
         initConstructors();
      }
      return constructorsList;
   }
   
   /**
    * Returns the constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such constructor exists
    * 
    * @see Class#getDeclaredConstructor(Class...)
    */
   public ArConstructor getDeclaredConstructor(ArClass... parameterTypes) throws NoSuchMethodException {
      return getDeclaredConstructor(Arrays.asList(parameterTypes));
   }
   
   /**
    * Returns the constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such constructor exists
    * 
    * @see Class#getDeclaredConstructor(Class...)
    */
   public ArConstructor getDeclaredConstructor(List<ArClass> parameterTypes) throws NoSuchMethodException {
      ArMethodSignature signature = new ArMethodSignature(CONSTRUCTOR_NAME, parameterTypes);
      ArConstructor ret = getMapOfConstructors().get(signature);
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
    * @see Class#getConstructors()
    */
   public Set<ArConstructor> getConstructors() {
      if (publicConstructorsSet == null) {
         initConstructors();
      }
      return publicConstructorsSet;
   }
   
   /**
    * Returns the public constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the public constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such public constructor exists
    * 
    * @see Class#getConstructor(Class...)
    */
   public ArConstructor getConstructor(ArClass... parameterTypes) throws NoSuchMethodException {
      return getConstructor(Arrays.asList(parameterTypes));
   }
   
   /**
    * Returns the public constructor with the specified argument types.
    * 
    * @param parameterTypes the parameter types
    * @return the public constructor for this class that accepts the specified parameter types
    * @throws NoSuchMethodException if no such public constructor exists
    * 
    * @see Class#getConstructor(Class...)
    */
   public ArConstructor getConstructor(List<ArClass> parameterTypes) throws NoSuchMethodException {
      ArMethodSignature signature = new ArMethodSignature(CONSTRUCTOR_NAME, parameterTypes);
      ArConstructor ret = getMapOfPublicConstructors().get(signature);
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
   List<ArField> getDeclaredFieldsInternal() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the list of declared fields for this class. The list is in order of declaration
    * in the Java source for the class.
    * 
    * @return the list of declared fields for this class
    * 
    * @see Class#getDeclaredFields()
    */
   public List<ArField> getDeclaredFields() {
      if (declaredFieldsList == null) {
         initDeclaredFields();
      }
      return declaredFieldsList;
   }

   /**
    * Returns the declared field with the specified name.
    * 
    * @param name the field name
    * @return the field for this class that has the specified name
    * @throws NoSuchFieldException if no such field exists
    * 
    * @see Class#getDeclaredField(String)
    */
   public ArField getDeclaredField(String name) throws NoSuchFieldException {
      ArField ret = getMapOfDeclaredFields().get(name);
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
    * @see Class#getFields()
    */
   public Set<ArField> getFields() {
      if (allPublicFieldsSet == null) {
         initPublicFields();
      }
      return allPublicFieldsSet;
   }
   
   /**
    * Returns the public field with the specified name. The field might be declared on an ancestor
    * class.
    * 
    * @param name the field name
    * @return the public field for this class that has the specified name
    * @throws NoSuchFieldException if no such field exists
    * 
    * @see Class#getField(String)
    */
   public ArField getField(String name) throws NoSuchFieldException {
      ArField ret = getMapOfAllPublicFields().get(name);
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
    * @see Class#getEnumConstants()
    */
   public List<ArField> getEnumConstants() {
      if (enumConstantsList == null) {
         initDeclaredFields();
      }
      return enumConstantsList;
   }
   
   /**
    * Returns the declared methods for this class. This is used internally to build the cached
    * map of methods, for improving the performance of public methods related to querying for
    * the class's methods.
    * 
    * @return the declared constructors for this class
    * 
    * @see #getDeclaredMethod(String, ArClass...)
    * @see #getMethod(String, ArClass...)
    */
   List<ArMethod> getDeclaredMethodsInternal() {
      // default
      return Collections.emptyList();
   }

   /**
    * Returns the list of declared methods for this class. The list is in order of declaration
    * in the Java source for the class.
    * 
    * @return the list of declared methods for this class
    * 
    * @see Class#getDeclaredMethods()
    */
   public List<ArMethod> getDeclaredMethods() {
      if (declaredMethodsList == null) {
         initDeclaredMethods();
      }
      return declaredMethodsList;
   }

   /**
    * Returns the declared method with the specified signature.
    * 
    * @param name the method name
    * @param parameterTypes the parameter types
    * @return the method for this class that has the specified signature
    * @throws NoSuchMethodException if no such method exists
    * 
    * @see Class#getDeclaredMethod(String, Class...)
    */
   public ArMethod getDeclaredMethod(String name, ArClass... parameterTypes)
         throws NoSuchMethodException {
      return getDeclaredMethod(name, Arrays.asList(parameterTypes));
   }

   /**
    * Returns the declared method with the specified signature.
    * 
    * @param name the method name
    * @param parameterTypes the parameter types
    * @return the method for this class that has the specified signature
    * @throws NoSuchMethodException if no such method exists
    * 
    * @see Class#getDeclaredMethod(String, Class...)
    */
   public ArMethod getDeclaredMethod(String name, List<ArClass> parameterTypes)
         throws NoSuchMethodException {
      ArMethodSignature signature = new ArMethodSignature(name, parameterTypes);
      ArMethod ret = getMapOfDeclaredMethods().get(signature);
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
    * @see Class#getMethods()
    */
   public Set<ArMethod> getMethods() {
      if (allPublicMethodsSet == null) {
         initPublicMethods();
      }
      return allPublicMethodsSet;
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
    * @see Class#getMethod(String, Class...)
    */
   public ArMethod getMethod(String name, ArClass... parameterTypes) throws NoSuchMethodException {
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
    * @see Class#getMethod(String, Class...)
    */
   public ArMethod getMethod(String name, List<ArClass> parameterTypes) throws NoSuchMethodException {
      ArMethodSignature signature = new ArMethodSignature(name, parameterTypes);
      ArMethod ret = getMapOfAllPublicMethods().get(signature);
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
    * @see Class#getSuperclass()
    */
   public abstract ArClass getSuperclass();

   /**
    * Returns this class's super-class with generic type information. If this class is an interface,
    * a primitive type, or {@code java.lang.Object} then {@code null} is returned. If this class is
    * an array type then the class representing {@code java.lang.Object} is returned.
    * 
    * @return the super-class
    * 
    * @see Class#getGenericSuperclass()
    */
   public abstract ArDeclaredType getGenericSuperclass();
   
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
    * @see Class#getInterfaces()
    */
   public abstract List<ArClass> getInterfaces();
   
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
    * @see Class#getGenericInterfaces()
    */
   public abstract List<ArDeclaredType> getGenericInterfaces();
   
   /**
    * Returns true if the specified type is assignable to this type. This will be true when this
    * type is a super-type of the specified type.
    * 
    * @param clazz another type
    * @return true if the other type is assignable to this type
    * 
    * @see Class#isAssignableFrom(Class)
    */
   public abstract boolean isAssignableFrom(ArClass clazz);

   /**
    * Returns true if this class is a super-type of the specified class. This is synonymous with
    * being assignable from the specified class, which implies the following:
    * <pre>class1.isSuperTypeOf(class2) == class1.isAssignableFrom(class2)</pre>
    * 
    * @param clazz another type
    * @return true if this type is a super-type of the other type
    */
   public boolean isSuperTypeOf(ArClass clazz) {
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
   public boolean isSubTypeOf(ArClass clazz) {
      return clazz.isAssignableFrom(this);
   }

   @Override
   public List<ArTypeParameter<ArClass>> getTypeParameters() {
      // default
      return Collections.emptyList();
   }

   @Override
   public ArAnnotation getAnnotation(ArClass annotationClass) {
      // default
      return null;
   }

   @Override
   public ArAnnotation getAnnotation(
         Class<? extends Annotation> annotationClass) {
      // default
      return null;
   }

   @Override
   public <T extends Annotation> T getAnnotationBridge(
         Class<T> annotationClass) {
      // default
      return null;
   }

   @Override
   public List<ArAnnotation> getAnnotations() {
      // default
      return Collections.emptyList();
   }

   @Override
   public boolean isAnnotationPresent(ArClass annotationClass) {
      // default
      return false;
   }

   @Override
   public boolean isAnnotationPresent(
         Class<? extends Annotation> annotationClass) {
      // default
      return false;
   }

   @Override
   public List<ArAnnotation> getDeclaredAnnotations() {
      // default
      return Collections.emptyList();
   }
   
   /**
    * Returns a {@link Class} token for this class. The current thread's context class loader is
    * used in the attempt to load the class.
    * 
    * @return a {@code Class}
    * @throws ClassNotFoundException if a class cannot be loaded (which means it does not exist
    *       in compiled form on the classpath or in a fashion that is available/visible to the
    *       current thread's context class loader)
    */
   public abstract Class<?> asClass() throws ClassNotFoundException;
   
   /**
    * Returns a {@link Class} token for this class. The specified class loader is used in the
    * attempt to load the class.
    * 
    * @param classLoader a class loader
    * @return a {@code Class}
    * @throws ClassNotFoundException if no such class could be loaded by the specified class loader
    */
   public abstract Class<?> asClass(ClassLoader classLoader)
         throws ClassNotFoundException;
   
   @Override
   public TypeElement asElement() {
      throw new UnsupportedOperationException();
   }

   public abstract ArType asType();

   public abstract TypeMirror asTypeMirror();

   @Override
   public abstract boolean equals(Object o);

   @Override
   public abstract int hashCode();
   
   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getClassKind().getClassStringPrefix());
      sb.append(getName());
      int l = sb.length();
      ArTypeParameter.appendTypeParameters(sb, getTypeParameters());
      if (sb.length() != l) {
         sb.append(" ");
      }
      return sb.toString();
   }

   /**
    * A concrete sub-class of {@link ArClass} for representing the nine primitive types.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class PrimitiveClass extends ArClass {
      /**
       * Memoized reference to a map of primitive types to their corresponding type mirrors and the
       * {@link Types} used to query and create it. We only use the memoized value when the
       * current {@link Types} is the same (e.g. same compile context).
       */
      private static Pair<Map<Class<?>, TypeMirror>, Types> memoizedTypeMirrorMap;
      
      private static Map<Class<?>, TypeMirror> getTypeMirrorMap() {
         // Use memoized version if we can. Not worrying about synchronization because
         // we don't require updates to be immediately visible to other threads. That
         // could improve the "hit rate" for being able to re-use existing object, but
         // it's not worth incurring a volatile access every time.
         Pair<Map<Class<?>, TypeMirror>, Types> memoized = memoizedTypeMirrorMap;
         if (memoized != null && memoized.getSecond() == types()) {
            return memoized.getFirst();
         }
         // different Types means different compile context, so we should re-compute
         Map<Class<?>, TypeMirror> typeMirrorMap = new HashMap<Class<?>, TypeMirror>(12);
         typeMirrorMap.put(void.class, types().getNoType(TypeKind.VOID));
         typeMirrorMap.put(boolean.class, types().getPrimitiveType(TypeKind.BOOLEAN));
         typeMirrorMap.put(byte.class, types().getPrimitiveType(TypeKind.BYTE));
         typeMirrorMap.put(char.class, types().getPrimitiveType(TypeKind.CHAR));
         typeMirrorMap.put(double.class, types().getPrimitiveType(TypeKind.DOUBLE));
         typeMirrorMap.put(float.class, types().getPrimitiveType(TypeKind.FLOAT));
         typeMirrorMap.put(int.class, types().getPrimitiveType(TypeKind.INT));
         typeMirrorMap.put(long.class, types().getPrimitiveType(TypeKind.LONG));
         typeMirrorMap.put(short.class, types().getPrimitiveType(TypeKind.SHORT));
         memoizedTypeMirrorMap = Pair.of(typeMirrorMap, types());
         return typeMirrorMap;
      }
      
      private final Class<?> clazz;
      
      PrimitiveClass(Class<?> clazz) {
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
      public ArType asType() {
         return ArPrimitiveType.forTypeMirror(asTypeMirror());
      }
      
      @Override
      public TypeMirror asTypeMirror() {
         return getTypeMirrorMap().get(clazz);
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
      public EnumSet<ArModifier> getModifiers() {
         return EnumSet.of(ArModifier.PUBLIC, ArModifier.FINAL);
      }

      @Override
      public ArPackage getPackage() {
         return null;
      }
      
      @Override
      public ArClass getSuperclass() {
         return null;
      }

      @Override
      public ArDeclaredType getGenericSuperclass() {
         return null;
      }

      @Override
      public List<ArClass> getInterfaces() {
         return Collections.emptyList();
      }
      
      @Override
      public List<ArDeclaredType> getGenericInterfaces() {
         return Collections.emptyList();
      }

      @Override
      public boolean isAssignableFrom(ArClass otherClass) {
         return equals(otherClass);
      }

      @Override
      public Class<?> asClass() {
         return clazz;
      }

      @Override
      public Class<?> asClass(ClassLoader classLoader) {
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
    * A concrete sub-class of {@link ArClass} for representing array types.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ArrayClass extends ArClass {
      
      private static Map<Class<?>, Character> primitiveNameMap;
      static {
         primitiveNameMap = new HashMap<Class<?>, Character>(8);
         primitiveNameMap.put(boolean.class, 'Z');
         primitiveNameMap.put(byte.class, 'B');
         primitiveNameMap.put(char.class, 'C');
         primitiveNameMap.put(double.class, 'D');
         primitiveNameMap.put(float.class, 'F');
         primitiveNameMap.put(int.class, 'I');
         primitiveNameMap.put(long.class, 'J');
         primitiveNameMap.put(short.class, 'S');
      }

      private final ArClass componentClass;
      private final int dimensions;
      
      ArrayClass(ArClass componentClass) {
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

      private ArrayClass(ArClass componentClass, int dimensions) {
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
      public ArClass getComponentType() {
         if (dimensions == 1) {
            return componentClass;
         } else {
            return new ArrayClass(componentClass, dimensions - 1);
         }
      }
      
      @Override
      public ArType asType() {
         return ArArrayType.forTypeMirror((ArrayType) asTypeMirror());
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
      public ArClass getSuperclass() {
         return forObject();
      }

      @Override
      public ArDeclaredType getGenericSuperclass() {
         return ArDeclaredType.forObject();
      }

      @Override
      public List<ArClass> getInterfaces() {
         ArClass cloneable, serializable;
         try {
            cloneable = forClass(Cloneable.class);
         }
         catch (ClassNotFoundException e) {
            // really should never happen
            throw new AssertionError("Failed to create class for java.lang.Cloneable");
         }
         try {
            serializable = forClass(Serializable.class);
         }
         catch (ClassNotFoundException e) {
            // really should never happen
            throw new AssertionError("Failed to create class for java.io.Serializable");
         }
         return Arrays.asList(cloneable, serializable);
      }

      @Override
      public List<ArDeclaredType> getGenericInterfaces() {
         return Arrays.asList(getInterfaces().stream()
               .map(ArClass::asType)
               .toArray(ArDeclaredType[]::new));
      }
      
      @Override
      public String getName() {
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < dimensions; i++) {
            sb.append("[");
         }
         if (componentClass instanceof PrimitiveClass) {
            sb.append(primitiveNameMap.get(((PrimitiveClass) componentClass).asClass()));
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
      public EnumSet<ArModifier> getModifiers() {
         EnumSet<ArModifier> mods = EnumSet.of(ArModifier.FINAL);
         EnumSet<ArModifier> componentMods = componentClass.getModifiers();
         if (componentMods.contains(ArModifier.PUBLIC)) {
            mods.add(ArModifier.PUBLIC);
         } else if (componentMods.contains(ArModifier.PROTECTED)) {
            mods.add(ArModifier.PROTECTED);
         } else if (componentMods.contains(ArModifier.PACKAGE_PRIVATE)) {
            mods.add(ArModifier.PACKAGE_PRIVATE);
         } else if (componentMods.contains(ArModifier.PRIVATE)) {
            mods.add(ArModifier.PRIVATE);
         }
         return mods;
      }

      @Override
      public ArPackage getPackage() {
         return componentClass.getPackage();
      }

      @Override
      public boolean isAssignableFrom(ArClass clazz) {
         if (clazz instanceof ArrayClass) {
            ArrayClass other = (ArrayClass) clazz;
            return (dimensions == other.dimensions && componentClass.isAssignableFrom(other.componentClass))
                  || (dimensions < other.dimensions && componentClass.equals(forObject()));
         }
         return false;
      }

      @Override
      public Class<?> asClass() throws ClassNotFoundException {
         return Array.newInstance(getComponentType().asClass(), 0).getClass();
      }

      @Override
      public Class<?> asClass(ClassLoader classLoader)
            throws ClassNotFoundException {
         return Array.newInstance(getComponentType().asClass(classLoader), 0).getClass();
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
    * A concrete sub-class of {@link ArClass} for representing declared reference types (basically
    * anything other than an array type or primitive type).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class DeclaredClass extends ArClass {
      
      private static final Map<ElementKind, Kind> kinds =
            new EnumMap<ElementKind, ArClass.Kind>(ElementKind.class);
      static {
         // array and primitive types not here since they aren't represented by this class
         kinds.put(ElementKind.ANNOTATION_TYPE, Kind.ANNOTATION_TYPE);
         kinds.put(ElementKind.ENUM, Kind.ENUM);
         kinds.put(ElementKind.INTERFACE, Kind.INTERFACE);
         kinds.put(ElementKind.CLASS, Kind.CLASS);
      }
      
      // since we need to extend Class, we can't also extend AbstractAnnotatedElement, so we'll pick
      // up its implementations of AnnotatedElement methods by composition
      private final ArAbstractAnnotatedElement<TypeElement> annotatedElement;
      
      DeclaredClass(TypeElement element) {
         if (element == null) {
            throw new NullPointerException();
         }
         this.annotatedElement = new ArAbstractAnnotatedElement<TypeElement>(element) {
            // Dummy implementations below -- we won't use them. We just want to use the
            // implementations of ArAnnotatedElement interface methods that this object provides.
            
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
      public ArAnnotation getAnnotation(ArClass annotationClass) {
         return annotatedElement.getAnnotation(annotationClass);
      }

      @Override
      public ArAnnotation getAnnotation(
            Class<? extends Annotation> annotationClass) {
         return annotatedElement.getAnnotation(annotationClass);
      }

      @Override
      public <T extends Annotation> T getAnnotationBridge(
            Class<T> annotationClass) {
         return annotatedElement.getAnnotationBridge(annotationClass);
      }

      @Override
      public boolean isAnnotationPresent(ArClass annotationClass) {
         return annotatedElement.isAnnotationPresent(annotationClass);
      }

      @Override
      public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
         return annotatedElement.isAnnotationPresent(annotationClass);
      }

      @Override
      public List<ArAnnotation> getAnnotations() {
         return annotatedElement.getAnnotations();
      }

      @Override
      public List<ArAnnotation> getDeclaredAnnotations() {
         return annotatedElement.getDeclaredAnnotations();
      }

      @Override
      public TypeElement asElement() {
         return (TypeElement) annotatedElement.asElement();
      }

      @Override
      public ArType asType() {
         return ArDeclaredType.forTypeMirror((DeclaredType) asTypeMirror());
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
      List<ArConstructor> getDeclaredConstructorsInternal() {
         List<ExecutableElement> constructors = ElementFilter.constructorsIn(asElement().getEnclosedElements());
         List<ArConstructor> ret = new ArrayList<ArConstructor>(constructors.size());
         for (ExecutableElement constructor : constructors) {
            ret.add(ArConstructor.forElement(constructor));
         }
         return ret;
      }

      @Override
      List<ArField> getDeclaredFieldsInternal() {
         List<VariableElement> fields = ElementFilter.fieldsIn(asElement().getEnclosedElements());
         List<ArField> ret = new ArrayList<ArField>(fields.size());
         for (VariableElement field : fields) {
            ret.add(ArField.forElement(field));
         }
         return ret;
      }
      
      @Override
      List<ArMethod> getDeclaredMethodsInternal() {
         List<ExecutableElement> methods = ElementFilter.methodsIn(asElement().getEnclosedElements());
         List<ArMethod> ret = new ArrayList<ArMethod>(methods.size());
         for (ExecutableElement method : methods) {
            ret.add(ArMethod.forElement(method));
         }
         return ret;
      }

      @Override
      public ArClass getDeclaringClass() {
         if (isMemberClass()) {
            ArClass ret = ReflectionVisitors.CLASS_VISITOR.visit(asElement().getEnclosingElement());
            if (ret == null) {
               throw new AssertionError("Member class has enclosing element that is not enclosing class");
            }
            return ret;
         } else {
            return null;
         }
      }
      
      @Override
      public ArClass getEnclosingClass() {
         if (isMemberClass()) {
            return getDeclaringClass();
         } else if (isAnonymousClass() || isLocalClass()) {
            Element enclosingElement = asElement().getEnclosingElement();
            ArClass ret = ReflectionVisitors.CLASS_VISITOR.visit(enclosingElement);
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
      public ArConstructor getEnclosingConstructor() {
         if (isAnonymousClass() || isLocalClass()) {
            return ReflectionVisitors.CONSTRUCTOR_VISITOR.visit(asElement().getEnclosingElement());
         } else {
            return null;
         }
      }
      
      @Override
      public ArMethod getEnclosingMethod() {
         if (isAnonymousClass() || isLocalClass()) {
            return ReflectionVisitors.METHOD_VISITOR.visit(asElement().getEnclosingElement());
         } else {
            return null;
         }
      }
      
      @Override
      public Set<ArClass> getClasses() {
         List<TypeElement> types = ElementFilter.typesIn(elements().getAllMembers(asElement()));
         Set<ArClass> ret = new HashSet<ArClass>();
         for (TypeElement type : types) {
            ArClass clazz = forElement(type);
            if (clazz.isMemberClass() && clazz.getModifiers().contains(ArModifier.PUBLIC)) {
               ret.add(clazz);
            }
         }
         return ret;
      }
      
      @Override
      public List<ArClass> getDeclaredClasses() {
         List<TypeElement> types = ElementFilter.typesIn(asElement().getEnclosedElements());
         List<ArClass> ret = new ArrayList<ArClass>(types.size());
         for (TypeElement type : types) {
            ArClass clazz = forElement(type);
            if (clazz.isMemberClass()) {
               ret.add(clazz);
            }
         }
         return ret;
      }
      
      @Override
      public List<ArTypeParameter<ArClass>> getTypeParameters() {
         List<? extends TypeParameterElement> parameters = asElement().getTypeParameters();
         List<ArTypeParameter<ArClass>> ret = new ArrayList<>(parameters.size());
         for (TypeParameterElement parameter : parameters) {
            @SuppressWarnings("unchecked")
            ArTypeParameter<ArClass> p =
                  (ArTypeParameter<ArClass>) ArTypeParameter.forElement(parameter);
            ret.add(p);
         }
         return Collections.unmodifiableList(ret);
      }
      
      @Override
      public ArClass getSuperclass() {
         return forTypeMirror(asElement().getSuperclass());
      }

      @Override
      public ArDeclaredType getGenericSuperclass() {
         return ArDeclaredType.forTypeMirror((DeclaredType) asElement().getSuperclass());
      }
      
      @Override
      public List<ArClass> getInterfaces() {
         List<? extends TypeMirror> interfaces = asElement().getInterfaces();
         List<ArClass> ret = new ArrayList<>();
         for (TypeMirror iface : interfaces) {
            ret.add(forTypeMirror(iface));
         }
         return ret;
      }

      @Override
      public List<ArDeclaredType> getGenericInterfaces() {
         List<? extends TypeMirror> interfaces = asElement().getInterfaces();
         List<ArDeclaredType> ret = new ArrayList<>();
         for (TypeMirror iface : interfaces) {
            ret.add(ArDeclaredType.forTypeMirror((DeclaredType) iface));
         }
         return ret;
      }
      
      @Override
      public Class<?> asClass() throws ClassNotFoundException {
         return asClass(Thread.currentThread().getContextClassLoader());
      }
      
      @Override
      public Class<?> asClass(ClassLoader classLoader)
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
      public EnumSet<ArModifier> getModifiers() {
         return ArModifier.fromElementModifiers(asElement().getModifiers());
      }

      @Override
      public ArPackage getPackage() {
         PackageElement pkgElement = elements().getPackageOf(asElement());
         if (pkgElement == null) {
            return null;
         }
         return ArPackage.forElement(pkgElement);
      }

      @Override
      public boolean isAssignableFrom(ArClass clazz) {
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
