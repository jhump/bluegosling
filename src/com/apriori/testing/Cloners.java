package com.apriori.testing;

import static junit.framework.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Utility methods and constants for using and creating {@code Cloner}
 * instances.
 * 
 * @author jhumphries
 */
public class Cloners {

   /**
    * Checks that the clone is valid. This will assert that the clone
    * is <em>not</em> the same instance as the original object and
    * also assert that its class is identical to that of the
    * original.
    * 
    * <p>The utility method is used by implementations of {@code Cloner}
    * in this class and is recommended for use by other custom
    * implementations of {@code Cloner}.
    * 
    * @param orig    The original object
    * @param clone   The object's clone
    */
   public static void checkClone(Object orig, Object clone) {
      if (clone == orig || clone.getClass() != orig.getClass()) {
         fail();
      }
   }
   
   /**
    * Returns a cloner for cloning instances of {@code Cloneable}. This uses the
    * instance's {@code clone()} method but furthermore checks that the resulting
    * clone is the same class but not the same instance as the source instance.
    * So classes that implement {@code Cloneable}, but in non-standard ways, may
    * not be suitable and may cause exceptions to occur during cloning.
    * 
    * @param <T>     The type of object cloned
    * @return        A cloner that will use an object's {@code clone()} method to
    *                perform the cloning operation
    */
   public static <T extends Cloneable> Cloner<T> forCloneable() {
      return new Cloner<T>() {
         @Override
         public T clone(T o) {
            Method cloneMethod = findCloneMethod(o.getClass());
            try {
               Object clone = cloneMethod.invoke(o); // o.clone()
               // check the object's value and type
               checkClone(o, clone);
               @SuppressWarnings("unchecked")
               T ret = (T) clone;
               return ret;
            } catch (InvocationTargetException e) {
               fail();
            } catch (IllegalAccessException e) {
               fail();
            }
            return null; // make compiler happy...
         }
         
         private Method findCloneMethod(Class<?> clazz) {
            try {
               Method m = clazz.getMethod("clone");
               m.setAccessible(true);
               return m;
               // Exceptions below shouldn't actually ever
               // happen since this method is defined on
               // java.lang.Object.
               // Unfortunately, we have to use reflection
               // to access it since the version on Object
               // is declared as protected instead of public
            }
            catch (SecurityException e) {
               fail();
            }
            catch (NoSuchMethodException e) {
               fail();
            }
            return null; // make compiler happy...
         }
      };
   }

   /**
    * Returns a cloner for cloning instances of {@code Serializable}. This will
    * actually serialize and de-serialize the object to create the clone. It
    * furthermore checks that the resulting clone is the same class but not the
    * same instance as the source instance. So classes that use custom serialization
    * via {@code writeReplace()} and/or {@code readResolve()} methods may not be
    * suitable.
    * 
    * @param <T>     The type of object cloned
    * @return        A cloner that will use object serialization and de-serialization
    *                to perform the cloning operation
    */
   public static <T extends Serializable> Cloner<T> forSerializable() {
      return new Cloner<T>() {
         @Override
         public T clone(T o) {
            ObjectOutputStream oos = null;
            ObjectInputStream ois = null;
            try {
               // write the object
               ByteArrayOutputStream bos = new ByteArrayOutputStream();
               oos = new ObjectOutputStream(bos);
               oos.writeObject(o);
               oos.close(); oos = null;
               // and read it back in
               ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
               ois = new ObjectInputStream(bis);
               Object clone = ois.readObject();
               ois.close(); ois = null;
               // check the object's value and type
               checkClone(o, clone);
               @SuppressWarnings("unchecked")
               T ret = (T) clone;
               return ret;
            }
            catch (IOException e) {
               fail();
            }
            catch (ClassNotFoundException e) {
               fail();
            } finally {
               // close streams in case an exception
               // was raised
               try {
                  if (oos != null) oos.close();
               } catch (IOException ignored) {}
               try {
                  if (ois != null) ois.close();
               } catch (IOException ignored) {}
            }
            return null; // make compiler happy...
         }
      };
   }
   
   /**
    * Interface that can look-up class members via reflection. This is generic because
    * it is used for both looking up constructors and methods.
    * 
    * @author jhumphries
    *
    * @param <T>  The type of member that is looked-up by the interface
    */
   private interface MemberGetter<T extends AccessibleObject> {
      /**
       * Retrieve a member that accepts the specified single argument
       * type as a parameter.
       * 
       * @param argType The argument type
       * @return  The member that matches this signature or null if none exists
       */
      T getMember(Class<?> argType);
   }

