package com.apriori.concurrent;

import static java.util.Objects.requireNonNull;

import com.apriori.reflect.Members;
import com.apriori.util.BooleanBinaryOperator;
import com.apriori.util.BooleanUnaryOperator;
import com.apriori.util.CallingClass;
import com.apriori.util.IsDerivedFrom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A reflection-based utility that enables atomic updates to designated {@code volatile boolean}
 * fields of designated classes. This class is designed for use in atomic data structures in which
 * several fields of the same node are independently subject to atomic updates.
 *
 * <p>Note that the guarantees of the {@code compareAndSet} method in this class are weaker than in
 * other atomic classes. Because this class cannot ensure that all uses of the field are appropriate
 * for purposes of atomic access, it can guarantee atomicity only with respect to other invocations
 * of {@code compareAndSet} and {@code set} on the same updater.
 *
 * @param <T> The type of the object holding the updatable field
 */
@IsDerivedFrom(AtomicIntegerFieldUpdater.class)
public abstract class AtomicBooleanFieldUpdater<T> {

   /*
    * This implementation was copied from AtomicIntegerFieldUpdater and then just adjusted for the
    * different type of volatile value and to remove most dependencies on sun.* code.
    */
   
   /**
    * Creates and returns an updater for objects with the given field. The Class argument is needed
    * to check that reflective types and generic types match.
    * 
    * <p>To enforce Java accessibility rules, this code will identify what class is calling this
    * method and ensure that class actually has valid access to the boolean field in question. To
    * resolve the caller, it needs to know its class loader. This version of this method will try
    * both the class loader of the given class token and the current thread's context class loader.
    * This method is overloaded to accept an explicit class loader:
    * {@code ThisClass.class.getClassLoader()}).
    *
    * @param tclass the class of the objects holding the field
    * @param fieldName the name of the field to be updated
    * @param <U> the type of instances of tclass
    * @return the updater
    * @throws IllegalArgumentException if the field is not a volatile boolean type or if the given
    *    class does not have a field with the given name
    * @throws RuntimeException with a nested reflection-based exception if the field is inaccessible
    *    to the caller according to Java language access control
    * @throws IllegalStateException if the calling class cannot be identified (in which case the
    *    overloaded version of the method should be used)
    * @see #newUpdater(ClassLoader, Class, String)
    */
   public static <U> AtomicBooleanFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
      // try both the current thread's context class loader and the given type's class loader
      List<ClassLoader> loaders = new ArrayList<>(2);
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      if (loader != null) {
         loaders.add(loader);
      }
      loader = tclass.getClassLoader();
      if (loader != null) {
         loaders.add(loader);
      }
      Class<?> caller = CallingClass.getCaller(1, loaders);
      if (caller == null) {
         throw new IllegalStateException("Could not identify caller");
      }
      return new AtomicBooleanFieldUpdaterImpl<U>(tclass, fieldName, caller);
   }

   /**
    * Creates and returns an updater for objects with the given field. The Class argument is needed
    * to check that reflective types and generic types match. The ClassLoader argument is used to
    * resolve the calling class (for permission checks and applying Java accessibility rules).
    *
    * @param tclass the class of the objects holding the field
    * @param fieldName the name of the field to be updated
    * @param <U> the type of instances of tclass
    * @return the updater
    * @throws IllegalArgumentException if the field is not a volatile boolean type or if the given
    *    class does not have a field with the given name
    * @throws RuntimeException with a nested reflection-based exception if the field is inaccessible
    *    to the caller according to Java language access control
    * @throws IllegalStateException if the calling class cannot be identified, which is usually due
    *    to the calling class being loaded by a different class loader than the one given
    *         
    */
   public static <U> AtomicBooleanFieldUpdater<U> newUpdater(ClassLoader callingClassLoader,
         Class<U> tclass, String fieldName) {
      requireNonNull(callingClassLoader);
      Class<?> caller = CallingClass.getCaller(1, Collections.singletonList(callingClassLoader));
      if (caller == null) {
         throw new IllegalStateException("Could not identify caller");
      }
      return new AtomicBooleanFieldUpdaterImpl<U>(tclass, fieldName, caller);
   }

   /**
    * Protected do-nothing constructor for use by subclasses.
    */
   protected AtomicBooleanFieldUpdater() {
   }

   /**
    * Atomically sets the field of the given object managed by this updater to the given updated
    * value if the current value {@code ==} the expected value. This method is guaranteed to be
    * atomic with respect to other calls to {@code compareAndSet} and {@code set}, but not
    * necessarily with respect to other changes in the field.
    *
    * @param obj An object whose field to conditionally set
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful
    * @throws ClassCastException if {@code obj} is not an instance of the class possessing the field
    *         established in the constructor
    */
   public abstract boolean compareAndSet(T obj, boolean expect, boolean update);

   /**
    * Atomically sets the field of the given object managed by this updater to the given updated
    * value if the current value {@code ==} the expected value. This method is guaranteed to be
    * atomic with respect to other calls to {@code compareAndSet} and {@code set}, but not
    * necessarily with respect to other changes in the field.
    *
    * <p>
    * <a href="package-summary.html#weakCompareAndSet">May fail spuriously and does not provide
    * ordering guarantees</a>, so is only rarely an appropriate alternative to {@code compareAndSet}.
    *
    * @param obj An object whose field to conditionally set
    * @param expect the expected value
    * @param update the new value
    * @return {@code true} if successful
    * @throws ClassCastException if {@code obj} is not an instance of the class possessing the field
    *         established in the constructor
    */
   public abstract boolean weakCompareAndSet(T obj, boolean expect, boolean update);

   /**
    * Sets the field of the given object managed by this updater to the given updated value. This
    * operation is guaranteed to act as a volatile store with respect to subsequent invocations of
    * {@code compareAndSet}.
    *
    * @param obj An object whose field to set
    * @param newValue the new value
    */
   public abstract void set(T obj, boolean newValue);

   /**
    * Eventually sets the field of the given object managed by this updater to the given updated
    * value.
    *
    * @param obj An object whose field to set
    * @param newValue the new value
    */
   public abstract void lazySet(T obj, boolean newValue);

   /**
    * Gets the current value held in the field of the given object managed by this updater.
    *
    * @param obj An object whose field to get
    * @return the current value
    */
   public abstract boolean get(T obj);

   /**
    * Atomically sets the field of the given object managed by this updater to the given value and
    * returns the old value.
    *
    * @param obj An object whose field to get and set
    * @param newValue the new value
    * @return the previous value
    */
   public boolean getAndSet(T obj, boolean newValue) {
      boolean prev;
      do {
         prev = get(obj);
      } while (!compareAndSet(obj, prev, newValue));
      return prev;
   }

   /**
    * Atomically updates the field of the given object managed by this updater with the results of
    * applying the given function, returning the previous value. The function should be
    * side-effect-free, since it may be re-applied when attempted updates fail due to contention
    * among threads.
    *
    * @param obj An object whose field to get and set
    * @param updateFunction a side-effect-free function
    * @return the previous value
    */
   public final boolean getAndUpdate(T obj, BooleanUnaryOperator updateFunction) {
      boolean prev, next;
      do {
         prev = get(obj);
         next = updateFunction.applyAsBoolean(prev);
      } while (!compareAndSet(obj, prev, next));
      return prev;
   }

   /**
    * Atomically updates the field of the given object managed by this updater with the results of
    * applying the given function, returning the updated value. The function should be
    * side-effect-free, since it may be re-applied when attempted updates fail due to contention
    * among threads.
    *
    * @param obj An object whose field to get and set
    * @param updateFunction a side-effect-free function
    * @return the updated value
    */
   public final boolean updateAndGet(T obj, BooleanUnaryOperator updateFunction) {
      boolean prev, next;
      do {
         prev = get(obj);
         next = updateFunction.applyAsBoolean(prev);
      } while (!compareAndSet(obj, prev, next));
      return next;
   }

   /**
    * Atomically updates the field of the given object managed by this updater with the results of
    * applying the given function to the current and given values, returning the previous value. The
    * function should be side-effect-free, since it may be re-applied when attempted updates fail
    * due to contention among threads. The function is applied with the current value as its first
    * argument, and the given update as the second argument.
    *
    * @param obj An object whose field to get and set
    * @param x the update value
    * @param accumulateFunction a side-effect-free function of two arguments
    * @return the previous value
    */
   public final boolean getAndAccumulate(T obj, boolean x,
         BooleanBinaryOperator accumulateFunction) {
      boolean prev, next;
      do {
         prev = get(obj);
         next = accumulateFunction.applyAsBoolean(prev, x);
      } while (!compareAndSet(obj, prev, next));
      return prev;
   }

   /**
    * Atomically updates the field of the given object managed by this updater with the results of
    * applying the given function to the current and given values, returning the updated value. The
    * function should be side-effect-free, since it may be re-applied when attempted updates fail
    * due to contention among threads. The function is applied with the current value as its first
    * argument, and the given update as the second argument.
    *
    * @param obj An object whose field to get and set
    * @param x the update value
    * @param accumulateFunction a side-effect-free function of two arguments
    * @return the updated value
    */
   public final boolean accumulateAndGet(T obj, boolean x,
         BooleanBinaryOperator accumulateFunction) {
      boolean prev, next;
      do {
         prev = get(obj);
         next = accumulateFunction.applyAsBoolean(prev, x);
      } while (!compareAndSet(obj, prev, next));
      return next;
   }

   /**
    * Standard hotspot implementation using intrinsics
    */
   private static class AtomicBooleanFieldUpdaterImpl<T> extends AtomicBooleanFieldUpdater<T> {

      // The one place we depend on sun.* code -- the infamous Unsafe.
      private static final Unsafe unsafe;
      static {
         // The only way to get this instance is to either be on the boot classpath or to use
         // reflection like so:
         try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
         } catch (Exception e) {
            if (e instanceof RuntimeException) {
               throw (RuntimeException) e;
            } else {
               throw new RuntimeException(e);
            }
         } 
      }
      
      private final long offset;
      private final Class<T> tclass;
      private final Class<?> cclass;

      AtomicBooleanFieldUpdaterImpl(final Class<T> tclass, final String fieldName,
            final Class<?> caller) {
         final Field field;
         final int modifiers;
         try {
            field = tclass.getDeclaredField(fieldName);
            modifiers = field.getModifiers();
            if (!Members.isAccessible(field, caller)) {
               throw new IllegalAccessException("Class " + caller.getName() +
                     " cannot access a member of class " + field.getDeclaringClass().getName() +
                     " with modifiers \"" + Modifier.toString(modifiers) + "\"");
            }
            ClassLoader cl = tclass.getClassLoader();
            ClassLoader ccl = caller.getClassLoader();
            if ((ccl != null) && (ccl != cl) && ((cl == null) || !isAncestor(cl, ccl))) {
               checkPackageAccess(tclass);
            }

         } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(
                  tclass.getName() + " has no field named " + fieldName);
         } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
               throw (RuntimeException) ex;
            }
            throw new RuntimeException(ex);
         }

         Class<?> fieldt = field.getType();
         if (fieldt != boolean.class) throw new IllegalArgumentException("Must be boolean type");

         if (!Modifier.isVolatile(modifiers))
            throw new IllegalArgumentException("Must be volatile type");

         this.cclass = (Modifier.isProtected(modifiers) && caller != tclass) ? caller : null;
         this.tclass = tclass;
         offset = unsafe.objectFieldOffset(field);
      }

      private void fullCheck(T obj) {
         if (!tclass.isInstance(obj)) throw new ClassCastException();
         if (cclass != null) ensureProtectedAccess(obj);
      }

      @Override
      public boolean compareAndSet(T obj, boolean expect, boolean update) {
         if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
         int e = expect ? 1 : 0;
         int u = update ? 1 : 0;
         return unsafe.compareAndSwapInt(obj, offset, e, u);
      }

      @Override
      public boolean weakCompareAndSet(T obj, boolean expect, boolean update) {
         if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
         int e = expect ? 1 : 0;
         int u = update ? 1 : 0;
         return unsafe.compareAndSwapInt(obj, offset, e, u);
      }

      @Override
      public void set(T obj, boolean newValue) {
         if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
         int n = newValue ? 1 : 0;
         unsafe.putIntVolatile(obj, offset, n);
      }

      @Override
      public void lazySet(T obj, boolean newValue) {
         if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
         int n = newValue ? 1 : 0;
         unsafe.putOrderedInt(obj, offset, n);
      }

      @Override
      public final boolean get(T obj) {
         if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
         return unsafe.getIntVolatile(obj, offset) != 0;
      }

      @Override
      public boolean getAndSet(T obj, boolean newValue) {
         if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
         int n = newValue ? 1 : 0;
         return unsafe.getAndSetInt(obj, offset, n) != 0;
      }

      private void ensureProtectedAccess(T obj) {
         if (cclass.isInstance(obj)) {
            return;
         }
         throw new RuntimeException(new IllegalAccessException("Class " + cclass.getName()
               + " can not access a protected member of class " + tclass.getName()
               + " using an instance of " + obj.getClass().getName()));
      }

      
      /*
       * The following methods come from sun.reflect.misc.ReflectUtil. AtomicIntegerFieldUpdater
       * uses them. To prevent this class from relying on sun.* code, implementations for these
       * methods was copied here.
       */

      
      /**
       * Checks package access on the given class.
       *
       * If it is a {@link Proxy#isProxyClass(java.lang.Class)} that implements a non-public
       * interface (i.e. may be in a non-restricted package), also check the package access on the
       * proxy interfaces.
       */
      private static void checkPackageAccess(Class<?> clazz) {
         checkPackageAccess(clazz.getName());
         if (isNonPublicProxyClass(clazz)) {
            checkProxyPackageAccess(clazz);
         }
      }

      /**
       * Checks package access on the given classname. This method is typically called when the
       * Class instance is not available and the caller attempts to load a class on behalf the true
       * caller (application).
       */
      private static void checkPackageAccess(String name) {
         SecurityManager s = System.getSecurityManager();
         if (s != null) {
            String cname = name.replace('/', '.');
            if (cname.startsWith("[")) {
               int b = cname.lastIndexOf('[') + 2;
               if (b > 1 && b < cname.length()) {
                  cname = cname.substring(b);
               }
            }
            int i = cname.lastIndexOf('.');
            if (i != -1) {
               s.checkPackageAccess(cname.substring(0, i));
            }
         }
      }

      /**
       * Check package access on the proxy interfaces that the given proxy class implements.
       *
       * @param clazz Proxy class object
       */
      public static void checkProxyPackageAccess(Class<?> clazz) {
         SecurityManager s = System.getSecurityManager();
         if (s != null) {
            // check proxy interfaces if the given class is a proxy class
            if (Proxy.isProxyClass(clazz)) {
               for (Class<?> intf : clazz.getInterfaces()) {
                  checkPackageAccess(intf);
               }
            }
         }
      }

      /**
       * Test if the given class is a proxy class that implements non-public interface. Such proxy
       * class may be in a non-restricted package that bypasses checkPackageAccess.
       */
      private static boolean isNonPublicProxyClass(Class<?> cls) {
         return Proxy.isProxyClass(cls) && containsNonPublicInterfaces(cls);
      }

      private static boolean containsNonPublicInterfaces(Class<?> cls) {
         for (Class<?> iface : cls.getInterfaces()) {
            if (!Modifier.isPublic(iface.getModifiers())) {
               return true;
            }
            if (containsNonPublicInterfaces(iface)) {
               return true;
            }
         }
         return false;
      }
      
      /**
       * Returns true if the second classloader can be found in the first classloader's delegation
       * chain. Equivalent to the inaccessible: first.isAncestor(second).
       */
      private static boolean isAncestor(ClassLoader first, ClassLoader second) {
         ClassLoader acl = first;
         do {
            acl = acl.getParent();
            if (second == acl) {
               return true;
            }
         } while (acl != null);
         return false;
      }
   }
}
