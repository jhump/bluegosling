package com.apriori.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: javadoc
public final class Members {
   private Members() {}
   
   public static Method findMethod(Class<?> clazz, final String name, final Class<?>... argTypes) {
      return ClassHierarchyCrawler.crawlWith(clazz, null, new ClassVisitor<Method, Void>() {
         @Override
         public Method visit(Class<?> clazz, Void v) {
            try {
               return clazz.getDeclaredMethod(name, argTypes);
            } catch (NoSuchMethodException e) {
               // TODO: would it be more efficient to do linear search through all methods rather
               // than try/catch?
               return null;
            }
         }
      });
   }
   
   public static Method findMethod(Class<?> clazz, String name, List<Class<?>> argTypes) {
      return findMethod(clazz, name, argTypes.toArray(new Class<?>[argTypes.size()]));
   }
   
   public static Collection<Method> findMethods(Class<?> clazz, final String name) {
      Map<MethodSignature, Method> methods = new HashMap<MethodSignature, Method>();
      ClassHierarchyCrawler.<Void, Map<MethodSignature, Method>> builder()
            .postOrderVisit() // overwrite super-class or interface methods w/ sub-class methods
            .forEachClass(new ClassVisitor<Void, Map<MethodSignature, Method>>() {
               @Override
               public Void visit(Class<?> clazz, Map<MethodSignature, Method> methods) {
                  for (Method m : clazz.getDeclaredMethods()) {
                     if (m.getName().equals(name)) {
                        methods.put(new MethodSignature(m), m);
                     }
                  }
                  return null;
               }
            }).build().visit(clazz, methods);
      return methods.values();
   }
   
   public static Field findField(Class<?> clazz, final String name) {
      return ClassHierarchyCrawler.crawlWith(clazz, null, new ClassVisitor<Field, Void>() {
         @Override
         public Field visit(Class<?> clazz, Void v) {
            try {
               return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
               // TODO: would it be more efficient to do linear search through all fields rather
               // than try/catch?
               return null;
            }
         }
      });
   }
}