   /**
    * Utility class for looking up static methods.
    */
   private static class MethodGetter implements MemberGetter<Method> {
      private Class<?> clazz;
      private String methodName;
      
      public MethodGetter(Class<?> clazz, String methodName) {
         this.clazz = clazz;
         this.methodName = methodName;
      }

      @Override
      public Method getMember(Class<?> argType) {
         try {
            Method ret = clazz.getDeclaredMethod(methodName, argType);
            int mods = ret.getModifiers();
            if (Modifier.isStatic(mods)) {
               return ret;
            } else {
               return null;
            }
         }
         catch (SecurityException e) {
            return null;
         }
         catch (NoSuchMethodException e) {
            return null;
         }
      }
   }

   /**
    * Utility class for looking up constructors.
    * 
    * @param <T>  The generic type of the constructors that this class looks up
    */
   private static class ConstructorGetter<T> implements MemberGetter<Constructor<T>> {
      private Class<T> clazz;
      
      public ConstructorGetter(Class<T> clazz) {
         this.clazz = clazz;
      }
      
      @Override
      public Constructor<T> getMember(Class<?> argType) {
         try {
            return clazz.getDeclaredConstructor(argType);
         }
         catch (SecurityException e) {
            return null;
         }
         catch (NoSuchMethodException e) {
            return null;
         }
      }
   }
   
   /**
    * Helper class used for breadth-first search in {@link #findMemberForInterface}.
    */
   private static class InterfaceSearchNode {
      public Class<?> iface;
      public int level;

      public InterfaceSearchNode(Class<?> iface, int level) {
         this.iface = iface;
         this.level = level;
      }
   }
   
   /**
    * Adds all interfaces declared by the specified class to the queue
    * (for implementing the breadth-first search in {@link #findMemberForInterface}).
    * 
    * @param clazz   The class whose declared interfaces are added to the queue
    * @param level   The level for the interfaces added
    * @param queue   The queue
    */
   private static void addAllInterfacesToSearch(Class<?> clazz, int level, ArrayDeque<InterfaceSearchNode> queue) {
      for (Class<?> iface : clazz.getInterfaces()) {
         queue.addLast(new InterfaceSearchNode(iface, level));
      }
   }

   /**
    * Finds a member that accepts an interface implemented by the specified class.
    * This performs a breadth-first search through the specified class's interfaces.
    * The first "level" of the search are the interfaces directly implemented by the
    * class. The second level consists of super-interfaces for those interfaces, the
    * third level includes super-interfaces of those super-interfaces, and so on.
    * 
    * @param <T>     The type of member (constructor or method) that we're looking for
    * @param clazz   The class whose interfaces are searched
    * @param getter  Object that can retrieve a matching member
    * @return  The matching member or null if there isn't one
    * @throws IllegalArgumentException If the search is ambiguous because multiple matches
    *                      are found with the same level of "specificity" (same level in
    *                      in the search)
    */
   private static <T extends AccessibleObject> T findMemberForInterface(Class<?> clazz, MemberGetter<T> getter) {
      ArrayList<T> candidateMembers = new ArrayList<T>();
      ArrayList<Class<?>> candidateArgTypes = new ArrayList<Class<?>>();

      ArrayDeque<InterfaceSearchNode> queue = new ArrayDeque<InterfaceSearchNode>();
      // seed the queue with the first level
      int lastLevel = 1;
      addAllInterfacesToSearch(clazz, lastLevel, queue);

      while (!queue.isEmpty()) {
         InterfaceSearchNode n = queue.removeFirst();
         if (n.level != lastLevel) {
            // reached end of a level; check results
            if (candidateMembers.size() > 0) {
               break;
            }
            lastLevel = n.level;
         }
         T result = getter.getMember(n.iface);
         if (result != null) {
            // record this result
            candidateMembers.add(result);
            candidateArgTypes.add(n.iface);
         }
         // add next level to queue
         addAllInterfacesToSearch(n.iface, n.level + 1, queue);
      }
      
      if (candidateMembers.size() > 1) {
         // uh oh - ambiguous results
         StringBuilder msg = new StringBuilder();
         msg.append("Ambiguous results: can't decide from ");
         boolean first = true;
         for (Class<?> iface : candidateArgTypes) {
            if (first) {
               first = false;
            } else {
               msg.append(", ");
            }
            msg.append(iface.getName());
         }
         throw new IllegalArgumentException(msg.toString());
         
      } else if (candidateMembers.size() == 1) {
         // got it!
         return candidateMembers.get(0);

      } else {
         // nothing found
         return null;
      }
   }
   
