package com.apriori.apt.testing;

import com.apriori.reflect.TypeRef;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Utility methods for injecting arguments into test methods. This allows tests to define various incoming
 * parameters to receive information about the processing environment. This methods in this class are used
 * to inject values for the parameters at runtime.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see TestEnvironment
 * @see TestMethodProcessor
 * @see AnnotationProcessorTestRunner
 */
final class TestMethodParameterInjector {
   private TestMethodParameterInjector() {}
   
   /**
    * An interface for providing an injected value for a given {@link TestEnvironment}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of value injected
    */
   private interface Provider<T> {
      T getValueFrom(TestEnvironment env);
   }
   
   private static final Map<TypeRef<?>, Provider<?>> providers = new LinkedHashMap<TypeRef<?>, Provider<?>>();
   static {
      // update javadoc for AnnotationProcessorTestRunner whenever you add anything new here!!!
      addProvider(new TypeRef<TestEnvironment>() {}, new Provider<TestEnvironment>() {
         @Override
         public TestEnvironment getValueFrom(TestEnvironment env) {
            return env;
         }
      });
      addProvider(new TypeRef<TestJavaFileManager>() {}, new Provider<TestJavaFileManager>() {
         @Override
         public TestJavaFileManager getValueFrom(TestEnvironment env) {
            return env.fileManager();
         }
      });
      addProvider(new TypeRef<JavaFileManager>() {}, new Provider<JavaFileManager>() {
         @Override
         public JavaFileManager getValueFrom(TestEnvironment env) {
            return env.fileManager();
         }
      });
      addProvider(new TypeRef<CategorizingDiagnosticCollector>() {}, new Provider<CategorizingDiagnosticCollector>() {
         @Override
         public CategorizingDiagnosticCollector getValueFrom(TestEnvironment env) {
            return env.diagnosticCollector();
         }
      });
      addProvider(new TypeRef<ProcessingEnvironment>() {}, new Provider<ProcessingEnvironment>() {
         @Override
         public ProcessingEnvironment getValueFrom(TestEnvironment env) {
            return env.processingEnvironment();
         }
      });
      addProvider(new TypeRef<Elements>() {}, new Provider<Elements>() {
         @Override
         public Elements getValueFrom(TestEnvironment env) {
            return env.processingEnvironment().getElementUtils();
         }
      });
      addProvider(new TypeRef<Types>() {}, new Provider<Types>() {
         @Override
         public Types getValueFrom(TestEnvironment env) {
            return env.processingEnvironment().getTypeUtils();
         }
      });
      addProvider(new TypeRef<Filer>() {}, new Provider<Filer>() {
         @Override
         public Filer getValueFrom(TestEnvironment env) {
            return env.processingEnvironment().getFiler();
         }
      });
      addProvider(new TypeRef<Messager>() {}, new Provider<Messager>() {
         @Override
         public Messager getValueFrom(TestEnvironment env) {
            return env.processingEnvironment().getMessager();
         }
      });
      addProvider(new TypeRef<SourceVersion>() {}, new Provider<SourceVersion>() {
         @Override
         public SourceVersion getValueFrom(TestEnvironment env) {
            return env.processingEnvironment().getSourceVersion();
         }
      });
      addProvider(new TypeRef<Map<String, String>>() {}, new Provider<Map<String, String>>() {
         @Override
         public Map<String, String> getValueFrom(TestEnvironment env) {
            return env.processingEnvironment().getOptions();
         }
      });
      addProvider(new TypeRef<RoundEnvironment>() {}, new Provider<RoundEnvironment>() {
         @Override
         public RoundEnvironment getValueFrom(TestEnvironment env) {
            return env.roundEnvironment();
         }
      });
      addProvider(new TypeRef<Set<TypeElement>>() {}, new Provider<Set<TypeElement>>() {
         @Override
         public Set<TypeElement> getValueFrom(TestEnvironment env) {
            return Collections.unmodifiableSet(new LinkedHashSet<TypeElement>(env.annotationTypes()));
         }
      });
      // update javadoc for AnnotationProcessorTestRunner whenever you add anything new here!!!
   }

   private static <T> void addProvider(TypeRef<T> typeRef, Provider<T> provider) {
      providers.put(typeRef, provider);
   }
   
   /**
    * Gets the parameter values to use for invoking the specified method.
    * 
    * @param m the method whose parameter values are returned
    * @param testEnv the current test environment
    * @return parameter values, injected from the environment
    * @throws IllegalArgumentException if the specified method contains an argument whose
    *       type cannot be injected
    * @throws ClassCastException if the specified method accepts an argument whose type is
    *       a sub-class or sub-interface of {@link Processor} that is not compatible with
    *       the actual type of the processor under test
    */
   public static Object[] getInjectedParameters(Method m, TestEnvironment testEnv) {
      Class<?> argClasses[] = m.getParameterTypes();
      Type argTypes[] = m.getGenericParameterTypes();
      Object ret[] = new Object[argClasses.length];
      for (int i = 0, len = ret.length; i < len; i++) {
         Type t = argTypes[i];
         Provider<?> provider = providers.get(TypeRef.forType(t));
         if (provider != null) {
            ret[i] = provider.getValueFrom(testEnv);
         } else if (!Processor.class.isAssignableFrom(argClasses[i])) {
               throw badArgumentType(m, i);
         } else {
            Processor p = testEnv.processorUnderTest();
            if (p != null && !argClasses[i].isInstance(p)) {
               throw new ClassCastException("Argument #" + (i + 1) + " of method " + m.getName()
                     + "\nin class " + m.getDeclaringClass().getName()
                     + "\nexpecting type " + argClasses[i].getName()
                     + "\nbut got " + p.getClass().getName());
            }
            ret[i] = p;
         }
      }
      return ret;
   }
   
   /**
    * Verifies that the specified method has injectable parameter types. If it
    * has invalid types, {@link IllegalArgumentException}s will be added to the
    * specified list of errors that contain messages describing the invalid
    * argument.
    * 
    * @param m the method to verify
    * @param errors the list of errors to which to add verification failures
    */
   public static void verifyParameterTypes(Method m, List<Throwable> errors) {
      Class<?> argClasses[] = m.getParameterTypes();
      Type argTypes[] = m.getGenericParameterTypes();
      for (int i = 0, len = argClasses.length; i < len; i++) {
         Type t = argTypes[i];
         if (!providers.containsKey(TypeRef.forType(t))) {
            if (!Processor.class.isAssignableFrom(argClasses[i])) {
               errors.add(badArgumentType(m, i));
            }
         }
      }
   }
   
   private static IllegalArgumentException badArgumentType(Method m, int argIndex) {
      StringBuilder sb = new StringBuilder();
      sb.append("Argument #").append(argIndex + 1).append(" of method ").append(m.getName())
            .append("\nin class ").append(m.getDeclaringClass().getName())
            .append("\nhas unsupported type:")
            .append("\n  ").append(TypeRef.forType(m.getGenericParameterTypes()[argIndex]))
            .append("\nSupported types include:");
      for (TypeRef<?> typeRef : providers.keySet()) {
         sb.append("\n  ").append(typeRef);
      }
      sb.append("\n  ? extends ").append(Processor.class.getName());
      return new IllegalArgumentException(sb.toString());
   }
}