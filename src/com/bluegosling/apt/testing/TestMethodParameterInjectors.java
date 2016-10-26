package com.bluegosling.apt.testing;

import com.bluegosling.collections.TransformingSet;
import com.bluegosling.reflect.TypeRef;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager;

/**
 * Utility implementations of {@link TestMethodParameterInjector}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class TestMethodParameterInjectors {
   private TestMethodParameterInjectors() {}

   /**
    * Creates an exception that indicates a problem with a parameter type of an injected method.
    * 
    * @param m the method
    * @param argIndex the index of the argument with a bad type
    * @param supportedTypes the set of supported types
    * @param includesProcessor true if the types supported include {@link Processor} (or any
    *       subtype thereof)
    * @return the exception with a detailed message of the offending argument
    */
   static IllegalArgumentException badArgumentType(Method m, int argIndex,
         Set<TypeRef<?>> supportedTypes, boolean includesProcessor) {
      StringBuilder sb = new StringBuilder();
      sb.append("Argument #").append(argIndex + 1).append(" of method ").append(m.getName())
            .append(" in class ").append(m.getDeclaringClass().getName())
            .append(" has unsupported type:")
            .append("\n  ").append(TypeRef.forType(m.getGenericParameterTypes()[argIndex]))
            .append("\nSupported types include:");
      for (TypeRef<?> typeRef : supportedTypes) {
         sb.append("\n  ").append(typeRef);
      }
      if (includesProcessor) {
         sb.append("\n  ? extends ").append(Processor.class.getName());
      }
      return new IllegalArgumentException(sb.toString());
   }

   // For test and before methods:
   
   /**
    * An interface for providing an injected value for a given {@link TestEnvironment}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of value injected
    */
   private interface Provider<T> extends Function<TestEnvironment, T> {
   }
   
   static final Map<TypeRef<?>, Provider<?>> providers =
         new LinkedHashMap<TypeRef<?>, Provider<?>>();
   static {
      // update javadoc for AnnotationProcessorTestRunner whenever you add anything new here!!!
      addProvider(new TypeRef<TestEnvironment>() {}, env -> env);
      addProvider(new TypeRef<TestJavaFileManager>() {}, env -> env.fileManager());
      addProvider(new TypeRef<JavaFileManager>() {}, env -> env.fileManager());
      addProvider(new TypeRef<CategorizingDiagnosticCollector>() {},
            env -> env.diagnosticCollector());
      addProvider(new TypeRef<ProcessingEnvironment>() {}, env -> env.processingEnvironment());
      addProvider(new TypeRef<Elements>() {}, env -> env.processingEnvironment().getElementUtils());
      addProvider(new TypeRef<Types>() {}, env -> env.processingEnvironment().getTypeUtils());
      addProvider(new TypeRef<Filer>() {}, env -> env.processingEnvironment().getFiler());
      addProvider(new TypeRef<Messager>() {}, env -> env.processingEnvironment().getMessager());
      addProvider(new TypeRef<SourceVersion>() {},
            env -> env.processingEnvironment().getSourceVersion());
      addProvider(new TypeRef<Map<String, String>>() {},
            env -> env.processingEnvironment().getOptions());
      addProvider(new TypeRef<RoundEnvironment>() {}, env -> env.roundEnvironment());
      addProvider(new TypeRef<Set<TypeElement>>() {}, 
            env -> Collections.unmodifiableSet(new LinkedHashSet<>(env.annotationTypes())));
      // update javadoc for AnnotationProcessorTestRunner whenever you add anything new here!!!
   }

   private static <T> void addProvider(TypeRef<T> typeRef, Provider<T> provider) {
      providers.put(typeRef, provider);
   }

   /**
    * An injector that can be used to inject parameters into methods under test.
    */
   public static TestMethodParameterInjector<TestEnvironment> FOR_TEST_METHODS =
         new TestMethodParameterInjector<TestEnvironment>() {
            @Override
            public Object[] getInjectedParameters(Method m, TestEnvironment testEnv) {
               Class<?> argClasses[] = m.getParameterTypes();
               Type argTypes[] = m.getGenericParameterTypes();
               Object ret[] = new Object[argClasses.length];
               for (int i = 0, len = ret.length; i < len; i++) {
                  Type t = argTypes[i];
                  Provider<?> provider = providers.get(TypeRef.forType(t));
                  if (provider != null) {
                     ret[i] = provider.apply(testEnv);
                  } else if (!Processor.class.isAssignableFrom(argClasses[i])) {
                        throw badArgumentType(m, i, providers.keySet(), true);
                  } else {
                     Processor p = testEnv.processorUnderTest();
                     if (p != null && !argClasses[i].isInstance(p)) {
                        throw new ClassCastException(
                              "Argument #" + (i + 1) + " of method "+ m.getName()
                              + " in class " + m.getDeclaringClass().getName()
                              + " expecting type " + argClasses[i].getName()
                              + " but got " + p.getClass().getName());
                     }
                     ret[i] = p;
                  }
               }
               return ret;
            }
            @Override
            public void validateParameterTypes(Method m,
                  List<? super IllegalArgumentException> errors) {
               Class<?> argClasses[] = m.getParameterTypes();
               Type argTypes[] = m.getGenericParameterTypes();
               for (int i = 0, len = argClasses.length; i < len; i++) {
                  Type t = argTypes[i];
                  if (!providers.containsKey(TypeRef.forType(t))) {
                     if (!Processor.class.isAssignableFrom(argClasses[i])) {
                        errors.add(badArgumentType(m, i, providers.keySet(), true));
                     }
                  }
               }
            }
         };
   
   // For before methods:

   static Set<Class<?>> allowedBeforeClasses = new HashSet<>(
         Arrays.<Class<?>>asList(TestJavaFileManager.class, JavaFileManager.class));
      
   static Set<TypeRef<?>> allowedBeforeTypes =
         new TransformingSet<>(allowedBeforeClasses, clazz -> TypeRef.forClass(clazz));
         
   /**
    * An injector that can be used to inject parameters into "before" methods (those run before the
    * method under test, typically to perform setup that applies to all methods).
    */
   public static TestMethodParameterInjector<TestJavaFileManager> FOR_BEFORE_METHODS =
         new TestMethodParameterInjector<TestJavaFileManager>() {
            @Override
            public Object[] getInjectedParameters(Method m, TestJavaFileManager fileManager) {
               Class<?> argClasses[] = m.getParameterTypes();
               Object ret[] = new Object[argClasses.length];
               for (int i = 0, len = ret.length; i < len; i++) {
                  if (allowedBeforeClasses.contains(argClasses[i])) {
                     ret[i] = fileManager;
                  } else {
                     throw badArgumentType(m, i, allowedBeforeTypes, false);
                  }
               }
               return ret;
            }
            @Override
            public void validateParameterTypes(Method m,
                  List<? super IllegalArgumentException> errors) {
               Class<?> argClasses[] = m.getParameterTypes();
               for (int i = 0, len = argClasses.length; i < len; i++) {
                  if (!allowedBeforeClasses.contains(argClasses[i])) {
                     errors.add(badArgumentType(m, i, allowedBeforeTypes, false));
                  }
               }
            }
         };
}