   /**
    * Finds the member (constructor or method) that accepts the most specific
    * compatible argument type for copying the specified class.
    * 
    * @param <T>     The type of member (constructor or method) that we're looking for
    * @param clazz   The class whose interfaces are searched
    * @param getter  Object that can retrieve a matching member
    * @return        The matching member or null if there isn't one
    * @throws IllegalArgumentException If the search is ambiguous because multiple matches
    *                      are found with the same level of "specificity" (same level in
    *                      in the search)
    */
   private static <T extends AccessibleObject> T findMostSpecificMember(Class<?> clazz, MemberGetter<T> getter) {
      T ret = null;
      // first check for items that take this class and then super-classes
      Class<?> current = clazz;
      while (current != Object.class) {
         ret = getter.getMember(current);
         if (ret != null) {
            // got it!
            ret.setAccessible(true);
            return ret;
         }
         current = current.getSuperclass();
      }

      // now we start looking for interfaces
      current = clazz;
      while (current != null) {
         ret = findMemberForInterface(current, getter);
         if (ret != null) {
            ret.setAccessible(true);
            return ret;
         }
         current = current.getSuperclass();
      }

      // we'll accept members that take Object as a last resort
      ret = getter.getMember(Object.class);
      if (ret != null) {
         ret.setAccessible(true);
      }
      return ret;
   }

   /**
    * Finds a copy constructor for the specified class. This requires that the class
    * have a single argument constructor that accepts a compatible argument type.
    * 
    * <p>If there are multiple such copy constructors (like one that takes
    * precisely the same type and an alternate constructor whose parameter
    * is a super-type) then the one with the <em>most specific</em> type is returned.
    * The class hierarchy is checked first, so a constructor that accepts a super-class
    * or ancestor class is preferred over constructors that accept compatible interfaces,
    * with an exception made for constructors that take {@code Object} (which is the
    * <em>least</em> specific). Next, interfaces declared on the class itself
    * are checked and then ancestors of the declared interfaces. Then interfaces
    * declared on the super-class are checked and so on and so forth.
    * 
    * <p>If multiple constructors are found with the same level of "specificity"
    * then an exception will be thrown. This can happen, for example, when no
    * constructor accepts the specified class or an ancestor class and more than one
    * constructor exists that each accept an interface declared on the class.
    * 
    * <p>The following example demonstrates how the level of specificity is
    * determined. Given the following (contrived) interfaces and classes
    * <pre>
    * interface Interface1A1 { }
    * interface Interface1A extends Interface1A1 { }
    * interface Interface1B { }
    * interface Interface1 extends Interface1A, Interface1B { }
    * 
    * interface Interface2A { }
    * interface Interface2B { }
    * interface Interface2 extends Interface2A, Interface2B { }
    * 
    * interface Interface3 { }
    * 
    * class Class1 { } // implicitly extends Object
    * class Class2 extends Class1 implements Interface2, Interface3 { }
    * class Class3 extends Class2 { }
    * class MyClass extends Class3 implements Interface1 { }
    * </pre>
    * the following details the level of specificity for each eligible constructor
    * argument type when searching for a copy constructor for {@code MyClass}:
    * <table border=1>
    *   <tr><th>Most Specific (preferred)</th>                   <td>{@code MyClass}</td></tr>
    *   <tr><th rowspan=3>Ancestor classes...</th>               <td>{@code Class3}</td></tr>
    *   <tr>                                                     <td>{@code Class2}</td></tr>
    *   <tr>                                                     <td>{@code Class1}</td></tr>
    *   <tr><th>Interfaces for {@code MyClass}</th>              <td>{@code Interface1}</td></tr>
    *   <tr><th rowspan=2>Super-interfaces</th>                  <td>{@code Interface1A}, {@code Interface1B}</td></tr>
    *   <tr>                                                     <td>{@code Interface1A1}</td></tr>
    *   <tr><th rowspan=2>Interfaces for ancestor classes...</th><td>{@code Interface2}, {@code Interface3}</td></tr>
    *   <tr>                                                     <td>{@code Interface2A}, {@code Interface2B}</td></tr>
    *   <tr><th>Least Specific (last resort)</th>                <td>{@code Object}</td></tr>
    * </table>
    * As seen in the table above, {@code Interface1A} and {@code Interface1B} have the
    * same level of specificity. So if {@code MyClass} only had two compatible constructors,
    * one that took an {@code Interface1A} and one that took an {@code Interface1B}, then
    * an exception would be thrown because it is ambiguous which constructor should be used.
    * If, however, the class also had a constructor that took a {@code Class1} then that
    * constructor would be used since it is more specific than the other two.
    * 
    * @param <T>     The type of object to be copied
    * @param clazz   The class token for the type of object to be copied and the
    *                class that must have the copy constructor
    * @return        The copy constructor or null if no compatible constructor
    *                exists
    * @throws IllegalArgumentException If the choice of constructor is ambiguous because
    *                there are multiple such constructors with the same level of specificity
    * @throws NullPointerException If the specified class token is null                
    */
   public static <T> Constructor<T> findCopyConstructor(Class<T> clazz) {
      if (clazz == null) {
         throw new NullPointerException();
      }
      return findMostSpecificMember(clazz, new ConstructorGetter<T>(clazz));
   }

