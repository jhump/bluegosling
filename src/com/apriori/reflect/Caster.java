package com.apriori.reflect;

import com.apriori.util.Function;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An object that can cast objects to arbitrary interfaces. This can be used to cast objects to
 * interfaces they don't <em>technically</em> implement but do provide compatible methods. This can
 * even be used to cast objects to an interface that it only partially implements (i.e. some methods
 * on the interface have corresponding methods on the object, but not all). Finally, it can be used
 * to create proxies that perform dynamic dispatch. This reduces method invocation performance but
 * allows for dynamic methods (not otherwise available in Java unless "simulated" through techniques
 * like the visitor pattern).
 * 
 * <p>A few examples:
 * <pre>
 * MyAnnotation1 ann1;
 * // Annotation types cannot implement or extend other interfaces or other
 * // annotation types. But if it has the same methods as another interface,
 * // Caster comes to the rescue!
 * Caster&lt;Interface&gt; caster = Caster.builder(Interface.class).build();
 * Interface i = caster.cast(ann1);
 * 
 * SomeClass obj = new SomeClass();
 * // SomeClass implements some but not all methods on Interface, so it can't
 * // be cast normally because it doesn't actually implement this interface.
 * Interface i = Caster.builder(Interface.class)
 *                  .allowingUnsupportedOperations()
 *                  .build()
 *                  .cast(obj);
 * 
 * ClassWithManyOverriddenMethodsForMultipleDispatch obj;
 * // Creates a very "loose" proxy, that is forgiving in many aspects, performs
 * // dynamic dispatch, converts or re-casts values as necessary, etc.
 * Interface i = Caster.builder(Interface.class)
 *                  .withDynamicMethods()
 *                  .castingArguments()
 *                  .castingReturnTypes()
 *                  .ignoringAmbiguities()
 *                  .expandingVarArgs()
 *                  .allowingUnsupportedOperations()
 *                  .build()
 *                  .cast(obj);
 * </pre>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the interface type to which objects are cast
 */
// TODO: javadoc!!!
public class Caster<T> {
   private final Class<T> iface;
   private final boolean dynamicMethods;
   private final boolean allowUnsupportedOperations;
   final boolean castReturnTypes;
   final boolean castArguments;
   final boolean expandVarArgs;
   final boolean ignoreAmbiguities;
   
   /**
    * Constructs a new caster.
    * 
    * @param iface the interface to which objects are cast
    * @param dynamicMethods true if proxy performs dynamic dispatch
    * @param castReturnTypes true if proxy will "cast" return types if necessary
    * @param castArguments true if proxy will "cast" arguments if necessary
    * @param expandVarArgs true if proxy will expand var-args to find suitable dispatch method
    * @param allowUnsupportedOperations true if caster will cast objects that do not fully implement
    *       the target interface
    * @param ignoreAmbiguities true if cast will cast objects that have possibly ambiguous methods
    *       to which a target interface method could be dispatched
    *       
    * @see Builder#build()
    */
   Caster(Class<T> iface, boolean dynamicMethods, boolean castReturnTypes,
         boolean castArguments, boolean expandVarArgs,
         boolean allowUnsupportedOperations,
         boolean ignoreAmbiguities) {
      this.iface = iface;
      this.dynamicMethods = dynamicMethods;
      this.castReturnTypes = castReturnTypes;
      this.castArguments = castArguments;
      this.expandVarArgs = expandVarArgs;
      this.allowUnsupportedOperations = allowUnsupportedOperations;
      this.ignoreAmbiguities = ignoreAmbiguities;
   }
   
   /**
    * Creates a caster with the same properties as this one except that it casts to a different
    * target interface.
    * 
    * @param otherInterface the target interface
    * @return a new caster
    */
   public <O> Caster<O> to(Class<O> otherInterface) {
      return new Caster<O>(otherInterface, dynamicMethods, castReturnTypes,
            castArguments, expandVarArgs, allowUnsupportedOperations, ignoreAmbiguities);
   }
   
