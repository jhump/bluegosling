package com.apriori.reflect;

import static java.util.Objects.requireNonNull;

import com.apriori.reflect.DispatchSettings.Visibility;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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
// TODO: use Types.isAssignable when determining method compatibility instead of raw class tokens
public class Caster<T> {
   /** The interface to which objects are cast. */
   private final Class<T> iface;
   
   /**
    * Flag indicating whether methods are dispatched dynamically based on runtime parameter types
    * instead of formal/declared argument types.
    */
   private final boolean dynamicMethods;
   
   /** Flag indicating whether an object can be cast that doesn't have all interface methods. */
   private final boolean allowUnsupportedOperations;
   
   /** Settings that contain other dispatch flags. */
   private final DispatchSettings settings;

   /**
    * Constructs a new caster.
    * 
    * @param iface the interface to which objects are cast
    * @param dynamicMethods true if proxy performs dynamic dispatch
    * @param allowUnsupportedOperations true if caster will cast objects that do not fully implement
    *       the target interface
    * @param dispatchSettings settings and flags that control dispatch options
    *       
    * @see Builder#build()
    */
   Caster(Class<T> iface, boolean dynamicMethods, boolean allowUnsupportedOperations,
         DispatchSettings settings) {
      this.iface = iface;
      this.dynamicMethods = dynamicMethods;
      this.allowUnsupportedOperations = allowUnsupportedOperations;
      this.settings = settings;
   }
   
   // TODO: javadoc for these accessors
   public boolean isUsingDynamicDispatch() {
      return dynamicMethods;
   }
   
   public boolean isAllowingUnsupportedOperations() {
      return allowUnsupportedOperations;
   }
   
   public DispatchSettings getDispatchSettings() {
      return settings;
   }
   