   /**
    * Finds a static copy method for the specified class with the specified name. This
    * requires that the class have a single argument method with the specified name that
    * accepts a compatible argument type.
    * 
    * <p>This will involve the same "specificity" semantics as {@link #findCopyConstructor(Class)}
    * when searching for the copy method.
    * 
    * @param <T>        The type of object to be copied
    * @param clazz      The class token for the type of object to be copied and the
    *                   class that must have the copy method
    * @param methodName The name of the static copy method
    * @return           The static copy method or null if no compatible method
    *                   with the specified name exists
    * @throws IllegalArgumentException If the choice of method is ambiguous because
    *                there are multiple such methods with the same level of specificity
    * @throws NullPointerException If the specified class token is null
    */
   public static <T> Method findStaticCopyMethod(Class<T> clazz, String methodName) {
      if (clazz == null) {
         throw new NullPointerException();
      }
      return findMostSpecificMember(clazz, new MethodGetter(clazz, methodName));
   }
   
   /**
    * Returns a cloner that uses the specified copy constructor to perform
    * the cloning operation. The constructor should take no arguments or take
    * a single argument that is the object being copied.
    *
    * @param <T>     The type of object being cloned
    * @param cons    The constructor to use to create a clone
    * @return        A cloner that will use the constructor to create a clone
    * @throws IllegalArgumentException If the specified constructor requires
    *                      more than one argument or requires an argument that
    *                      is a primitive type
    * @throws NullPointerException If the specified constructor is null
    */
   public static <T> Cloner<T> withCopyConstructor(final Constructor<T> cons) {
      if (cons == null) {
         throw new NullPointerException();
      }
      final Class<?> argTypes[] = cons.getParameterTypes();
      if (argTypes.length > 1) {
         throw new IllegalArgumentException("Constructor must take no more than one argument");
      } else if (argTypes.length == 1 && argTypes[0].isPrimitive()) {
         throw new IllegalArgumentException("Constructor argument cannot be primitive");
      }
      
      return new Cloner<T>() {
         @Override
         public T clone(T o) {
            try {
               T clone =
                  argTypes.length == 0 ? cons.newInstance() : cons.newInstance(o);
               checkClone(o, clone);
               return clone;
            }
            catch (IllegalArgumentException e) {
               fail();
            }
            catch (InstantiationException e) {
               fail();
            }
            catch (IllegalAccessException e) {
               fail();
            }
            catch (InvocationTargetException e) {
               fail();
            }
            return null; // make compiler happy...
         }
      };
   }

