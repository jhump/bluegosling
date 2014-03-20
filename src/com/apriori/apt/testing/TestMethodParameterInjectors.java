package com.apriori.apt.testing;

import com.apriori.collections.TransformingSet;
import com.apriori.reflect.TypeRef;

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
    * @return the exception with a detailed message of the offending argument
    */
   static IllegalArgumentException badArgumentType(Method m, int argIndex,
         Set<TypeRef<?>> supportedTypes) {
      StringBuilder sb = new StringBuilder();
      sb.append("Argument #").append(argIndex + 1).append(" of method ").append(m.getName())
            .append("\nin class ").append(m.getDeclaringClass().getName())
            .append("\nhas unsupported type:")
            .append("\n  ").append(TypeRef.forType(m.getGenericParameterTypes()[argIndex]))
            .append("\nSupported types include:");
      for (TypeRef<?> typeRef : supportedTypes) {
         sb.append("\n  ").append(typeRef);
      }
      // TODO: figure a way to get this into supportedTypes since that's where it belongs...
      sb.append("\n  ? extends ").append(Processor.class.getName());
      return new IllegalArgumentException(sb.toString());
   }

   // For test and after methods:
   
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
   
   static final Map<TypeRef<?>, Provider<?>> providers = new LinkedHashMap<TypeRef<?>, Provider<?>>();
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
    * TODO: doc me!
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
                     ret[i] = provider.getValueFrom(testEnv);
                  } else if (!Processor.class.isAssignableFrom(argClasses[i])) {
                        throw badArgumentType(m, i, providers.keySet());
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
            @Override
            public void validateParameterTypes(Method m, List<Throwable> errors) {
               Class<?> argClasses[] = m.getParameterTypes();
               Type argTypes[] = m.getGenericParameterTypes();
               for (int i = 0, len = argClasses.length; i < len; i++) {
                  Type t = argTypes[i];
                  if (!providers.containsKey(TypeRef.forType(t))) {
                     if (!Processor.class.isAssignableFrom(argClasses[i])) {
                        errors.add(badArgumentType(m, i, providers.keySet()));
                     }
                  }
               }
            }
         };
   
   // For before methods:

   static Set<Class<?>> allowedBeforeClasses =
         new HashSet<Class<?>>(Arrays.<Class<?>> asList(TestJavaFileManager.class, JavaFileManager.class));
      
   static Set<TypeRef<?>> allowedBeforeTypes =
         new TransformingSet<Class<?>, TypeRef<?>>(allowedBeforeClasses,
               (clazz) -> TypeRef.forClass(clazz));
         
   /**
    * TODO: doc me!
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
                     throw badArgumentType(m, i, allowedBeforeTypes);
                  }
               }
               return ret;
            }
            @Override
            public void validateParameterTypes(Method m, List<Throwable> errors) {
               Class<?> argClasses[] = m.getParameterTypes();
               for (int i = 0, len = argClasses.length; i < len; i++) {
                  if (!allowedBeforeClasses.contains(argClasses[i])) {
                     errors.add(badArgumentType(m, i, allowedBeforeTypes));
                  }
               }
            }
         };
}
