package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection utility methods for working with class members: methods and fields.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class Members {
   private Members() {}

   /**
    * Finds an instance method on the specified class with the specified signature. Unlike
    * {@link Class#getMethod(String, Class...)}, it will find non-public methods. Unlike
    * {@link Class#getDeclaredMethod(String, Class...)}, it will find any method for the class,
    * including those defined by super-types. If a matching method is declared by more than one
    * type in the class's hierarchy then the effective override (the one declared "lowest" in the
    * type hierarchy) is the one returned. If more than one method matches that are at the same
    * level in the hierarchy, the declared order of implemented interfaces is used to determine
    * which one is returned. A matching method on an interface declared sooner in the list will be
    * returned, and the matching method on a later interface will not.
    * 
    * @param clazz the class whose method to find
    * @param name the name of the method to find
    * @param argTypes the parameter types of the method to find
    * @return the method with the specified signature or {@code null}
    */
   public static Method findMethod(Class<?> clazz, final String name, final Class<?>... argTypes) {
      return ClassHierarchyCrawler.crawlWith(clazz, null, (aClass, v) -> {
         try {
            Method m = aClass.getDeclaredMethod(name, argTypes);
            // only want instance methods
            return Modifier.isStatic(m.getModifiers()) ? null : m;
         } catch (NoSuchMethodException e) {
            return null;
         }
      });
   }
   
   /**
    * Finds an instance method on the specified class with the specified signature. This is a
    * convenience method that allows the parameter types to be specified as a list instead of an
    * array.
    * 
    * @param clazz the class whose method to find
    * @param name the name of the method to find
    * @param argTypes the parameter types of the method to find
    * @return the method with the specified signature or {@code null}
    * 
    * @see #findMethod(Class, String, Class...)
    */
   public static Method findMethod(Class<?> clazz, String name, List<Class<?>> argTypes) {
      return findMethod(clazz, name, argTypes.toArray(new Class<?>[argTypes.size()]));
   }
   
   /**
    * Finds all instance methods with the specified name. This finds all methods, regardless of
    * visibility. It also finds methods declared not just on the specified type but also on any
    * super-type. If a matching method exists with the same signature in multiple types in the
    * class's hierarchy (e.g. is overridden), then the effective override (the one declared "lowest"
    * in the type hierarchy) is the one present in the collection. Overridden methods are not
    * included.
    * 
    * @param clazz the class whose methods to find
    * @param name the name of the methods to find
    * @return a collection of matching methods (an empty collection if there are no matches)
    * 
    * @see #findMethod(Class, String, Class...)
    */
   public static Collection<Method> findMethods(Class<?> clazz, final String name) {
      Map<MethodSignature, Method> methods = new HashMap<MethodSignature, Method>();
      ClassHierarchyCrawler.crawlWith(clazz, methods, (aClass, methodMap) -> {
            for (Method m : aClass.getDeclaredMethods()) {
               if (m.getName().equals(name) && !Modifier.isStatic(m.getModifiers())) {
                  MethodSignature sig = new MethodSignature(m);
                  // don't overwrite an entry with one from a more distant type
                  if (!methodMap.containsKey(sig)) {
                     methodMap.put(new MethodSignature(m), m);
                  }
               }
            }
            return null;
      });
      return methods.values();
   }
   
   /**
    * Finds an instance field on the specified class with the specified name. Unlike
    * {@link Class#getField(String)}, it will find non-public fields. Unlike
    * {@link Class#getDeclaredField(String)}, it will find any field for the class,
    * including those defined by super-types. If a matching field is declared by more than one
    * type in the class's hierarchy then the effectively visible field (the one declared "lowest" in
    * the type hierarchy) is the one returned.
    * 
    * @param clazz the class whose field to find
    * @param name the name of the field to find
    * @return the field with the specified name or {@code null}
    */
   public static Field findField(Class<?> clazz, final String name) {
      return ClassHierarchyCrawler.crawlWith(clazz, null, (aClass, v) -> {
         try {
            return aClass.getDeclaredField(name);
         } catch (NoSuchFieldException e) {
            return null;
         }
      });
   }

   /**
    * Finds all instance fields with the specified name. This finds all fields, regardless of
    * visibility or on which super-type which defines it. If a class "hides" a super-class field
    * by declaring one with the same name, then all fields so-named are returned.
    * 
    * @param clazz the class whose fields to find
    * @param name the name of the fields to find
    * @return a collection of matching fields (an empty collection if there are no matches)
    * 
    * @see #findField(Class, String)
    */
   public static Collection<Field> findFields(Class<?> clazz, final String name) {
      List<Field> fields = new ArrayList<Field>();
      ClassHierarchyCrawler.<Void, List<Field>>builder()
            .forEachClass((aClass, fieldList) -> {
               try {
                  fieldList.add(aClass.getDeclaredField(name));
               } catch (NoSuchFieldException ignore) {
               }
               return null;
            }).build().visit(clazz, fields);
      return fields;
   }
   
   /**
    * Determines if the given member is accessible from the given calling class, per the rules of
    * member visibility in the Java language.
    *
    * @param member the member
    * @param callingClass the calling class
    * @return true if the member is accessible from the given calling class; false otherwise
    */
   public static boolean isAccessible(Member member, Class<?> callingClass) {
      switch (requireNonNull(getEffectiveVisibility(member))) {
         case PUBLIC:
            return true;
         case PROTECTED:
            Class<?> owner = member.getDeclaringClass();
            return samePackage(callingClass, owner) || owner.isAssignableFrom(callingClass);
         case PACKAGE_PRIVATE:
            return samePackage(callingClass, member.getDeclaringClass());
         case PRIVATE:
            return sameEnclosingClass(callingClass, member.getDeclaringClass());
         default:
            throw new AssertionError();
      }
   }
   
   private enum EffectiveVisibility {
      PUBLIC, PROTECTED, PACKAGE_PRIVATE, PRIVATE
   }
   
   private static EffectiveVisibility getEffectiveVisibility(Member member) {
      Class<?> owner = member.getDeclaringClass();
      int memberMods = member.getModifiers();
      int classMods = owner.getModifiers();
      boolean memberIsPublic = Modifier.isPublic(memberMods);
      boolean classIsPublic = Modifier.isPublic(classMods);
      // Member can be public, private, protected, or default.
      // Class can only be public, private, or default (and private is only allowed for
      // enclosed classes)
      if (memberIsPublic && classIsPublic) {
         return EffectiveVisibility.PUBLIC;
      }
      boolean memberIsPrivate = Modifier.isPrivate(memberMods);
      boolean classIsPrivate = Modifier.isPrivate(classMods);
      assert !classIsPrivate || owner.getEnclosingClass() != null;
      if (memberIsPrivate || classIsPrivate) {
         return EffectiveVisibility.PRIVATE;
      }
      boolean memberIsProtected = Modifier.isProtected(memberMods);
      if (memberIsProtected && classIsPublic) {
         return EffectiveVisibility.PROTECTED;
      }
      // default visibility
      return EffectiveVisibility.PACKAGE_PRIVATE;
   }
   
   private static boolean samePackage(Class<?> a, Class<?> b) {
      return a.getClassLoader() == b.getClassLoader() && getPackage(a).equals(getPackage(b));
   }
   
   private static String getPackage(Class<?> clazz) {
      while (clazz.isArray()) {
         clazz = clazz.getComponentType();
      }
      String name = clazz.getName();
      int pos = name.lastIndexOf('.');
      return pos == -1 ? "" : name.substring(0, pos);
   }
   
   private static boolean sameEnclosingClass(Class<?> a, Class<?> b) {
      // Find top-level types that enclose each class. If they are the same then the two types
      // are contained in the same enclosing class (which means private members are accessible).
      while (true) {
         if (a.equals(b)) {
            return true;
         }
         Class<?> enclosingClass = a.getEnclosingClass();
         if (enclosingClass == null) {
            // found top-level type
            break;
         }
         a = enclosingClass;
      }
      while (true) {
         Class<?> enclosingClass = b.getEnclosingClass();
         if (enclosingClass == null) {
            // found top-level type
            break;
         }
         b = enclosingClass;
      }
      return a.equals(b);
   }
}
