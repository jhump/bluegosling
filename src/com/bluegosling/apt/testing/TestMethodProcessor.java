package com.bluegosling.apt.testing;

import com.bluegosling.reflect.Members;

import org.junit.runners.model.FrameworkMethod;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;

/**
 * A {@link Processor} that delegates many operations to a processor under test. The actual
 * {@link #process(Set, RoundEnvironment)} method is what actually invokes a test method. At
 * that point, the processing environment is set up.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AnnotationProcessorTestRunner
 */
class TestMethodProcessor extends AbstractProcessor {
   private static final Logger logger = Logger.getLogger(TestMethodProcessor.class.getName());
   
   private final FrameworkMethod method;
   private final Object test;
   private final TestJavaFileManager fileManager;
   private final CategorizingDiagnosticCollector diagnosticCollector;
   private final Processor processor;
   private int invocationCount;
   
   TestMethodProcessor(FrameworkMethod method, Object test, TestJavaFileManager fileManager,
         CategorizingDiagnosticCollector diagnosticCollector)
         throws NoSuchFieldException, IllegalAccessException, InstantiationException {
      this.method = method;
      this.test = test;
      this.fileManager = fileManager;
      this.diagnosticCollector = diagnosticCollector;
      this.processor = determineProcessorUnderTest(method, test);
   }
   
   /**
    * Returns the {@link Processor} under test. If necessary, this instance may be created. The
    * {@link ProcessorUnderTest @ProcessorUnderTest} and {@link InitializeProcessorField @InitializeProcessorField}
    * annotations are used to determine the processor under test.
    * 
    * <p>If no processor can be determined (i.e. there are no such annotations present), {@code null} is returned.
    * 
    * @param method the method under test
    * @param test the test instance
    * @return the processor for this test or {@code null}
    * @throws NoSuchFieldException if an {@link InitializeProcessorField} annotation refers to a non-existent field
    * @throws IllegalAccessException if the processor class specified by a {@link ProcessorUnderTest} annotation
    *       or its no-argument constructor is inaccessible 
    * @throws InstantiationException if the processor class specified by a {@link ProcessorUnderTest} annotation is
    *       abstract or an interface or if it does not have a no-argument constructor
    * @throws ExceptionInInitializerError if an exception is thrown by the constructor when instantiating the
    *       processor specified by a {@link ProcessorUnderTest} annotation
    * @throws SecurityException if the current security manager denies access to reflectively instantiating the
    *       class specified by a {@link ProcessorUnderTest} annotation or denies the call to
    *       {@link Field#setAccessible(boolean)} prior to accessing the field specified by a
    *       {@link InitializeProcessorField} annotation
    */
   private static Processor determineProcessorUnderTest(FrameworkMethod method, Object test)
         throws NoSuchFieldException, IllegalAccessException, InstantiationException {
      // first look for an annotation on the test method
      ProcessorUnderTest processorUnderTest = method.getMethod().getAnnotation(ProcessorUnderTest.class);
      if (processorUnderTest == null) {
         // failing that, look for an annotation on the test class
         processorUnderTest = test.getClass().getAnnotation(ProcessorUnderTest.class); 
      }
      if (processorUnderTest == null) {
         // no @ProcessorUnderTest annotations? then check for @InitializeProcessorField
         InitializeProcessorField initField = test.getClass().getAnnotation(InitializeProcessorField.class);
         if (initField == null) {
            // no annotations, so no processor under test (test code will have to instantiate and
            // initialize any processors itself)
            return null;
         } else {
            Field field = Members.findField(test.getClass(), initField.value());
            if (field == null) {
               throw new NoSuchFieldException(test.getClass().getName() + "#" + initField.value());
            }
            field.setAccessible(true);
            return (Processor) field.get(test);
         }
      } else {
         return processorUnderTest.value().newInstance();
      }
   }