   /**
    * Returns a cloner that uses a class's copy constructor to perform
    * the cloning operation. This requires that the class have a single-argument
    * constructor that accepts a compatible type.
    * 
    * <p>The copy constructor to use is determined by {@link #findCopyConstructor(Class)}.
    * If multiple constructors are found with the same level of "specificity" then an exception
    * will be thrown and {@link #withCopyConstructor(Class, Class)} or
    * {@link #withCopyConstructor(Constructor)} should be used instead.
    * 
    * @param <T>     The type of object being cloned
    * @param clazz   The class token for the type of object being cloned
    * @return        A cloner that will use a copy constructor to create a
    *                clone
    * @throws IllegalArgumentException If the specified class has no suitable copy
    *                      constructor or if it has multiple constructors of the
    *                      same level specificity
    * @throws NullPointerException If the specified class token is null
    * @see #findCopyConstructor(Class)
    */
   public static <T> Cloner<T> withCopyConstructor(Class<T> clazz) {
      if (clazz == null) {
         throw new NullPointerException();
      }
      Constructor<T> cons = findCopyConstructor(clazz);
      if (cons == null) {
         throw new IllegalArgumentException("No copy constructor exists for " + clazz.getName());
      }
      return withCopyConstructor(cons);
   }

   /**
    * Returns a cloner that uses a class's copy constructor to perform
    * the cloning operation. This requires that the class have a single-argument
    * constructor that accepts the specified type.
    * 
    * @param <T>        The type of object being cloned
    * @param clazz      The class token for the type of object being cloned
    * @param argClazz   The argument type of the copy constructor
    * @return           A cloner that will use a copy constructor to create a
    *                   clone
    * @throws IllegalArgumentException If the specified class has no matching
    *                      constructor
    * @throws NullPointerException If either of the specified class tokens is null
    */
   public static <T> Cloner<T> withCopyConstructor(Class<T> clazz, Class<? super T> argClazz) {
      if (clazz == null || argClazz == null) {
         throw new NullPointerException();
      }
      Constructor<T> cons = new ConstructorGetter<T>(clazz).getMember(argClazz);
      if (cons == null) {
         throw new IllegalArgumentException("No copy constructor exists for " + clazz.getName());
      }
      return withCopyConstructor(cons);
   }

   /**
    * Returns a cloner that uses the specified copy method to perform
    * the cloning operation. If the method is a static method then it
    * should take no arguments or take a single argument that is the object
    * being copied. If the method is an instance method then it should
    * take no arguments and the instance it is invoked on will be the
    * object being copied.
    *
    * @param <T>     The type of object being cloned
    * @param method  The method to use to create a clone
    * @return        A cloner that will use the method to create a clone
    * @throws IllegalArgumentException If the specified method requires
    *                      more than one argument or requires an argument that
    *                      is a primitive type
    * @throws NullPointerException If the specified method is null
    */
   public static <T> Cloner<T> withCopyMethod(final Method method) {
      if (method == null) {
         throw new NullPointerException();
      }
      final Class<?> argTypes[] = method.getParameterTypes();
      final boolean isStatic = Modifier.isStatic(method.getModifiers());
      if (argTypes.length > 0 && !isStatic) {
         throw new IllegalArgumentException("Instance method must take no argument");
      } else if (argTypes.length > 1) {
         throw new IllegalArgumentException("Static method must take no more than one argument");
      } else if (argTypes.length == 1 && argTypes[0].isPrimitive()) {
         throw new IllegalArgumentException("Static method argument cannot be primitive");
      }
      
      return new Cloner<T>() {
         @Override
         public T clone(T o) {
            try {
               T receiver = isStatic ? null : o;
               Object clone =
                  argTypes.length == 0 ? method.invoke(receiver) : method.invoke(receiver, o);
               checkClone(o, clone);
               @SuppressWarnings("unchecked")
               T ret = (T) clone;
               return ret;
            }
            catch (IllegalArgumentException e) {
               fail();
            }
            catch (IllegalAccessException e) {
               fail();
            }
            catch (InvocationTargetException e) {
               fail();
            }
            return null; // make compiler happy...
         }
      };
   }

