package com.bluegosling.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods and constants for using and creating {@code ObjectVerifier} instances.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class ObjectVerifiers {
   /**
    * Prevents instantiation.
    */
   private ObjectVerifiers() {}

   /**
    * An object verifier that doesn't actually do any verification but just returns the test object.
    */
   public static final ObjectVerifier<Object> NO_OP = new ObjectVerifier<Object>() {
      @Override
      public Object verify(Object test, Object reference) {
         return test;
      }
   };

   /**
    * An object that verifies that the test object is null if the reference object is null and
    * non-null otherwise.
    */
   public static final ObjectVerifier<Object> NULLS = new ObjectVerifier<Object>() {
      @Override
      public Object verify(Object test, Object reference) {
         if (reference == null) {
            assertNull(test);
            return null;
         }
         assertNotNull(test);
         return test;
      }
   };

   /**
    * An object that verifies that the test object is equal to the reference object via
    * {@code Object.equals()}. If the reference object is an array then this will verify that their
    * contents are equal using {@code Arrays.deepEquals(Object[], Object[])}. If the reference
    * object is an array but the test object is not, the verification will fail.
    */
   public static final ObjectVerifier<Object> EQUALS = new ObjectVerifier<Object>() {
      @Override
      public Object verify(Object test, Object reference) {
         if (reference instanceof Object[]) {
            // must both be arrays
            assertTrue(test instanceof Object[]);
            assertTrue(Arrays.deepEquals((Object[]) reference, (Object[]) test));

         }
         else {
            assertEquals(reference, test);
         }
         return test;
      }
   };

   /**
    * An object that verifies that the test object is equal to the reference object by checking that
    * their hash codes are the same. If one of the objects is null and the other is non-null,
    * verification will fail.
    */
   public static final ObjectVerifier<Object> HASH_CODES = new ObjectVerifier<Object>() {
      @Override
      public Object verify(Object test, Object reference) {
         if (NULLS.verify(test, reference) == null)
            return null;
         assertEquals(reference.hashCode(), test.hashCode());
         return test;
      }
   };

   /**
    * An object that verifies that the test object is the same instance as the reference object (via
    * the == operator).
    */
   public static final ObjectVerifier<Object> SAME = new ObjectVerifier<Object>() {
      @Override
      public Object verify(Object test, Object reference) {
         assertSame(reference, test);
         return test;
      }
   };

   /**
    * An object that verifies that the test exception has the same class as that of the reference
    * exception. If one of the objects is null and the other is non-null, verification will fail.
    */
   public static final ObjectVerifier<Throwable> STRICT_EXCEPTIONS = new ObjectVerifier<Throwable>() {
      @Override
      public Throwable verify(Throwable test, Throwable reference) {
         if (NULLS.verify(test, reference) == null)
            return null;
         assertSame(reference.getClass(), test.getClass());
         return test;
      }
   };

   /**
    * Returns an object that verifies that the test exception is <em>compatible</em> with the
    * reference exception.
    * 
    * <p>
    * The test exception is compatible if any of the following are true:
    * <ul>
    * <li>The test exception's class is the same as that of the reference exception</li>
    * <li>The test exception's class is a sub-class of that of the reference exception</li>
    * <li>Both the test exception and reference exception are sub-classes of one of the specified
    * exception classes</li>
    * </ul>
    * 
    * <p>
    * If one of the objects is null and the other is non-null, verification will fail.
    * 
    * @param exceptions the set of exception super-classes
    * @return the object verifier
    * @throws NullPointerException if the specified set or any class therein is {@code null}
    */
   public static ObjectVerifier<Throwable> relaxedExceptions(
         Set<Class<? extends Throwable>> exceptions) {
      // check inputs
      if (exceptions == null) {
         throw new NullPointerException("exception classes");
      }
      for (Class<?> c : exceptions) {
         if (c == null) {
            throw new NullPointerException("exception class");
         }
      }
      final Set<Class<? extends Throwable>> myCopy = new HashSet<Class<? extends Throwable>>(
            exceptions);
      return new ObjectVerifier<Throwable>() {
         @Override
         public Throwable verify(Throwable test, Throwable reference) {
            if (NULLS.verify(test, reference) == null)
               return null;
            Class<?> testClass = test.getClass();
            Class<?> refClass = reference.getClass();
            // see if they are directly compatible
            if (refClass.isAssignableFrom(testClass)) {
               return test;
            }
            // otherwise, they could be siblings in hierarchy -- see if
            // they have a common ancestor
            for (Class<?> c : myCopy) {
               if (c.isAssignableFrom(refClass) && c.isAssignableFrom(testClass)) {
                  return test;
               }
            }
            // no match? no good
            fail();
            return null; // make compiler happy
         }
      };
   }

   /**
    * Returns an object that verifies that the test exception is <em>compatible</em> with the
    * reference exception.
    * 
    * @param exceptions the set of allowed exception types
    * @return the object verifier
    * @throws NullPointerException if any class specified is {@code null}
    * 
    * @see #relaxedExceptions(Set)
    */
   @SafeVarargs
   public static ObjectVerifier<Throwable> relaxedExceptions(
         Class<? extends Throwable>... exceptions) {
      return relaxedExceptions(new HashSet<Class<? extends Throwable>>(Arrays.asList(exceptions)));
   }

   /**
    * Returns an object that verifies that the test object is the same as a specified instance.
    * 
    * <p>
    * This verifier is useful for testing method chaining with mutable objects. In such cases, it is
    * expected that the method will return the instance under test itself.
    * 
    * @param <T> The type of the instance and the verifier
    * @param instance The objected expected as the test value
    * @return A verifier that verifies that the test value is the same object as {@code instance}
    */
   public static <T> ObjectVerifier<T> checkInstance(final T instance) {
      return new ObjectVerifier<T>() {
         @Override
         public T verify(T test, T reference) {
            assertSame(instance, test);
            return test;
         }
      };
   }

   /**
    * Returns an object that verifies that the test object matches the reference object by checking
    * that {@link Comparable#compareTo(Object)} is zero. If one of the objects is null and the other
    * is non-null, verification will fail.
    * 
    * @param <T> the type of object being verified
    * @return the object verifier
    */
   public static <T extends Comparable<T>> ObjectVerifier<T> forComparable() {
      return new ObjectVerifier<T>() {
         @Override
         public T verify(T test, T reference) {
            if (NULLS.verify(test, reference) == null)
               return null;
            assertEquals(0, test.compareTo(reference));
            return test;
         }
      };
   }

   /**
    * Returns an object that verifies that the test object matches the reference object by using the
    * specified {@code Comparator}. The object will verify that
    * {@link Comparator#compare(Object, Object)} returns zero when comparing the test and reference
    * objects. If one of the objects is null and the other is non-null, verification will fail.
    * 
    * @param <T> the type of object being verified
    * @param c the comparator that performs the comparison
    * @return the object verifier
    * @throws NullPointerException if the specified comparator is {@code null}
    */
   public static <T> ObjectVerifier<T> fromComparator(final Comparator<T> c) {
      if (c == null) {
         throw new NullPointerException("comparator");
      }
      return new ObjectVerifier<T>() {
         @Override
         public T verify(T test, T reference) {
            if (NULLS.verify(test, reference) == null)
               return null;
            assertEquals(0, c.compare(test, reference));
            return test;
         }
      };
   }

   /**
    * Returns an object that performs all specified verifications. This verifier will return the
    * value returned by the <em>last</em> verifier provided.
    * 
    * @param <T> the type of object being verified
    * @param verifiers the sequence of verifiers to use
    * @return the object verifier
    * @throws NullPointerException if any of the specified verifiers is {@code null}
    */
   public static <T> ObjectVerifier<T> compositeVerifier(final List<ObjectVerifier<T>> verifiers) {
      // check inputs
      if (verifiers == null) {
         throw new NullPointerException("verifiers");
      }
      for (ObjectVerifier<T> v : verifiers) {
         if (v == null) {
            throw new NullPointerException("verifier");
         }
      }
      return new ObjectVerifier<T>() {
         @Override
         public T verify(T test, T reference) {
            T ret = null;
            for (ObjectVerifier<T> v : verifiers) {
               ret = v.verify(test, reference);
            }
            return ret;
         }
      };
   }

   /**
    * Returns an object that performs all specified verifications. This verifier will return the
    * value returned by the <em>last</em> verifier provided.
    * 
    * @param <T> the type of object being verified
    * @param verifiers the sequence of verifiers to use
    * @return the object verifier
    * @throws NullPointerException if any of the specified verifiers is {@code null}
    */
   @SafeVarargs
   public static <T> ObjectVerifier<T> compositeVerifier(ObjectVerifier<T>... verifiers) {
      return compositeVerifier(new ArrayList<ObjectVerifier<T>>(Arrays.asList(verifiers)));
   }

   /**
    * Returns a special object verifier that returns a testing proxy.
    * 
    * @param <T> the interface implemented by the proxy
    * @param iface a class token for the interface
    * @return a testing proxy
    * @throws IllegalArgumentException if the specified class token does not represent an interface
    * @throws NullPointerException if the specified class token is {@code null}
    * 
    * @see #forTesting(Class, ClassLoader)
    */
   public static <T> ObjectVerifier<T> forTesting(Class<T> iface) {
      if (iface == null) {
         throw new NullPointerException("interface");
      }
      return forTesting(iface, iface.getClassLoader());
   }

   /**
    * Returns a special object verifier that returns a testing proxy. The returned proxy
    * subsequently verifies that the test object implements its methods in the same way as the
    * reference object. Callers can use {@link InterfaceVerifier#verifierFor(Object)} to configure
    * how the test object's implementation should be tested. If one of the objects is null and the
    * other is non-null, verification will fail.
    * 
    * @param <T> the interface implemented by the proxy
    * @param iface a class token for the interface
    * @param classLoader the class loader used to define the proxy class
    * @return a testing proxy
    * @throws IllegalArgumentException if the specified class token does not represent an interface
    *            or if the interface is not visible from the specified class loader
    * @throws NullPointerException if the specified interface is {@code null}
    */
   public static <T> ObjectVerifier<T> forTesting(final Class<T> iface,
         final ClassLoader classLoader) {
      // verify arguments
      if (iface == null) {
         throw new NullPointerException("interface");
      }
      if (!iface.isInterface()) {
         throw new IllegalArgumentException(iface.getName()
               + " must be an interface");
      }
      // create the verifier
      return new ObjectVerifier<T>() {
         @Override
         public T verify(T test, T reference) {
            if (NULLS.verify(test, reference) == null)
               return null;
            InterfaceVerifier<T> verifier = new InterfaceVerifier<T>(iface);
            return verifier.createProxy(test, reference, classLoader);
         }
      };
   }
}