   /**
    * Casts the specified object to this caster's target interface. If the specified object is
    * {@code null} then {@code null} is returned.
    * 
    * <p>The result object is a proxy that implements the target interface. If the caster is
    * creating proxies with dynamic methods, compatibility checks are delayed to runtime when a
    * target interface method is actually invoked. Otherwise, several compatibility checks are done
    * in this method, and a proxy will not be created if the checks fail. The following conditions
    * will cause the checks to fail:
    * <ul>
    * <li>The object does not have a compatible method for every target interface method
    * <strong>and</strong> the caster is not allowing unsupported operations.</li>
    * <li>The object has multiple compatible methods, all of which are maximally specific, for any
    * target interface method <strong>and</strong> the caster is not ignoring ambiguities.</li>
    * </ul>
    * 
    * <p>Definition of a "compatible method" warrants some attention as well.
    * <ul>
    * <li>A method on the object that has the same signature (i.e. same name and same argument
    * types) and same return type is compatible.</li>
    * <li>A method on the object that has a "compatible" signature and "compatible" return type
    * is also compatible.
    *   <ul>
    *   <li>A "compatible" signature means that for a given argument position, the argument type on
    *   the object's method <em>is assignable from</em> the corresponding argument type on the
    *   target interface method.</li>
    *   <li>A "compatible" return type means that the target method's return type <em>is assignable
    *   from</em> the return type of the object's method.</li>
    *   <li>A numeric primitive is assignable from a smaller primitive type; e.g. an {@code int}
    *   is assignable from a {@code byte}. Perhaps counter-intuitively, an integer type can always
    *   be assigned to a floating point type, even if the integer type is not smaller. So a
    *   {@code float} is assignable from a {@code long}. These allowed numeric type conversions are
    *   the same as allowed without an explicit cast in the Java programming language.</li>
    *   <li>A primitive type is compatible (both assignable <em>to</em> and assignable <em>from</em>)
    *   its corresponding boxed type.</li>
    *   </ul>
    * </li>
    * <li>If a method's signature is not compatible per the points above, but the object has a
    * method with the same number of arguments and is only incompatible due to the argument type and
    * the object method's argument type is an interface, then the method is considered compatible if
    * and only if the caster is casting arguments. At runtime, the object supplied to the target
    * interface method will be cast (using the same caster configuration as this caster) to
    * the appropriate interface type.</li>
    * <li>If a method's return type is not compatible per the points above, but the target interface
    * method's return type is an interface, then the method is considered compatible if and only if
    * the caster is casting returned values. At runtime, the value returned by the object's method
    * will be cast (using the same caster configuration as this caster) to the interface method's
    * return type.</li>
    * <li>If a method's signature is not compatible per the points above, the object has a method
    * that is variable arity, and the target interface method's signature is compatible if viewed
    * as a variable arity callsite (such that the argument types match the var-args element type and
    * could thus be packaged as an array and passed into the variable arity method) then the method
    * is considered compatible.</li>
    * <li>If a method's signature is not compatible per the points above, and the target interface
    * method is variable arity, and it could be compatible at runtime after expanding the actual
    * var-args array contents and comparing runtime types to the object's methods then the method
    * is considered compatible if and only if the caster creates proxies that perform dynamic
    * dispatch and expanding of var-args is allowed.</li>
    * </ul>
    * 
    * <p>In the event that there is more than one compatible method, the most specific one will be
    * selected. The following rules, presented in terms of precedence, are used to determine the
    * relative specificity of two methods.
    * <ul>
    * <li>A method that does not require expanding an incoming var-args array is more specific than
    * any method that does.</li>
    * <li>A method with more arguments than another is more specific than the other (actual
    * number of arguments only comes into play when dealing with var-args methods).</li>
    * <li>A method that requires fewer "casts" than another (whether to cast arguments to
    * interfaces or to cast a return value to an interface) is more specific than the other.</li>
    * <li>A method that does not require target method arguments to be packaged into a var-args
    * array is more specific than one that does.</li>
    * <li>A method that does not require auto-boxing or auto-unboxing of primitive arguments or
    * return types is more specific than one that does.</li>
    * <li>A method whose arguments are all assignable to (e.g. are sub-types of) the corresponding
    * arguments of another method is more specific than that other method.</li>
    * </ul>
    * It is possible for multiple methods to be compatible and for none to be "more specific" than
    * another. This means the dispatch method is ambiguous. This is generally not allowed, but if
    * the caster is ignoring ambiguities then one of the methods will be selected arbitrarily.
    * 
    * <p>The rules for specificity are similar to how the Java compiler selects a method to invoke
    * given a set of arguments in a method invocation expression. The main difference is that with
    * this caster, a var-args method with more declared parameters is considered more specific than
    * one with fewer. In contract, the Java compiler can fail complaining about ambiguous methods in
    * such cases, since it does not use parameter count explicitly as a factor in specificity.
    * 
    * @param o the object
    * @return a proxy that implements the target interface and delegates method calls to the
    *       specified object
    * @throws ClassCastException if a proxy cannot be created
    */
   @SuppressWarnings("unchecked")
   public T cast(final Object o) {
      if (o == null || (iface.isInstance(o) && !dynamicMethods)) {
         return (T) o;
      }
      final Map<Method, InvocationHandler> methodMap = new HashMap<Method, InvocationHandler>();
      Class<?> targetClass = o.getClass();
      for (Method m : iface.getMethods()) {
         boolean hasCompatibleMethod = true;
         if (dynamicMethods) {
            // for dynamic methods, we just need to catalog the possible candidates here and then
            // bind the actual method at runtime depending on runtime types of the arguments
            final DispatchCandidates candidates =
                  getDynamicDispatchCandidates(m, targetClass, expandVarArgs);
            if (candidates.isEmpty()) {
               // no way to even do a dynamic bind
               hasCompatibleMethod = false;
            } else {
               methodMap.put(m,  new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args)
                        throws Throwable {
                     return invokeDynamic(Caster.this, o, candidates, method, args, castArguments,
                           castReturnTypes, expandVarArgs, ignoreAmbiguities);
                  }
               });
            }
         } else {
            try {
               final Method targetMethod = targetClass.getMethod(m.getName(), m.getParameterTypes());
               ConversionStrategy strategy = getConversionStrategy(targetMethod.getReturnType(),
                     m.getReturnType(), castReturnTypes);
               if (strategy != null) {
                  final Converter converter = strategy.getConverter();
                  if (converter == null) {
                     methodMap.put(m, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                           return targetMethod.invoke(o, args);
                        }
                     });
                  } else {
                     methodMap.put(m, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                           return converter.convert(targetMethod.invoke(o, args), Caster.this);
                        }
                     });
                  }
               } else {
                  // incompatible return type!
                  hasCompatibleMethod = false;
               }
            } catch (NoSuchMethodException e) {
               // no exact match found, so try to find a compatible method
               DispatchCandidates candidates =
                     getDynamicDispatchCandidates(m, targetClass, expandVarArgs);
               if (candidates.isEmpty()) {
                  hasCompatibleMethod = false;
               } else {
                  Collection<DispatchCandidate> bestCandidates = null;
                  bestCandidates = getBestCandidates(candidates, m, m.getParameterTypes(),
                        castArguments, castReturnTypes, false);
                  try {
                     requireOneCandidate(m, bestCandidates, ignoreAmbiguities);
                  } catch (NoSuchMethodException nsme) {
                     hasCompatibleMethod = false;
                  } catch (AmbiguousMethodException ame) {
                     hasCompatibleMethod = false;
                  }
                  if (hasCompatibleMethod) {
                     final DispatchCandidate finalCandidate = bestCandidates.iterator().next();
                     methodMap.put(m, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args)
                              throws Throwable {
                           return finalCandidate.invoke(Caster.this, o, args);
                        }
                     });
                  }
               }
            }
         }
         if (!hasCompatibleMethod) {
            if (allowUnsupportedOperations) {
               methodMap.put(m, new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                     throw new UnsupportedOperationException(method.getName());
                  }
               });
            } else {
               // TODO handle the case where ambiguous methods is the cause vs. no such method...
               StringBuilder sb = new StringBuilder();
               sb.append("Specified object, of type ");
               sb.append(targetClass.getName());
               sb.append(", has no method that is compatible with:\n");
               sb.append(m.getReturnType().getName());
               sb.append(" ");
               sb.append(m.getName());
               sb.append("(");
               boolean first = true;
               for (Class<?> argClass : m.getParameterTypes()) {
                  if (first) {
                     first = false;
                  } else {
                     sb.append(",");
                  }
                  sb.append(argClass.getName());
               }
               sb.append(")");
               throw new ClassCastException(sb.toString());
            }
         }
      }
      return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface },
            new InvocationHandler() {
               @Override
               public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  InvocationHandler dispatcher = methodMap.get(method);
                  if (dispatcher == null) {
                     // this should only happen for methods declared on Object since all interface
                     // methods should be accounted for in the map
                     return method.invoke(o, args);
                  }
                  return dispatcher.invoke(proxy, method, args);
               }
            });      
   }
   
   /**
    * Creates a new builder for configuring and creating {@link Caster}s.
    * 
    * @param iface the interface to which the caster will cast objects
    * @return a new builder
    */
   public static <T> Builder<T> builder(Class<T> iface) {
      return new Builder<T>(iface);
   }

   /**
    * A builder of {@link Caster}s.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the interface type to which objects will be cast
    */
   public static class Builder<T> {
      private final Class<T> iface;
      private boolean dynamicMethods;
      private boolean castReturnTypes;
      private boolean castArguments;
      private boolean expandVarArgs;
      private boolean allowUnsupportedOperations;
      private boolean ignoreAmbiguities;
      
      /**
       * Constructs a new builder.
       * 
       * @param iface the interface type to which the caster will cast objects
       * 
       * @see Caster#builder(Class)
       */
      Builder(Class<T> iface) {
         if (!iface.isInterface()) {
            throw new IllegalArgumentException("Specified class, " + iface.getName() + ", is not an interface");
         }
         this.iface = iface;
      }
      
      /**
       * Creates a builder with the same properties as this one except that it creates casters that
       * cast to a different target interface.
       * 
       * @param otherInterface the target interface
       * @return a new builder
       */
      public <O> Builder<O> to(Class<O> otherInterface) {
         Builder<O> ret = new Builder<O>(otherInterface);
         ret.dynamicMethods = this.dynamicMethods;
         ret.castReturnTypes = this.castReturnTypes;
         ret.castArguments = this.castArguments;
         ret.expandVarArgs = this.expandVarArgs;
         ret.allowUnsupportedOperations = this.allowUnsupportedOperations;
         ret.ignoreAmbiguities = this.ignoreAmbiguities;
         return ret;
      }

      public Builder<T> withDynamicMethods() {
         this.dynamicMethods = true;
         return this;
      }

      public Builder<T> castingReturnTypes() {
         this.castReturnTypes = true;
         return this;
      }

      public Builder<T> castingArguments() {
         this.castArguments = true;
         return this;
      }
      
      public Builder<T> expandingVarArgs() {
         this.expandVarArgs = true;
         return this;
      }

      public Builder<T> allowingUnsupportedOperations() {
         this.allowUnsupportedOperations = true;
         return this;
      }
      
      public Builder<T> ignoringAmbiguities() {
         this.ignoreAmbiguities = true;
         return this;
      }

      public Caster<T> build() {
         return new Caster<T>(iface, dynamicMethods, castReturnTypes, castArguments,
               expandVarArgs, allowUnsupportedOperations, ignoreAmbiguities);
      }
   }
   
   /**
    * Returns true if the first specified method is more specific than the second. This does not
    * quite use all of the specificity rules described above. It only looks at argument types,
    * checking if the types of one method are sub-types of corresponding types of the other. If
    * the two methods have a different number of arguments then {@code false} will always be
    * returned.
    * 
    * @param method the first method
    * @param other the second method
    * @return true if the first method is "more specific" than the second (based solely on argument
    *       types)
    */
   static boolean isMoreSpecific(Method method, Method other) {
      Class<?> argTypes1[] = method.getParameterTypes();
      Class<?> argTypes2[] = other.getParameterTypes();
      if (argTypes1.length != argTypes2.length) {
         return false;
      } else {
         for (int i = 0, len = argTypes1.length; i < len; i++) {
            if (argTypes1[i].isAssignableFrom(argTypes2[i])
                  && !argTypes1[i].equals(argTypes2[i])) {
               return false;
            }
         }
         return true;
      }
   }
   
   /**
    * A set of candidates for selecting a dispatch method. Internally, the set is organized into
    * a list of lists of methods. Each top-level list contains a list of methods that whose types
    * are related in that one method will be more or less specific than another in that list. A
    * list of methods is sorted such that the most specific method is first and the least specific
    * method is last. Methods in one top-level list are orthogonal to those in another top-level
    * list -- meaning that a method in one list will neither be more nor less specific than a method
    * in a different list.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class DispatchCandidates implements Iterable<Iterable<Method>> {
      private final List<List<Method>> methods;
      
      DispatchCandidates() {
         methods = new ArrayList<List<Method>>();
      }
      
      public void add(Method m) {
         for (List<Method> category : methods) {
            Method other = category.get(0);
            if (isMoreSpecific(m, other) || isMoreSpecific(other,  m)) {
               category.add(m);
            }
         }
         List<Method> category = new ArrayList<Method>();
         methods.add(category);
         category.add(m);
      }
      
      public void organize() {
         for (List<Method> category : methods) {
            Collections.sort(category, new Comparator<Method>() {
               @Override
               public int compare(Method o1, Method o2) {
                  if (isMoreSpecific(o1, o2)) {
                     return -1;
                  } else if (isMoreSpecific(o2, o1)) {
                     return 1;
                  } else {
                     return 0;
                  }
               }
            });
         }
      }
      
      public boolean isEmpty() {
         return methods.isEmpty();
      }
      
      @SuppressWarnings({ "unchecked", "rawtypes" })
      @Override
      public Iterator<Iterable<Method>> iterator() {
         return (Iterator) methods.iterator();
      }
   }
   
   static DispatchCandidates getDynamicDispatchCandidates(Method method, Class<?> dispatchClass,
         boolean expandVarArgs) {
      DispatchCandidates methods = new DispatchCandidates();
      int argsLen = method.getParameterTypes().length;
      String name = method.getName();
      for (Method m : dispatchClass.getMethods()) {
         if (m.getName().equals(name)) {
            int otherLen = m.getParameterTypes().length;
            if (otherLen == argsLen || (m.isVarArgs() && otherLen <= argsLen + 1)
                  || (expandVarArgs && method.isVarArgs() && argsLen <= otherLen + 1)) {
               methods.add(m);
            }
         }
      }
      methods.organize();
      return methods;
   }
   
   /**
    * Converts a value from one form to another, possible with the aid of a {@link Caster}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private interface Converter {
      Object convert(Object in, Caster<?> caster);
   }
   
   /**
    * A single dispatch candidate. A dispatch candidate represents a single method on the cast
    * object and includes sufficient information to actually invoke the method, given the set of
    * arguments passed to the target interface method.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class DispatchCandidate {
      private final Method method;
      private final int numArgs;
      private final int phase;
      private final int numCastArgs;
      private final Converter argConverters[];
      private final Function<Object[], Object[]> varArgsConverter;
      private final Converter returnConverter;
      
      DispatchCandidate(Method method, boolean autobox, boolean varArgs, int numCastArgs,
            Converter argConverters[], Function<Object[], Object[]> varArgsConverter,
            Converter returnConverter) {
         this.method = method;
         this.numArgs = method.getParameterTypes().length;
         this.phase = (autobox ? 1 : 0) + (varArgs ? 2 : 0);
         this.numCastArgs = numCastArgs;
         this.argConverters = argConverters;
         this.varArgsConverter = varArgsConverter;
         this.returnConverter = returnConverter;
      }
      
      public Method getMethod() {
         return method;
      }
      
      public int getNumArgs() {
         return numArgs;
      }
      
      public int getPhase() {
         return phase;
      }
      
      public int getNumCastArgs() {
         return numCastArgs;
      }
      
      public Object invoke(Caster<?> caster, Object obj, Object args[]) throws Throwable {
         if (argConverters != null) {
            for (int i = 0, len = args.length; i < len; i++) {
               Converter converter = argConverters[i];
               if (converter != null) {
                  args[i] = converter.convert(args[i], caster);
               }
            }
         }
         if (varArgsConverter != null) {
            args = varArgsConverter.apply(args);
         }
         Object ret = method.invoke(obj, args);
         if (returnConverter != null) {
            ret = returnConverter.convert(ret, caster);
         }
         return ret;
      }
   }
   
   /**
    * A simple conversion pair. This is just a pair of types: "from" and "to". These are used as
    * keys in a map of implicit numeric primitive conversions.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Conversion {
      public final Class<?> from;
      public final Class<?> to;

      Conversion(Class<?> from, Class<?> to) {
         this.from = from;
         this.to = to;
      }
      
      @Override
      public boolean equals(Object o) {
         if (o instanceof Conversion) {
            Conversion other = (Conversion) o;
            return from.equals(other.from) && to.equals(other.to);
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return from.hashCode() * 31 + to.hashCode();
      }
   }
   
   static final Map<Class<?>, Class<?>> autoBoxTypes = new HashMap<Class<?>, Class<?>>();
   static final Map<Class<?>, Class<?>> autoUnboxTypes = new HashMap<Class<?>, Class<?>>();
   static final Map<Conversion, Function<Object, ?>> conversions =
         new HashMap<Conversion, Function<Object, ?>>();
   
   /*
    * Populate the three maps above.
    */
   static {
      autoBoxTypes.put(boolean.class, Boolean.class);
      autoBoxTypes.put(byte.class, Byte.class);
      autoBoxTypes.put(char.class, Character.class);
      autoBoxTypes.put(short.class, Short.class);
      autoBoxTypes.put(int.class, Integer.class);
      autoBoxTypes.put(long.class, Long.class);
      autoBoxTypes.put(float.class, Float.class);
      autoBoxTypes.put(double.class, Double.class);
      
      autoUnboxTypes.put(Boolean.class, boolean.class);
      autoUnboxTypes.put(Byte.class, byte.class);
      autoUnboxTypes.put(Character.class, char.class);
      autoUnboxTypes.put(Short.class, short.class);
      autoUnboxTypes.put(Integer.class, int.class);
      autoUnboxTypes.put(Long.class, long.class);
      autoUnboxTypes.put(Float.class, float.class);
      autoUnboxTypes.put(Double.class, double.class);
      
      addConversion(byte.class, short.class, new Function<Byte, Short>() {
         @Override public Short apply(Byte input) {
            return (short) input.byteValue();
         }
      });
      addConversion(byte.class, int.class, new Function<Byte, Integer>() {
         @Override public Integer apply(Byte input) {
            return (int) input.byteValue();
         }
      });
      addConversion(byte.class, long.class, new Function<Byte, Long>() {
         @Override public Long apply(Byte input) {
            return (long) input.byteValue();
         }
      });
      addConversion(byte.class, float.class, new Function<Byte, Float>() {
         @Override public Float apply(Byte input) {
            return (float) input.byteValue();
         }
      });
      addConversion(byte.class, double.class, new Function<Byte, Double>() {
         @Override public Double apply(Byte input) {
            return (double) input.byteValue();
         }
      });
      addConversion(char.class, int.class, new Function<Character, Integer>() {
         @Override public Integer apply(Character input) {
            return (int) input.charValue();
         }
      });
      addConversion(char.class, long.class, new Function<Character, Long>() {
         @Override public Long apply(Character input) {
            return (long) input.charValue();
         }
      });
      addConversion(char.class, float.class, new Function<Character, Float>() {
         @Override public Float apply(Character input) {
            return (float) input.charValue();
         }
      });
      addConversion(char.class, double.class, new Function<Character, Double>() {
         @Override public Double apply(Character input) {
            return (double) input.charValue();
         }
      });
      addConversion(short.class, int.class, new Function<Short, Integer>() {
         @Override public Integer apply(Short input) {
            return (int) input.shortValue();
         }
      });
      addConversion(short.class, long.class, new Function<Short, Long>() {
         @Override public Long apply(Short input) {
            return (long) input.shortValue();
         }
      });
      addConversion(short.class, float.class, new Function<Short, Float>() {
         @Override public Float apply(Short input) {
            return (float) input.shortValue();
         }
      });
      addConversion(short.class, double.class, new Function<Short, Double>() {
         @Override public Double apply(Short input) {
            return (double) input.shortValue();
         }
      });
      addConversion(int.class, long.class, new Function<Integer, Long>() {
         @Override public Long apply(Integer input) {
            return (long) input.intValue();
         }
      });
      addConversion(int.class, float.class, new Function<Integer, Float>() {
         @Override public Float apply(Integer input) {
            return (float) input.intValue();
         }
      });
      addConversion(int.class, double.class, new Function<Integer, Double>() {
         @Override public Double apply(Integer input) {
            return (double) input.intValue();
         }
      });
      addConversion(long.class, float.class, new Function<Long, Float>() {
         @Override public Float apply(Long input) {
            return (float) input.longValue();
         }
      });
      addConversion(long.class, double.class, new Function<Long, Double>() {
         @Override public Double apply(Long input) {
            return (double) input.longValue();
         }
      });
      addConversion(float.class, double.class, new Function<Float, Double>() {
         @Override public Double apply(Float input) {
            return (double) input.floatValue();
         }
      });
   }
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   static <F, T> void addConversion(Class<F> from, Class<T> to, Function<F, T> converter) {
      Function f = converter;
      conversions.put(new Conversion(from, to), f);
   }
   
   private static class ConversionStrategy {
      private boolean requiresCast;
      private boolean requiresAutoBoxOrUnbox;
      private Converter converter;
      
      ConversionStrategy() {
      }
      
      void setRequiresCast() {
         requiresCast = true;
      }
      
      void setRequestAutoBoxOrUnbox() {
         requiresAutoBoxOrUnbox  = true;
      }
      
      void setConverter(Converter converter) {
         this.converter = converter;
      }
      
      boolean doesRequireCast() {
         return requiresCast;
      }
      
      boolean doesRequireAutoBoxOrUnbox() {
         return requiresAutoBoxOrUnbox;
      }
      
      Converter getConverter() {
         return converter;
      }
   }
   
   private static ConversionStrategy getConversionStrategy(Class<?> from, final Class<?> to,
         boolean castArguments) {
      ConversionStrategy strategy = new ConversionStrategy();
      if (to.isAssignableFrom(from)) {
         return strategy;
      }
      // types aren't assignable, so we need to figure out if we can convert
      Conversion conversion = new Conversion(from, to);
      if (conversions.containsKey(conversion)) {
         final Function<Object, ?> converter = conversions.get(conversion);
         strategy.setConverter(new Converter() {
            @Override
            public Object convert(Object in, Caster<?> caster) {
               return converter.apply(in);
            }
         });
         return strategy;
      }
      if (to.equals(autoBoxTypes.get(from)) || to.equals(autoUnboxTypes.get(from))) {
         // conversion not needed since reflection always resorts to boxed
         // types, but we need to know if auto-boxing/unboxing was required to
         // rank dispatch candidates
         strategy.setRequestAutoBoxOrUnbox();
         return strategy;
      }
      if (castArguments && to.isInterface()) {
         strategy.setRequiresCast();
         strategy.setConverter(new Converter() {
            @Override
            public Object convert(Object in, Caster<?> caster) {
               return caster.to(to).cast(in);
            }
         });
         return strategy;
      }
      // incompatible!!
      return null;
   }
   
   /**
    * Creates a {@link DispatchCandidate} that represents the specified candidate method as a
    * possible dispatch method for the specified target interface method.
    * 
    * @param candidateMethod the candidate method
    * @param m the target interface method
    * @param argTypes the argument types for the target interface method (could represent actual
    *       runtime argument types instead of method's declared types)
    * @param castArguments if true then the candidate is allowed to cast arguments to interfaces
    *       using a {@link Caster}, if necessary for compatibility with the target method
    * @param castReturnType if true then the candidate is allowed to cast the return value to an
    *       interface using a {@link Caster}, if necessary for compatibility with the target method
    * @param varArgsExpanded if true then the argument types specified represent runtime types with
    *       the contents of a var-args array expanded into multiple arguments
    * @return the candidate or {@code null} if the specified candidate method is not eligible
    */
   static DispatchCandidate getCandidate(Method candidateMethod, Method m, Class<?> argTypes[],
         boolean castArguments, boolean castReturnType, boolean varArgsExpanded) {
      Class<?> candidateTypes[] = candidateMethod.getParameterTypes();
      if (candidateTypes.length != argTypes.length &&
            (!candidateMethod.isVarArgs() || candidateTypes.length > argTypes.length + 1)) {
         // bad number of args? no candidate
         return null;
      } else {
         boolean varArgs = candidateTypes.length != argTypes.length;
         boolean autobox = false;
         int numCastArgs = 0;
         Converter argConverters[] = new Converter[argTypes.length];
         boolean anyArgNeedsConversion = false;
         int len = varArgs ? candidateTypes.length - 1 : candidateTypes.length;
         int i;
         for (i = 0; i < len; i++) {
            ConversionStrategy strategy = getConversionStrategy(argTypes[i], candidateTypes[i],
                  castArguments);
            if (strategy != null) {
               if (strategy.doesRequireAutoBoxOrUnbox()) {
                  autobox = true;
               }
               if (strategy.doesRequireCast()) {
                  numCastArgs++;
               }
               Converter argConverter = strategy.getConverter(); 
               if (argConverter != null) {
                  anyArgNeedsConversion = true;
                  argConverters[i] = argConverter; 
               }
            } else if (i == candidateTypes.length - 1 && candidateMethod.isVarArgs()) {
               // we'll check if argument is compatible w/ var arg element type below
               varArgs = true;
               break;
            } else {
               // incompatible!
               return null;
            }
         }
         Function<Object[], Object[]> varArgsConverter = null;
         boolean appendIncomingVarArgs = false;
         if (varArgs) {
            // compare to component type of var args array
            Class<?> candidateType = candidateTypes[i].getComponentType();
            for (; i < argTypes.length; i++) {
               if (!candidateType.isAssignableFrom(argTypes[i])
                     && !conversions.containsKey(new Conversion(argTypes[i], candidateType))) {
                  if (candidateType.equals(autoBoxTypes.get(argTypes[i]))
                        || candidateType.equals(autoUnboxTypes.get(argTypes[i]))) {
                     anyArgNeedsConversion = true;
                     Class<?> from = argTypes[i];
                     if (from.isPrimitive()) {
                        from = autoBoxTypes.get(from);
                     }
                     final Function<Object, ?> converter = conversions.get(from);
                     argConverters[i] = new Converter() {
                        @Override
                        public Object convert(Object in, Caster<?> caster) {
                           return converter.apply(in);
                        }
                     };
                     autobox = true;
                  } else if (i == argTypes.length - 1 && m.isVarArgs() && !varArgsExpanded) {
                     // the incoming method call is var args, so check its element type and compare
                     // to see if contents can be converted/appended to candidate var arg array
                     appendIncomingVarArgs = true;
                     Class<?> argType = argTypes[i].getComponentType();
                     if (candidateType.equals(autoBoxTypes.get(argType))
                           || candidateType.equals(autoUnboxTypes.get(argType))) {
                        anyArgNeedsConversion = true;
                        Class<?> from = argType;
                        if (from.isPrimitive()) {
                           from = autoBoxTypes.get(from);
                        }
                        final Function<Object, ?> converter = conversions.get(from);
                        argConverters[i] = new Converter() {
                           @Override
                           public Object convert(Object in, Caster<?> caster) {
                              // have to convert whole array
                              Object vals[] = (Object[]) in;
                              for (int j = 0, arrayLength = vals.length; j < arrayLength; j++) {
                                 vals[j] = converter.apply(vals[j]);
                              }
                              return in;
                           }
                        };
                        autobox = true;
                     } else if (castArguments && candidateType.isInterface()) {
                        anyArgNeedsConversion = true;
                        final Class<?> argInterface = candidateTypes[i];
                        argConverters[i] = new Converter() {
                           @Override
                           public Object convert(Object in, Caster<?> caster) {
                              // have to convert whole array
                              Object vals[] = (Object[]) in;
                              for (int j = 0, arrayLength = vals.length; j < arrayLength; j++) {
                                 vals[j] = caster.to(argInterface).cast(vals[j]);
                              }
                              return in;
                           }
                        };
                        numCastArgs++;
                     } else {
                        // incompatible!
                        return null;
                     }
                  } else if (castArguments && candidateType.isInterface()) {
                     anyArgNeedsConversion = true;
                     final Class<?> argInterface = candidateTypes[i];
                     argConverters[i] = new Converter() {
                        @Override
                        public Object convert(Object in, Caster<?> caster) {
                           return caster.to(argInterface).cast(in);
                        }
                     };
                     numCastArgs++;
                  } else {
                     // incompatible!
                     return null;
                  }
               }
            }
            final int candidateArgsLen = candidateTypes.length;
            final Class<?> varArgType = candidateTypes[candidateArgsLen - 1].getComponentType();
            final boolean doAppendVarArgs = appendIncomingVarArgs;
            varArgsConverter = new Function<Object[], Object[]>() {
               // we are sufficiently guarding access by checking doAppendVarArgs, but compiler
               // can't tell that this prevents null pointer de-reference
               @SuppressWarnings("null")
               @Override
               public Object[] apply(Object[] input) {
                  Object ret[] = new Object[candidateArgsLen];
                  if (candidateArgsLen > 1) {
                     System.arraycopy(input, 0, ret, 0, candidateArgsLen - 1);
                  }
                  int varArgLen = input.length - candidateArgsLen + 1;
                  Object incomingVarArgs[] = doAppendVarArgs ? (Object[]) input[input.length - 1]
                        : null;
                  Object varArgValues[];
                  if (doAppendVarArgs) {
                     varArgLen--; // skip the last arg, which is the incoming var arg array
                     varArgValues = (Object[]) Array.newInstance(varArgType, varArgLen +
                           incomingVarArgs.length);
                  } else {
                     varArgValues = (Object[]) Array.newInstance(varArgType, varArgLen);
                  }
                  System.arraycopy(input, candidateArgsLen - 1, varArgValues, 0, varArgLen);
                  if (doAppendVarArgs) {
                     System.arraycopy(incomingVarArgs, 0, varArgValues, varArgLen,
                           incomingVarArgs.length);
                  }
                  ret[ret.length - 1] = varArgValues;
                  return ret;
               }
            };
         }
         // args are good, so now check return type
         ConversionStrategy strategy = getConversionStrategy(candidateMethod.getReturnType(),
               m.getReturnType(), castReturnType);
         if (strategy == null) {
            // incompatible return type!
            return null;
         }
         if (strategy.doesRequireAutoBoxOrUnbox()) {
            autobox = true;
         }
         if (strategy.doesRequireCast()) {
            numCastArgs++;
         }
         Converter returnConverter = strategy.getConverter();
         // got a good candidate!
         return new DispatchCandidate(candidateMethod, autobox, varArgs, numCastArgs,
               anyArgNeedsConversion ? argConverters : null, varArgsConverter, returnConverter);
      }
   }
   
   static Collection<DispatchCandidate> getBestCandidates(DispatchCandidates candidates, Method m,
         Class<?> argTypes[], boolean castArguments, boolean castReturnType,
         boolean varArgsExpanded) {
      List<DispatchCandidate> bestMatches = new ArrayList<DispatchCandidate>();
      for (Iterable<Method> methods : candidates) {
         DispatchCandidate currentCandidate = null;
         for (Method method : methods) {
            DispatchCandidate maybe = getCandidate(method, m, argTypes, castArguments,
                  castReturnType, varArgsExpanded);
            if (maybe != null) {
               if (currentCandidate == null
                     || currentCandidate.getNumArgs() < maybe.getNumArgs()
                     || (currentCandidate.getNumArgs() == maybe.getNumArgs()
                           && currentCandidate.getNumCastArgs() > maybe.getNumCastArgs())
                     || (currentCandidate.getNumArgs() == maybe.getNumArgs()
                           && currentCandidate.getNumCastArgs() == maybe.getNumCastArgs()
                           && currentCandidate.getPhase() > maybe.getPhase())) {
                  currentCandidate = maybe;
                  if (currentCandidate.getPhase() == 0 && currentCandidate.getNumCastArgs() == 0) {
                     // best we'll find in this category, so don't bother looking at the others
                     break;
                  }
               }
            }
         }
         if (currentCandidate != null) {
            if (bestMatches.isEmpty()) {
               bestMatches.add(currentCandidate);
            } else {
               DispatchCandidate bestSoFar = bestMatches.get(0);
               if (bestSoFar.getNumCastArgs() > currentCandidate.getNumCastArgs()) {
                  bestMatches.clear(); // replace current best with even better
                  bestMatches.add(currentCandidate);
               } else if (bestSoFar.getNumCastArgs() == currentCandidate.getNumCastArgs()) {
                  if (currentCandidate.getPhase() < bestSoFar.getPhase()) {
                     bestMatches.clear(); // replace current best with even better
                     bestMatches.add(currentCandidate);
                  } else if (currentCandidate.getPhase() == bestSoFar.getPhase()) {
                     // add another match
                     bestMatches.add(currentCandidate);
                  }
               }
            }
         }
      }
      return bestMatches;
   }
   
   static Object invokeDynamic(Caster<?> caster, Object o, DispatchCandidates candidates, Method m,
         Object args[], boolean castArguments, boolean castReturnType, boolean expandVarArgs,
         boolean ignoreAmbiguities) throws Throwable {
      Class<?> argTypes[] = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
         argTypes[i] = args[i].getClass();
      }
      Collection<DispatchCandidate> bestCandidates = getBestCandidates(candidates, m, argTypes,
            castArguments, castReturnType, false);
      if (bestCandidates.isEmpty() && m.isVarArgs() && expandVarArgs
            && args[args.length-1] != null) {
         // expand var args array and re-try finding a dispatch method
         int nonVarArgLen = args.length - 1;
         Object varArgs[] = (Object[]) args[nonVarArgLen];
         Object newArgs[] = new Object[nonVarArgLen + varArgs.length];
         System.arraycopy(args, 0, newArgs, 0, nonVarArgLen);
         System.arraycopy(varArgs, 0, newArgs, nonVarArgLen, varArgs.length);
         Class<?> newArgTypes[] = new Class<?>[newArgs.length];
         System.arraycopy(argTypes, 0, newArgTypes, 0, nonVarArgLen);
         for (int i = 0; i < varArgs.length; i++) {
            newArgTypes[nonVarArgLen + i] = varArgs[i].getClass();
         }
         // swap
         args = newArgs;
         argTypes = newArgTypes;
         bestCandidates = getBestCandidates(candidates, m, argTypes, castArguments, castReturnType,
               true);
      }
      requireOneCandidate(m, bestCandidates, ignoreAmbiguities);
      return bestCandidates.iterator().next().invoke(caster, o, args);
   }
   
   static void requireOneCandidate(Method m, Collection<DispatchCandidate> candidates,
         boolean ignoreAmbiguities)
         throws NoSuchMethodException, AmbiguousMethodException {
      if (candidates.isEmpty()) {
         // TODO message
         throw new NoSuchMethodException(m.toString());
      } else if (candidates.size() > 1 && !ignoreAmbiguities) {
         Collection<Method> methods = new ArrayList<Method>(candidates.size());
         for (DispatchCandidate candidate : candidates) {
            methods.add(candidate.getMethod());
         }
         throw new AmbiguousMethodException(m, methods);
      }
   }
}