   /**
    * Returns a cloner that uses an instance method on the class to perform the
    * cloning operation. This requires that the class have a no-argument
    * method with the specified name that accepts a compatible type. The instance
    * method will be invoked on the object to be cloned.
    * 
    * @param <T>        The type of object being cloned
    * @param clazz      The class token for the type of object being cloned
    * @param methodName The name of the instance method
    * @return           A cloner that will use an instance method to create a
    *                   clone
    * @throws IllegalArgumentException If the specified class has no suitable method
    * @throws NullPointerException If either argument is null
    */
   public static <T> Cloner<T> withInstanceCopyMethod(Class<T> clazz, String methodName) {
      if (clazz == null || methodName == null) {
         throw new NullPointerException();
      }
      Method method;
      try {
         method = clazz.getDeclaredMethod(methodName);
      }
      catch (SecurityException e) {
         throw new IllegalArgumentException("No copy method named " + methodName
               + " exists for " + clazz.getName());
      }
      catch (NoSuchMethodException e) {
         throw new IllegalArgumentException("No copy method named " + methodName
               + " exists for " + clazz.getName());
      }
      int mods = method.getModifiers();
      if (Modifier.isStatic(mods)) {
         throw new IllegalArgumentException("No instance method named " + methodName
               + " exists for " + clazz.getName());
      }
      return withCopyMethod(method);
   }

   /**
    * Returns a cloner that uses a static method on the class to perform the
    * cloning operation. This requires that the class have a single-argument
    * method with the specified name that accepts a compatible type.
    * 
    * <p>The copy method to use is determined by {@link #findStaticCopyMethod(Class, String)}.
    * If multiple matching methods are found with the same level of "specificity" then
    * an exception will be thrown and {@link #withStaticCopyMethod(Class, String, Class)}
    * or {@link #withCopyMethod(Method)} should be used instead.
    * 
    * @param <T>        The type of object being cloned
    * @param clazz      The class token for the type of object being cloned
    * @param methodName The name of the static method
    * @return           A cloner that will use a static method to create a
    *                   clone
    * @throws IllegalArgumentException If the specified class has no suitable static
    *                      method or if it has multiple such methods of the
    *                      same level specificity
    * @throws NullPointerException If either argument is null
    * @see #findStaticCopyMethod(Class, String)
    */
   public static <T> Cloner<T> withStaticCopyMethod(Class<T> clazz, String methodName) {
      if (clazz == null || methodName == null) {
         throw new NullPointerException();
      }
      Method method = findStaticCopyMethod(clazz, methodName);
      if (method == null) {
         throw new IllegalArgumentException("No copy method named " + methodName
               + " exists for " + clazz.getName());
      }
      return withCopyMethod(method);
   }

   /**
    * Returns a cloner that uses a static method on the class to perform the
    * cloning operation. This requires that the class have a single-argument
    * method with the specified name that accepts the specified type.
    * 
    * @param <T>        The type of object being cloned
    * @param clazz      The class token for the type of object being cloned
    * @param methodName The name of the static method
    * @param argClazz   The argument type of the static method
    * @return           A cloner that will use a static method to create a
    *                   clone
    * @throws IllegalArgumentException If the specified class has no matching
    *                      static method
    * @throws NullPointerException If any of the arguments are null
    */
   public static <T> Cloner<T> withStaticCopyMethod(Class<T> clazz, String methodName, Class<? super T> argClazz) {
      if (clazz == null || methodName == null || argClazz == null) {
         throw new NullPointerException();
      }
      Method method = new MethodGetter(clazz, methodName).getMember(argClazz);
      if (method == null) {
         throw new IllegalArgumentException("No copy method named " + methodName
               + " exists for " + clazz.getName());
      }
      return withCopyMethod(method);
   }

   /**
    * Creates a cloner that just returns the specified object. This is useful when the clone
    * is created beforehand and needs to be embedded in a {@code Cloner} interface. The
    * cloner will perform a check that the specified clone object has the same class as the
    * object to be cloned and is not the same instance as the object being cloned.
    * 
    * @param <T>     The type of object being cloned
    * @param clone   The clone
    * @return  A cloner that returns {@code clone} (regardless of the object passed to
    *          {@link Cloner#clone(Object)})
    * @throws NullPointerException If the specified instance is null
    */
   public static <T> Cloner<T> fromInstance(final T clone) {
      if (clone == null) {
         throw new NullPointerException();
      }
      return new Cloner<T>() {
         @Override
         public T clone(T o) {
            // check the object's value and type
            checkClone(o, clone);
            return clone;
         }
      };
   }

   /**
    * Creates a list of cloners that just return the specified objects. This is useful when
    * the clones are created beforehand and need to be embedded in {@code Cloner} interfaces.
    * The cloners will perform checks that the specified clone objects have the same classes as
    * the objects to be cloned and are not the same instances as the objects being cloned.
    * 
    * @param clones  The clones
    * @return  A list of cloners
    * 
    * @see #fromInstances(List)
    */
   public static List<Cloner<?>> fromInstances(Object... clones) {
      return fromInstances(Arrays.asList(clones));
   }