   /**
    * Finds an annotation on the specified class or on one of its ancestor classes. This
    * is used to provide "inheritance" of an annotation for annotation types that are not
    * actually defined as {@link Inherited}.
    * 
    * @param annotationType the type to look for
    * @param clazz the class
    * @return an annotation on the class or an ancestor class or {@code null} if not found
    */
   private static <T extends Annotation> T findAnnotation(Class<T> annotationType, Class<?> clazz) {
      if (annotationType.isAnnotationPresent(Inherited.class)) {
         return clazz.getAnnotation(annotationType);
      } else {
         // manually search the hierarchy
         for (;clazz != null; clazz = clazz.getSuperclass()) {
            T annotation = clazz.getAnnotation(annotationType);
            if (annotation != null) {
               return annotation;
            }
         }
         return null; // not found
      }
   }
   
   @Override
   public void init(ProcessingEnvironment env) {
      super.init(env);
      if (processor != null) {
         processor.init(env);
      }
   }
   
   @Override
   public Set<String> getSupportedAnnotationTypes() {
      Set<String> ret;
      if (processor != null) {
         ret = processor.getSupportedAnnotationTypes();
      } else {
         SupportedAnnotationTypes annotations = findAnnotation(SupportedAnnotationTypes.class, test.getClass());
         if (annotations == null) {
            ret = Collections.singleton("*");
         } else {
            ret = new HashSet<String>(Arrays.asList(annotations.value()));
         }
      }
      if (ret.isEmpty()) {
         logger.warning("No supported annotations for test. Test method " + method.getName() + " will not get invoked.");
      }
      return ret;
   }

   @Override
   public Set<String> getSupportedOptions() {
      if (processor != null) {
         return processor.getSupportedOptions();
      } else {
         SupportedOptions options = findAnnotation(SupportedOptions.class, test.getClass());
         if (options == null) {
            return Collections.emptySet();
         } else {
            return new HashSet<String>(Arrays.asList(options.value()));
         }
      }
   }

   @Override
   public SourceVersion getSupportedSourceVersion() {
      if (processor != null) {
         return processor.getSupportedSourceVersion();
      } else {
         SupportedSourceVersion version = findAnnotation(SupportedSourceVersion.class, test.getClass());
         return version == null ? super.getSupportedSourceVersion() : version.value();
      }
   }
   
   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      try {
         invocationCount++;
         if (invocationCount > 1 && !method.getMethod().isAnnotationPresent(Reentrant.class)) {
            // don't re-enter non-re-entrant test methods
            return false;
         }
         
         // create test environment
         TestEnvironment testEnv = new TestEnvironment(fileManager, diagnosticCollector, processingEnv,
               roundEnv, invocationCount, annotations, processor, test); 
         // and call method
         Object params[] =
               TestMethodParameterInjectors.FOR_TEST_METHODS.getInjectedParameters(method.getMethod(), testEnv);
         Object result;
         result = method.invokeExplosively(test, params);
         
         // validate outputs
         List<FileDefinition> filesToValidate = FileDefinition.getFilesToValidate(method.getMethod(), test.getClass());
         Set<String> usedPaths = new HashSet<String>();
         for (FileDefinition fileDef : filesToValidate) {
            String path = fileDef.getTargetPath();
            if (!usedPaths.contains(path)) {
               // validate this file
               FileObject fileObject = fileManager.getFileForInput(fileDef.getTargetLocation(), "", fileDef.getFileName());
               String resourceName = fileDef.getResourcePath();
               testEnv.validateGeneratedFile(fileObject, resourceName, true);
            }
         }

         if (result instanceof Boolean) {
            return (Boolean) result;
         }
         return false;
      } catch (Throwable t) {
         throw new ProcessorInvocationException(t);
      }
   }
   
   /**
    * Returns the number of invocations of the {@link #process(Set, RoundEnvironment)} method.
    * This is generally equivalent to the current round number.
    * 
    * @return the number of invocations
    */
   public int getInvocationCount() {
      return invocationCount;
   }
}