   /**
    * Creates a caster with the same properties as this one except that it casts to a different
    * target interface.
    * 
    * @param otherInterface the target interface
    * @return a new caster
    */
   public <O> Caster<O> to(Class<O> otherInterface) {
      return new Caster<O>(otherInterface, dynamicMethods, allowUnsupportedOperations, settings);
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
         Throwable cause = null;
         if (dynamicMethods) {
            // for dynamic methods, we just need to catalog the possible candidates here and then
            // bind the actual method at runtime depending on runtime types of the arguments
            final DispatchCandidates candidates =
                  DispatchCandidates.getEligibleCandidates(m, targetClass,
                        settings.isExpandingVarArgs(), settings.visibility());
            if (candidates.isEmpty()) {
               // no way to even do a dynamic bind
               hasCompatibleMethod = false;
            } else {
               methodMap.put(m,  new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args)
                        throws Throwable {
                     return candidates.invokeDynamic(Caster.this, o, method, args);
                  }
               });
            }
         } else {
            try {
               final Method targetMethod;
               if (settings.visibility() == Visibility.PUBLIC) {
                  targetMethod = targetClass.getMethod(m.getName(), m.getParameterTypes());
               } else {
                  Method candidate = Members.findMethod(targetClass, m.getName(), m.getParameterTypes());
                  if (settings.visibility().isVisible(candidate.getModifiers())) {
                     targetMethod = candidate;
                     targetMethod.setAccessible(true);
                  } else {
                     throw new NoSuchMethodException(m.toString());
                  }
               }
               ConversionStrategy<?, ?> strategy = ConversionStrategy.getConversionStrategy(targetMethod.getReturnType(),
                     m.getReturnType(), settings.isCastingReturnTypes());
               if (strategy != null) {
                  @SuppressWarnings("rawtypes")
                  final Converter converter = strategy.getConverter();
                  if (converter == null) {
                     methodMap.put(m, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                           try {
                              return targetMethod.invoke(o, args);
                           } catch (InvocationTargetException e) {
                              throw e.getCause();
                           }
                        }
                     });
                  } else {
                     methodMap.put(m, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                           try {
                              return converter.convert(targetMethod.invoke(o, args), Caster.this);
                           } catch (InvocationTargetException e) {
                              throw e.getCause();
                           }
                        }
                     });
                  }
               } else {
                  // incompatible return type!
                  hasCompatibleMethod = false;
               }
            } catch (NoSuchMethodException e) {
               // no exact match found, so try to find a compatible method
               try {
                  final DispatchCandidate finalCandidate = DispatchCandidates.getBestCandidate(m,
                        targetClass, settings);
                  methodMap.put(m, new InvocationHandler() {
                     @Override
                     public Object invoke(Object proxy, Method method, Object[] args)
                           throws Throwable {
                        return finalCandidate.invoke(Caster.this, o, args);
                     }
                  });
               } catch (NoSuchMethodException nsme) {
                  hasCompatibleMethod = false;
                  cause = nsme;
               } catch (AmbiguousMethodException ame) {
                  hasCompatibleMethod = false;
                  cause = ame;
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
               StringBuilder sb = new StringBuilder();
               sb.append("Specified object, of type ");
               sb.append(targetClass.getName());
               if (cause instanceof AmbiguousMethodException) {
                  sb.append(", has ambiguous methods that are compatible with:\n");
               } else {
                  sb.append(", has no method that is compatible with:\n");
               }
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
               ClassCastException cce = new ClassCastException(sb.toString());
               if (cause != null) {
                  cce.initCause(cause);
               }
               throw cce;
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
      // see above doc for eponymously named fields of Caster
      private final Class<T> iface;
      private boolean dynamicMethods;
      private boolean allowUnsupportedOperations;
      private boolean castReturnTypes;
      private boolean castArguments;
      private boolean expandVarArgs;
      private boolean ignoreAmbiguities;
      private Visibility visibility = Visibility.PUBLIC;
      
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
         ret.allowUnsupportedOperations = this.allowUnsupportedOperations;
         ret.castReturnTypes = this.castReturnTypes;
         ret.castArguments = this.castArguments;
         ret.expandVarArgs = this.expandVarArgs;
         ret.ignoreAmbiguities = this.ignoreAmbiguities;
         ret.visibility = this.visibility;
         return ret;
      }

      /**
       * Enables dynamic methods. A {@link Caster} built with this builder will perform dynamic
       * dispatch. During dynamic dispatch, the actual method to be invoked is not chosen until
       * runtime, and it is based on the runtime types of the arguments vs. the static types
       * declared on the target interface methods.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<T> withDynamicMethods() {
         this.dynamicMethods = true;
         return this;
      }

      /**
       * Enables casting of return values. If the target interface method returns an interface and
       * the underlying object's method does not return an object that implements it, the return
       * value will be "cast" (using a {@link Caster}) to that interface. The {@link Caster} that
       * is used to perform the cast will have the same options as the one built by this builder.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<T> castingReturnTypes() {
         this.castReturnTypes = true;
         return this;
      }

      /**
       * Enables casting of arguments. If the object does not have a method that is otherwise
       * compatible then, with this enabled, an alternative may be selected. The alternative must
       * accept an interface (or multiple interface arguments) that the corresponding argument type
       * of the target interface method does not implement. In this case, the argument(s) will be
       * "cast" (using a {@link Caster}) to the interface(s). The {@link Caster} that is used to
       * perform the cast will have the same options as the one built by this builder.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<T> castingArguments() {
         this.castArguments = true;
         return this;
      }
      
      /**
       * Enables expanding of var-args. This is only performed during dynamic dispatch so it will
       * have no impact unless {@link #withDynamicMethods()} is also called on this builder. If no
       * method can be identified during dynamic dispatch that is compatible with the runtime
       * arguments of the invocation, and the method being invoked is a variable-arity method, then
       * the var-args array will be expanded and methods will be re-examined for compatibility based
       * on this expanded list of argument types (as opposed to the abbreviated argument types that
       * included an array as the last argument).
       * 
       * @return {@code this}, for method chaining
       * 
       * @see #withDynamicMethods()
       */
      public Builder<T> expandingVarArgs() {
         this.expandVarArgs = true;
         return this;
      }

      /**
       * Enables unsupported operations. Without this, if an object to be cast has no methods that
       * can be used to dispatch a target interface method, the cast will fail with a
       * {@link ClassCastException}. With this setting, the cast will succeed but invoking any of
       * these methods will result in an {@link UnsupportedOperationException}.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<T> allowingUnsupportedOperations() {
         this.allowUnsupportedOperations = true;
         return this;
      }
      
      /**
       * Configures the cast to <em>not</em> throw {@link AmbiguousMethodException}s. If any
       * ambiguities are encountered, one of the methods will be chosen arbitrarily for dispatch.
       * 
       * @return {@code this}, for method chaining
       */
      public Builder<T> ignoringAmbiguities() {
         this.ignoreAmbiguities = true;
         return this;
      }
      
      // TODO: doc!
      public Builder<T> withVisibility(Visibility visibility) {
         this.visibility = requireNonNull(visibility);
         return this;
      }
      
      // TODO: doc!
      public Builder<T> withDispatchSettings(DispatchSettings settings) {
         this.castArguments = settings.isCastingArguments();
         this.castReturnTypes = settings.isCastingReturnTypes();
         this.expandVarArgs = settings.isExpandingVarArgs();
         this.ignoreAmbiguities = settings.isIgnoringAmbiguities();
         this.visibility = settings.visibility();
         return this;
      }

      /**
       * Builds a {@link Caster}.
       * 
       * @return a {@link Caster} with the specified settings
       */
      public Caster<T> build() {
         return new Caster<T>(iface, dynamicMethods, allowUnsupportedOperations,
               new DispatchSettings(castReturnTypes, castArguments, expandVarArgs,
                     ignoreAmbiguities, visibility));
      }
   }
}

// TODO: add cool stuff!
// 1) consider add'l out-of-box conversions:
//    * primitive int to Object or Number, for example, via auto-boxing
//    * converting of array elements (like "casting" or auto-boxing/unboxing) not just for var-args
//      (especially useful for annotation return types)
//    * arrays -> lists and vice versa?
// 2) add hooks to make conversions extensible by app code. provide more control over "casting" of
//    interface return types and args -- like being to specify which methods and even which types
// 3) allow any interface method to return void and just discard any non-void return from dispatch