   /**
    * Creates a list of cloners that just return the specified objects. This is useful when
    * the clones are created beforehand and need to be embedded in {@code Cloner} interfaces.
    * The cloners will perform checks that the specified clone objects have the same classes as
    * the objects to be cloned and are not the same instances as the objects being cloned.
    * 
    * <p>The returned list will have cloners in the same order as the specified clones. So the
    * first cloner in the list will return the first item from the provided list of clones.
    * 
    * <p>If any null values are specified as clones, the corresponding cloner in the returned
    * list will also be null. This is useful for specifying cloners to
    * {@link InterfaceVerifier.MethodConfigurator#cloneArgumentsWith(List)} under situations
    * where some of the method arguments will be null (and thus not need cloning).
    * 
    * @param clones  The clones
    * @return  A list of cloners
    */
   public static List<Cloner<?>> fromInstances(List<?> clones) {
      ArrayList<Cloner<?>> ret = new ArrayList<Cloner<?>>(clones.size());
      for (Object clone : clones) {
         if (clone == null) {
            ret.add(null);
         } else {
            ret.add(fromInstance(clone));
         }
      }
      return ret;
   }

   /**
    * Creates a cloner that gets the clone from the specified object. Since
    * {@link Callable#call()} does not take any parameters, the clone must be
    * agnostic to the actual value being cloned. From that perspective, this
    * is similar to using {@link #fromInstance(Object)}.
    * 
    * @param <T>  The type of object being cloned
    * @param c    The object called to get the clone
    * @return     A cloner that calls the specified object and returns the
    *             result as the clone
    * @throws NullPointerException If the specified instance is null
    * 
    * @see #fromInstance(Object)
    */
   public static <T> Cloner<T> fromCallable(final Callable<T> c) {
      if (c == null) {
         throw new NullPointerException();
      }
     return new Cloner<T>() {
         @Override
         public T clone(T o) {
            T clone = null;
            try {
               clone = c.call();
            }
            catch (Exception e) {
               fail();
            }
            // check the object's value and type
            checkClone(o, clone);
            return clone;
         }
      };
   }

   /**
    * Creates a cloner for arrays that performs a deep copy by using the specified
    * cloner to clone elements. If you want a cloner that does a <em>shallow</em>
    * copy of the array, you can use {@link #forCloneable()} since arrays in Java
    * implement {@code Cloneable}.
    * 
    * @param <T>     The element type of the array
    * @param cloner  The cloner for copying elements
    * @return        A cloner for deep-copying arrays
    */
   public static <T> Cloner<T[]> forArray(final Cloner<T> cloner) {
      return new Cloner<T[]>() {
         @Override
         public T[] clone(T[] o) {
            T[] copy = o.clone();
            for (int i = 0; i < o.length; i++) {
               // clone all of the elements
               copy[i] = cloner.clone(o[i]);
            }
            return copy;
         }
      };
   }

   /**
    * Creates a cloner for arrays that performs a deep copy by cloning all
    * array elements. The "leaf" components (not arrays) will be cloned
    * using the specifie cloner. The rest of the structure, regardless of
    * nesting depth, will be deep copied in the resulting clone.
    * 
    * This can be useful if you have a cloner for {@code SpreadsheetCell}
    * and a sheet of cells represented by a two-dimensional array of
    * these objects: {@code SpreadsheetCell[][]}. In such a case, a cloner
    * for this structure could be build like so:
    * <pre>
    * Cloner&lt;SpreadsheetCell&gt; cellCloner = new MyCellCloner();
    * Cloner&lt;SpreadsheetCell[][]&gt; sheetCloner =
    *       <strong>Cloners.forNestedArray(SpreadsheetCell[][].class, cellCloner);</strong>
    * </pre>
    * This is effectively shorthand for repeated calls to {@link #forArray(Cloner)}
    * that look like this:
    * <pre>
    * Cloner&lt;SpreadsheetCell&gt; cellCloner = new MyCellCloner();
    * Cloner&lt;SpreadsheetCell[]&gt; rowCloner = <strong>Cloners.forArray(cellCloner);</strong>
    * Cloner&lt;SpreadsheetCell[][]&gt; sheetCloner = <strong>Cloners.forArray(rowCloner);</strong>
    * </pre>
    * And this method works, regardless of nesting depth. So the following
    * call is just as valid and will create a cloner for a four-dimensional
    * array (and will save even more typing since using {@code forArray} with
    * a four-dimensional array is even more verbose):
    * <pre>
    * Cloner&lt;MyObject&gt; elementCloner = new MyObjectCloner();
    * Cloner&lt;MyObject[][][][]&gt; matrixCloner =
    *       <strong>Cloners.forNestedArray(MyObject[][][][].class, elementCloner);</strong>
    * </pre>
    * 
    * @param <T>        The component type of the array type to clone (which can itself
    *                   be an array type since this method is designed to work with
    *                   nested arrays)
    * @param arrayClass The class token for the array type to clone
    * @param cloner     The cloner for cloning leaf components
    * @return           A cloner for deep-copying arrays
    */
   public static <T> Cloner<T[]> forNestedArray(Class<T[]> arrayClass, Cloner<?> cloner) {
      Class<?> clazz = arrayClass;
      while (clazz.isArray()) {
         cloner = forArray(cloner);
         clazz = clazz.getComponentType();
      }
      @SuppressWarnings("unchecked")
      Cloner<T[]> ret = (Cloner<T[]>) cloner;
      return ret;
   }

