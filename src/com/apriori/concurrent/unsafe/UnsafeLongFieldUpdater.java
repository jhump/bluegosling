package com.apriori.concurrent.unsafe;

import static com.apriori.reflect.ClassLoaders.isAncestor;
import static com.apriori.reflect.SecureUtils.checkPackageAccess;

import com.apriori.reflect.Members;
import com.apriori.util.CallingClass;
import com.apriori.util.IsDerivedFrom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Like {@link AtomicLongFieldUpdater} except elides the per-access checks. This improves
 * performance but at the cost of safety (hence the name).
 * 
 * <p>Without these checks, it is possible for classes to have access to fields they ought not per
 * the Java visibility rules.
 * 
 * <p>Due to erasure, it is even possible to pass the wrong type of owner object, which could result
 * in dangerous memory corruption.
 *
 * @param <T> the type of the object that owns the long field
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@IsDerivedFrom(AtomicLongFieldUpdater.class)
public final class UnsafeLongFieldUpdater<T> extends UnsafeFieldUpdater<T> {
   
   private static final Unsafe unsafe = UnsafeUtils.getUnsafe();

   private final long offset;

   /**
    * Creates and returns an updater for objects with the given field. The Class argument is needed
    * to check that reflective types and generic types match.
    *
    * @param tclass the class of the objects holding the field
    * @param fieldName the name of the field to be updated
    * @throws IllegalArgumentException if the field is not a volatile long type
    * @throws RuntimeException with a nested reflection-based exception if the class does not hold
    *         field or is the wrong type, or the field is inaccessible to the caller according to
    *         Java language access control
    */
   public UnsafeLongFieldUpdater(Class<T> tclass, String fieldName) {
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

      final Field field;
      final int modifiers;
      try {
         field = tclass.getDeclaredField(fieldName);
         modifiers = field.getModifiers();
         if (!Members.isAccessible(field, caller)) {
            throw new IllegalAccessException("Class " + caller.getName()
                  + " cannot access a member of class " + field.getDeclaringClass().getName()
                  + " with modifiers \"" + Modifier.toString(modifiers) + "\"");
         }
         ClassLoader cl = tclass.getClassLoader();
         ClassLoader ccl = caller.getClassLoader();
         if ((ccl != null) && (ccl != cl) && ((cl == null) || !isAncestor(cl, ccl))) {
            checkPackageAccess(tclass);
         }

      } catch (NoSuchFieldException ex) {
         throw new IllegalArgumentException(tclass.getName() + " has no field named " + fieldName);
      } catch (RuntimeException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }

      Class<?> fieldt = field.getType();
      if (fieldt != long.class) throw new IllegalArgumentException("Must be long type");

      if (!Modifier.isVolatile(modifiers))
         throw new IllegalArgumentException("Must be volatile type");

      offset = unsafe.objectFieldOffset(field);
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
    */
   public boolean compareAndSet(T obj, long expect, long update) {
      return unsafe.compareAndSwapLong(obj, offset, expect, update);
   }

   /**
    * Sets the field of the given object managed by this updater to the given updated value. This
    * operation is guaranteed to act as a volatile store with respect to subsequent invocations of
    * {@code compareAndSet}.
    *
    * @param obj An object whose field to set
    * @param newValue the new value
    */
   public void set(T obj, long newValue) {
      unsafe.putLongVolatile(obj, offset, newValue);
   }

   /**
    * Eventually sets the field of the given object managed by this updater to the given updated
    * value.
    *
    * @param obj An object whose field to set
    * @param newValue the new value
    */
   public void lazySet(T obj, long newValue) {
      unsafe.putOrderedLong(obj, offset, newValue);
   }

   /**
    * Gets the current value held in the field of the given object managed by this updater.
    *
    * @param obj An object whose field to get
    * @return the current value
    */
   public long get(T obj) {
      return unsafe.getLongVolatile(obj, offset);
   }

   /**
    * Atomically sets the field of the given object managed by this updater to the given value and
    * returns the old value.
    *
    * @param obj An object whose field to get and set
    * @param newValue the new value
    * @return the previous value
    */
   public long getAndSet(T obj, long newValue) {
      return unsafe.getAndSetLong(obj, offset, newValue);
   }

   /**
    * Atomically increments by one the current value of the field of the given object managed by
    * this updater.
    *
    * @param obj An object whose field to get and set
    * @return the previous value
    */
   public long getAndIncrement(T obj) {
      return getAndAdd(obj, 1);
   }

   /**
    * Atomically decrements by one the current value of the field of the given object managed by
    * this updater.
    *
    * @param obj An object whose field to get and set
    * @return the previous value
    */
   public long getAndDecrement(T obj) {
      return getAndAdd(obj, -1);
   }

   /**
    * Atomically adds the given value to the current value of the field of the given object managed
    * by this updater.
    *
    * @param obj An object whose field to get and set
    * @param delta the value to add
    * @return the previous value
    */
   public long getAndAdd(T obj, long delta) {
      return unsafe.getAndAddLong(obj, offset, delta);
   }

   /**
    * Atomically increments by one the current value of the field of the given object managed by
    * this updater.
    *
    * @param obj An object whose field to get and set
    * @return the updated value
    */
   public long incrementAndGet(T obj) {
      return getAndAdd(obj, 1) + 1;
   }

   /**
    * Atomically decrements by one the current value of the field of the given object managed by
    * this updater.
    *
    * @param obj An object whose field to get and set
    * @return the updated value
    */
   public long decrementAndGet(T obj) {
      return getAndAdd(obj, -1) - 1;
   }

   /**
    * Atomically adds the given value to the current value of the field of the given object managed
    * by this updater.
    *
    * @param obj An object whose field to get and set
    * @param delta the value to add
    * @return the updated value
    */
   public long addAndGet(T obj, long delta) {
      return getAndAdd(obj, delta) + delta;
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
   public final long getAndUpdate(T obj, LongUnaryOperator updateFunction) {
      long prev, next;
      do {
         prev = get(obj);
         next = updateFunction.applyAsLong(prev);
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
   public final long updateAndGet(T obj, LongUnaryOperator updateFunction) {
      long prev, next;
      do {
         prev = get(obj);
         next = updateFunction.applyAsLong(prev);
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
    * @param accumulatorFunction a side-effect-free function of two arguments
    * @return the previous value
    */
   public final long getAndAccumulate(T obj, long x, LongBinaryOperator accumulatorFunction) {
      long prev, next;
      do {
         prev = get(obj);
         next = accumulatorFunction.applyAsLong(prev, x);
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
    * @param accumulatorFunction a side-effect-free function of two arguments
    * @return the updated value
    */
   public final long accumulateAndGet(T obj, long x, LongBinaryOperator accumulatorFunction) {
      long prev, next;
      do {
         prev = get(obj);
         next = accumulatorFunction.applyAsLong(prev, x);
      } while (!compareAndSet(obj, prev, next));
      return next;
   }
}