   /**
    * Creates a "default" cloner for the specified class. The default cloner is chosen like
    * so:
    * <ul>
    *    <li>If the type represents an array then {@link Cloners#forArray(Cloner)} is used to
    *    create a cloner that <em>deep copies</em> arrays. Elements in the array will be
    *    cloned by a "default" cloner for their type.
    *    <li>If the type implements {@code Cloneable} then {@link Cloners#forCloneable()}
    *    is used to create the cloner.</li>
    *    <li>If the type implements {@code Serializable} then
    *    {@link Cloners#forSerializable()} is used.</li>
    *    <li>Otherwise, {@link Cloners#withCopyConstructor(Class)} is used to create the
    *    cloner.
    * </ul>
    * If the type is not an array, implements neither interface, and has no suitable copy
    * constructor then an exception is thrown. Similarly, if the type is an array but
    * has an element type that implements neither interface and has no suitable copy
    * constructor then an exception is thrown.
    * 
    * @param <T>     The type of object to be cloned
    * @param clazz   The class token for the type of object to be cloned
    * @return        A cloner for the specified type
    */
   public static <T> Cloner<T> defaultClonerFor(Class<T> clazz) {
      if (clazz.isArray()) {
         Class<?> element  = clazz.getComponentType();
         @SuppressWarnings("unchecked")
         Cloner<T> ret = (Cloner<T>) Cloners.<Object> forArray((Cloner<Object>) defaultClonerFor(element));
         return ret;
         
      } else if (Cloneable.class.isAssignableFrom(clazz)) {
         @SuppressWarnings("unchecked")
         Cloner<T> ret = (Cloner<T>) Cloners.<Cloneable> forCloneable();
         return ret;
         
      } else if (Serializable.class.isAssignableFrom(clazz)) {
         @SuppressWarnings("unchecked")
         Cloner<T> ret = (Cloner<T>) Cloners.<Serializable> forSerializable();
         return ret;
         
      } else {
         return withCopyConstructor(clazz);
      }
   }
   
   /**
    * A generic cloner that will try to clone any type of object. Its strategy
    * for cloning objects follows the same flow as {@code #defaultClonerFor(Class)}
    * except that it evaluates the logic for <em>every object cloned</em> instead
    * of trying to do it statically based on a given type.
    * 
    * <p>So it is more flexible than {@code #defaultClonerFor(Class)} since you
    * don't have to know the runtime type of objects being cloned ahead of time.
    * But its performance is worse and cloning may result in runtime exceptions
    * during the cloning operation, instead of when creating the cloner. This
    * occurs if a given object to be cloned is not array and implements neither
    * {@code Cloneable} nor {@code Serializable} and whose class does not provide
    * a suitable copy constructor.
    */
   public static Cloner<Object> GENERIC_CLONER = new Cloner<Object>() {
      @Override
      public Object clone(Object o) {
         @SuppressWarnings("unchecked")
         Cloner<Object> delegate = (Cloner<Object>) Cloners.defaultClonerFor(o.getClass());
         return delegate.clone(o);
      }
   };
}